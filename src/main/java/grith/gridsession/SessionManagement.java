package grith.gridsession;

import grisu.jcommons.constants.Constants;
import grisu.jcommons.constants.Enums.LoginType;
import grisu.jcommons.exceptions.CredentialException;
import grisu.jcommons.utils.OutputHelpers;
import grisu.jcommons.utils.WalltimeUtils;
import grith.jgrith.control.SlcsLoginWrapper;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.AbstractCred.PROPERTY;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.MyProxyCred;
import grith.jgrith.cred.ProxyCred;
import grith.jgrith.cred.SLCSCred;
import grith.jgrith.cred.X509Cred;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;

public class SessionManagement implements ISessionManagement,
PropertyChangeListener {

	public static Logger myLogger = LoggerFactory
			.getLogger(SessionManagement.class);

	private static AbstractCred cred = null;

	public static ISessionServer server;

	private static AbstractCred getCredential() {

		if ( (cred == null) || !cred.isValid() ) {

			ProxyCred pc = null;
			try {
				pc = new ProxyCred();
				if (pc.isValid()) {
					cred = pc;
					myLogger.debug("Loaded local credential");
				} else {
					myLogger.debug("No valid credential.");
					cred = null;
				}
			} catch (CredentialException e) {
				cred = null;
			}



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


	@Override
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

	@Override
	public void destroy() {

		myLogger.debug("Destroyting credential");
		// Cred c = getCredential();
		// if (c == null) {
		// return;
		// }
		// c.destroy();
		logout();
		stop();

	}

	@Override
	public String dn() {
		myLogger.debug("Getting DN");
		Cred c = getCredential();
		if (c == null) {
			return null;
		}
		return c.getDN();
	}

	private AbstractCred getGroupCredential(String group) {

		AbstractCred c = getCredential();
		if (c == null) {
			throw new CredentialException("Not logged in.");
		}

		AbstractCred gc = c.getGroupCredential(group);
		if ( gc == null ) {
			throw new CredentialException("Not member of "+group);
		}

		return gc;

	}

	@Override
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

	@Override
	public List<String> groups() {

		AbstractCred c = getCredential();
		if (c == null) {
			return Lists.newArrayList();
		}

		return Lists.newArrayList(c.getGroups());

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see grith.jgrith.session.ISessionManagement#isLoggedIn()
	 */
	@Override
	public Boolean is_logged_in() {
		myLogger.debug("Is logged in?");
		Cred currentCredential = getCredential();
		if (currentCredential == null) {
			return false;
		}
		return currentCredential.isValid();
	}

	@Override
	public boolean is_renewable() {
		myLogger.debug("Checking renewability...");
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}

		return c.isRenewable();
	}

	@Override
	public Boolean is_uploaded() {
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}
		return c.isUploaded();
	}

	@Override
	public Boolean is_uploaded(String group) {
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}
		AbstractCred gc = getGroupCredential(group);
		return gc.isUploaded();
	}

	@Override
	public int lifetime() {

		myLogger.debug("Remaining lifetime...");
		Cred currentCredential = getCredential();

		if ( currentCredential == null ) {
			return 0;
		}

		return currentCredential.getRemainingLifetime();
	}

	@Override
	public List<String> list_institutions() {
		myLogger.debug("List institutions...");
		try {
			return SlcsLoginWrapper.getAllIdps();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
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

	@Override
	public Boolean login(Map<String, Object> config) {
		return start(config);
	}

	@Override
	public void logout() {
		myLogger.debug("Logging out...");
		Cred currentCredential = getCredential();

		if (currentCredential != null) {
			currentCredential.destroy();
		}
		cred = null;

	}


	@Override
	public String myproxy_host() {
		myLogger.debug("MyProxy Host...");
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		// c.uploadMyProxy(false);
		return c.getMyProxyHost();

	}

	@Override
	public String myproxy_password() {
		myLogger.debug("MyProxy password...");
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		// c.uploadMyProxy(false);
		return new String(c.getMyProxyPassword());
	}

	@Override
	public String myproxy_password(String group) {
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		AbstractCred gc = getCredential().getGroupCredential(group);
		return new String(gc.getMyProxyPassword());
	}

	@Override
	public int myproxy_port() {
		myLogger.debug("MyProxy port...");
		myLogger.debug("myproxy port");
		Cred c = getCredential();
		if (c == null) {
			return -1;
		}

		return c.getMyProxyPort();
	}

	@Override
	public String myproxy_username() {
		myLogger.debug("MyProxy Username...");
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		// c.uploadMyProxy(false);
		return c.getMyProxyUsername();
	}

	@Override
	public String myproxy_username(String group) {
		AbstractCred c = getCredential();
		if (c == null) {
			return null;
		}
		AbstractCred gc = getCredential().getGroupCredential(group);
		return gc.getMyProxyUsername();
	}

	@Override
	public String ping() {
		myLogger.debug("Ping...");
		return "ping";
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {


		myLogger.debug("EVENT: "+evt.getNewValue().toString());
	}

	@Override
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

	@Override
	public boolean refresh() {
		myLogger.debug("Refreshing...");
		AbstractCred currentCredential = getCredential();

		return currentCredential.refresh();
	}

	public String save_proxy() {
		return save_proxy(null);
	}

	public String save_proxy(String path) {

		myLogger.debug("Saving credential to "+path);
		AbstractCred c = getCredential();
		if (c == null) {
			return "";
		}

		try {
			c.saveProxy(path);
			return c.getProxyPath();
		} catch (Exception e) {
			myLogger.error("Can't upload to myproxy: {}", e);
			return "";
		}
	}
	
	public String save_group_proxy(String fqan, String path) {

		myLogger.debug("Saving group credential for group {} to {}", fqan, path);
		AbstractCred c = getCredential().getGroupCredential(fqan);
		if (c == null) {
			return "";
		}

		try {
			c.saveProxy(path);
			return c.getProxyPath();
		} catch (Exception e) {
			myLogger.error("Can't upload to myproxy: {}", e);
			return "";
		}
	}

	@Override
	public boolean set_min_lifetime(Integer seconds) {
		myLogger.debug("setting min lifetime to {} seconds", seconds);
		AbstractCred currentCredential = getCredential();
		if (currentCredential == null ) {
			return false;
		}
		currentCredential.setMinimumLifetime(seconds);
		return true;

	}

	@Override
	public void set_myproxy_host(String myProxyServer) {
		myLogger.debug("Setting myproxy host");
		AbstractCred c = getCredential();
		if (c == null) {
			return;
		}

		c.setMyProxyHost(myProxyServer);

	}


	@Override
	public void set_myproxy_password(char[] pw) {
		myLogger.debug("Setting myproxy password");
		AbstractCred c = getCredential();
		if (c == null) {
			return;
		}

		c.setMyProxyPassword(pw);

	}

	@Override
	public void set_myproxy_port(int port) {

		myLogger.debug("Setting myproxy host");
		AbstractCred c = getCredential();
		if (c == null) {
			return;
		}

		c.setMyProxyPort(port);
	}

	@Override
	public void set_myproxy_username(String un) {
		myLogger.debug("Setting myproxy username to: " + un);
		AbstractCred c = getCredential();
		if (c == null) {
			return;
		}

		c.setMyProxyUsername(un);

	}

	private synchronized void setCredential(AbstractCred c) {
		cred = c;
	}

	@Override
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

	@Override
	public synchronized Boolean start(Map<String, Object> config) {
		myLogger.debug("Logging in...");
		AbstractCred c = cred;

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

		String proxyPath = proxy_path();

		myLogger.debug("Saved proxy to: " + proxyPath);

		return true;
	}

	@Override
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

		boolean isRenewable = c.isRenewable();
		if (isRenewable) {

			temp.put("Renewable", "Yes");

			int minlifetime = c.getMinimumLifetime();

			String[] hrmlifetime = WalltimeUtils
					.convertSecondsInHumanReadableString(minlifetime);
			temp.put("Min. lifetime before renew", hrmlifetime[0] + " "
					+ hrmlifetime[1]
							+ " (" + minlifetime + " seconds)");
		} else {
			temp.put("Renewable", "No");
		}

		temp.put("User ID", c.getDN());


		String output = OutputHelpers.getTable(temp);
		return output;
	}

	@Override
	public void stop() {

		myLogger.debug("Stopping daemon...");

		// Cred currentCredential = getCredential();
		//
		// if (currentCredential != null) {
		// currentCredential.destroy();
		// }

		shutdown();

	}

	@Override
	public boolean upload() {

		myLogger.debug("Uploading credential");
		AbstractCred c = getCredential();
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

	@Override
	public boolean upload(String group) {
		myLogger.debug("Uploading credential for group " + group);
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}

		if (StringUtils.isBlank(group) || Constants.NON_VO_FQAN.equals(group)) {
			try {
				c.uploadMyProxy(false);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		} else {
			AbstractCred gc = getGroupCredential(group);
			try {
				gc.uploadMyProxy(false);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		}
	}

	@Override
	public boolean upload(String group, String myproxyhost) {

		myLogger.debug("Uploading credential to " + myproxyhost);
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}
		if (StringUtils.isBlank(group) || Constants.NON_VO_FQAN.equals(group)) {
			c.setMyProxyHost(myproxyhost);
			try {
				c.uploadMyProxy(true);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		} else {
			AbstractCred gc = getGroupCredential(group);
			gc.setMyProxyHost(myproxyhost);
			try {
				gc.uploadMyProxy(true);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		}
	}

	@Override
	public boolean upload(String group, String myproxyhost,
			String myproxyusername) {

		myLogger.debug("Uploading credential to " + myproxyhost);
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}
		if (StringUtils.isBlank(group) || Constants.NON_VO_FQAN.equals(group)) {
			try {
				c.setMyProxyHost(myproxyhost);
				c.setMyProxyUsername(myproxyusername);
				c.setMyProxyPassword(null);
				c.uploadMyProxy(true);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		} else {
			AbstractCred gc = getGroupCredential(group);

			try {
				gc.setMyProxyHost(myproxyhost);
				gc.setMyProxyUsername(myproxyusername);
				gc.setMyProxyPassword(null);
				gc.uploadMyProxy(true);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		}
	}

	@Override
	public boolean upload(String group, String myproxyhost,
			String myproxyusername, char[] myproxypassword) {

		myLogger.debug("Uploading credential to " + myproxyhost);
		AbstractCred c = getCredential();
		if (c == null) {
			return false;
		}

		if (StringUtils.isBlank(group) || Constants.NON_VO_FQAN.equals(group)) {
			try {
				c.setMyProxyHost(myproxyhost);
				c.setMyProxyUsername(myproxyusername);
				c.setMyProxyPassword(myproxypassword);
				c.uploadMyProxy(true);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		} else {
			AbstractCred gc = getGroupCredential(group);

			try {
				gc.setMyProxyHost(myproxyhost);
				gc.setMyProxyUsername(myproxyusername);
				gc.setMyProxyPassword(myproxypassword);
				gc.uploadMyProxy(true);
				return true;
			} catch (Exception e) {
				myLogger.error("Can't upload to myproxy: {}", e);
				return false;
			}
		}

	}

	@Override
	public int version() {
		return ISessionManagement.API_VERSION;
	}

}
