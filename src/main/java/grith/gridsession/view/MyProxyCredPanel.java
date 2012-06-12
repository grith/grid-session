package grith.gridsession.view;

import grisu.jcommons.configuration.CommonGridProperties;
import grisu.jcommons.constants.Enums.LoginType;
import grith.jgrith.cred.MyProxyCred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;
import com.google.common.collect.Maps;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class MyProxyCredPanel extends CredPanel {
	private JPanel panel;
	private JTextField myproxyUsername;
	private JPanel panel_1;
	private JPasswordField passwordField;

	/**
	 * Create the panel.
	 */
	public MyProxyCredPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("125px:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, RowSpec.decode("51px"),
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getPanel_1(), "2, 1, 1, 2, fill, fill");
		add(getPanel_1_1(), "2, 4, fill, fill");

	}

	@Override
	public Map<PROPERTY, Object> createCredConfig() {
		Map<PROPERTY, Object> config = Maps.newHashMap();

		config.put(PROPERTY.LoginType, LoginType.MYPROXY);
		String un = getMyproxyUsername().getText();
		String possibleHost = MyProxyCred.extractMyProxyServerFromUsername(un);
		if (StringUtils.isBlank(possibleHost)) {
			config.put(PROPERTY.MyProxyUsername, un);
		} else {
			String username = MyProxyCred.extractUsernameFromUsername(un);
			config.put(PROPERTY.MyProxyUsername, username);
			config.put(PROPERTY.MyProxyHost, possibleHost);
		}
		config.put(PROPERTY.MyProxyPassword, getPasswordField().getPassword());

		return config;
	}

	@Override
	public String getCredTitle() {
		return "MyProxy";
	}

	private JTextField getMyproxyUsername() {
		if (myproxyUsername == null) {
			myproxyUsername = new JTextField();
			myproxyUsername.setColumns(10);
			final String lastUn = CommonGridProperties.getDefault()
					.getLastMyProxyUsername();
			if (StringUtils.isNotBlank(lastUn)) {
				myproxyUsername.setText(lastUn);
			}
		}
		return myproxyUsername;
	}

	private JPanel getPanel_1() {
		if (panel == null) {
			panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "MyProxy username", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC, }));
			panel.add(getMyproxyUsername(), "2, 2, fill, default");
		}
		return panel;
	}

	private JPanel getPanel_1_1() {
		if (panel_1 == null) {
			panel_1 = new JPanel();
			panel_1.setBorder(new TitledBorder(null, "MyProxy password",
					TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel_1.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC, }));
			panel_1.add(getPasswordField(), "2, 2, fill, default");
		}
		return panel_1;
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
				getMyproxyUsername().setEnabled(!lock);
			}
		});

	}
}
