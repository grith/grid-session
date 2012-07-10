package grith.gridsession.view;

import grisu.jcommons.utils.WalltimeUtils;
import grith.jgrith.cred.Cred;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import com.google.common.collect.Maps;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class GridSessionCredPanel extends CredPanel {

	private static String generateHtml(Map<String, String> sessionProps) {

		final StringBuffer html = new StringBuffer(
				"<html><table width=\"100%\">");

		boolean alternate = true;
		for (final String key : sessionProps.keySet()) {
			if (alternate) {
				html.append("<tr bgcolor=\"#FFFFFF\"><td>");
			} else {
				html.append("<tr><td>");
			}
			html.append(key);
			html.append("</td><td>");
			html.append(sessionProps.get(key));
			html.append("</td></tr>");
			alternate = !alternate;
		}
		html.append("</table></html>");

		return html.toString();
	}

	private JPanel panel;
	private JScrollPane scrollPane;
	private JEditorPane propertiesPane;

	private Cred cred = null;

	/**
	 * Create the panel.
	 */
	public GridSessionCredPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getPanel(), "2, 2, fill, fill");

		setProperties();

	}

	@Override
	public Map<PROPERTY, Object> createCredConfig() {
		return Maps.newHashMap();
	}

	public Cred getCredential() {
		return cred;
	}

	@Override
	public String getCredTitle() {
		return "Use existing session";
	}

	private JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			panel.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					RowSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_ROWSPEC, }));
			panel.add(getScrollPane(), "2, 2, fill, fill");
			setBorderTitle("Session details");
		}
		return panel;
	}

	private JEditorPane getPropertiesPane() {
		if (propertiesPane == null) {
			propertiesPane = new JEditorPane();
			propertiesPane.setContentType("text/html");
			propertiesPane.setEditable(false);
		}
		return propertiesPane;
	}

	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane(getPropertiesPane());
		}
		return scrollPane;
	}

	@Override
	public void lockUI(boolean lock) {
		// nothing to do here...

	}

	private void setBorderTitle(String title) {
		panel.setBorder(new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null));
	}

	public void setCred(Cred cred) {
		this.cred = cred;
		setProperties();
	}

	private void setProperties() {

		String propText = null;
		if ((cred == null) || !cred.isValid()) {
			propText = "No existing session";
		} else {

			Map<String, String> props = new LinkedHashMap<String, String>();

			props.put("Logged in", "");
			props.put("", "");

			props.put("Identity", cred.getDN());
			String lifetime = WalltimeUtils.convertSeconds(cred
					.getRemainingLifetime());
			props.put("Remaining lifetime", lifetime);

			boolean autorenews = cred.isRenewable();
			if (autorenews) {
				props.put("Auto-refresh", "yes");
			} else {
				props.put("Auto-refresh",
						"no (to enable, re-login via one of the other methods)");
			}
			// props.put("Login type", cred.g)
			propText = generateHtml(props);
		}
		getPropertiesPane().setText(propText);



	}
}
