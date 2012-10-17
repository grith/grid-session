package grith.gridsession.view;

import grisu.jcommons.exceptions.CredentialException;
import grith.gridsession.GridSessionCred;
import grith.gridsession.SessionClient;
import grith.jgrith.cred.AbstractCred.PROPERTY;
import grith.jgrith.cred.Cred;

import java.util.Map;
import java.util.logging.Level;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class CredCreationPanel extends JPanel {

	static final Logger myLogger = LoggerFactory
			.getLogger(CredCreationPanel.class.getName());

	private boolean useSSL = false;

	private JTabbedPane tabbedPane;
	private JButton btnNewButton;
	private JPanel panel;
	private X509CredentialPanel x509CredentialPanel;
	private Action action;
	private MyProxyCredPanel myProxyCredPanel;
	private SLCSCredPanel credPanel;
	private GridSessionCredPanel gridSessionCredPanel;

	private SessionClient sessionClient;

	private volatile boolean isCreatingCredential = false;
	private volatile boolean isCredentialCreationFailed = false;

	private Cred lastCredential = null;

	public CredCreationPanel() {
		this(null);
	}
	/**
	 * Create the panel.
	 */
	public CredCreationPanel(SessionClient client) {
		if ( client != null ) {
			setSessionClient(client);
		}
		setLayout(new FormLayout(
				new ColumnSpec[] { ColumnSpec.decode("166px:grow"), },
				new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC,
						FormFactory.MIN_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getTabbedPane(), "1, 2, fill, fill");

	}

	private synchronized Cred createCred() {

		isCreatingCredential = true;
		firePropertyChange("creatingCredential", false, true);
		lockUI(true);
		try {
			CredPanel selPanel = (CredPanel) getTabbedPane()
					.getSelectedComponent();

			if (selPanel instanceof GridSessionCredPanel) {
				myLogger.debug("Using existing session credential.");
				GridSessionCredPanel gscp = (GridSessionCredPanel) selPanel;
				return gscp.getCredential();
			}

			Map<PROPERTY, Object> credConfig = selPanel.createCredConfig();

			Cred cred = null;
			// if (CommonGridProperties.getDefault().useGridSession()) {
			if (sessionClient == null) {
				myLogger.debug("No sessionclient set. returning without credential creation.");
				return null;
			}
			cred = new GridSessionCred(sessionClient, credConfig);
			// } else {
			// cred = AbstractCred.loadFromConfig(credConfig);
			// }

			myLogger.debug(" Created credential, dn: {}", cred.getDN());
			isCreatingCredential = false;
			return cred;
		} catch (Exception e) {
			isCredentialCreationFailed = true;
			firePropertyChange("credentialCreationFailed", false, true);
			throw new CredentialException("Could not create credential.", e);
		} finally {
			firePropertyChange("creatingCredential", true, false);
			lockUI(false);
		}
	}

	public void createCredential() {

		Thread t = new Thread() {
			@Override
			public void run() {

				try {
					Cred c = createCred();
					lastCredential = c;
					if (c != null) {
						firePropertyChange("credential", null, c);
					}
				} catch (CredentialException ex) {

					final String msg = ex.getCause().getLocalizedMessage();
					final ErrorInfo info = new ErrorInfo("Login error",
							"Can't create credential for login.", msg, "Error",
							ex.getCause(),
							Level.SEVERE, null);

					final JXErrorPane pane = new JXErrorPane();
					pane.setErrorInfo(info);

					JXErrorPane.showDialog(CredCreationPanel.this, pane);
					return;
				}

			}
		};
		t.setName("Credential Creation Thread");
		t.start();

	}

	public Cred getCredential() {
		return lastCredential;
	}

	private SLCSCredPanel getCredPanel() {
		if (credPanel == null) {
			credPanel = new SLCSCredPanel();
		}
		return credPanel;
	}

	private GridSessionCredPanel getGridSessionCredPanel() {
		if (gridSessionCredPanel == null) {
			gridSessionCredPanel = new GridSessionCredPanel();
		}
		return gridSessionCredPanel;
	}


	private MyProxyCredPanel getMyProxyCredPanel() {
		if (myProxyCredPanel == null) {
			myProxyCredPanel = new MyProxyCredPanel();
		}
		return myProxyCredPanel;
	}

	private JTabbedPane getTabbedPane() {
		if (tabbedPane == null) {
			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			// if ((sessionClient != null)
			// && sessionClient.getSession().is_logged_in()) {
			tabbedPane.addTab(getGridSessionCredPanel().getCredTitle(), null, getGridSessionCredPanel(), null);
			// }
			tabbedPane.addTab(getCredPanel().getCredTitle(), null, getCredPanel(), null);
			tabbedPane.addTab(getX509CredentialPanel().getCredTitle(), null,
					getX509CredentialPanel(), null);
			tabbedPane.addTab(getMyProxyCredPanel().getCredTitle(), null, getMyProxyCredPanel(), null);

			if (getGridSessionCredPanel().validGridSessionCredentialExists()) {
				tabbedPane.setSelectedIndex(0);
			} else {
				tabbedPane.setSelectedIndex(1);
			}
		}
		return tabbedPane;
	}

	private X509CredentialPanel getX509CredentialPanel() {
		if (x509CredentialPanel == null) {
			x509CredentialPanel = new X509CredentialPanel();
		}
		return x509CredentialPanel;
	}
	public boolean isCreatingCredential() {
		return isCreatingCredential;
	}
	
	public boolean isCredentialCreationFailed() {
		return isCredentialCreationFailed;
	}


	private void lockUI(boolean lock) {
		getMyProxyCredPanel().lockUI(lock);
		getX509CredentialPanel().lockUI(lock);
		getGridSessionCredPanel().lockUI(lock);
		getCredPanel().lockUI(lock);
	}

	public void setSessionClient(SessionClient c) {
		this.sessionClient = c;
		if (sessionClient == null) {
			getGridSessionCredPanel().setCred(null);
			return;
		}

		GridSessionCred cred = new GridSessionCred(this.sessionClient);
		getGridSessionCredPanel().setCred(cred);

		if (getGridSessionCredPanel().validGridSessionCredentialExists()) {
			SwingUtilities.invokeLater(new Thread() {
				@Override
				public void run() {
					tabbedPane.setSelectedIndex(0);
				}
			});
		}

	}

	public void setUseSSL(boolean use) {
		this.useSSL = use;
	}
}
