package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grith.jgrith.cred.Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class GridSessionCred implements Cred {

	public static boolean useGridSession = CommonGridProperties.getDefault()
			.useGridSession();


	static final Logger myLogger = LoggerFactory
			.getLogger(GridSessionCred.class.getName());


	private final SessionClient session;

	public GridSessionCred(SessionClient client) {

		if (client == null) {
			throw new RuntimeException("Client can't be null");
		}

		this.session = client;
	}

	public GridSessionCred(SessionClient client, Map<PROPERTY, Object> params) {
		this(client);
		init(params);
	}

	public void destroy() {
		session.getSession().stop();
	}

	public String getDN() {
		return session.getSession().dn();
	}

	public String getMyProxyHost() {
		return session.getSession().myproxy_host();
	}

	public char[] getMyProxyPassword() {
		return session.getSession().myproxy_password().toCharArray();
	}

	public int getMyProxyPort() {
		return session.getSession().myproxy_port();
	}

	public String getMyProxyUsername() {
		return session.getSession().myproxy_username();
	}

	public int getRemainingLifetime() {
		return session.getSession().lifetime();
	}

	public void init(Map<PROPERTY, Object> config) {

		Map<String, Object> configTemp = Maps.newHashMap();

		for (PROPERTY key : config.keySet()) {
			configTemp.put(key.toString(), config.get(key));
		}

		session.getSession().start(configTemp);

	}

	public boolean isRenewable() {
		return session.getSession().is_renewable();
	}

	public boolean isValid() {
		return session.getSession().is_logged_in();
	}

	public boolean refresh() {
		return session.getSession().refresh();
	}

	public void setMinimumLifetime(int lifetimeInSeconds) {
		session.getSession().set_min_lifetime(lifetimeInSeconds);

	}

	public void setMyProxyHost(String myProxyServer) {
		session.getSession().set_myproxy_host(myProxyServer);

	}

	public void setMyProxyPort(int port) {
		session.getSession().set_myproxy_port(port);

	}

	public void uploadMyProxy() {
		session.getSession().upload();
	}

}
