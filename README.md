grid-session
============

**grid-session** is a small java app which manages authentication credentials to the grid. It's written in Java (mainly because all the supporting libraries exist in Java already) and includes a pinch of Groovy. 

Implementation / Design
-----------------------

At startup, **grid-session** checks whether a background daemon is running to manage sessions. If not, it'll spawn one and then execute the command it was started with. That is done via an xml-rpc/https interface. 

It is necessary to run as a daemon because otherwise it wouldn't be possible to auto-refresh short-lived credentials (which we get from SLCS or MyProxy). That is done by caching the users password in memory after the initial login process, so everytime the credential is about to expire, the daemon logs in again with the same credentials and updates the internal instance of the credential. 

Because of this design, it is important that the control script and the daemon are sitting on the same host, otherwise a user password would sit in memory on a 3rd party machine and the user would loose control over it.

To further strengthen security, the communication happens via https where the daemon creates a self-signed certificate with every startup which is protected by a random (uuid-)token password that is stored on the filesystem and which only has read permissions for the user running the daemon/control-script. This way it is ensured that only the same local user can use the session daemon and also that only the local user who has read-access to the token and certificate files could possibly intercept the traffic between control script and daemon (which is important since the users IdP password is transferred).


Download/Install
-----------------

Download/install deb/rpm/jar from:

https://code.ceres.auckland.ac.nz/jenkins/job/Grid-Session-SNAPSHOT/
(for now)

And run via

    grid-session <command>

in case you used a package, or use executable jar and run via

    java -jar grid-session-bin.jar <command>


Usage
-----

Possible commands:

 - **start**:			 kicks off the login process and runs the daemon in the background to auto-renew the credential whenever necessary
 - **stop**:	      	 destroys the credential and stops the daemon
 - **status**:			 displays information about the login state (lifetime left, whether the credential is auto-renewable)
 - **set_min_lifetime**	 sets the minimum lifetime (in seconds) before auto-renewing a credential (if the credential supports auto-renew)
 - **lifetime**:		 displays the lifetime that is left for the current credential (in seconds)
 - **is_logged_in**:		 displays whether there is currently a valid credential
 - **refresh**:		 forces an auto-refresh of the credential. only works for auto-renewable credentials
 - **list_institutions**:	 displays a list of all available institutions (you wouldn't reallly need this command)
