package grith.gridsession.view.tray.group;

import grith.gridsession.view.tray.GridSessionController;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class GroupMenu extends JMenu {

	private class MyProxyDialogAction extends AbstractAction {

		public MyProxyDialogAction() {
			putValue(NAME, "MyProxy details");
			putValue(SHORT_DESCRIPTION, "Show MyProxy details dialog");
		}

		public void actionPerformed(ActionEvent e) {
			controller.showGroupMyProxyDialog(group);

		}

	}

	private final String group;
	private final GridSessionController controller;
	private UploadCredMenuItem pldcrdmntmUpload;
	private JMenuItem mntmMyproxyDetails;
	private final Action action;

	public GroupMenu(GridSessionController controller, String group) {
		super(group);
		this.group = group;
		this.controller = controller;
		this.action = new MyProxyDialogAction();
		add(getPldcrdmntmUpload());
		add(getMntmMyproxyDetails());
	}


	private JMenuItem getMntmMyproxyDetails() {
		if (mntmMyproxyDetails == null) {
			mntmMyproxyDetails = new JMenuItem("MyProxy details");
			mntmMyproxyDetails.setAction(action);
		}
		return mntmMyproxyDetails;
	}


	private UploadCredMenuItem getPldcrdmntmUpload() {
		if (pldcrdmntmUpload == null) {
			pldcrdmntmUpload = new UploadCredMenuItem(controller, group);
		}
		return pldcrdmntmUpload;
	}
}
