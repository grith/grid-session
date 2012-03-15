package grith.gridsession;

import java.net.BindException;
import java.net.ServerSocket;

import org.apache.xmlrpc.webserver.ServletWebServer;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.akuma.Daemon;

public class SessionServer implements ISessionServer {

	public static Logger myLogger = LoggerFactory
			.getLogger(SessionServer.class);

	public static final int START_PORT = 49152;

	public static void main(String[] args) throws Exception {

		Daemon d = new Daemon();

		if (d.isDaemonized()) {
			d.init();
		} else {

			d.daemonize();
			System.exit(0);
		}

		System.out.println("Starting server...");
		SessionServer s = new SessionServer();
	}

	private XmlRpcServlet servlet;
	private ServletWebServer server;

	private RpcPort port;

	public SessionServer() throws Exception {

		try {
			port = new RpcPort();

			// that's quicker
			ServerSocket s = new ServerSocket(port.getPort());
			s.close();

			servlet = new GrithXmlRpcServlet();
			server = new ServletWebServer(servlet, port.getPort());

			server.start();
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
		server.shutdown();
		port.shutdown();

	}



}
