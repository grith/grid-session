package grith.gridsession;

import grisu.jcommons.constants.GridEnvironment;
import grisu.jcommons.dependencies.BouncyCastleTool;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class RPCKeyStore {

	public static File KEYSTORE_FILE = new File(
			GridEnvironment.getGridConfigDirectory(), "rpc.keystore");

	/**
	 * Create a self-signed X.509 Certificate
	 * 
	 * @param dn
	 *            the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
	 * @param pair
	 *            the KeyPair
	 * @param days
	 *            how many days from now the Certificate is valid for
	 * @param algorithm
	 *            the signing algorithm, eg "SHA1withRSA"
	 */
	public static void generateCertificate() throws Exception {

		if (!KEYSTORE_FILE.exists()) {

			RpcAuthToken auth = new RpcAuthToken();

			String domainName = "localhost";
			String certPath = KEYSTORE_FILE.toString();

			KeyPairGenerator keyPairGenerator;

			keyPairGenerator = KeyPairGenerator.getInstance("RSA");

			keyPairGenerator.initialize(1024);
			KeyPair KPair = keyPairGenerator.generateKeyPair();

			X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();

			int authTokenHash = auth.getAuthToken().hashCode();
			v3CertGen.setSerialNumber(BigInteger.valueOf(authTokenHash).abs());
			v3CertGen.setIssuerDN(new X509Principal("CN=" + domainName
					+ ", OU=None, O=None L=None, C=None"));
			v3CertGen.setNotBefore(new Date(System.currentTimeMillis()
					- (1000L * 60 * 60 * 24 * 30)));
			v3CertGen.setNotAfter(new Date(System.currentTimeMillis()
					+ (1000L * 60 * 60 * 24 * 365 * 10)));
			v3CertGen.setSubjectDN(new X509Principal("CN=" + domainName
					+ ", OU=None, O=None L=None, C=None"));

			v3CertGen.setPublicKey(KPair.getPublic());
			v3CertGen.setSignatureAlgorithm("MD5WithRSAEncryption");

			X509Certificate pkCertificate = v3CertGen
					.generateX509Certificate(KPair.getPrivate());

			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(null, null);
			keystore.setKeyEntry("test", KPair.getPrivate(), auth
					.getAuthToken().toCharArray(),
					new X509Certificate[] { pkCertificate });

			FileOutputStream fos;

			fos = new FileOutputStream(certPath);

			keystore.store(fos, auth.getAuthToken().toCharArray());
			// fos.write(pkCertificate.getEncoded());
			fos.close();

		}

	}

	public static void main(String[] args) throws Exception {

		BouncyCastleTool.initBouncyCastle();

		RPCKeyStore ks = new RPCKeyStore();

	}

	private final RpcAuthToken auth;

	public RPCKeyStore() throws Exception {
		this.auth = new RpcAuthToken();
		generateCertificate();
	}



}
