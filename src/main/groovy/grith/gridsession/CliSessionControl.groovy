package grith.gridsession


import grisu.jcommons.configuration.CommonGridProperties
import grisu.jcommons.constants.Enums.LoginType
import grisu.jcommons.utils.EnvironmentVariableHelpers
import grisu.jcommons.view.cli.CliHelpers
import grith.jgrith.utils.CliLogin
import groovy.util.logging.Slf4j

@Slf4j
class CliSessionControl extends SessionClient {

	public static void main(String[] args) throws Exception {

		EnvironmentVariableHelpers.loadEnvironmentVariablesToSystemProperties()

		CliSessionControl control = new CliSessionControl()

		myLogger.debug("Executing command.")

		if ( ! args || args.length == 0 ) {

			if ( control.sm.is_logged_in() ) {
				args = ['status']
			} else {
				args = ['start']
			}
		}

		control.runCommand(args)
	}


	private ISessionManagement sm

	private boolean silent = false

	public CliSessionControl() {
		super()

		sm = getSession()
	}

	public void runCommand(String[] commandline) {
		runCommand(Arrays.asList(commandline))
	}

	public void runCommand(List<String> commandline) {

		def command
		def result
		def args
		command = commandline[0]
		args = commandline.drop(1)

		try {
			args = prepare(command, args)
		} catch (all) {
			println 'Wrong syntax: '+command
			System.exit(1)
		}
		try {
			if ( args ) {
				log.debug 'executing '+command +' with args'
				result = sm."$command"(args)
				log.debug 'executed '+command +' with args'
			} else {
				log.debug 'executing '+command
				result = sm."$command"()
			}
		} catch (all) {
			println 'Command "'+command+ '" failed: '+all.getLocalizedMessage()
			//			System.exit(1)
		}

		if (! silent ) {
			postprocess(command, result)
		}
	}

	private post_login(def result) {
		post_start(result)
	}

	private post_start(def result) {
		if ( result ) {
			println ("Login successful")
		} else {
			println ("Login failed")
		}
	}

	private post_groups(def result) {
		for (def g : result ) {
			println(g)
		}
	}

	private post_refresh(def result) {
		if ( result ) {
			println ("Refresh successful")
		} else {
			println ("Refresh failed")
		}
	}

	private post_logout(def result) {
		println "Logged out - session daemon still running"
	}

	private post_stop(def result) {
		println "Stopped session daemon"
	}

	private post_destroy(def result) {
		println "Destroyed credential and stopped session daemon"
	}

	private post_list_institutions(def result) {
		for (def i : result ) {
			println(i)
		}
	}

	private post_proxy_path(def result) {
		if ( ! result ) {
			println "No proxy"
		} else {
			println result
		}
	}

	private post_upload(def result) {
		if ( result ) {
			println "Upload successful"
		} else {
			println "Upload failed"
		}
	}

	private postprocess(def command, def result) {
		try {
			return this."post_$command"(result)
		} catch (all) {
			if ( result || result == false) {
				println result
			}
		}
	}

	private prepare(def command, def args) {
		try {
			return this."$command"(args)
		} catch (MissingMethodException e) {
			return args
		}
	}

	private set_myproxy_password(def pw) {
		if (! pw) {
			pw = CliLogin.askPassword("Please enter the MyProxy password")
			return pw
		}  else {
			return pw[0].toCharArray()
		}
	}

	private login(def args) {
		return start(args)
	}

	public set_min_lifetime(def secs) {

		if ( ! secs ) {
			def msg = 'Minimum lifetime in seconds: '
			secs = CliLogin.ask(msg, '259200')
		} else {
			secs = secs[0]
		}

		def s = Integer.parseInt(secs)

		return s
	}

	public group_proxy_path(def group) {
		if (! group) {
			def msg = 'Group you want the proxy for: '
			group = CliLogin.ask(msg)
		} else {
			group = group[0]
		}
		return group
	}

	public set_min_autorefresh(def secs) {

		if (! secs ) {
			def msg = 'Minimum time inbetween autorefreshes (in seconds): '
			secs = CliLogin.ask(msg, '300')
		} else {
			secs = secs[0]
		}

		def s = Integer.parseInt(secs)

		return s
	}

	public setSilent(boolean silent) {
		this.silent = silent
	}


	private start(def args) {

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
				'Certificate login',
				'Certificate login (custom path)'
			]
		} else {
			choices = [
				'Institution login',
				'MyProxy login',
				'Certificate login',
				'Certificate login (custom path)'
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
					"Please select the institution you are associated with:", true)
				if (!idpToUse) {
					System.exit(0)
				}


			case lastIdpChoice:
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.SHIBBOLETH
				if ( ! idpToUse ) {
					idpToUse = lastIdp
				}
				loginConf[Credential.PROPERTY.IdP.toString()] = idpToUse
				def msg = "Your institution username"
				def lastUsername = CommonGridProperties.getDefault()
					.getLastShibUsername()

				def username = CliLogin.ask(msg, lastUsername)
				loginConf[Credential.PROPERTY.Username.toString()] = username
				char[] pw = CliLogin.askPassword("Your institution password")
				loginConf[Credential.PROPERTY.Password.toString()] = pw

				break
			case 'MyProxy login':
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.MYPROXY

				def username = CliLogin.ask("MyProxy username", CommonGridProperties
					.getDefault().getLastMyProxyUsername())

				loginConf[Credential.PROPERTY.MyProxyUsername.toString()] = username

				def pw = CliLogin.askPassword("MyProxy password")
				loginConf[Credential.PROPERTY.MyProxyPassword.toString()] = pw

				break
			case 'Certificate login (custom path)':
				File cert
				String path
				while (!path) {
					path = CliLogin.ask('Please enter the path to the certificate')
					cert = new File(path)
					if (! cert.exists() || ! cert.canRead()) {
						println "File "+path+" does not exists or can't be read"
						path = null
					}
				}
				File key
				path = null
				while (!path) {
					path = CliLogin.ask('Please enter the path to the private key')
					key = new File(path)
					if (! key.exists() || ! key.canRead()) {
						println "File "+path+" does not exists or can't be read"
						path = null
					}
				}
				loginConf[Credential.PROPERTY.CertFile.toString()] = cert.getAbsolutePath()
				loginConf[Credential.PROPERTY.KeyFile.toString()] = key.getAbsolutePath()
			case 'Certificate login':
				loginConf[Credential.PROPERTY.LoginType.toString()] = LoginType.X509_CERTIFICATE

				char[] pw = CliLogin
					.askPassword("Please enter your certificate passphrase")

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
