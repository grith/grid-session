package grith.gridsession.view;

import grisu.jcommons.configuration.CommonGridProperties;
import grith.gridsession.GridSessionCred;
import grith.gridsession.SessionClient;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

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

	/**
	 * Create the panel.
	 */
	public CredCreationPanel() {
		setLayout(new FormLayout(
				new ColumnSpec[] { ColumnSpec.decode("166px:grow"), },
				new RowSpec[] { FormFactory.LINE_GAP_ROWSPEC,
						RowSpec.decode("25px:grow"),
						FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getTabbedPane(), "1, 2, fill, fill");

	}

	public Cred createCredential() {

		CredPanel selPanel = (CredPanel) getTabbedPane().getSelectedComponent();

		if (selPanel instanceof GridSessionCredPanel) {
			myLogger.debug("Using existing session credential.");
			GridSessionCredPanel gscp = (GridSessionCredPanel) selPanel;
			return gscp.getCredential();
		}

		Map<PROPERTY, Object> credConfig = selPanel.createCredConfig();

		Cred cred = null;
		if (CommonGridProperties.getDefault().useGridSession()) {
			if (sessionClient == null) {
				myLogger.debug("No sessionclient set. returning without credential creation.");
				return null;
			}
			cred = new GridSessionCred(sessionClient, credConfig);
		} else {
			cred = AbstractCred.loadFromConfig(credConfig);
		}

		System.out.println("DN: " + cred.getDN());
		return cred;
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
		}
		return tabbedPane;
	}


	private X509CredentialPanel getX509CredentialPanel() {
		if (x509CredentialPanel == null) {
			x509CredentialPanel = new X509CredentialPanel();
		}
		return x509CredentialPanel;
	}

	public void setSessionClient(SessionClient c) {
		this.sessionClient = c;
		if (sessionClient == null) {
			getGridSessionCredPanel().setCred(null);
			return;
		}

		GridSessionCred cred = new GridSessionCred(this.sessionClient);
		getGridSessionCredPanel().setCred(cred);
	}

	public void setUseSSL(boolean use) {
		this.useSSL = use;
	}
}
