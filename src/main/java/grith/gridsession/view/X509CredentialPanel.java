package grith.gridsession.view;

import grisu.jcommons.constants.Enums.LoginType;
import grith.jgrith.cred.AbstractCred.PROPERTY;

import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.google.common.collect.Maps;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class X509CredentialPanel extends CredPanel {
	private JPanel panel;
	private JPasswordField passwordField;

	/**
	 * Create the panel.
	 */
	public X509CredentialPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				FormSpecs.DEFAULT_ROWSPEC,
				FormSpecs.RELATED_GAP_ROWSPEC, }));
		add(getPanel(), "2, 2, fill, fill");

	}

	@Override
	public Map<PROPERTY, Object> createCredConfig() {

		Map<PROPERTY, Object> credConfig = Maps.newHashMap();
		credConfig.put(PROPERTY.LoginType, LoginType.X509_CERTIFICATE);
		credConfig.put(PROPERTY.Password, getPasswordField().getPassword());

		return credConfig;

	}

	@Override
	public String getCredTitle() {
		return "X509 Certificate";
	}

	private JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "Credential passphrase",
					TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel.setLayout(new FormLayout(new ColumnSpec[] { ColumnSpec
					.decode("default:grow"), },
					new RowSpec[] { FormSpecs.DEFAULT_ROWSPEC, }));
			panel.add(getPasswordField(), "1, 1, fill, default");
		}
		return panel;
	}

	private JPasswordField getPasswordField() {
		if (passwordField == null) {
			passwordField = new JPasswordField();
		}
		return passwordField;
	}

	@Override
	public void lockUI(final boolean lock) {

		SwingUtilities.invokeLater(new Thread() {
			@Override
			public void run() {
				getPasswordField().setEnabled(!lock);
			}
		});

	}
}
