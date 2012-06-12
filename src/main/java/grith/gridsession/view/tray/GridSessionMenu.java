package grith.gridsession.view.tray;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class GridSessionMenu extends JPopupMenu implements
PropertyChangeListener {

	private class SwingAction extends AbstractAction {
		public SwingAction() {
			putValue(NAME, "SwingAction");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}
		public void actionPerformed(ActionEvent e) {
		}
	}
	private JMenuItem connectItem;
	private final GridSessionController controller;
	private MiniGridSessionInfoPanel panel;
	private JMenuItem logoutItem;

	private Action action;
	private JMenuItem mntmExit;

	public GridSessionMenu(GridSessionController controller) {
		this.controller = controller;
		this.controller.addPropertyChangeListener(this);
		add(getPanel());
		add(getConnectItem());
		add(getLogoutItem());
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
			logoutItem = new JMenuItem("New menu item");
			logoutItem.setAction(controller.logoutAction);
		}
		return logoutItem;
	}

	private JMenuItem getMntmExit() {
		if (mntmExit == null) {
			mntmExit = new JMenuItem("Exit");
			mntmExit.setAction(controller.stopAction);
		}
		return mntmExit;
	}

	private MiniGridSessionInfoPanel getPanel() {
		if (panel == null) {
			panel = new MiniGridSessionInfoPanel();
			panel.setSessionController(controller);
		}
		return panel;
	}

	public void propertyChange(PropertyChangeEvent evt) {

		if ("online".equals(evt.getPropertyName())) {
			boolean online = (Boolean) evt.getNewValue();
			if (!online) {
				getLogoutItem().setEnabled(false);
			} else {
				getLogoutItem().setEnabled(true);
			}
		}

	}

}
