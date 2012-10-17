package grith.gridsession.view;

import grith.jgrith.cred.AbstractCred.PROPERTY;

import java.util.Map;

import javax.swing.JPanel;

public abstract class CredPanel extends JPanel {

	abstract public Map<PROPERTY, Object> createCredConfig();

	abstract public String getCredTitle();

	abstract public void lockUI(boolean lock);

}
