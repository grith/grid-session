package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grisu.jcommons.exceptions.CredentialException;
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


	public static GridSessionCred create() {

		SessionClient sc;
		try {
			sc = new SessionClient();
			return new GridSessionCred(sc);
		} catch (Exception e) {
			throw new CredentialException("Can't create session: "
					+ e.getLocalizedMessage());
		}


	}


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

	@Override
	public void destroy() {
		session.getSession().stop();
	}

	@Override
	public String getDN() {
		return session.getSession().dn();
	}

	@Override
	public String getMyProxyHost() {
		return session.getSession().myproxy_host();
	}

	@Override
	public char[] getMyProxyPassword() {
		return session.getSession().myproxy_password().toCharArray();
	}

	@Override
	public int getMyProxyPort() {
		return session.getSession().myproxy_port();
	}

	@Override
	public String getMyProxyUsername() {
		return session.getSession().myproxy_username();
	}

	@Override
	public int getRemainingLifetime() {
		return session.getSession().lifetime();
	}

	@Override
	public void init(Map<PROPERTY, Object> config) {

		Map<String, Object> configTemp = Maps.newHashMap();

		for (PROPERTY key : config.keySet()) {
			configTemp.put(key.toString(), config.get(key));
		}

		session.getSession().start(configTemp);

	}

	@Override
	public boolean isRenewable() {
		return session.getSession().is_renewable();
	}

	@Override
	public boolean isValid() {
		return session.getSession().is_logged_in();
	}

	@Override
	public boolean refresh() {
		return session.getSession().refresh();
	}

	@Override
	public void setMinimumLifetime(int lifetimeInSeconds) {
		session.getSession().set_min_lifetime(lifetimeInSeconds);

	}

	@Override
	public void setMyProxyHost(String myProxyServer) {
		session.getSession().set_myproxy_host(myProxyServer);

	}

	@Override
	public void setMyProxyPort(int port) {
		session.getSession().set_myproxy_port(port);

	}

	@Override
	public void uploadMyProxy() {
		session.getSession().upload();
	}

}
