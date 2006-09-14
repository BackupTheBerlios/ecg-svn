/*
 * Class: FileSystemTargetModule
 * Version: 1.0
 * Date: 16.10.2005
 * By: Frank@Schlesinger.com
 */

package org.electrocodeogram.module.target.implementation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.logging.LogHelper;
import org.electrocodeogram.modulepackage.ModuleProperty;
import org.electrocodeogram.modulepackage.ModulePropertyException;
import org.electrocodeogram.module.target.TargetModule;
import org.electrocodeogram.module.target.TargetModuleException;

/**
 * This class is an ECG module used to write ECG events into a file in
 * the file system.
 */
public class DatabaseTargetModule extends TargetModule {

    public static final String MSDT_FOLDER = "../ECG_Lab/msdt/";

    /**
     * This is the logger.
     */
    private static Logger logger = LogHelper
        .createLogger(DatabaseTargetModule.class.getName());

    /**
     * The JDBC database connection string of the form jdbc:dbms://host:port/database.
     */
    private String jdbcConnection;

    /**
     * The user name for the database.
     */
    private String username;

    /**
     * The password for the user.
     */
    private String password;

    /**
     * This creates the module instance. It is called by the ECG
     * <em>ModuleRegistry</em> subsystem, when the user requested a
     * new instance of this module.
     * @param id
     *            This is the unique <code>String</code> id of the
     *            module
     * @param name
     *            This is the name which is assigned to the module
     *            instance
     */
    public DatabaseTargetModule(final String id, final String name) {
        super(id, name);

        logger.entering(this.getClass().getName(), "DatabaseTargetModule",
            new Object[] {id, name});

        logger.exiting(this.getClass().getName(), "DatabaseTargetModule");

    }

    /**
     * @see org.electrocodeogram.module.target.TargetModule#write(org.electrocodeogram.event.ValidEventPacket)
     */
    @Override
    public final void write(final ValidEventPacket packet) {

        logger.entering(this.getClass().getName(), "write",
            new Object[] {packet});
        
        DBCommunicator.insertEvent(packet);

        logger.log(Level.INFO, "An event has been written to the database "
                               + this.jdbcConnection
                               + " by the module " + this.getName());

        logger.exiting(this.getClass().getName(), "write");

    }

    /**
     * @see org.electrocodeogram.module.Module#propertyChanged(org.electrocodeogram.modulepackage.ModuleProperty)
     */
    @Override
    public final void propertyChanged(final ModuleProperty moduleProperty)
        throws ModulePropertyException {

        logger.entering(this.getClass().getName(), "propertyChanged",
            new Object[] {moduleProperty});

        if (moduleProperty.getName().equals("JDBC Connection")) {

            logger.log(Level.INFO, "Request to set the property: "
                                   + moduleProperty.getName());

            if (moduleProperty.getValue() == null) {
                logger.log(Level.WARNING, "The property value is null for: "
                                          + moduleProperty.getName());

                logger.exiting(this.getClass().getName(), "propertyChanged");

                throw new ModulePropertyException(
                    "The property value is null.", this.getName(),
                    this.getId(), moduleProperty.getName(), moduleProperty
                        .getValue());
            }
            
            this.jdbcConnection = moduleProperty.getValue();

            // TODO reconnect to database here

            logger.log(Level.INFO, "Set the property: "
                                   + moduleProperty.getName() + " to "
                                   + this.jdbcConnection);

        } else if (moduleProperty.getName().equals("Username")) {
            logger.log(Level.INFO, "Request to set the property: "
                                   + moduleProperty.getName());

            if (moduleProperty.getValue() == null) {
                logger.log(Level.WARNING, "The property value is null for: "
                                          + moduleProperty.getName());

                logger.exiting(this.getClass().getName(), "propertyChanged");

                throw new ModulePropertyException(
                    "The property value is null.", this.getName(),
                    this.getId(), moduleProperty.getName(), moduleProperty
                        .getValue());
            }
            
            // TODO reconnect to database here

            this.username  = moduleProperty.getValue(); 

        } else if (moduleProperty.getName().equals("Password")) {
            logger.log(Level.INFO, "Request to set the property: "
                                   + moduleProperty.getName());

            if (moduleProperty.getValue() == null) {
                logger.log(Level.WARNING, "The property value is null for: "
                                          + moduleProperty.getName());

                logger.exiting(this.getClass().getName(), "propertyChanged");

                throw new ModulePropertyException(
                    "The property value is null.", this.getName(),
                    this.getId(), moduleProperty.getName(), moduleProperty
                        .getValue());
            }
            
            // TODO reconnect to database here

            this.password  = moduleProperty.getValue(); 


        } else {
            logger.log(Level.WARNING,
                "The module does not support a property with the given name: "
                                + moduleProperty.getName());

            logger.exiting(this.getClass().getName(), "propertyChanged");

            throw new ModulePropertyException(
                "The module does not support this property.", this.getName(),
                this.getId(), moduleProperty.getName(), moduleProperty
                    .getValue());

        }

        logger.exiting(this.getClass().getName(), "propertyChanged");
    }

    /**
     * @see org.electrocodeogram.module.Module#update() This method is
     *      not implemented in this module, as this module does not
     *      need to be informed about ECG Lab subsystem's state
     *      changes.
     */
    public void update() {

    // not implemented
    }

    /**
     * @see org.electrocodeogram.module.Module#initialize()
     */
    public final void initialize() {
        // not implemented
    }

    /**
     * @see org.electrocodeogram.module.target.TargetModule#startWriter()
     */
    public final void startWriter() throws TargetModuleException {

        logger.entering(this.getClass().getName(), "startWriter");

        // connect to database here
        
        connectDatabase();
        
        logger.exiting(this.getClass().getName(), "startWriter");

    }

    /**
     * @see org.electrocodeogram.module.target.TargetModule#stopWriter()
     *      This method is not implemented in this module.
     */
    @Override
    public void stopWriter() {
        
        logger.entering(this.getClass().getName(), "stopWriter");

        // disconnect to database here
        
        disconnectDatabase();
        
        logger.exiting(this.getClass().getName(), "stopWriter");

    }

    /**
     * 
     */
    private void connectDatabase() {
        /**
         * 1. for each schema in ./msdt: a. create Proxy b.
         * getSchemaProperties(String) --> Vector with Table instances c. for
         * each table of the schema --> synchronizeTableToDatabase
         */

        File msdtFolder = new File(MSDT_FOLDER);
        if (msdtFolder.exists()) {
            // TODO
            System.out.println("Ordner msdt exisitert");
        }
        String[] msdtNames = msdtDirectory.getSchemes(msdtFolder);
        if (!(DBCommunicator.tableExists("commondata"))) DBCommunicator
                .executeStmt(CreateSQL.createCommonDataTable());

        for (int i = 0; i < msdtNames.length; i++) {
            if (msdtNames[i].equalsIgnoreCase("msdt.common.xsd")) continue;
            if (msdtNames[i].contains("msdt")) {
                XMLSchemaProxy schemaProxy = new XMLSchemaProxy(msdtNames[i]);
                schemaProxy.synchronizeSchemaToDatabase();
            }

        }        
    }

    /**
     * 
     */
    private void disconnectDatabase() {
        // TODO Auto-generated method stub
        
    }
}
