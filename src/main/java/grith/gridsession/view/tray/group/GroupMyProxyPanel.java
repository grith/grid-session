package grith.gridsession.view.tray.group;

import grisu.jcommons.constants.Constants;
import grith.gridsession.view.tray.GridSessionController;
import grith.jgrith.cred.MyProxyCred;

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class GroupMyProxyPanel extends JPanel implements PropertyChangeListener {

	private class MyProxyUploadAction extends AbstractAction {

		public MyProxyUploadAction() {
			putValue(NAME, "Upload");
			putValue(SHORT_DESCRIPTION, "Upload credential to MyProxy");
		}

		public void actionPerformed(ActionEvent e) {

			lockUI(true);
			Thread t = new Thread() {
				@Override
				public void run() {

					try {
						String group = (String) groupModel.getSelectedItem();
						if (StringUtils.isBlank(group)) {
							return;
						}
						String myproxyusername = getUsernameField().getText();
						String myproxyhost = null;
						if (!StringUtils.isBlank(myproxyusername)) {
							myproxyhost = MyProxyCred
									.extractMyProxyServerFromUsername(myproxyusername);
						}
						if (myproxyhost == null) {
							myproxyhost = "";
						}
						String myproxypassword = getPasswordField().getText();

						if (StringUtils.isBlank(myproxypassword)) {
							if (StringUtils.isNotBlank(myproxyusername)) {
								controller.getSessionManagement().upload(group,
										myproxyhost, myproxyusername);
							} else {
								if (StringUtils.isNotBlank(myproxyhost)) {
									controller.getSessionManagement().upload(group,
											myproxyhost);
								} else {
									controller.getSessionManagement().upload(group);
								}
							}

						} else {
							controller.getSessionManagement().upload(group,
									myproxyhost, myproxyusername,
									myproxypassword.toCharArray());
						}

						setGroup(group);
					} finally {
						lockUI(false);
					}
				}
			};
			t.setName("UploadMyProxyThread");

			t.start();

		}

	}

	private final GridSessionController controller;
	private JLabel lblAvailableGroups;
	private JComboBox comboBox;
	private JSeparator separator;
	private JLabel lblMyproxyDetails;
	private JLabel lblNewLabel;
	private JLabel lblPassword;
	private JTextField usernameField;
	private JTextField passwordField;
	private JButton btnNewButton;

	private DefaultComboBoxModel groupModel = new DefaultComboBoxModel();

	private volatile boolean loading = false;

	/**
	 * Create the panel.
	 */
	public GroupMyProxyPanel(GridSessionController c) {
		this.controller = c;
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC, }));
		add(getLblAvailableGroups(), "2, 2, right, default");
		add(getComboBox(), "4, 2, fill, default");
		add(getSeparator(), "2, 4, 3, 1");
		add(getLblMyproxyDetails(), "2, 6, 3, 1");
		add(getLblNewLabel(), "2, 8, right, default");
		add(getUsernameField(), "4, 8, fill, default");
		add(getLblPassword(), "2, 10, right, default");
		add(getPasswordField(), "4, 10, fill, default");
		add(getBtnNewButton(), "4, 12, right, default");

		loadGroups();
		controller.addPropertyChangeListener(this);

	}

	private JButton getBtnNewButton() {
		if (btnNewButton == null) {
			btnNewButton = new JButton("Upload");
			btnNewButton.setAction(new MyProxyUploadAction());
		}
		return btnNewButton;
	}

	private JComboBox getComboBox() {
		if (comboBox == null) {
			comboBox = new JComboBox(groupModel);
			comboBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						String group = (String) e.getItem();
						if (StringUtils.isNotBlank(group)
								&& group.startsWith("/")) {
							setGroup(group);
						}
					}
				}
			});
			comboBox.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		}
		return comboBox;
	}

	private JLabel getLblAvailableGroups() {
		if (lblAvailableGroups == null) {
			lblAvailableGroups = new JLabel("Available Groups:");
		}
		return lblAvailableGroups;
	}

	private JLabel getLblMyproxyDetails() {
		if (lblMyproxyDetails == null) {
			lblMyproxyDetails = new JLabel("MyProxy details:");
		}
		return lblMyproxyDetails;
	}

	private JLabel getLblNewLabel() {
		if (lblNewLabel == null) {
			lblNewLabel = new JLabel("Username:");
			lblNewLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		}
		return lblNewLabel;
	}

	private JLabel getLblPassword() {
		if (lblPassword == null) {
			lblPassword = new JLabel("Password:");
			lblPassword.setHorizontalAlignment(SwingConstants.RIGHT);
		}
		return lblPassword;
	}

	private JTextField getPasswordField() {
		if (passwordField == null) {
			passwordField = new JTextField();
			passwordField.setColumns(10);
		}
		return passwordField;
	}

	private JSeparator getSeparator() {
		if (separator == null) {
			separator = new JSeparator();
		}
		return separator;
	}

	private JTextField getUsernameField() {
		if (usernameField == null) {
			usernameField = new JTextField();
			usernameField.setColumns(10);
		}
		return usernameField;
	}

	private void loadGroups() {

		Thread t = new Thread() {
			@Override
			public void run() {
				if (loading) {
					return;
				}
				loading = true;
				try {
					groupModel.removeAllElements();
					if (controller.isOnline()) {
						groupModel.addElement("Loading groups...");
						getComboBox().setEnabled(false);
						List<String> groups = controller.getSessionManagement()
								.groups();
						groupModel.removeAllElements();
						groupModel.addElement(Constants.NON_VO_FQAN);
						for (String group : groups) {
							groupModel.addElement(group);
						}
						getComboBox().setEnabled(true);
					} else {
						groupModel.addElement("Not available");
						getComboBox().setEnabled(false);
					}
				} finally {
					loading = false;
				}
			}
		};
		t.setName("GroupLoadThread");
		t.start();

	}

	public void lockUI(boolean lock) {

		getComboBox().setEnabled(!lock);
		getUsernameField().setEnabled(!lock);
		getPasswordField().setEnabled(!lock);
		getBtnNewButton().setEnabled(!lock);

	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("online".equals(evt.getPropertyName())) {
			boolean value = (Boolean) evt.getNewValue();
			setOnline(value);
		}
	}

	public synchronized void setGroup(final String group) {

		lockUI(true);
		Thread t = new Thread() {
			@Override
			public void run() {

				try {
					if ( groupModel.getIndexOf(group) >= 0 ) {
						groupModel.setSelectedItem(group);

						boolean isUploaded = controller.getSessionManagement().is_uploaded(
								group);
						if (isUploaded) {
							String un = controller.getSessionManagement().myproxy_username(
									group);
							getUsernameField().setText(un);
							String pw = controller.getSessionManagement().myproxy_password(
									group);
							getPasswordField().setText(pw);

						} else {
							getUsernameField().setText("");
							getPasswordField().setText("");
						}

					}
				} finally {
					lockUI(false);
				}
			}
		};
		t.setName("SetGroupThread");
		t.start();

	}

	private void setOnline(boolean online) {
		loadGroups();
	}
}
