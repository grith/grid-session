package grith.gridsession;

import grisu.jcommons.exceptions.CredentialException;
import grith.jgrith.cred.Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.Map;

import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridSessionCred implements Cred {

	private static GridSessionCred cred;

	static final Logger myLogger = LoggerFactory
			.getLogger(GridSessionCred.class.getName());

	public static synchronized GridSessionCred getExistingSession() {
		if ( cred == null ) {
			try {
				cred = new GridSessionCred();
			} catch (Exception e) {
				myLogger.debug("Can't retieve GridSessionCred: {}",
						e.getLocalizedMessage());
				return null;
			}

		}
		return cred;
	}

	public static synchronized boolean validSessionExists() {

		GridSessionCred c = getExistingSession();
		if (c == null) {
			return false;
		}
		return c.isValid();
	}

	private final SessionClient session;

	public GridSessionCred() {
		this(false);
	}

	public GridSessionCred(boolean initSSL) {
		try {
			this.session = SessionClient.create(initSSL);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CredentialException("Could not create session cred: "
					+ e.getLocalizedMessage(), e);
		}
	}

	public GridSessionCred(Map<PROPERTY, Object> config) {
		this();
		init(config);
	}

	public GridSessionCred(Map<PROPERTY, Object> config, boolean useSSL) {
		this(useSSL);
		init(config);
	}

	public void destroy() {
		session.getSessionManagement().stop();
	}

	public String getDN() {
		return session.getSessionManagement().dn();
	}

	public String getMyProxyHost() {
		return session.getSessionManagement().myproxy_host();
	}

	public char[] getMyProxyPassword() {
		return session.getSessionManagement().myproxy_password().toCharArray();
	}

	public int getMyProxyPort() {
		return session.getSessionManagement().myproxy_port();
	}

	public String getMyProxyUsername() {
		return session.getSessionManagement().myproxy_username();
	}

	public int getRemainingLifetime() {
		return session.getSessionManagement().lifetime();
	}

	public void init(Map<PROPERTY, Object> config) {

		Map<String, Object> configTemp = Maps.newHashMap();

		for (PROPERTY key : config.keySet()) {
			configTemp.put(key.toString(), config.get(key));
		}

		session.getSessionManagement().start(configTemp);

	}

	public boolean isValid() {
		return session.getSessionManagement().is_logged_in();
	}

	public boolean refresh() {
		return session.getSessionManagement().refresh();
	}

	public void setMinimumLifetime(int lifetimeInSeconds) {
		session.getSessionManagement().set_min_lifetime(lifetimeInSeconds);

	}

	public void setMyProxyHost(String myProxyServer) {
		session.getSessionManagement().set_myProxy_host(myProxyServer);

	}

	public void setMyProxyPort(int port) {
		session.getSessionManagement().set_myProxy_port(port);

	}

	public void uploadMyProxy() {
		session.getSessionManagement().upload();
	}

}
