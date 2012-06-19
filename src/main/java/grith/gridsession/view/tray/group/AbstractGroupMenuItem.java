package grith.gridsession.view.tray.group;

import grith.gridsession.view.tray.GridSessionController;

import javax.swing.JMenuItem;

public class AbstractGroupMenuItem extends JMenuItem {

	protected final String group;
	protected final GridSessionController controller;

	public AbstractGroupMenuItem(GridSessionController c, String group) {
		this.controller = c;
		this.group = group;
	}

}
