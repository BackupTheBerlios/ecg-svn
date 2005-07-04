package org.electrocodeogram.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.electrocodeogram.event.EventPacket;
import org.electrocodeogram.event.IllegalEventParameterException;
import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.module.ModuleRegistry;
import org.electrocodeogram.module.source.SourceModule;
import org.hackystat.kernel.admin.SensorProperties;
import org.hackystat.kernel.shell.SensorShell;

/**
 * This is the root class of the ECG Server & Lab component. It is also the entry point
 * for the event data that is recorded by all running ECG sensors
 * to be processed through the ECG Lab's modules for analysis and storage.
 *  
 * The SensorShellWrapper extends and uses the HackyStat SensorShell class to validate
 * incoming events as beeing events of a legal HackyStat SensorDataType.
 */
public class SensorShellWrapper extends SensorShell
{
    private Logger logger = null;
    
    private static SensorShellWrapper theInstance = null;

    private SourceModule sensorSource = null;

    private SensorServer sensorServer = null;

    private ModuleRegistry moduleRegistry = null;
    
    private int processingID = 0;

    /*
     * The private constructot creates one instance of the SensorShellWrapper and
     * also creates all other needed ECG Server & Lab components.
     */
    private SensorShellWrapper()
    {
        super(new SensorProperties("", ""), false, "ElectroCodeoGram");

        this.logger = Logger.getLogger("ECG Server");
        
        Console gob = new Console();

        // start the ECG server to listen for incoming events
        this.sensorServer = new SensorServer();

        //this.sensorServer.start();

        this.sensorSource = new SourceModule();

        this.sensorSource.setName("Sensor Source");

        
        // instanciate the ModuleRegistry to manage the modules
        this.moduleRegistry = ModuleRegistry.getInstance();

        this.moduleRegistry.addModuleInstance(this.sensorSource);

        gob.start();
        
        SensorShellWrapper.theInstance = this;

    }

    /*
     * If a File is given to the constructor as a parameter that File
     * is taken for the module-directory of the ECG Lab.
     * The filename can be provided at startup as a command line parameter.
     */
    private SensorShellWrapper(File file)
    {
        this();

        ModuleRegistry.getInstance(file);
    }

    /**
     * This method returns the singleton instance of the SensorShellWrapper object.
     * @return The singleton instance of the SensorShellWrapper object
     */
    public static SensorShellWrapper getInstance()
    {
        if (theInstance == null) {
            theInstance = new SensorShellWrapper();
        }

        return theInstance;
    }

    /**
     * This method quits the ECG Server & Lab application.
     * 
     */
    public void quit()
    {
        this.sensorServer.shutDown();
        
        System.exit(0);
    }
    
    /**
     * @see org.hackystat.kernel.shell.SensorShell#doCommand(java.util.Date, java.lang.String, java.util.List)
     * 
     * This is the overriden version of the HackyStat SensorShell's method. After calling the original
     * method it performs further steps to pass the event data into the ECG Lab. It is marked
     * as synchronized as it can be called by multiple running ServerThreads at the same time.
     */
    @Override
    public synchronized boolean doCommand(Date timeStamp, String commandName, List argList)
    {
        this.processingID++;
        
        this.logger.log(Level.INFO,this.processingID + ": Begin to process new event data at " +  new Date().toString());
        
        // validate the incoming event data by using the HackyStat framework
        boolean result = super.doCommand(timeStamp, commandName, argList);
        
        if (result) {

            this.logger.log(Level.INFO,this.processingID + ": Event data is conforming to a HackyStat SensorDataType and is processed.");
            
            List<String> newArgList = new ArrayList<String>(argList.size());

            Object[] entries = argList.toArray();

            for (int i = 0; i < entries.length; i++) {
                String entryString = (String) entries[i];

                if (commandName.equals("Activity") && i == 0) {
                    entryString = "HS_ACTIVITY_TYPE:" + entryString;
                }

                newArgList.add(entryString);
            }

            appendToEventSource(timeStamp, "HS_COMMAND:" + commandName, newArgList);

            return true;
        }
        
        this.logger.log(Level.INFO,this.processingID + ": Event data is not conforming to a HackyStat SensorDataType and is discarded.");
        
        this.logger.log(Level.INFO,this.processingID + ":" + new EventPacket(0,timeStamp,commandName,argList).toString());
        
        return false;

    }

    /*
     * This method is used to pass the event data to the first module.
     *
     */
    private void appendToEventSource(Date timeStamp, String commandName, List argList)
    {
        if (this.sensorSource != null) {
            ValidEventPacket toAppend;
            try {

                toAppend = new ValidEventPacket(0, timeStamp, commandName,
                        argList);
                
                this.sensorSource.append(toAppend);
            }
            catch (IllegalEventParameterException e) {

                // As only ValidEventPackets come this far, this should never happen
                
                e.printStackTrace();
                
                this.logger.log(Level.SEVERE,"An unexpected exception has occured. Please report this at www.electrocodeogram.org");
            }
        }
    }

    /*
     * This thread maintains the console of the ECG Server & Lab application.
s     * 
     */
    private class Console extends Thread
    {

        private BufferedReader bufferedReader = null;

        /**
         * Creates the console to manage the ECG Server & Lab.
         *
         */
        public Console()
        {
            System.out.println("ElectroCodeoGram Server & Lab is starting...");

            this.bufferedReader = new BufferedReader(new InputStreamReader(
                    System.in));

        }

        /**
         * Here the reading of the console-input is done.
         */
        @Override
        public void run()
        {

            while (true) {

                System.out.println(">>");

                String inputString = "" + this.readLine();

                System.out.println("Echo: " + inputString);

                if (inputString.equalsIgnoreCase("quit")) {
                    this.quit();
                    return;
                }
            }
        }

        private String readLine()
        {
            try {
                return this.bufferedReader.readLine();
            }
            catch (IOException e) {
                return "quit";
            }
        }

        private void quit()
        {
            SensorShellWrapper.getInstance().quit();

        }
    }

    /**
     * This method starts the ECG Server & Lab application. 
     * @param args If a String parameter is given, it is taken to be the mdoule-directory path.
     */
    public static void main(String[] args)
    {

        File file = null;

        if (args != null && args.length > 0) {

            file = new File(args[0]);

            if (file.exists() && file.isDirectory()) {
                new SensorShellWrapper(file);
            }
            else {
                new SensorShellWrapper();
            }

        }
        else {
            new SensorShellWrapper();
        }

    }
}