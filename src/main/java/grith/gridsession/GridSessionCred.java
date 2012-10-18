package grith.gridsession;

import grisu.jcommons.constants.Constants;
import grisu.jcommons.exceptions.CredentialException;
import grisu.model.info.dto.VO;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.AbstractCred.PROPERTY;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.ProxyCred;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.globus.common.CoGProperties;
import org.ietf.jgss.GSSCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

public class GridSessionCred implements Cred {

	// public static boolean useGridSession = CommonGridProperties.getDefault()
	// .useGridSession();

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

	private final String tempFilePath = CoGProperties.getDefault().getProxyFile()+"_temp_"+UUID.randomUUID().toString();

	private final File tempFile = new File(tempFilePath);
	
	private AbstractCred cachedCredential = null;
	private Map<String, AbstractCred> cachedGroupCredentials = Maps.newHashMap();


	public GridSessionCred(SessionClient client) {

		tempFile.deleteOnExit();
		
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
	public String saveProxy() {
		return session.getSession().save_proxy();
	}

	@Override
	public String saveProxy(String path) {
		return session.getSession().save_proxy(path);
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
	
	private synchronized AbstractCred getCachedCredential() {
		if ( cachedCredential == null ) {
			// might be that it's already a proxycred, then it doesn't get saved again.ls -l
			String temp = session.getSession().save_proxy(tempFilePath);
			cachedCredential = new ProxyCred(temp);
		}
		return cachedCredential;
	}
	
	private synchronized AbstractCred getCachedGroupCredential(String fqan) {
		if ( cachedGroupCredentials.get(fqan)  == null) {
			String fqanNormailzed = fqan.replace('/', '_');
			String path = tempFilePath+"_"+fqanNormailzed;
			new File(path).deleteOnExit();
			session.getSession().save_group_proxy(fqan, path);
			ProxyCred c = new ProxyCred(path);
			cachedGroupCredentials.put(fqan, c);
		}
		return cachedGroupCredentials.get(fqan);
	}

	@Override
	public GSSCredential getGSSCredential() {

		return getCachedCredential().getGSSCredential();
	}

	@Override
	public String getFqan() {
		// a session credential is always non-vomsified
		return Constants.NON_VO_FQAN;
	}

	@Override
	public Cred getGroupCredential(String fqan) {
		return getCachedGroupCredential(fqan);
	}

	@Override
	public Map<String, VO> getAvailableFqans() {
		return getCachedCredential().getAvailableFqans();
	}

}
