package grith.gridsession.view.tray;

import grisu.jcommons.constants.Constants;
import grith.gridsession.view.tray.group.GroupMenu;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class GridSessionMenu extends JPopupMenu implements
PropertyChangeListener {

	private class MyProxyDialogAction extends AbstractAction {

		public MyProxyDialogAction() {
			putValue(NAME, "MyProxy");
			putValue(SHORT_DESCRIPTION, "Show MyProxy upload dialog");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			controller.showGroupMyProxyDialog(Constants.NON_VO_FQAN);

		}

	}

	private JMenuItem connectItem;
	private final GridSessionController controller;
	private MiniCredInfoPanel panel;
	private JMenuItem logoutItem;

	private JMenuItem mntmExit;
	private JMenu mnAdvanced;
	private JMenuItem mntmSettings;
	private JMenuItem mntmUpload;
	private JMenu mnGroups;

	public GridSessionMenu(GridSessionController controller) {
		this.controller = controller;
		this.controller.addPropertyChangeListener(this);
		add(getPanel());
		add(getConnectItem());
		add(getLogoutItem());
		add(getMntmUpload());
		add(getMnGroups());
		add(getMnAdvanced());
		add(getMntmExit());
	}

	private JMenuItem getConnectItem() {
		if (connectItem == null) {
			connectItem = new JMenuItem("Connect");
			connectItem.setAction(controller.loginAction);
		}
		return connectItem;
	}


	private JMenuItem getLogoutItem() {
		if (logoutItem == null) {
			logoutItem = new JMenuItem("Logout");
			logoutItem.setAction(controller.logoutAction);
		}
		return logoutItem;
	}

	private JMenu getMnAdvanced() {
		if (mnAdvanced == null) {
			mnAdvanced = new JMenu("Advanced");
			mnAdvanced.add(getMntmSettings());
		}
		return mnAdvanced;
	}

	private JMenu getMnGroups() {
		if (mnGroups == null) {
			mnGroups = new JMenu("Groups");
		}
		return mnGroups;
	}

	private JMenuItem getMntmExit() {
		if (mntmExit == null) {
			mntmExit = new JMenuItem("Exit");
			mntmExit.setAction(controller.stopAction);
		}
		return mntmExit;
	}

	private JMenuItem getMntmSettings() {
		if (mntmSettings == null) {
			mntmSettings = new JMenuItem("Settings");
		}
		return mntmSettings;
	}

	private JMenuItem getMntmUpload() {
		if (mntmUpload == null) {
			mntmUpload = new JMenuItem();
			mntmUpload.setAction(new MyProxyDialogAction());
		}
		return mntmUpload;
	}

	private MiniCredInfoPanel getPanel() {
		if (panel == null) {
			panel = new MiniCredInfoPanel();
			panel.setSessionController(controller);
		}
		return panel;
	}

	private void populateGroups() {

		for ( String g : controller.getSessionManagement().groups() ) {
			GroupMenu mi = new GroupMenu(controller, g);
			getMnGroups().add(mi);
		}

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		if ("online".equals(evt.getPropertyName())) {
			boolean online = (Boolean) evt.getNewValue();
			if (!online) {
				getMntmUpload().setEnabled(false);
				getLogoutItem().setEnabled(false);
			} else {
				populateGroups();
				getMntmUpload().setEnabled(true);
				getLogoutItem().setEnabled(true);
			}
		}

	}
}
