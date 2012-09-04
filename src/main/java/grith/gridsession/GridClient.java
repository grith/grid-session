package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.GridLoginParameters;
import grith.jgrith.cred.ProxyCred;
import grith.jgrith.cred.callbacks.CliCallback;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridClient extends SessionClient {

	public static final Logger myLogger = LoggerFactory
			.getLogger(GridClient.class);



	private final GridLoginParameters loginParams;

	private Cred cred = null;

	public GridClient() throws Exception {
		this(new GridLoginParameters());

	}

	public GridClient(GridLoginParameters loginParams) throws Exception {
		super(loginParams.isLogout(), loginParams.isStartGridSessionDeamon());
		this.loginParams = loginParams;
	}

	/**
	 * This constructor only works if you don't have additional cli parameters.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public GridClient(String[] args) throws Exception {
		this(GridLoginParameters.fillFromCommandlineArgs(args));
	}

	public Cred getCredential() {
		if ((cred == null) || getLoginParameters().isNologin()) {

			if (CommonGridProperties.getDefault().startGridSessionThreadOrDaemon()) {

				cred = new GridSessionCred(this);
				boolean force = getLoginParameters().isForceAuthenticate();

				String mpHost = getLoginParameters().getMyProxyHost();
				if (StringUtils.isNotBlank(mpHost)) {
					cred.setMyProxyHost(mpHost);
				}

				if (force || !cred.isValid()) {

					if (!getLoginParameters().validConfig()) {
						myLogger.debug("Trying to retieve remaining login details.");
						getLoginParameters().setCallback(new CliCallback());
						getLoginParameters().populate();
					}

					cred.init(getLoginParameters().getCredProperties());

				}
			} else {

				try {
					cred = new ProxyCred();
				} catch (Exception e) {
					myLogger.debug("Can't find valid credential, creating new one...");
				}

				if ((cred == null) || !cred.isValid()) {

					if (!getLoginParameters().validConfig()) {
						myLogger.debug("Trying to retieve remaining login details.");
						getLoginParameters().setCallback(new CliCallback());
						getLoginParameters().populate();
					}
					cred = AbstractCred.loadFromConfig(getLoginParameters()
							.getCredProperties());
				}
			}
		}
		return cred;
	}


	public GridLoginParameters getLoginParameters() {

		return loginParams;
	}

}
