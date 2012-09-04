package grith.gridsession.view.tray.group;

import grith.gridsession.view.tray.GridSessionController;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;

public class UploadCredMenuItem extends JMenuItem {

	private class UploadAction extends AbstractAction {
		private final String group;

		public UploadAction(String group) {
			putValue(NAME, "Upload");
			putValue(SHORT_DESCRIPTION, "Login to the grid");
			this.group = group;
		}

		public void actionPerformed(ActionEvent e) {
			controller.getSessionManagement().upload(group);
			// controller.refreshStatus();
			controller.showGroupMyProxyDialog(group);
		}

	}

	private final GridSessionController controller;

	private final Action action;

	public UploadCredMenuItem(GridSessionController control, String group) {
		this.controller = control;
		this.action = new UploadAction(group);
		setAction(this.action);
	}

}
