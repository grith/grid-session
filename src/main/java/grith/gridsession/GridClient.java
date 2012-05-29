package grith.gridsession;

import grisu.jcommons.configuration.CommonGridProperties;
import grisu.jcommons.dependencies.BouncyCastleTool;
import grisu.jcommons.utils.EnvironmentVariableHelpers;
import grisu.jcommons.utils.JythonHelpers;

import java.io.File;
import java.io.InputStream;
import java.net.BindException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;

public class GridClient {

	public static void execute(GridClient client) {

		if (CommonGridProperties.getDefault().useGridSession()) {

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

			Daemon d = new Daemon();

			try {

				// check whether server already running
				try {
					ServerSocket s = new ServerSocket(RpcPort.getUserPort());
					s.close();
				} catch (BindException be) {
					// that's good, means something already running, we don't
					// need to try to daemonize...
					myLogger.debug("Starting grid-session client");
					client.execute();

					System.exit(0);
				}

				if (d.isDaemonized()) {
					d.init();

				} else {

					String memMin = "-Xms24m";
					String memMax = "-Xmx24m";

					JavaVMArguments args = JavaVMArguments.current();
					int indexMax = -1;
					int indexMin = -1;
					for (int i = 0; i < args.size(); i++) {
						String arg = args.get(i);
						if (arg.startsWith("-Xmx")) {
							indexMax = i;
						}
						if (arg.startsWith("-Xms")) {
							indexMin = i;
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
					d.daemonize(args);

					myLogger.debug("Starting grid-session client");
					client.execute();

					System.exit(0);
				}
			} catch (Exception e) {
				e.printStackTrace();
				myLogger.error(
						"Error when trying to daemonize grid-session service.", e);
				System.exit(1);
			}
			try {
				LoggerContext context = (LoggerContext) LoggerFactory
						.getILoggerFactory();
				JoranConfigurator configurator = new JoranConfigurator();
				configurator.setContext(context);

				InputStream config = SessionClient.class
						.getResourceAsStream("/logback_server.xml");
				configurator.doConfigure(config);
			} catch (Exception e) {
				myLogger.error("Error when trying to configure grid-session service logging");

			}

			myLogger.debug("Starting grid-session service...");

			try {
				TinySessionServer server = new TinySessionServer();
			} catch (Exception e) {
				myLogger.error(
						"Error starting grid-session service: "
								+ e.getLocalizedMessage(), e);
				System.exit(1);
			}
		} else {
			client.execute();
		}

	}

	public static void main(String[] args) {

		GridClient client = new GridClient();

		execute(client);

	}

	private boolean useSSL = false;

	private boolean useLocalTransport = false;

	public static final Logger myLogger = LoggerFactory
			.getLogger(GridClient.class);

	private ISessionManagement session;

	public GridClient() {
	}

	private void execute() {
		BouncyCastleTool.initBouncyCastle();
		run();
	}

	public ISessionManagement getSession() {

		if (session == null) {
			if (!isUseLocalTransport()) {
				SessionClient client = SessionClient.create(isUseSSL());
				this.session = client.getSessionManagement();
			} else {
				this.session = new SessionManagement();
				SessionManagement.kickOffIdpPreloading();
			}
		}
		return session;

	}

	public boolean isUseLocalTransport() {
		return useLocalTransport;
	}

	public boolean isUseSSL() {
		return useSSL;
	}

	/**
	 * Overwrite this method with the code that starts your own client.
	 *
	 * @param args
	 *            the arguments for your client
	 */
	public void run() {

		System.out.println("Dummy grid client");
		System.out.println("Grid-Session status:");
		System.out.println(getSession().status());
	}

	public void setUseLocalTransport(boolean useLocalTransport) {
		this.useLocalTransport = useLocalTransport;
	}

	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
	}

}
