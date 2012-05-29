package grith.gridsession;

import grisu.jcommons.dependencies.BouncyCastleTool;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Acme.Serve.Serve;

public class TinySessionServer implements ISessionServer {

	public static Logger myLogger = LoggerFactory
			.getLogger(TinySessionServer.class);

	public static final int START_PORT = 49152;

	public static void main(String[] args) throws Exception {

		BouncyCastleTool.initBouncyCastle();

		// Daemon d = new Daemon();
		//
		// if (d.isDaemonized()) {
		// d.init();
		// } else {
		//
		// d.daemonize();
		// System.exit(0);
		// }

		System.out.println("Starting server...");
		TinySessionServer s = new TinySessionServer();


	}

	private XmlRpcServlet servlet;
	private Serve srv;

	private RpcPort port;
	private RPCKeyStore ks;
	private RpcAuthToken auth;

	public TinySessionServer() throws Exception {


		try {
			port = new RpcPort();

			// that's quicker
			ServerSocket s = new ServerSocket(port.getPort());
			s.close();

			BouncyCastleTool.initBouncyCastle();
			// delete all old rpc-related things
			FileUtils.deleteQuietly(RPCKeyStore.KEYSTORE_FILE);
			FileUtils.deleteQuietly(RpcAuthToken.TOKEN_FILE);

			auth = new RpcAuthToken();

			ks = new RPCKeyStore();

			servlet = new GrithXmlRpcServlet();

			srv = new Serve();

			java.util.Properties properties = new java.util.Properties();
			properties.put("port", new RpcPort().getPort());
			properties.put("acceptorImpl", "Acme.Serve.SSLAcceptor");
			properties
			.put("keystoreFile", RPCKeyStore.KEYSTORE_FILE.toString());
			properties.put("keystorePass", auth.getAuthToken());
			properties.put("keystoreType", "JKS");
			// properties.setProperty(Acme.Serve.Serve.ARG_NOHUP, "nohup");
			srv.arguments = properties;
			// srv.addDefaultServlets(null); // optional file servlet

			Hashtable servletConfig = new Hashtable();
			servletConfig.put("enabledForExceptions", "true");
			servletConfig.put("enabledForExtensions", "true");
			srv.addServlet("/xmlrpc", servlet, servletConfig); // optional

			srv.init();

			new Thread() {
				@Override
				public void run() {
					srv.serve();
				}
			}.start();

			// uhuh...
			SessionManagement.server = this;

			SessionManagement.kickOffIdpPreloading();

		} catch (BindException be) {
			// that's ok
			myLogger.debug("Server already running on that port...");
		}
	}

	public RpcPort getPort() {
		return this.port;
	}

	public void shutdown() {

		try {
			srv.notifyStop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		srv.destroyAllServlets();
		port.shutdown();

	}



}
