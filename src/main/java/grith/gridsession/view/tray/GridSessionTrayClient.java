package grith.gridsession.view.tray;

import grith.gridsession.GridClient;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public class GridSessionTrayClient extends GridClient implements
PropertyChangeListener {

	public static void main(String[] args) throws Exception {

		// System.setProperty(
		// CommonGridProperties.Property.DAEMONIZE_GRID_SESSION.toString(),
		// "false");
		GridSessionTrayClient c = new GridSessionTrayClient();

	}

	private final Image onlineIcon;
	private final Image offlineIcon;
	private final Image loadingIcon;

	private final GridSessionMenu popup;
	private JXTrayIcon trayIcon;
	private final SystemTray tray = SystemTray.getSystemTray();

	private final GridSessionController controller;

	public GridSessionTrayClient() throws Exception {
		super();

		controller = new GridSessionController(this);
		popup = new GridSessionMenu(controller);
		loadingIcon = ImageIO.read(GridSessionTrayClient.class.getClassLoader()
				.getResourceAsStream("icons/connecting.png"));
		onlineIcon = ImageIO.read(GridSessionTrayClient.class.getClassLoader()
				.getResourceAsStream("icons/online.png"));
		offlineIcon = ImageIO.read(GridSessionTrayClient.class.getClassLoader()
				.getResourceAsStream("icons/offline.png"));
		trayIcon = new JXTrayIcon(loadingIcon);
		trayIcon.setJPopupMenu(popup);

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					tray.add(trayIcon);
				} catch (AWTException e) {
					System.err.println("Can't add to tray");
				}
			}
		};
		SwingUtilities.invokeLater(t);

		controller.addPropertyChangeListener(this);
		controller.init();

	}

	public void propertyChange(PropertyChangeEvent evt) {

		if ( "online".equals(evt.getPropertyName()) ) {
			boolean value = (Boolean) evt.getNewValue();
			setOnline(value);
		}

	}

	public void setOnline(boolean online) {
		final Image icon;
		if (online) {
			icon = onlineIcon;
		} else {
			icon = offlineIcon;
		}
		Thread t = new Thread() {
			@Override
			public void run() {
				trayIcon.setImage(icon);
			}
		};
		SwingUtilities.invokeLater(t);
	}


}
