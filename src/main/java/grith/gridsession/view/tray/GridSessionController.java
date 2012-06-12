package grith.gridsession.view.tray;

import grisu.jcommons.exceptions.CredentialException;
import grith.gridsession.ISessionManagement;
import grith.gridsession.SessionClient;
import grith.gridsession.view.GridLoginDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridSessionController {

	private class LoginAction extends AbstractAction {
		public LoginAction() {
			putValue(NAME, "Login");
			putValue(SHORT_DESCRIPTION, "Login to the grid");
		}

		public void actionPerformed(ActionEvent e) {
			getLoginDialog().setVisible(true);
		}

	}

	private class LogoutAction extends AbstractAction {
		public LogoutAction() {
			putValue(NAME, "Logout");
			putValue(SHORT_DESCRIPTION, "Destroy login credential");
		}

		public void actionPerformed(ActionEvent e) {
			if (getSessionManagement() == null) {
				return;
			}

			client.getSession().logout();

		}

	}

	private class StopAction extends AbstractAction {
		public StopAction() {
			putValue(NAME, "Exit");
			putValue(SHORT_DESCRIPTION,
					"Destroy login credential and close session daemon if one exists");
		}

		public void actionPerformed(ActionEvent e) {
			if (getSessionManagement() == null) {
				return;
			}

			client.getSession().stop();

			System.exit(0);

		}

	}

	static final Logger myLogger = LoggerFactory
			.getLogger(GridSessionController.class.getName());

	private Timer timer;

	private GridLoginDialog loginDialog = null;

	private SessionClient client;

	public final LoginAction loginAction = new LoginAction();
	public final LogoutAction logoutAction = new LogoutAction();
	public final StopAction stopAction = new StopAction();

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public static final int DELAY = 10000;

	private long lastChecked = new Date().getTime() / 1000;

	private int lifetime = -1;

	private boolean online = false;

	public GridSessionController(SessionClient s) {
		this.client = s;

	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		this.pcs.addPropertyChangeListener(l);
	}

	public int getLifetime() {

		long now = new Date().getTime()/1000;
		long endtime = lastChecked - lifetime;

		return new Long(now - endtime).intValue();
	}

	public GridLoginDialog getLoginDialog() {
		if (loginDialog == null) {
			loginDialog = new GridLoginDialog(client);
			loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		}
		return loginDialog;
	}

	private ISessionManagement getSession() {
		if (this.client == null) {
			return null;
		}
		return this.client.getSession();
	}

	public ISessionManagement getSessionManagement() {
		if (client == null) {
			return null;
		}
		return client.getSession();
	}

	public void init() {
		pcs.firePropertyChange("online", null, false);
		refreshStatus();

		timer = new Timer(DELAY, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// System.out.println("Checking status");
				try {
					refreshStatus();
				} catch (CredentialException ce) {
					ce.printStackTrace();
				}
			}
		});

		timer.start();
	}

	public boolean isOnline() {
		return online;
	}

	private synchronized void refreshStatus() {

		boolean lastOnline = online;
		// try {
		lifetime = getSession().lifetime();
		lastChecked = new Date().getTime();
		// } catch (UndeclaredThrowableException ute) {
		// // maybe session was stopped? Trying to restart it...
		// myLogger.debug("Can't connect session, trying to restart Daemon...");
		// Daemon d = new Daemon();
		// client.startDaemon(d);
		//
		// try {
		// startClient();
		// lt = getSession().lifetime();
		// } catch (Exception e) {
		// throw new CredentialException(
		// "Can't connect to session deamon.", e);
		// }
		// }

		online = (lifetime > 0);
		if (lastOnline != online) {
			pcs.firePropertyChange("online", !online, online);
			pcs.firePropertyChange("lifetime", null, 0);

		}

		if (online) {
			pcs.firePropertyChange("lifetime", null, lifetime);
		}

	}

	public void removePropertyChangeListener(PropertyChangeListener l) {
		this.pcs.removePropertyChangeListener(l);
	}

	public void setSessionClient(SessionClient s) {
		this.client = s;
	}

}
