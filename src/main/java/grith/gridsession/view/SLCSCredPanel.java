package grith.gridsession.view;

import grisu.jcommons.configuration.CommonGridProperties;
import grisu.jcommons.constants.Enums.LoginType;
import grith.jgrith.control.SlcsLoginWrapper;
import grith.jgrith.credential.Credential.PROPERTY;

import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang.StringUtils;
import org.python.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class SLCSCredPanel extends CredPanel {

	static final Logger myLogger = LoggerFactory.getLogger(SLCSCredPanel.class
			.getName());

	{
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					myLogger.debug("Preloading idps...");
					SlcsLoginWrapper.getAllIdps();
				} catch (final Throwable e) {
					myLogger.error(e.getLocalizedMessage(), e);
				}
			}
		};
	}

	private JPanel panel;
	private JComboBox comboBox;

	private DefaultComboBoxModel idpModel = new DefaultComboBoxModel();
	private JPanel panel_1;
	private JTextField unField;
	private JPanel panel_2;
	private JPasswordField passwordField;

	/**
	 * Create the panel.
	 */
	public SLCSCredPanel() {
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				RowSpec.decode("max(35dlu;min)"),
				FormFactory.RELATED_GAP_ROWSPEC, FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC, }));
		add(getPanel(), "2, 2, fill, fill");
		add(getPanel_1(), "2, 4, fill, fill");
		add(getPanel_2(), "2, 6, fill, fill");
		loadIdpList();
	}

	@Override
	public Map<PROPERTY, Object> createCredConfig() {

		Map<PROPERTY, Object> config = Maps.newHashMap();
		config.put(PROPERTY.LoginType, LoginType.SHIBBOLETH);
		config.put(PROPERTY.IdP, idpModel.getSelectedItem());
		config.put(PROPERTY.Username, getUnField().getText());
		config.put(PROPERTY.Password, getPasswordField().getPassword());

		return config;

	}

	private JComboBox getComboBox() {
		if (comboBox == null) {
			comboBox = new JComboBox(idpModel);
			final String lastIdp = CommonGridProperties.getDefault()
					.getLastShibIdp();
			if (StringUtils.isNotBlank(lastIdp)) {
				idpModel.addElement(lastIdp);
			}
		}
		return comboBox;
	}

	@Override
	public String getCredTitle() {
		return "Institution login";
	}

	private JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			panel.setBorder(new TitledBorder(null, "Institution",
					TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC, }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC, }));
			panel.add(getComboBox(), "2, 2, fill, default");
		}
		return panel;
	}

	private JPanel getPanel_1() {
		if (panel_1 == null) {
			panel_1 = new JPanel();
			panel_1.setBorder(new TitledBorder(null, "Institution username", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel_1.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC,},
					new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,}));
			panel_1.add(getUnField(), "2, 2, fill, default");
		}
		return panel_1;
	}

	private JPanel getPanel_2() {
		if (panel_2 == null) {
			panel_2 = new JPanel();
			panel_2.setBorder(new TitledBorder(null, "Institution password", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel_2.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC,},
					new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,}));
			panel_2.add(getPasswordField(), "2, 2, fill, default");
		}
		return panel_2;
	}

	private JPasswordField getPasswordField() {
		if (passwordField == null) {
			passwordField = new JPasswordField();
		}
		return passwordField;
	}

	private JTextField getUnField() {
		if (unField == null) {
			unField = new JTextField();
			unField.setColumns(10);
			final String lastUn = CommonGridProperties.getDefault()
					.getLastShibUsername();
			if (StringUtils.isNotBlank(lastUn)) {
				unField.setText(lastUn);
			}
		}
		return unField;
	}

	private void loadIdpList() {



		final Thread loadThread = new Thread() {

			@Override
			public void run() {

				List<String> allIdps = null;
				try {
					allIdps = SlcsLoginWrapper.getAllIdps();
				} catch (Throwable e) {
					e.printStackTrace();
					return;
				}

				String currentIdp = (String) idpModel.getSelectedItem();

				idpModel.removeAllElements();

				for (String idp : allIdps) {
					idpModel.addElement(idp);
				}

				final String lastIdp;
				if (StringUtils.isNotBlank(currentIdp)) {
					lastIdp = currentIdp;
				} else {
					lastIdp = CommonGridProperties.getDefault()
							.getLastShibIdp();
				}

				SwingUtilities.invokeLater(new Thread() {

					@Override
					public void run() {

						if (idpModel.getIndexOf(lastIdp) >= 0) {
							idpModel.setSelectedItem(lastIdp);
						}


						getComboBox().setEnabled(true);
					}

				});

			}

		};

		loadThread.start();

	}
}
