package grith.gridsession;

import grisu.jcommons.dependencies.BouncyCastleTool;
import grisu.jcommons.utils.EnvironmentVariableHelpers;
import grisu.jcommons.utils.JythonHelpers;

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

	public static SessionClient create() {

		int tries = 0;
		while (tries < 5) {
			try {
				SessionClient client = new SessionClient();
				myLogger.debug("Executing command.");
				client.getSessionManagement().status();
				return client;
			} catch (UndeclaredThrowableException e) {
				// e.printStackTrace();
				myLogger.debug("Could not execute command, trying again.");
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

		EnvironmentVariableHelpers.loadEnvironmentVariablesToSystemProperties();
		Thread.currentThread().setName("main");
		JythonHelpers.setJythonCachedir();
		BouncyCastleTool.initBouncyCastle();

		Daemon d = new Daemon();

		if (d.isDaemonized()) {
			d.init();
		} else {

			d.daemonize();

			CliSessionControl control = new CliSessionControl(false);

			myLogger.debug("Executing command.");
			control.execute(args[0]);

			System.exit(0);
		}

		// make sure server is started...
		TinySessionServer server = new TinySessionServer();

	}

	private final ISessionManagement sm;

	private final BigInteger acceptedCertSerial;

	public SessionClient() throws Exception {

		initSSL();

		// create configuration
		RpcAuthToken auth = new RpcAuthToken();
		acceptedCertSerial = BigInteger.valueOf(auth.getAuthToken().hashCode())
				.abs();

		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
		RpcPort port = new RpcPort();
		config.setServerURL(new URL("https://127.0.0.1:" + port.getPort()
				+ "/xmlrpc"));
		config.setEnabledForExtensions(true);
		config.setConnectionTimeout(60 * 1000);
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
		System.out.println(StringUtils.join(sm.getIdPs(), "\n"));
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
