package grith.gridsession.view.tray.group;

import grith.gridsession.SessionClient;
import grith.gridsession.view.tray.GridSessionController;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class GroupMyProxyDialog extends JDialog {

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			SessionClient s = new SessionClient(false, false);
			GridSessionController gsc = new GridSessionController(s);
			gsc.init();

			GroupMyProxyDialog dialog = new GroupMyProxyDialog(gsc);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final JPanel contentPanel = new JPanel();
	private final GroupMyProxyPanel groupMyProxyPanel;

	/**
	 * Create the dialog.
	 */
	public GroupMyProxyDialog(GridSessionController gsc) {
		super();
		setModal(true);
		setBounds(100, 100, 645, 293);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			groupMyProxyPanel = new GroupMyProxyPanel(gsc);
			contentPanel.add(groupMyProxyPanel, BorderLayout.CENTER);
		}
	}

	public void setGroup(String group) {
		groupMyProxyPanel.setGroup(group);
	}

}
