package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.Cred;
import grith.jgrith.cred.GridLoginParameters;
import grith.jgrith.cred.callbacks.CliCallback;

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
		super();
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

			if (CommonGridProperties.getDefault().useGridSession()) {

				cred = new GridSessionCred(this);
				if (!cred.isValid()) {

					if (!getLoginParameters().validConfig()) {
						myLogger.debug("Trying to retieve remaining login details.");
						getLoginParameters().setCallback(new CliCallback());
						getLoginParameters().populate();
					}

					cred.init(getLoginParameters().getCredProperties());

				}
			} else {
				if (!getLoginParameters().validConfig()) {
					myLogger.debug("Trying to retieve remaining login details.");
					getLoginParameters().setCallback(new CliCallback());
					getLoginParameters().populate();
				}
				cred = AbstractCred.loadFromConfig(getLoginParameters()
						.getCredProperties());
			}
		}
		return cred;
	}


	public GridLoginParameters getLoginParameters() {

		return loginParams;
	}

}
