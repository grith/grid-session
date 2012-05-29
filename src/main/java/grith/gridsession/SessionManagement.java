package grith.gridsession;

import grisu.jcommons.utils.OutputHelpers;
import grisu.jcommons.utils.WalltimeUtils;
import grith.jgrith.control.SlcsLoginWrapper;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.Cred;
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

	private static AbstractCred cred = null;

	private static synchronized AbstractCred getCredential() {


		if ( (cred == null) || !cred.isValid() ) {
			myLogger.debug("No valid credential.");
			cred = null;
		}
		// try {
		// cred = Credential.load(location);
		// } catch (Exception e) {
		// myLogger.debug("Error loading credential: "
		// + e.getLocalizedMessage());
		// return null;
		// }
		// }
		return cred;
	}

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

	public void destroy() {

		myLogger.debug("Uploading credential");
		Cred c = getCredential();
		if (c == null) {
			return;
		}
		c.destroy();

	}

	public String dn() {
		Cred c = getCredential();
		if (c == null) {
			return null;
		}
		return c.getDN();
	}

	public String group_proxy_path(String group) {
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}

		String path = c.getGroupProxyPath(group);
		if (StringUtils.isBlank(path)) {
			c.saveGroupProxy(group);
		}

		return c.getGroupProxyPath(group);

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see grith.jgrith.session.ISessionManagement#isLoggedIn()
	 */
	public Boolean is_logged_in() {

		Cred currentCredential = getCredential();
		if (currentCredential == null) {
			return false;
		}
		return currentCredential.isValid();
	}

	public int lifetime() {

		Cred currentCredential = getCredential();

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

		Cred currentCredential = getCredential();

		if (currentCredential != null) {
			currentCredential.destroy();
		}
		cred = null;

	}

	public String myproxy_host() {

		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		c.uploadMyProxy(false);
		return c.getMyProxyHost();

	}

	public String myproxy_password() {
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		c.uploadMyProxy(false);
		return new String(c.getMyProxyPassword());
	}

	public int myproxy_port() {
		myLogger.debug("myproxy port");
		Cred c = getCredential();
		if (c == null) {
			return -1;
		}

		return c.getMyProxyPort();
	}

	public String myproxy_username() {
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		c.uploadMyProxy(false);
		return c.getMyProxyUsername();
	}

	// public boolean set_min_autorefresh(Integer seconds) {
	// if ((seconds == null) || (seconds <= 0)) {
	// return false;
	// }
	// AbstractCred currentCredential = getCredential();
	// if (currentCredential == null ) {
	// return false;
	// }
	// currentCredential.setMinTimeBetweenAutoRefreshes(seconds);
	// return true;
	// }

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
				while (!c.autorefresh() && (tries < 25)) {
					myLogger.debug("Auto-refresh of credential failed. Trying again in a minute.");
					tries = tries + 1;
					try {
						Thread.sleep(600000);
					} catch (InterruptedException e) {
					}
				}
				if (tries >= 5) {
					myLogger.debug("Could not auto-refresh credential.");
				} else {
					myLogger.debug("Credential auto-refresh successful, new lifetime: "
							+ c.getRemainingLifetime());
				}

			}

		}

	}

	public String proxy_path() {
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		if (StringUtils.isBlank(c.getProxyPath())) {
			c.saveProxy();
		}
		return c.getProxyPath();
	}

	public boolean refresh() {
		AbstractCred currentCredential = getCredential();

		return currentCredential.refresh();
	}

	public boolean set_min_lifetime(Integer seconds) {

		AbstractCred currentCredential = getCredential();
		if (currentCredential == null ) {
			return false;
		}
		currentCredential.setMinimumLifetime(seconds);
		return true;

	}

	public void set_myProxy_host(String myProxyServer) {
		myLogger.debug("Setting myproxy host");
		AbstractCred c = getCredential();
		if (c == null) {
			return;
		}

		c.setMyProxyHost(myProxyServer);

	}

	public void set_myProxy_port(int port) {

		myLogger.debug("Setting myproxy host");
		AbstractCred c = getCredential();
		if (c == null) {
			return;
		}

		c.setMyProxyPort(port);
	}

	private synchronized void setCredential(AbstractCred c) {
		cred = c;
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

		AbstractCred c = getCredential();

		if (c != null) {
			c.removePropertyChangeListener(this);
		}

		Map<PROPERTY, Object> newMap = Maps.newHashMap();
		for (Object key : config.keySet()) {
			PROPERTY p = PROPERTY.valueOf((String) key);
			newMap.put(p, config.get(key));
		}

		AbstractCred currentCredential = AbstractCred.loadFromConfig(newMap);

		setCredential(currentCredential);

		currentCredential.addPropertyChangeListener(this);

		return true;
	}

	public String status() {

		if (!is_logged_in()) {
			return "Not logged in";
		}
		int remaining = lifetime();
		AbstractCred c = getCredential();

		Map<String, String> temp = Maps.newLinkedHashMap();
		String[] remainingString = WalltimeUtils
				.convertSecondsInHumanReadableString(remaining);
		temp.put("Remaining session lifetime", remainingString[0] + " "
				+ remainingString[1] + " (" + remaining + " seconds)");

		int minlifetime = c.getMinimumLifetime();
		String[] hrmlifetime = WalltimeUtils
				.convertSecondsInHumanReadableString(minlifetime);
		temp.put("Min. lifetime before renew", hrmlifetime[0] + " "
				+ hrmlifetime[1]
						+ " (" + minlifetime + " seconds)");

		temp.put("User ID", c.getDN());

		String output = OutputHelpers.getTable(temp);
		return output;
	}

	public void stop() {

		Cred currentCredential = getCredential();

		if (currentCredential != null) {
			currentCredential.destroy();
		}

		shutdown();

	}

	public boolean upload() {

		myLogger.debug("Uploading credential");
		Cred c = getCredential();
		if (c == null) {
			return false;
		}
		try {
			c.uploadMyProxy();
			return true;
		} catch (Exception e) {
			myLogger.error("Can't upload to myproxy: {}", e);
			return false;
		}

	}

	public boolean upload(String myproxyhost) {

		myLogger.debug("Uploading credential to " + myproxyhost);
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}
		c.setMyProxyHost(myproxyhost);
		try {
			c.uploadMyProxy(true);
			return true;
		} catch (Exception e) {
			myLogger.error("Can't upload to myproxy: {}", e);
			return false;
		}
	}

}
