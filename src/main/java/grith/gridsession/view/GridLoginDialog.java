package grith.gridsession.view;

import grith.gridsession.GridClient;
import grith.gridsession.SessionClient;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class GridLoginDialog extends JDialog implements PropertyChangeListener {

	private class GetCredAction extends AbstractAction {
		public GetCredAction() {
			putValue(NAME, "Login");
			putValue(SHORT_DESCRIPTION, "Login use selected login method");
		}
		public void actionPerformed(ActionEvent e) {
			credCreationPanel.createCredential();
		}
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			GridClient client = new GridClient();
			GridLoginDialog dialog = new GridLoginDialog(client);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private final JPanel contentPanel = new JPanel();

	private final Action loginAction = new GetCredAction();
	private final CredCreationPanel credCreationPanel = new CredCreationPanel();
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	private final SessionClient client;

	/**
	 * Create the dialog.
	 */
	public GridLoginDialog(SessionClient client) {
		super();
		setModal(true);
		this.client = client;
		credCreationPanel.setSessionClient(this.client);
		setBounds(100, 100, 590, 424);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			contentPanel.add(credCreationPanel, BorderLayout.CENTER);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{

				okButton.setAction(loginAction);
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		credCreationPanel.addPropertyChangeListener(this);
	}

	private void lockUI(boolean lock) {
		okButton.setEnabled(!lock);
		cancelButton.setEnabled(!lock);
	}


	public void propertyChange(PropertyChangeEvent evt) {

		if (evt.getSource() != credCreationPanel) {
			return;
		}

		if ("creatingCredential".equals(evt.getPropertyName())) {
			boolean creating = (Boolean) evt.getNewValue();
			lockUI(creating);
		}

		if ("credential".equals(evt.getPropertyName())) {
			System.out.println("NEW CREDENTIAL CREATED: "
					+ credCreationPanel.getCredential().getDN());
			dispose();
		}
	}


}
