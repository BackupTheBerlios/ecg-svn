package org.electrocodeogram.module.source.implementation;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import org.electrocodeogram.module.source.SourceModule;
import org.electrocodeogram.module.target.implementation.DBCommunicator;
import org.electrocodeogram.module.source.implementation.Event;

/**
 * @author jule This window displays the ResultSet of an Query in a Table
 * 
 */
public class ResultWindow extends JFrame {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JScrollPane scrollpane;

    /**
     * the Panel to hold the Result Table
     */
    private JPanel tablePanel;

    /**
     * the Panel to hold all Buttons
     */
    private JPanel buttonPanel;

    /**
     * Label for displaying messages
     */
    private JLabel msgLabel;

    /**
     * the table for displaying the Result of the Queries
     */
    private JTable resultTable;

    /**
     * the putton to press if one ore mode ValidEventPacket have to be created
     * from the Events in the ResultSet
     */
    private JButton createPacket;

    /**
     * the Button to press if the user wants to see all details of an Event in
     * the ResultSet
     */
    private JButton showCompleteEvent;

    /**
     * the Button to press if the user wants to execute the query for selecting
     * all events between two other events
     */
    private JButton getEventsBetween;

    private int buttonOrNot;

    /**
     * the DBCommunicator for direct Communication with the database
     */
    private DBCommunicator dbCom;

    /**
     * the source Module
     */
    SourceModule mysourceModule;
    
    /**
     * the Components are placed on the Content Panel of the JFrame  
     */
    Container contentPane;
    
    

    /**
     * the constructor
     * 
     */
    public ResultWindow(JTable table, int button,
            DBCommunicator dbCommunicator, SourceModule sourceModule) {
        this.setTitle("Result window");
        this.dbCom = dbCommunicator;
        this.mysourceModule = sourceModule;
        this.buttonOrNot = button;
        contentPane = this.getContentPane();
        this.resultTable = table;
        setSize(750, 500);
        init();
        setVisible(true);
    }

    private void init() {
        // Panel holding the Buttons
        buttonPanel = new JPanel();
        tablePanel = new JPanel();
        msgLabel = new JLabel(); // Displays messages
        // properies for the result Table
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        resultTable.setAutoscrolls(true);
        resultTable.setPreferredScrollableViewportSize(new Dimension(700, 400));
        // the Button for the create Packet function. with this button you can
        // create new ValidEventPackets from the selected Events in the result
        // Table
        createPacket = new JButton("create ValidEventPackets");
        showCompleteEvent = new JButton("get Event Details");
        // the result table for representing the Event results
        scrollpane = new JScrollPane(resultTable);
        buttonPanel.add(createPacket);
        buttonPanel.add(showCompleteEvent);
        if (this.buttonOrNot == 1) {
            resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            getEventsBetween = new JButton("EventsBetwennRuns");
            buttonPanel.add(getEventsBetween);
        }
        buttonPanel.add(msgLabel);
        tablePanel.add(scrollpane);
        
        contentPane.setLayout(new BorderLayout());
        contentPane.add(tablePanel, BorderLayout.NORTH);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
        // Action Listener for the create Packet Button
        ActionListener createPacketListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                createEventPackets();
            }
        };
        ActionListener showEventDetailsListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getSelectedEvents();
            }
        };
        // Action Listener for the create Packet Button
        ActionListener getEventsBetweenRunsListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int[] selectedRowNumbers = resultTable.getSelectedRows();
                int selectedRow = selectedRowNumbers[0];
                String run1_TS = resultTable.getValueAt(selectedRow, 1)
                        .toString();
                String run2_TS = resultTable.getValueAt(selectedRow, 3)
                        .toString();
                CachedRowSet eventsBetween;
                try {
                    eventsBetween = DBQueries
                            .getEventsBetweenTimestamps(run1_TS, run2_TS, dbCom);
                } catch (SQLException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                    return;
                }
                final JTable newtable = new JTable();
                // It may take a while to get the results, so give the user some
                // immediate feedback that their query was accepted.
                msgLabel.setText("Contacting database...");
                // In order to allow the feedback message to be displayed, we
                // don't
                // run the query directly, but instead place it on the event
                // queue
                // to be run after all pending events and redisplays are done.
                try {
                    newtable
                            .setModel(new TableModelFromResultSet(eventsBetween));
                }
                catch (SQLException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                new ResultWindow(newtable, 0, dbCom, mysourceModule);
            }
        };
        /**
         * add the ActionListeners to the JButtons
         */
        createPacket.addActionListener(createPacketListener);
        showCompleteEvent.addActionListener(showEventDetailsListener);
        if (this.buttonOrNot == 1) {
            getEventsBetween.addActionListener(getEventsBetweenRunsListener);
        }
    } // END init

    private void getSelectedEvents() {
        int[] selectedRowNumbers = resultTable.getSelectedRows();
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        TableColumn column = null;
        try {
            column = resultTable.getColumn("linkID");
        }
        catch (IllegalArgumentException e) {
            JOptionPane
                    .showMessageDialog(ResultWindow.this,
                            "To create an EventPacket there must be a row 'linkid' in your ResultSet");
            return;
        }
        int idIndex = column.getModelIndex();
        // for each selected Event...
        for (int i = 0; i < selectedRowNumbers.length; i++) {
            int selectedRow = selectedRowNumbers[i];
            // ...get the id
            String eventID = resultTable.getValueAt(selectedRow, idIndex)
                    .toString();
            // get the event data from the database
            Event eventMap;
            try {
                eventMap = DBQueries.getEventByID(eventID, dbCom);
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return;
            }
            eventMap.printEventToConsole();
            String eventData = eventMap.eventDataToString();
            JTextArea ta = new JTextArea(eventData, 20, 30);
            panel.add(ta);
            
        }
        new ShowEventDetails(panel);
        
    }

    private void createEventPackets() {
        int[] selectedRowNumbers = resultTable.getSelectedRows();
        TableColumn column = null;
        try {
            column = resultTable.getColumn("linkID");
        }
        catch (IllegalArgumentException e) {
            JOptionPane
                    .showMessageDialog(ResultWindow.this,
                            "To create an EventPacket there must be a row 'linkid' in your ResultSet");
            return;
        }
        int idIndex = column.getModelIndex();
        // for each selected Event...
        Object[] a = new Object[selectedRowNumbers.length];
        for (int i = 0; i < selectedRowNumbers.length; i++) {
            int selectedRow = selectedRowNumbers[i];
            // ...get the id
            String eventID = resultTable.getValueAt(selectedRow, idIndex)
                    .toString();
            a[i] = (Object) eventID;
        }
        // ...create the EventPacket for the event with the given linkid value
        DatabaseReaderThread readerThread3 = new DatabaseReaderThread(
                this.mysourceModule, this.dbCom);
        readerThread3.setEventIDs(a);
    }
}
