/*
 * Class: MSDTFilterIntermediateModule
 * Version: 1.0
 * Date: 16.10.2005
 * By: Frank@Schlesinger.com
 */

package org.electrocodeogram.module.intermediate.implementation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.logging.LogHelper;
import org.electrocodeogram.modulepackage.ModuleProperty;
import org.electrocodeogram.module.UIModule;
import org.electrocodeogram.module.event.MessageEvent;
import org.electrocodeogram.module.intermediate.IntermediateModule;
import org.electrocodeogram.msdt.MicroSensorDataType;
import org.electrocodeogram.system.ModuleSystem;

/**
 * This module is filtering events depending on the event's <em>MicroSensorDataType</em>.
 * It provides a GUI dialog where the user can choose, which <em>MicroSensorDataTypes</em>
 * are to be filtered.
 */
public class MSDTFilterIntermediateModule extends IntermediateModule implements UIModule {

    /**
     * This is the logger.
     */
    private static Logger logger = LogHelper
        .createLogger(MSDTFilterIntermediateModule.class.getName());

    /**
     * This is a map of <em>MicroSensorDataTypes</em>, which are filtered.
     */
    private HashMap < MicroSensorDataType, Boolean > msdtFilterMap;

    /**
     * A reference to the GUi dialog.
     */
    //private JDialog dlgFilterConfiguration;

    /**
     * This is a map containing the checkboxes from the user dialog.
     */
    private HashMap < MicroSensorDataType, JCheckBox > chkMsdtSelection;

    /**
     * The panel with the checkboxes.
     */
    private JPanel pnlCheckBoxes;

    /**
     * The main panel of the dialog.
     */
    private JPanel pnlMain;

    /**
     * This creates the module instance. It is not to be
     * called by developers, instead it is called from the <em>ECG
     * ModuleRegistry</em> subsystem, when the user requested a new instance of this
     * module.
     * @param id
     *            This is the unique <code>String</code> id of the module
     * @param name
     *            This is the name which is assigned to the module
     *            instance
     */
    public MSDTFilterIntermediateModule(final String id, final String name) {
        super(id, name);

        logger.entering(this.getClass().getName(),
            "MSDTFilterIntermediateModule", new Object[] {id, name});

        logger.exiting(this.getClass().getName(),
            "MSDTFilterIntermediateModule");

    }

    /**
     * @see org.electrocodeogram.module.intermediate.IntermediateModule#analyse(org.electrocodeogram.event.ValidEventPacket)
     */
    @Override
    public final ValidEventPacket analyse(final ValidEventPacket packet) {

        logger.entering(this.getClass().getName(),
            "MSDTFilterIntermediateModule", new Object[] {packet});

        if (this.msdtFilterMap.containsKey(packet.getMicroSensorDataType())) {
            if (this.msdtFilterMap.get(packet.getMicroSensorDataType()).equals(
                new Boolean(true))) {

                logger.log(Level.FINE, "The event is passing the filter.");

                logger.exiting(this.getClass().getName(), "packet");

                return packet;
            }
        }

        logger.log(Level.FINE, "The event is filtered out.");

        logger.exiting(this.getClass().getName(), null);

        return null;

    }

    /**
     * @see org.electrocodeogram.module.Module#propertyChanged(org.electrocodeogram.modulepackage.ModuleProperty)
     */
    @Override
    public final void propertyChanged(final ModuleProperty moduleProperty) {

        logger.entering(this.getClass().getName(), "propertyChanged",
            new Object[] {moduleProperty});

        if (moduleProperty.getValue().equals("configureFilter")) {
            configureFilter();
        }

        logger.exiting(this.getClass().getName(), "propertyChanged");
    }

    /**
     * @see org.electrocodeogram.module.Module#update()
     */
    @Override
    public final void update() {

        logger.entering(this.getClass().getName(), "update");

        setFilterMap();

        logger.exiting(this.getClass().getName(), "update");

    }

//    /**
//     * Returns the user dialog.
//     * @return The user dialog
//     */
//    final JDialog getDialog() {
//
//        logger.entering(this.getClass().getName(), "getDialog");
//
//        logger.exiting(this.getClass().getName(), "getDialog",
//            this.dlgFilterConfiguration);
//
//        return this.dlgFilterConfiguration;
//    }

    /**
     * This method fills the {@link #msdtFilterMap}
     * with the currently registered <em>MicroSensorDataTypes</em>.
     */
    private void setFilterMap() {

        logger.entering(this.getClass().getName(), "setFilterMap");

        this.msdtFilterMap = new HashMap < MicroSensorDataType, Boolean >();

        MicroSensorDataType[] msdts = ModuleSystem.getInstance()
            .getMicroSensorDataTypes();

        for (MicroSensorDataType msdt : msdts) {

            this.msdtFilterMap.put(msdt, new Boolean(true));

        }

        logger.exiting(this.getClass().getName(), "setFilterMap");
    }

    /**
     * @see org.electrocodeogram.module.intermediate.IntermediateModule#initialize()
     */
    @Override
    public final void initialize() {

        logger.entering(this.getClass().getName(), "initialize");

        this.msdtFilterMap = new HashMap < MicroSensorDataType, Boolean >();

        setFilterMap();

        this.setProcessingMode(ProcessingMode.FILTER);

        logger.exiting(this.getClass().getName(), "initialize");
    }

    /**
     * This method is used to configure this filter module.
     * It creates and displays a user dialog to set the filter rules.
     */
    public final void configureFilter() {

        logger.entering(this.getClass().getName(), "configureFilter");

        if (this.msdtFilterMap.size() == 0) {

            MessageEvent event = new MessageEvent(
                "There are no MicroSensorDataTypes loaded yet. Please add at least one SourceModule to load the predefined MicroSensorDataTypes.",
                MessageEvent.MessageType.INFO, getName(), getId());

            getGuiNotifiator().fireMessageNotification(event);

            logger.exiting(this.getClass().getName(), "configureFilter");

            return;
        }



        logger.exiting(this.getClass().getName(), "configureFilter");
    }

    /**
     * This method updates the filter rules according to the current user selection.
     *
     */
    final void updateMsdtFilterMap() {

        logger.entering(this.getClass().getName(), "updateMsdtFilterMap");

        for (MicroSensorDataType msdt : this.chkMsdtSelection.keySet()) {
            JCheckBox chkBox = this.chkMsdtSelection.get(msdt);

            this.msdtFilterMap.remove(msdt);

            if (chkBox.isSelected()) {
                this.msdtFilterMap.put(msdt, new Boolean(false));
            } else {
                this.msdtFilterMap.put(msdt, new Boolean(true));
            }
        }

        logger.exiting(this.getClass().getName(), "updateMsdtFilterMap");
    }

    /**
     * Creates and displays a checkbox for every <em>MicroSensorDataType</em>.
     *
     */
    final void initializeCheckBoxes() {

        logger.entering(this.getClass().getName(), "initializeCheckBoxes");

        this.chkMsdtSelection = new HashMap < MicroSensorDataType, JCheckBox >();

        for (MicroSensorDataType msdt : this.msdtFilterMap.keySet()) {
            if (this.msdtFilterMap.get(msdt).equals(new Boolean(true))) {
                this.chkMsdtSelection.put(msdt, new JCheckBox(msdt.getName(),
                    false));
            } else {
                this.chkMsdtSelection.put(msdt, new JCheckBox(msdt.getName(),
                    true));
            }

        }

        logger.exiting(this.getClass().getName(), "initializeCheckBoxes");
    }

    /**
     * Resets the selections of the checkboxes to the current filter rules.
     *
     */
    final void refreshCheckBoxes() {

        logger.entering(this.getClass().getName(), "refreshCheckBoxes");

        for (MicroSensorDataType msdt : this.msdtFilterMap.keySet()) {
            if (this.msdtFilterMap.get(msdt).equals(new Boolean(true))) {
                this.chkMsdtSelection.get(msdt).setSelected(false);
            } else {
                this.chkMsdtSelection.get(msdt).setSelected(true);
            }

        }

        logger.exiting(this.getClass().getName(), "refreshCheckBoxes");
    }

    /**
     * Creates and returns the panel with the checkboxes.
     * @return The panel with the checkboxes
     */
    final JPanel createCheckBoxPanel() {

        logger.entering(this.getClass().getName(), "createCheckBoxPanel");
        initializeCheckBoxes();
        this.pnlCheckBoxes = new JPanel();

        JPanel pnlLeft = new JPanel();

        pnlLeft.setLayout(new BoxLayout(pnlLeft, BoxLayout.Y_AXIS));

        JPanel pnlRight = new JPanel();

        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));

        boolean left = true;

        for (JCheckBox chkMsdt : this.chkMsdtSelection.values()) {
            if (left) {
                pnlLeft.add(chkMsdt);

                left = false;
            } else {
                pnlRight.add(chkMsdt);

                left = true;
            }

        }

        this.pnlCheckBoxes.add(pnlLeft);

        this.pnlCheckBoxes.add(pnlRight);

        logger.exiting(this.getClass().getName(), "createCheckBoxPanel",
            this.pnlCheckBoxes);

        return this.pnlCheckBoxes;
    }

    /**
     * Creates and returns the main panel of the dialog.
     * @return The main panel of the dialog
     */
    final JPanel createMainPanel() {

        logger.entering(this.getClass().getName(), "createMainPanel");

        this.pnlMain = new JPanel();

        this.pnlMain.setLayout(new BorderLayout());

        this.pnlMain.add(new JLabel(
            "Select the MicroSensorDataTypes that shall be filtered out"),
            BorderLayout.NORTH);

        this.pnlMain.add(createCheckBoxPanel(), BorderLayout.CENTER);

        this.pnlMain.add(getButtonPanel(), BorderLayout.SOUTH);

        logger.exiting(this.getClass().getName(), "createMainPanel",
            this.pnlMain);

        return this.pnlMain;
    }

    /**
     * Creates and returns the panel with the buttons.
     * @return The panel with the buttons
     */
    private Component getButtonPanel() {

        logger.entering(this.getClass().getName(), "getButtonPanel");

        JButton btnOK = new JButton("OK");

        btnOK.addActionListener(new ActionListener() {

            public void actionPerformed(@SuppressWarnings("unused")
            final ActionEvent e) {
                updateMsdtFilterMap();

                //getDialog().dispose();
                

            }
        });

        JButton btnCancel = new JButton("Cancel");

        btnCancel.addActionListener(new ActionListener() {

            public void actionPerformed(@SuppressWarnings("unused")
            final ActionEvent e) {
                //getDialog().dispose();

            }
        });

        JButton btnClearAll = new JButton("Clear all");

        btnClearAll.addActionListener(new ActionListener() {

            @SuppressWarnings( {"synthetic-access", "unqualified-field-access"})
            public void actionPerformed(@SuppressWarnings("unused")
            final ActionEvent e) {
                for (JCheckBox chkMsdt : chkMsdtSelection.values()) {
                    chkMsdt.setSelected(false);
                }

            }
        });

        JButton btnRestore = new JButton("Restore");

        btnRestore.addActionListener(new ActionListener() {

            public void actionPerformed(@SuppressWarnings("unused")
            final ActionEvent e) {
                refreshCheckBoxes();

            }

        });

        JPanel pnlButtons = new JPanel();

        pnlButtons.add(btnRestore);

        pnlButtons.add(btnClearAll);

        pnlButtons.add(btnCancel);

        pnlButtons.add(btnOK);

        logger.exiting(this.getClass().getName(), "getButtonPanel", pnlButtons);

        return pnlButtons;
    }

    /**
     * @see org.electrocodeogram.module.UIModule#getPanelName()
     */
    public final String getPanelName() {

        return "Configure Filter";
    }

    /**
     * @see org.electrocodeogram.module.UIModule#getPanel()
     */
    public final JPanel getPanel() {

        this.pnlMain = createMainPanel(); 
        
        return this.pnlMain;
    }

}
