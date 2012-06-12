package grith.gridsession;

import grisu.jcommons.constants.Enums.LoginType;
import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.utils.OutputHelpers;
import grisu.jcommons.utils.WalltimeUtils;
import grith.jgrith.control.SlcsLoginWrapper;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.MyProxyCred;
import grith.jgrith.cred.SLCSCred;
import grith.jgrith.cred.X509Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionManagement implements ISessionManagement,
PropertyChangeListener {

	public static Logger myLogger = LoggerFactory
			.getLogger(SessionManagement.class);

	private static AbstractCred cred = null;

	public static ISessionServer server;

	private static AbstractCred getCredential() {


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



	public SessionManagement() {
	}

	// public SessionManagement(String location) {
	//
	// if (StringUtils.isBlank(location)) {
	// this.location = CoGProperties.getDefault().getProxyFile();
	// } else {
	// this.location = location;
	// }
	// getCredential();
	// }

	public String credential_type() {

		Cred c = getCredential();
		if (c == null) {
			return null;
		}


		if (c instanceof SLCSCred) {
			return LoginType.SHIBBOLETH.toString();
		} else if (c instanceof MyProxyCred) {
			return LoginType.MYPROXY.toString();
		} else if (c instanceof X509Cred) {
			return LoginType.X509_CERTIFICATE.toString();
		} else {
			throw new CredentialException("Unknown credential type: "
					+ c.getClass().getSimpleName());
		}

	}

	public void destroy() {

		myLogger.debug("Destroyting credential");
		// Cred c = getCredential();
		// if (c == null) {
		// return;
		// }
		// c.destroy();
		stop();

	}

	public String dn() {
		myLogger.debug("Getting DN");
		Cred c = getCredential();
		if (c == null) {
			return null;
		}
		return c.getDN();
	}

	public String group_proxy_path(String group) {
		myLogger.debug("Group proxy path for: {}", group);
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
		myLogger.debug("Is logged in?");
		Cred currentCredential = getCredential();
		if (currentCredential == null) {
			return false;
		}
		return currentCredential.isValid();
	}

	public int lifetime() {

		myLogger.debug("Remaining lifetime...");
		Cred currentCredential = getCredential();

		if ( currentCredential == null ) {
			return 0;
		}

		return currentCredential.getRemainingLifetime();
	}

	public List<String> list_institutions() {
		myLogger.debug("List institutions...");
		try {
			return SlcsLoginWrapper.getAllIdps();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public Boolean login(Map<String, Object> config) {
		return start(config);
	}

	public void logout() {
		myLogger.debug("Logging out...");
		Cred currentCredential = getCredential();

		if (currentCredential != null) {
			currentCredential.destroy();
		}
		cred = null;

	}

	public String myproxy_host() {
		myLogger.debug("MyProxy Host...");
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		c.uploadMyProxy(false);
		return c.getMyProxyHost();

	}

	public String myproxy_password() {
		myLogger.debug("MyProxy password...");
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		c.uploadMyProxy(false);
		return new String(c.getMyProxyPassword());
	}

	public int myproxy_port() {
		myLogger.debug("MyProxy port...");
		myLogger.debug("myproxy port");
		Cred c = getCredential();
		if (c == null) {
			return -1;
		}

		return c.getMyProxyPort();
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

	public String myproxy_username() {
		myLogger.debug("MyProxy Username...");
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		c.uploadMyProxy(false);
		return c.getMyProxyUsername();
	}

	public String ping() {
		myLogger.debug("Ping...");
		return "ping";
	}


	public void propertyChange(PropertyChangeEvent evt) {


		myLogger.debug("EVENT: "+evt.getNewValue().toString());
	}

	public String proxy_path() {
		myLogger.debug("Proxy path...");
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
		myLogger.debug("Refreshing...");
		AbstractCred currentCredential = getCredential();

		return currentCredential.refresh();
	}

	public boolean set_min_lifetime(Integer seconds) {
		myLogger.debug("setting min lifetime to {} seconds", seconds);
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

		myLogger.debug("Shutting down...");

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
		myLogger.debug("Logging in...");
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

		myLogger.debug("getting status...");
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

		myLogger.debug("Stopping daemon...");

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
