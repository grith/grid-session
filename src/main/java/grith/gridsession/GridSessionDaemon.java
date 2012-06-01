package grith.gridsession;

import grisu.jcommons.dependencies.BouncyCastleTool;
import grisu.jcommons.utils.JythonHelpers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Acme.Serve.Serve;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;

import com.sun.akuma.Daemon;

public class GridSessionDaemon implements ISessionServer {

	public static Logger myLogger = LoggerFactory
			.getLogger(GridSessionDaemon.class);

	public static final int START_PORT = 49152;

	public static void main(String[] args) throws Exception {

		Daemon d = new Daemon();

		d.init();

		try {

			File file = new File("/tmp/jna");
			file.mkdirs();

			file.setWritable(true, false);

		} catch (Exception e) {
			myLogger.error("Can't create dir or change permissions for /tmp/jna: "
					+ e.getLocalizedMessage());
		}

		try {
			LoggerContext context = (LoggerContext) LoggerFactory
					.getILoggerFactory();
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);

			InputStream config = SessionClient.class
					.getResourceAsStream("/logback_server.xml");

			context.reset();
			configurator.doConfigure(config);

			StatusPrinter.printInCaseOfErrorsOrWarnings(context);
		} catch (Exception e) {
			myLogger.error("Error when trying to configure grid-session service logging");

		}

		Thread.currentThread().setName("main");
		JythonHelpers.setJythonCachedir();

		GridSessionDaemon s = new GridSessionDaemon();

		myLogger.debug("GridSessionService started");


	}

	private XmlRpcServlet servlet;
	private Serve srv;

	private RpcPort port;
	private RPCKeyStore ks;
	private RpcAuthToken auth;

	public GridSessionDaemon() throws Exception {


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
					myLogger.info("Service stopped.");
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
			myLogger.debug("Shutting down...");
			srv.notifyStop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		srv.destroyAllServlets();
		port.shutdown();

	}



}
