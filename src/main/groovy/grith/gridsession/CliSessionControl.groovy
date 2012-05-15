package grith.gridsession

import grisu.jcommons.configuration.CommonGridProperties
import grisu.jcommons.constants.Enums.LoginType
import grisu.jcommons.dependencies.BouncyCastleTool
import grisu.jcommons.view.cli.CliHelpers
import grith.jgrith.credential.Credential
import grith.jgrith.utils.CliLogin
import groovy.util.logging.Slf4j

@Slf4j
class CliSessionControl {

	public static void main(String[] args) {

		BouncyCastleTool.initBouncyCastle();

		def control = new CliSessionControl(false)


		control.execute('list_institutions')

		System.exit(0);
	}

	private SessionClient client
	private ISessionManagement sm

	private boolean silent = false

	public CliSessionControl(boolean useLocalTransport, boolean initSSL) {

		if ( ! useLocalTransport ) {
			this.client = SessionClient.create(initSSL)
			this.sm = client.getSessionManagement()
		} else {
			this.sm = new SessionManagement()
			SessionManagement.kickOffIdpPreloading()
		}
	}

	public SessionClient getSessionClient() {
		return client
	}

	public void execute(def command) {

		def result
		def args
		try {
			args = prepare(command)
		} catch (all) {
			println 'Wrong syntax: '+command
			System.exit(1)
		}
		try {
			if ( args ) {
				result = sm."$command"(args)
			} else {
				log.debug 'executing '+command
				result = sm."$command"()
			}
		} catch (all) {
			println 'Command "'+command+ '" failed: '+all.getLocalizedMessage()
			all.printStackTrace()
			System.exit(1)
		}

		if (! silent ) {
			println result
		}
	}

	private prepare(def command) {
		try {
			return this."$command"()
		} catch (MissingMethodException e) {
			return null
		}
	}

	private login() {
		return start()
	}

	public set_min_lifetime() {

		def msg = 'Minimum lifetime in seconds: '
		def secs = CliLogin.ask(msg, '259200')

		def s = Integer.parseInt(secs)

		return s
	}

	public set_min_autorefresh() {

		def msg = 'Minimum time inbetween autorefreshes (in seconds): '
		def secs = CliLogin.ask(msg, '300')

		def s = Integer.parseInt(secs)

		return s
	}

	public setSilent(boolean silent) {
		this.silent = silent
	}


	private start() {

		def loginConf = [:]

		println 'Please select your preferred login method:'

		def lastIdp = CommonGridProperties.getDefault().getLastShibIdp()
		def choices
		def lastIdpChoice = 'Institution login (using: '+lastIdp+')'
		if ( lastIdp ) {
			choices = [
				'Institution login',
				lastIdpChoice,
				'MyProxy login',
				'Certificate login'
			]
		} else {
			choices = [
				'Institution login',
				'MyProxy login',
				'Certificate login'
			]
		}


		def answer = CliHelpers.getUserChoice(choices, 'Login method', null, 'Exit')

		if ( answer == 'Exit' ) {
			System.exit(0)
		}

		def idpToUse

		switch(answer) {
			case 'Institution login':
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.SHIBBOLETH

				def idps = sm.list_institutions()
				idpToUse = CliLogin.ask("Your institution", lastIdp, idps,
						"Please select the institution you are associated with:", true);
				if (!idpToUse) {
					System.exit(0);
				}


			case lastIdpChoice:
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.SHIBBOLETH
				if ( ! idpToUse ) {
					idpToUse = lastIdp
				}
				loginConf[Credential.PROPERTY.IdP.toString()] = idpToUse
				def msg = "Your institution username";
				def lastUsername = CommonGridProperties.getDefault()
						.getLastShibUsername();

				def username = CliLogin.ask(msg, lastUsername);
				loginConf[Credential.PROPERTY.Username.toString()] = username
				char[] pw = CliLogin.askPassword("Your institution password");
				loginConf[Credential.PROPERTY.Password.toString()] = pw

				break
			case 'MyProxy login':
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.MYPROXY

				def username = CliLogin.ask("MyProxy username", CommonGridProperties
						.getDefault().getLastMyProxyUsername());

				loginConf[Credential.PROPERTY.MyProxyUsername.toString()] = username

				def pw = CliLogin.askPassword("MyProxy password");
				loginConf[Credential.PROPERTY.MyProxyPassword.toString()] = pw

				break
			case 'Certificate login':
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.X509_CERTIFICATE

				char[] pw = CliLogin
						.askPassword("Please enter your certificate passphrase");

				loginConf[Credential.PROPERTY.Password.toString()] = pw

				break
			default:
				println 'Invalid login method. Exiting...'
				System.exit(1)
		}

		loginConf[Credential.PROPERTY.StorePasswordInMemory.toString()] = true

		return loginConf
	}
}
