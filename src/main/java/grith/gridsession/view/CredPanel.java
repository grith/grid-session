package grith.gridsession.view;

import grith.jgrith.credential.Credential.PROPERTY;

import java.util.Map;

import javax.swing.JPanel;

public abstract class CredPanel extends JPanel {

	abstract public Map<PROPERTY, Object> createCredConfig();

	abstract public String getCredTitle();

}
