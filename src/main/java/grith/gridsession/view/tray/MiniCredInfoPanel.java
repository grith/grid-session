package grith.gridsession.view.tray;

import grisu.jcommons.utils.WalltimeUtils;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class MiniCredInfoPanel extends JPanel implements
PropertyChangeListener {
	private JLabel lblNewLabel;
	private JLabel lblNewLabel_1;
	private JTextField statusfield;
	private JTextField idField;
	private JPanel panel;
	private JSeparator separator;

	private GridSessionController session = null;
	private JLabel lblTimeLeft;
	private JTextField timeLeftField;

	private String status = "n/a";
	private String id = "n/a";
	private String dn = "";
	private String timeleft = "n/a";

	/**
	 * Create the panel.
	 */
	public MiniCredInfoPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getPanel(), "2, 2, 5, 1, fill, fill");

	}

	private JTextField getIdField() {
		if (idField == null) {
			idField = new JTextField();
			idField.setText("n/a");
			idField.setHorizontalAlignment(SwingConstants.CENTER);
			idField.setEditable(false);
			idField.setColumns(10);
		}
		return idField;
	}

	private JLabel getLblNewLabel() {
		if (lblNewLabel == null) {
			lblNewLabel = new JLabel("Status:");
		}
		return lblNewLabel;
	}

	private JLabel getLblNewLabel_1() {
		if (lblNewLabel_1 == null) {
			lblNewLabel_1 = new JLabel("Identity:");
		}
		return lblNewLabel_1;
	}

	private JLabel getLblTimeLeft() {
		if (lblTimeLeft == null) {
			lblTimeLeft = new JLabel("Time left:");
		}
		return lblTimeLeft;
	}

	private JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			panel.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null,
					null, null, null));
			panel.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC,},
					new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,}));
			panel.add(getLblNewLabel(), "2, 2");
			panel.add(getStatusfield(), "4, 2");
			panel.add(getLblTimeLeft(), "2, 4, right, default");
			panel.add(getTimeLeftField(), "4, 4, fill, default");
			panel.add(getLblNewLabel_1(), "2, 6");
			panel.add(getSeparator(), "2, 8, 3, 1");
			panel.add(getIdField(), "2, 10, 3, 1");
		}
		return panel;
	}

	private JSeparator getSeparator() {
		if (separator == null) {
			separator = new JSeparator();
		}
		return separator;
	}

	private JTextField getStatusfield() {
		if (statusfield == null) {
			statusfield = new JTextField();
			statusfield.setText("n/a");
			statusfield.setHorizontalAlignment(SwingConstants.CENTER);
			statusfield.setEditable(false);
			statusfield.setColumns(10);
		}
		return statusfield;
	}

	private JTextField getTimeLeftField() {
		if (timeLeftField == null) {
			timeLeftField = new JTextField();
			timeLeftField.setHorizontalAlignment(SwingConstants.CENTER);
			timeLeftField.setText("n/a");
			timeLeftField.setEditable(false);
			timeLeftField.setColumns(10);
		}
		return timeLeftField;
	}

	public void propertyChange(PropertyChangeEvent evt) {

		if ( "online".equals(evt.getPropertyName()) ) {
			boolean value = (Boolean) evt.getNewValue();
			if ( value ) {
				status = "logged in";
				if (StringUtils.isBlank(dn) || "n/a".equals(dn)) {
					dn = session.getSessionManagement().dn();
				}
				id = dn.substring(dn.lastIndexOf('=') + 1);
			} else {
				status = "logged out";
				dn = "n/a";
				id = "n/a";
			}

			getStatusfield().setText(status);
			getIdField().setText(id);
			getIdField().setToolTipText(dn);
		} else if ("lifetime".equals(evt.getPropertyName())) {
			Integer lt = (Integer) evt.getNewValue();
			if (lt <= 0) {
				timeleft = "n/a";
			} else {
				timeleft = WalltimeUtils.convertSeconds(lt);
			}
			getTimeLeftField().setText(timeleft);
		}
	}

	public void setSessionController(GridSessionController c) {
		this.session = c;
		this.session.addPropertyChangeListener(this);

	}




}
