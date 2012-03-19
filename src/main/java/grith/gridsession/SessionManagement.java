package grith.gridsession;

import grisu.jcommons.utils.OutputHelpers;
import grisu.jcommons.utils.WalltimeUtils;
import grith.jgrith.control.SlcsLoginWrapper;
import grith.jgrith.credential.Credential;
import grith.jgrith.credential.Credential.PROPERTY;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.globus.common.CoGProperties;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManagement implements ISessionManagement,
PropertyChangeListener {

	public static Logger myLogger = LoggerFactory
			.getLogger(SessionManagement.class);

	private static Credential cred = null;

	public static void kickOffIdpPreloading() {

		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					myLogger.debug("Preloading idps...");
					SlcsLoginWrapper.getAllIdps();
				} catch (final Throwable e) {
					myLogger.error(e.getLocalizedMessage(), e);
				}
			}
		};
		t.setDaemon(true);
		t.setName("preloadIdpsThread");

		t.start();

	}

	private static synchronized void setCredential(Credential c) {
		cred = c;
	}



	private final String location;

	public static ISessionServer server;

	public SessionManagement() {
		this(null);
	}

	public SessionManagement(String location) {
		if (StringUtils.isBlank(location)) {
			this.location = CoGProperties.getDefault().getProxyFile();
		} else {
			this.location = location;
		}
		getCredential();
	}

	private synchronized Credential getCredential() {

		if ( (cred == null) || !cred.isValid() ) {
			try {
				cred = Credential.load(location);
			} catch (Exception e) {
				myLogger.debug("Error loading credential: "
						+ e.getLocalizedMessage());
				return null;
			}
		}
		return cred;
	}

	/* (non-Javadoc)
	 * @see grith.jgrith.session.ISessionManagement#isLoggedIn()
	 */
	public Boolean is_logged_in() {

		Credential currentCredential = getCredential();
		if (currentCredential == null) {
			return false;
		}
		return currentCredential.isValid();
	}

	public int lifetime() {

		Credential currentCredential = getCredential();

		if ( currentCredential == null ) {
			return 0;
		}

		return currentCredential.getRemainingLifetime();
	}

	public List<String> list_institutions() {
		try {
			return SlcsLoginWrapper.getAllIdps();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized Boolean login(Map<String, Object> config) {
		return start(config);
	}

	public void logout() {

		stop();

	}

	public String ping() {
		return "ping";
	}

	public void propertyChange(PropertyChangeEvent evt) {

		Object o = evt.getSource();

		if (o instanceof Credential) {
			Credential c = (Credential) o;

			String propName = evt.getPropertyName();
			if ("belowMinLifetime".equals(propName)) {
				try {
					Thread.sleep(5000);
				} catch (Exception e) {
				}
				myLogger.debug("Kicking of auto-refresh of credential because min lifetime reached.");
				int tries = 0;
				while (!refresh() && (tries < 5)) {
					myLogger.debug("Auto-refresh of credential failed. Trying again in a minute.");
					tries = tries + 1;
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
					}
				}
				if (tries >= 5) {
					myLogger.debug("Could not auto-refresh credential.");
				}

			}

		}

	}

	public boolean refresh() {
		Credential currentCredential = getCredential();
		if ((currentCredential == null) || !currentCredential.isAutoRenewable()) {
			return false;
		}

		return currentCredential.autorefresh();
	}

	public boolean set_min_autorefresh(Integer seconds) {
		if ((seconds == null) || (seconds <= 0)) {
			return false;
		}
		Credential currentCredential = getCredential();
		if (currentCredential == null ) {
			return false;
		}
		currentCredential.setMinTimeBetweenAutoRefreshes(seconds);
		return true;
	}

	public boolean set_min_lifetime(Integer seconds) {

		Credential currentCredential = getCredential();
		if (currentCredential == null ) {
			return false;
		}
		currentCredential.setMinimumLifetime(seconds);
		return true;

	}

	public boolean shutdown() {

		System.out.println("Shutting down...");

		if (server != null) {
			new Thread() {
				@Override
				public void run() {
					myLogger.debug("Sleeping 1 sec before shutdown...");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
					server.shutdown();
					myLogger.debug("Server shutdown.");

					System.exit(0);
				}
			}.start();
		}

		return true;
	}

	public synchronized Boolean start(Map<String, Object> config) {

		Credential c = getCredential();

		if (c != null) {
			c.removePropertyChangeListener(this);
		}

		Map<PROPERTY, Object> newMap = Maps.newHashMap();
		for (Object key : config.keySet()) {
			PROPERTY p = PROPERTY.valueOf((String) key);
			newMap.put(p, config.get(key));
		}

		Credential currentCredential = Credential.loadFromConfig(newMap, true);
		currentCredential.saveCredential(location);

		setCredential(currentCredential);

		currentCredential.addPropertyChangeListener(this);

		return true;
	}

	public String status() {

		if (!is_logged_in()) {
			return "Not logged in";
		}
		int remaining = lifetime();
		Credential c = getCredential();

		Map<String, String> temp = Maps.newLinkedHashMap();
		String[] remainingString = WalltimeUtils
				.convertSecondsInHumanReadableString(remaining);
		temp.put("Remaining session lifetime", remainingString[0] + " "
				+ remainingString[1] + " (" + remaining + " seconds)");

		if (c.isAutoRenewable()) {
			// env.printMessage("Session auto-renew: yes");
			temp.put("Session auto-renew", "yes");
			int minlifetime = c.getMinLifetime();
			String[] hrmlifetime = WalltimeUtils
					.convertSecondsInHumanReadableString(minlifetime);
			temp.put("Min. lifetime", hrmlifetime[0] + " " + hrmlifetime[1]
					+ " (" + minlifetime + " seconds)");
		} else {
			temp.put(
					"Session auto-renew",
					"no (to enable, you need to use the 'start' or 'login' command again to renew credential information)");
			// env.printMessage("Session auto-renew: no (to enable, you need to issue the 'renew session' command or delete your proxy and log in again.)");
		}
		temp.put("User ID", c.getDn());

		String output = OutputHelpers.getTable(temp);
		return output;
	}

	public void stop() {

		Credential currentCredential = getCredential();

		if (currentCredential != null) {
			currentCredential.destroy();
		}

		shutdown();

	}

}
