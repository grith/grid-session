package grith.gridsession;

import grisu.jcommons.constants.GridEnvironment;

import java.io.File;
import java.net.ServerSocket;

import org.apache.commons.io.FileUtils;

public class RpcPort {

	public static File PORT_FILE = new File(
			GridEnvironment.getGridConfigDirectory(), "rpc.port");

	public static synchronized int findFreePort() throws Exception {
		ServerSocket serverSocket = new ServerSocket(0);

		int port = serverSocket.getLocalPort();
		serverSocket.close();

		return port;
	}

	public static synchronized int getUserPort() throws Exception {

		if ( PORT_FILE.exists() ) {
			String p = FileUtils.readFileToString(PORT_FILE);
			int port = Integer.parseInt(p);
			return port;
		} else {
			int port = findFreePort();
			PORT_FILE.getParentFile().mkdirs();
			FileUtils.writeStringToFile(PORT_FILE, Integer.toString(port));
			return port;
		}
	}

	private final int port;

	public RpcPort() throws Exception {
		port = getUserPort();
	}

	public int getPort() {
		return port;
	}

	public synchronized void shutdown() {
		PORT_FILE.delete();
	}



}
