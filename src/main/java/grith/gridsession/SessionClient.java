package grith.gridsession;

import grisu.jcommons.dependencies.BouncyCastleTool;
import grisu.jcommons.utils.EnvironmentVariableHelpers;
import grisu.jcommons.utils.JythonHelpers;

import java.io.File;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Map;

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

public class SessionClient {

	public static final Logger myLogger = LoggerFactory
			.getLogger(SessionClient.class);

	public static SessionClient create(boolean initSSL) {

		int tries = 0;
		while (tries < 5) {
			try {
				final SessionClient client = new SessionClient(initSSL);
				myLogger.debug("Executing command.");
				client.getSessionManagement().ping();

				new Thread() {
					@Override
					public void run() {
						client.getSessionManagement().list_institutions();
					}
				}.start();

				return client;
			} catch (UndeclaredThrowableException e) {
				myLogger.debug("Could not execute command, trying again.", e);
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

		throw new RuntimeException("Could not create session-client.");

	}

	public static void main(String[] args) throws Exception {

		try {

			File file = new File("/tmp/jna");
			file.mkdirs();

			file.setWritable(true, false);

		} catch (Exception e) {
			myLogger.error("Can't create dir or change permissions for /tmp/jna: "
					+ e.getLocalizedMessage());
		}

		EnvironmentVariableHelpers.loadEnvironmentVariablesToSystemProperties();
		Thread.currentThread().setName("main");
		JythonHelpers.setJythonCachedir();
		BouncyCastleTool.initBouncyCastle();

		Daemon d = new Daemon();

		if (d.isDaemonized()) {
			d.init();
		} else {

			d.daemonize();

			CliSessionControl control = new CliSessionControl(false, true);

			myLogger.debug("Executing command.");
			control.execute(args);

			System.exit(0);
		}

		// make sure server is started...
		TinySessionServer server = new TinySessionServer();

	}

	private final ISessionManagement sm;

	private final BigInteger acceptedCertSerial;

	public SessionClient(boolean initSSL) throws Exception {

		if (initSSL) {
			initSSL();
		}

		// create configuration
		RpcAuthToken auth = new RpcAuthToken();
		acceptedCertSerial = BigInteger.valueOf(auth.getAuthToken().hashCode())
				.abs();

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

		XmlRpcClient client = new XmlRpcClient();

		// use Commons HttpClient as transport
		client.setTransportFactory(new XmlRpcCommonsTransportFactory(client));
		// set configuration
		client.setConfig(config);

		// make a call using dynamic proxy
		ClientFactory factory = new ClientFactory(client);
		sm = (ISessionManagement) factory.newInstance(ISessionManagement.class);

	}

	public void execute(Map<String, String> config) {
		System.out.println(StringUtils.join(sm.list_institutions(), "\n"));
		// sm.shutdown();
	}

	public ISessionManagement getSessionManagement() {
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

}
