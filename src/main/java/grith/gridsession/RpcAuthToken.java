package grith.gridsession;

import grisu.jcommons.constants.GridEnvironment;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.globus.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RpcAuthToken {

	public static Logger myLogger = LoggerFactory.getLogger(RpcAuthToken.class);

	public static File TOKEN_FILE = new File(
			GridEnvironment.getGridConfigDirectory(), "rpc.auth");

	private synchronized static void createTokenFile() throws Exception {
		if (!TOKEN_FILE.exists()) {
			String uuid = UUID.randomUUID().toString();
			myLogger.debug("Creating auth token: {}", uuid);
			TOKEN_FILE.getParentFile().mkdirs();

			FileUtils.writeStringToFile(TOKEN_FILE, uuid);
			Util.setFilePermissions(TOKEN_FILE.toString(), 600);
		}
	}

	private final String authToken;

	public RpcAuthToken() throws Exception {

		createTokenFile();
		authToken = FileUtils.readFileToString(TOKEN_FILE);
	}

	public String getAuthToken() {
		return authToken;
	}

}
