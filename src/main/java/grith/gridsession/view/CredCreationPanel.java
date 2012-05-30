package grith.gridsession.view;

import grisu.jcommons.configuration.CommonGridProperties;
import grith.gridsession.GridSessionCred;
import grith.jgrith.cred.AbstractCred;
import grith.jgrith.cred.Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.AbstractAction;
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

	private class LoginAction extends AbstractAction {
		public LoginAction() {
			putValue(NAME, "Login");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}

		public void actionPerformed(ActionEvent e) {

			CredPanel selPanel = (CredPanel) getTabbedPane()
					.getSelectedComponent();

			Map<PROPERTY, Object> credConfig = selPanel.createCredConfig();

			Cred cred = null;
			if (CommonGridProperties.getDefault().useGridSession()) {
				cred = new GridSessionCred(credConfig, useSSL);
			} else {
				cred = AbstractCred.loadFromConfig(credConfig);
			}

			System.out.println("DN: " + cred.getDN());

		}
	}

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
			cred = new GridSessionCred(credConfig, useSSL);
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
			if (GridSessionCred.validSessionExists()) {
				tabbedPane.addTab(getGridSessionCredPanel().getCredTitle(), null, getGridSessionCredPanel(), null);
			}
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

	public void setUseSSL(boolean use) {
		this.useSSL = use;
	}
}
