package grith.gridsession;

import grisu.jcommons.exceptions.CredentialException;
import grith.jgrith.cred.Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.Map;

import org.python.google.common.collect.Maps;

public class GridSessionCred implements Cred {

	private final SessionClient session;

	public GridSessionCred() {
		try {
			this.session = SessionClient.create(false);
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
