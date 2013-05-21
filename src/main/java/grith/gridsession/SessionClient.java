package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grisu.jcommons.configuration.CommonGridProperties.Property;
import grisu.jcommons.dependencies.BouncyCastleTool;
import grisu.jcommons.utils.DefaultGridSecurityProvider;
import grisu.jcommons.utils.EnvironmentVariableHelpers;
import grisu.jcommons.utils.JythonHelpers;
import grith.jgrith.utils.CertificateFiles;
import grith.jgrith.utils.VomsesFiles;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcCommonsTransportFactory;
import org.apache.xmlrpc.client.util.ClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;

public class SessionClient {

	public static final Logger myLogger = LoggerFactory
			.getLogger(SessionClient.class);

	private static final int connect_retries = 2;

	private ISessionManagement sm;

	private BigInteger acceptedCertSerial;

	private boolean useSSL = false;

	private boolean useLocalTransport = false;

	protected final boolean logout;


	private boolean clientStarted = false;

	public SessionClient() throws Exception {
		this(false, false);
	}


	public SessionClient(boolean logout, boolean startSession) throws Exception {
		if (logout) {
			myLogger.debug("Creating SessionClient for logout");
		} else {
			myLogger.debug("Creating SessionClient");
		}
		this.logout = logout;

		CommonGridProperties.getDefault().setGridProperty(
				Property.USE_GRID_SESSION, Boolean.toString(startSession));
		CommonGridProperties.getDefault()
				.setGridProperty(
				Property.DAEMONIZE_GRID_SESSION, Boolean.toString(startSession));


		EnvironmentVariableHelpers.loadEnvironmentVariablesToSystemProperties();

		CertificateFiles.copyCACerts(false);
//		VomsesFiles.copyVomses(VomsesFiles.DEFAULT_VOS);
		BouncyCastleTool.initBouncyCastle();

		if (useSSL) {
			initSSL();
		} else {
			java.security.Security
			.addProvider(new DefaultGridSecurityProvider());

			java.security.Security
			.setProperty("ssl.TrustManagerFactory.algorithm",
					"TrustAllCertificates");
		}

		execute();

	}

	protected final void execute() {

		boolean alreadyRunning = false;
		// check whether server already running
		try {
			myLogger.debug("Checking whether grid-session server is running...");
			ServerSocket s = new ServerSocket(RpcPort.getUserPort());
			s.close();
			myLogger.debug("No grid session server running.");
		} catch (BindException be) {
			// that's good, means something already running, we don't
			// need to try to daemonize...
			myLogger.debug("Server already running...");
			alreadyRunning = true;
		} catch (Exception e) {
			myLogger.error("Error trying to connect to grid-session daemon.", e);
		}


		if (alreadyRunning
				|| CommonGridProperties.getDefault()
				.startGridSessionThreadOrDaemon()) {
			// check whether we run on windows, if that is the case, we can't
			// daemonize...
			String currentOs = System.getProperty("os.name").toUpperCase();

			boolean daemonize = CommonGridProperties.getDefault()
					.daemonizeGridSession();



			if (!(currentOs.contains("WINDOWS") || currentOs.contains("MAC"))
					&& daemonize && !alreadyRunning) {
				Daemon d = new Daemon();



				if (!d.isDaemonized() || !logout) {
					try {
						startDaemon(d);
					} catch (RuntimeException e) {
						myLogger.error("Can't start daemon: " + e, e);
						myLogger.error("Starting session within this process...");
						try {
							GridSessionDaemon daemon = new GridSessionDaemon();

						} catch (Exception e2) {
							myLogger.error("Error starting daemon", e2);
						}
					}
				}

			} else if (!alreadyRunning) {
				try {
					myLogger.debug("Starting session service...");
					GridSessionDaemon daemon = new GridSessionDaemon();

				} catch (Exception e) {
					myLogger.error("Error starting session service.", e);
				}
			}



		}

		Thread.currentThread().setName("main");
		JythonHelpers.setJythonCachedir();

		try {
			if (alreadyRunning) {
				startClient(true);
			} else {
				startClient(CommonGridProperties.getDefault()
						.startGridSessionThreadOrDaemon());
			}
		} catch (Exception e) {
			myLogger.debug("Error starting client.",e);
			throw new RuntimeException(e);
		}


	}

	public ISessionManagement getSession() {
		return sm;
	}

	private void initSSL() throws Exception {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain,
					String authType) {

				for (X509Certificate element : chain) {

					BigInteger serial = element.getSerialNumber();
					if (!serial.equals(acceptedCertSerial)) {
						throw new RuntimeException(
								"Cert serial numbers don't match.");
					}
				}
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {

				return null;
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("TLS");
		// Create empty HostnameVerifier
		HostnameVerifier hv = new HostnameVerifier() {
			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}
		};

		sc.init(new KeyManager[0], trustAllCerts,
				new java.security.SecureRandom());

		SSLContext.setDefault(sc);

	}

	public boolean isUseLocalTransport() {
		return useLocalTransport;
	}

	public boolean isUseSSL() {
		return useSSL;
	}


	public void setUseLocalTransport(boolean useLocalTransport) {
		this.useLocalTransport = useLocalTransport;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}
	
	protected void logout() {
		// can be overwritten
	}

	protected synchronized void startClient(boolean gridSessionServerRunning)
			throws Exception {

		String ping = null;

		if (!gridSessionServerRunning) {
			if (logout) {
				myLogger.debug("Logging out from non-grid-session client...");
				logout();
				System.exit(0);
			}
			myLogger.debug("Starting non-RPC grid session...");
			sm = new SessionManagement();
			return;
		}
		if (!clientStarted) {
			myLogger.debug("Starting RPC grid session..");
			int tries = 0;
			// create configuration
			RpcAuthToken auth = new RpcAuthToken();
			acceptedCertSerial = BigInteger.valueOf(
					auth.getAuthToken().hashCode()).abs();

			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			RpcPort port = new RpcPort();
			config.setServerURL(new URL("https://127.0.0.1:" + port.getPort()
					+ "/xmlrpc"));
			config.setEnabledForExtensions(true);
			config.setConnectionTimeout(80 * 1000);
			config.setReplyTimeout(60 * 1000);
			config.setBasicUserName(System.getProperty("user.name"));
			RpcAuthToken t = new RpcAuthToken();
			String token = t.getAuthToken();
			config.setBasicPassword(token);

			while (tries < connect_retries) {
				try {


					XmlRpcClient client = new XmlRpcClient();

					// use Commons HttpClient as transport
					client.setTransportFactory(new XmlRpcCommonsTransportFactory(
							client));
					// set configuration
					client.setConfig(config);

					// make a call using dynamic proxy
					ClientFactory factory = new ClientFactory(client);
					sm = (ISessionManagement) factory
							.newInstance(ISessionManagement.class);

					// check whether we can actually connect to grid-session
					// service...

					myLogger.debug("Pinging server...");
					ping = getSession().ping();
					myLogger.debug("Ping response: " + ping);
					;

					myLogger.debug("Checking version...");
					try {
						int version = getSession().version();
						if ( version > ISessionManagement.API_VERSION ) {
							System.out
							.println("New version of grid-session service detected. Will continue to run this client, in case of errors please update it.");
							myLogger.debug("New version of grid-session service detected. Will continue to run this client, in case of errors please update it.");
						} else if (version < ISessionManagement.API_VERSION) {
							getSession().stop();

							myLogger.error("Old version of grid-session service detected and stopped. Please restart this application to start an updated version.");
							System.out
							.println("Old version of grid-session service detected and stopped. Please restart this application to start an updated version.");
							System.exit(1);
						} else {
							myLogger.debug("Same version of client and server. Good...");
						}
					} catch (Exception e) {
						myLogger.debug("Version method could not be executed. Probably means old client...");

						getSession().stop();

						myLogger.error("Old version of grid-session service detected and stopped. Please restart this application to start an updated version.");
						System.out
						.println("Old version of grid-session service detected and stopped. Please restart this application to start an updated version.");
						System.exit(1);

					}

					break;

				} catch (UndeclaredThrowableException e) {
					myLogger.debug("Could not execute command, trying again.",
							e);
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
					}
					tries = tries + 1;
				} catch (Exception e) {
					myLogger.error("Could not execute command: {}",
							e.getLocalizedMessage());
					throw new RuntimeException("Can't create session-client.", e);
				}
			}

		}
		if (StringUtils.isBlank(ping)) {
			myLogger.error("Session client can't be reached, disabling use of grid session.");
			System.setProperty(
					CommonGridProperties.Property.USE_GRID_SESSION.toString(),
					"false");

		}

		clientStarted = true;

		myLogger.debug("Started grid-session client");

		if (logout) {
			myLogger.debug("Logging out...");
			if (sm != null) {
				sm.destroy();
			}
			System.exit(0);
		}
	}

	protected void startDaemon(Daemon d) {

		String memMin = "-Xms32m";
		String memMax = "-Xmx32m";

		JavaVMArguments args;
		try {
			args = JavaVMArguments.current();
		} catch (IOException e) {
			myLogger.debug("Error starting client.", e);
			throw new RuntimeException(e);
		}
		boolean jar = false;
		String classpath = null;

		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);

			if (arg.trim().equals("-jar")) {
				jar = true;
				classpath = args.get(i + 1);
				continue;
			}

			if (arg.equals("-cp") || arg.equals("-classpath")) {
				classpath = args.get(i + 1);
				continue;
			}

		}

		if (StringUtils.isBlank(classpath)) {
			throw new RuntimeException(
					"Can't create daemon startup arguments...");
		}

		String proxyLocation = System.getProperty("X509_USER_PROXY");
		myLogger.debug("Proxy location: " + proxyLocation);
		args.clear();
		args.add("java");
		if (StringUtils.isNotBlank(proxyLocation)) {
			args.add("-DX509_USER_PROXY=" + proxyLocation);
		}
		args.add(memMin);
		args.add(memMax);
		args.add("-cp");
		args.add(classpath);
		args.add("grith.gridsession.GridSessionDaemon");


		myLogger.debug("Daemonizing using args: {}",
				StringUtils.join(args, ", "));

		d.daemonize(args);

		// need to restart client now.
		clientStarted = false;
		myLogger.debug("Started daemon. Sleeping for a bit...");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			myLogger.error("Interrupted daemon startup...", e);
		}

		return;
	}

	// private void startServer() {
	//
	// try {
	//
	// File file = new File("/tmp/jna");
	// file.mkdirs();
	//
	// file.setWritable(true, false);
	//
	// } catch (Exception e) {
	// myLogger.error("Can't create dir or change permissions for /tmp/jna: "
	// + e.getLocalizedMessage());
	// }
	//
	// try {
	// LoggerContext context = (LoggerContext) LoggerFactory
	// .getILoggerFactory();
	// JoranConfigurator configurator = new JoranConfigurator();
	// configurator.setContext(context);
	//
	// InputStream config = SessionClient.class
	// .getResourceAsStream("/logback_server.xml");
	//
	// context.reset();
	// configurator.doConfigure(config);
	//
	// StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	// } catch (Exception e) {
	// myLogger.error("Error when trying to configure grid-session service logging");
	//
	// }
	//
	// myLogger.debug("Starting grid-session service...");
	//
	// try {
	// BouncyCastleTool.initBouncyCastle();
	// GridSessionDaemon server = new GridSessionDaemon();
	// myLogger.debug("Server started...");
	// } catch (Throwable e) {
	// myLogger.error(
	// "Error starting grid-session service: "
	// + e.getLocalizedMessage(), e);
	// System.exit(1);
	// }
	// }

}
