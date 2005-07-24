package org.electrocodeogram.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.module.registry.ModuleRegistry;
import org.electrocodeogram.msdt.MsdtManager;
import org.electrocodeogram.ui.Gui;

/**
 * This is the singleton root and main class of the ECG Server & Lab component. During its
 * initialization it creates all other core ECG components. It also provides
 * a console for logging output and reading input commands.
 * It is implemented as a singleton class that gives centralized access to
 * all components.
 */
public class Core {

	private static Core theInstance = null;

	private ModuleRegistry moduleRegistry = null;

	private Gui gui = null;
	
	private MsdtManager mstdManager = null;

	private Logger logger = null;
	
    
    
	private Core() {
		
		this.logger = Logger.getLogger("Core");
		
		Console console = new Console();
		
		try {
			
			this.mstdManager = new MsdtManager();
			
		} catch (FileNotFoundException e) {
			
			this.logger.log(Level.SEVERE,e.getMessage());
			
		}
		
        this.moduleRegistry = new ModuleRegistry();
        
        this.gui = new Gui(this.moduleRegistry);

		console.start();
		
		theInstance = this;
	}

	private Core(File file) {

		this();

		if (this.moduleRegistry == null) {
			this.moduleRegistry = new ModuleRegistry(file);
		} else {
			this.moduleRegistry.setFile(file);
		}
	}

	/**
	 * This method return sthe singleton instance of the Core object,
	 * which is primarily used to get access to other ECG components.
	 * @return The singleton instance of the Core object
	 */
	public static Core getInstance() {
		if (theInstance == null) {
			theInstance = new Core();
		}

		return theInstance;
	}

	
	/**
     * This method returns a reference to the MicroSensorDataType-Manager object. 
     * @return A reference to the MicroSensorDataType-Manager object
	 */
	public MsdtManager getMsdtManager()
	{
		return this.mstdManager;
	}

    /**
     * This method returns a reference to the ModuleRegistry object. 
     * @return A reference to the ModuleRegistry object
     */
	public ModuleRegistry getModuleRegistry() {
		return this.moduleRegistry;
	}

	/**
	 * This method quits the ECG Server & Lab application.
	 */
	public void quit()
    {
		System.exit(0);
	}

	private class Console extends Thread {

		private BufferedReader bufferedReader = null;

		/**
		 * Creates the console to manage the ECG Server & Lab.
		 *
		 */
		public Console() {
			System.out.println("ElectroCodeoGram Server & Lab is starting...");

			this.bufferedReader = new BufferedReader(new InputStreamReader(
					System.in));

		}

		/**
		 * Here the reading of the console-input is done.
		 */
		@Override
		public void run() {

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

		private String readLine() {
			try {
				return this.bufferedReader.readLine();
			} catch (IOException e) {
				return "quit";
			}
		}

		private void quit() {
			Core.getInstance().quit();

		}
	}

	/**
	 * This method starts the ECG Server & Lab application. 
	 * @param args If a String parameter is given, it is taken to be the mdoule-directory path.
	 */
	public static void main(String[] args) {

		File file = null;

		if (args != null && args.length > 0) {

			file = new File(args[0]);

			if (file.exists() && file.isDirectory()) {
				new Core(file);
			} else {
				new Core();
			}

		} else {
			new Core();
		}

	}

	/**
     * This method returns a reference to the gui main frame object. 
     * @return A reference to the gui main frame object
	 */
	public Gui getGui() {
		return this.gui;
	}

}