package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grisu.jcommons.dependencies.BouncyCastleTool;
import grisu.jcommons.utils.DefaultGridSecurityProvider;
import grisu.jcommons.utils.EnvironmentVariableHelpers;
import grisu.jcommons.utils.JythonHelpers;

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

	private final boolean logout;


	private boolean clientStarted = false;

	protected SessionClient() throws Exception {
		this(false);
	}

	protected SessionClient(boolean logout) throws Exception {
		if (logout) {
			myLogger.debug("Creating SessionClient for logout");
		} else {
			myLogger.debug("Creating SessionClient");
		}
		this.logout = logout;
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

		if (CommonGridProperties.getDefault().useGridSession()) {
			// check whether we run on windows, if that is the case, we can't
			// daemonize...
			String currentOs = System.getProperty("os.name").toUpperCase();

			if (!currentOs.contains("WINDOWS")) {
				Daemon d = new Daemon();

				boolean alreadyRunning = false;
				// check whether server already running
				try {
					ServerSocket s = new ServerSocket(RpcPort.getUserPort());
					s.close();
				} catch (BindException be) {
					// that's good, means something already running, we don't
					// need to try to daemonize...
					myLogger.debug("Server already running...");
					alreadyRunning = true;
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (!alreadyRunning) {
					if (!d.isDaemonized() || !logout) {
						startDaemon(d);
						myLogger.debug("Started daemon. Sleeping for a bit...");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						// } else {
						// try {
						// d.init();
						// } catch (Exception e) {
						// e.printStackTrace();
						// throw new RuntimeException(e);
						// }
						// myLogger.debug("Starting server...");
						// // startServer();
						// // try {
						// // Thread.sleep(10000);
						// // } catch (InterruptedException e) {
						// // // TODO Auto-generated catch block
						// // e.printStackTrace();
						// // }
						// return;
					}
				}

			}

		}

		EnvironmentVariableHelpers.loadEnvironmentVariablesToSystemProperties();
		Thread.currentThread().setName("main");
		JythonHelpers.setJythonCachedir();

		try {
			startClient();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}


	}

	public ISessionManagement getSession() {
		return sm;
	}

	private void initSSL() throws Exception {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
				// Trust always
			}

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

			public X509Certificate[] getAcceptedIssuers() {

				return null;
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("TLS");
		// Create empty HostnameVerifier
		HostnameVerifier hv = new HostnameVerifier() {
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

	private synchronized void startClient() throws Exception {

		String ping = null;
		if ( !clientStarted ) {
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

					break;

				} catch (UndeclaredThrowableException e) {
					e.printStackTrace();
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

	private void startDaemon(Daemon d) {

		String memMin = "-Xms24m";
		String memMax = "-Xmx24m";

		JavaVMArguments args;
		try {
			args = JavaVMArguments.current();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		int indexMax = -1;
		int indexMin = -1;
		int classpath = -1;
		// String className = this.getClass().toString();
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);
			if (arg.startsWith("-Xmx")) {
				indexMax = i;
			}
			if (arg.startsWith("-Xms")) {
				indexMin = i;
			}
			try {
				Class startup = Class.forName(arg);
				classpath = i;
				myLogger.debug("Found class: " + startup);
			} catch (ClassNotFoundException e) {
			}

		}

		if (indexMin >= 0) {
			args.set(indexMin, memMin);
		} else {
			args.add(memMin);
		}
		if (indexMax >= 0) {
			args.set(indexMax, memMax);
		} else {
			args.add(memMax);
		}

		if (classpath >= 0) {
			args.set(classpath, "grith.gridsession.GridSessionDaemon");
		}

		myLogger.debug("Daemonizing using args: {}",
				StringUtils.join(args, ", "));

		d.daemonize(args);

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
