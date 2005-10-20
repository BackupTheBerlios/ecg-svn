package org.hackystat.kernel.shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.client.EventPacketQueueOverflowException;
import org.electrocodeogram.client.IllegalHostOrPortException;
import org.electrocodeogram.client.SendingThread;
import org.electrocodeogram.event.IllegalEventParameterException;
import org.electrocodeogram.event.WellFormedEventPacket;
import org.electrocodeogram.logging.LogHelper;
import org.hackystat.kernel.admin.SensorProperties;


/**
 * This class is the ECG SensorShell. It is named
 * org.hackystat.kernel.shell.SensorShell after the class provided by the
 * HackyStat project (please visit www.hackystat.org for more information).
 * 
 * It's purpose is to be used by every ECG sensor for recorded data to be sended
 * to the ECG Server & Lab. So a sensor developer must not implement the
 * functionality of sending data to the ECG server himself.
 * 
 * Because the ECG framework directly supports the usage of original HackyStat
 * sensors this class acts like the original HackyStat SensorShell class
 * including the naming. That means that every original HackyStat sensor is able
 * to collect data for the ECG framework by simply replacing the HackyStat's
 * sensorshell.jar library with the ECG sensorshell.jar.
 * 
 * Sensors are able to use this class in to different ways depending on the
 * sensor programming language. If the sensor is written in Java, this class can
 * be instanciated to a SensorShell object. On the SensorShell object the method
 * doCommand is called to send recorded data to the ECG framework. The recorded
 * data must at least conform to a HackyStat SensorDataType.
 * 
 * To support even sensors that are not written in Java, this class provides a
 * main method that makes it a process that communicates via standard-input and
 * -output. The process is created via a "java -jar sensorshell.jar" command at
 * the operating system level. Every string that is passed to the standard-input
 * of the SensorShell process, is taken for collected data.
 * 
 */
public class SensorShell
{

	public static final String ECG_LAB_PATH = "ECG_Lab_Path";

	private static final String INLINE_SERVER = "INLINE";

	private static final String REMOTE_SERVER = "REMOTE";
	
	private static int RESET_COUNT = 100;
	
	private int _count;

	static Logger _logger = LogHelper.createLogger(SensorShell.class.getName());

	/*
	 * This is a reference to the SensorProperties object, that contains the
	 * information of the "sensor.properties" file. The file is used to
	 * configure all sensors in the system.
	 */
	private SensorProperties _properties;

	private BufferedReader _bufferedReader;

	private String _cr = System.getProperty("line.separator");

	private String _prompt = "SensorShell >> ";

	private String _delimiter = "#";

	private boolean _interactive;

	private SendingThread _sendingThread = null;

	protected enum ServerMode
	{
		INLINE, REMOTE
	}

	protected ServerMode _serverMode;

	// protected PrintWriter _toInlineServer;

	protected ObjectOutputStream _toInlineServer;

	protected String ecgEclipseSensorPath;

	private boolean _acceptEvents;

	private static InlineServer _inlineServer;

	/**
	 * This creates a ECG SensorShell instance with the given properties.
	 * 
	 * @param properties
	 *            The properties to configure the ECG SensorShell
	 * @param interactive
	 *            Is "true" if the SensorShell is run as a process and "false"
	 *            if it is instantiated to a SensorShell object.
	 * @param toolName
	 *            Is the tool name
	 */
	public SensorShell(SensorProperties properties, boolean interactive, @SuppressWarnings("unused")
	String toolName)
	{
		_logger.entering(this.getClass().getName(), "SensorShell");

//		String resource = "E:";
//		
//		URL url=null;
//        // Try to format as a URL?
//        ClassLoader loader=Thread.currentThread().getContextClassLoader();
//        if (loader!=null)
//        {
//            url=loader.getResource(resource);
//            if (url==null && resource.startsWith("/"))
//                url=loader.getResource(resource.substring(1));
//        }
//        if (url==null)
//        {
//            loader=SensorShell.class.getClassLoader();
//            if (loader!=null)
//            {
//                url=loader.getResource(resource);
//                if (url==null && resource.startsWith("/"))
//                    url=loader.getResource(resource.substring(1));
//            }
//        }
//        
//        if (url==null)
//        {
//            url=ClassLoader.getSystemResource(resource);
//            if (url==null && resource.startsWith("/"))
//                url=loader.getResource(resource.substring(1));
//        }

		
		this._acceptEvents = true;
		
		this._count = 0;

		if (properties == null)
		{
			_logger.log(Level.SEVERE, "SensorProperties are null. Can not create ECG SensorShell.");

			return;
		}

		this._properties = properties;

		this._interactive = interactive;

		initializeLogging();

		initializeCommunication();

		_logger.exiting(this.getClass().getName(), "SensorShell");
	}

	/**
	 * This creates a ECG SensorShell instance with the given properties.
	 * 
	 * @param properties
	 *            The properties to configure the ECG SensorShell
	 * @param interactive
	 *            Is "true" if the SensorShell is run as a process and "false"
	 *            if it is instantiated to a SensorShell object.
	 */
	public SensorShell(SensorProperties properties, boolean interactive)
	{
		this(properties, interactive, "ElectroCodeoGram");
	}

	/**
	 * This creates a ECG SensorShell instance with the given properties.
	 * 
	 * @param properties
	 *            The properties to configure the ECG SensorShell
	 * @param interactive
	 *            Is "true" if the SensorShell is run as a process and "false"
	 *            if it is
	 * @param toolName
	 *            Is the tool name instantiated to a SensorShell object.
	 * @param enableOfflineData
	 *            Is not used
	 * @param commandFile
	 *            Is not used
	 */
	public SensorShell(SensorProperties properties, boolean interactive, String toolName, @SuppressWarnings("unused")
	boolean enableOfflineData, @SuppressWarnings("unused")
	File commandFile)
	{
		this(properties, interactive, toolName);
	}

	/**
	 * This creates a ECG SensorShell instance with the given properties.
	 * 
	 * @param properties
	 *            The properties to configure the ECG SensorShell
	 * @param interactive
	 *            Is "true" if the SensorShell is run as a process and "false"
	 *            if it is
	 * @param toolName
	 *            Is the tool name instantiated to a SensorShell object.
	 * @param enableOfflineData
	 *            Is not used
	 */
	public SensorShell(SensorProperties properties, boolean interactive, String toolName, @SuppressWarnings("unused")
	boolean enableOfflineData)
	{
		this(properties, interactive, toolName);
	}

	private void initializeCommunication()
	{
		_logger.entering(this.getClass().getName(), "initializeECGLabCommunication");

		if (this._properties.getECGServerType() == null)
		{
			_logger.log(Level.WARNING, "ECG_SEVER_TYPE is null in sensor.properties.");

			_logger.log(Level.WARNING, "Going remote server per default.");

			startRemote();
		}
		else if (this._properties.getECGServerType().equals(INLINE_SERVER))
		{
			_logger.log(Level.INFO, "The ECG_SEVER_TYPE is INLINE in sensor.properties.");

			_logger.log(Level.INFO, "Going inline server...");

			startInline();
		}
		else if (this._properties.getECGServerType().equals(REMOTE_SERVER))
		{
			_logger.log(Level.INFO, "ECG_SEVER_TYPE is STANDALONE in sensor.properties.");

			_logger.log(Level.INFO, "Going remote server...");

			startRemote();
		}
		else
		{
			_logger.log(Level.INFO, "ECG_SEVER_TYPE is something else than REMOT or INLINE in sensor.properties.");

			_logger.log(Level.INFO, "Going remote server per default.");

			startRemote();
		}

		_logger.exiting(this.getClass().getName(), "initializeECGLabCommunication");
	}

	private void startRemote()
	{
		_logger.entering(this.getClass().getName(), "startClientThread");

		try
		{
			// Try to create the SendingThread with the given ECG server address
			// information

			this._sendingThread = new SendingThread(
					this._properties.getECGServerAddress(),
					this._properties.getECGServerPort());
		}
		catch (IllegalHostOrPortException e)
		{
			_logger.log(Level.SEVERE, "The ECG Server's address is invalid.\nPlease check the ECG_SERVER_ADDRESS and ECG_SERVER_PORT values in the file \".hackystat/sensor.properties\" in your home directory.");

			_logger.exiting(this.getClass().getName(), "startClientThread");

			return;

		}
		catch (UnknownHostException e)
		{
			_logger.log(Level.SEVERE, "The ECG Server's address is invalid.\nPlease check the ECG_SERVER_ADDRESS and ECG_SERVER_PORT values in the file \".hackystat/sensor.properties\" in your home directory.");

			_logger.exiting(this.getClass().getName(), "startClientThread");

			return;
		}

		this._serverMode = ServerMode.REMOTE;

		_logger.exiting(this.getClass().getName(), "startClientThread");
	}

	private void startInline()
	{
		_logger.entering(this.getClass().getName(), "startInlineServer");

		_inlineServer = new InlineServer(this);

		_inlineServer.start();

		_logger.exiting(this.getClass().getName(), "startInlineServer");
	}

	public static void stop()
	{
		_logger.log(Level.INFO, "SensorShell is stopping...");

		if (_inlineServer != null)
		{
			_logger.log(Level.INFO, "Found InlineServer");

			_inlineServer.stopECGServer();
		}

		_logger.log(Level.INFO, "No InlineServer found");
	}

	private void initializeLogging()
	{
		String logFile = this._properties.getProperty("ECG_LOG_FILE");

		if (logFile != null)
		{
			try
			{
				LogHelper.setLogFile(logFile);
			}
			catch (SecurityException e)
			{
				_logger.log(Level.SEVERE, "Error while creating the logfile" + logFile);

				_logger.log(Level.FINEST, e.getMessage());
			}
			catch (IOException e)
			{
				_logger.log(Level.SEVERE, "Error while creating the logfile" + logFile);

				_logger.log(Level.FINEST, e.getMessage());
			}
		}

		Level logLevel = LogHelper.getLogLevel(this._properties.getProperty("ECG_LOG_LEVEL"));

		LogHelper.setLogLevel(logLevel);

	}

	/**
	 * This method is called by the ECG sensors that are able to have an object
	 * reference to the SensorShell. Whenever they record an event they call the
	 * doCommand method and pass the parameters according to the HackyStat
	 * SensorDataType and ECG MicroSensorDataType definitions.
	 * 
	 * @param timeStamp
	 *            The timeStamp of the event
	 * @param commandType
	 *            The HackyStat SensorDataType or of the event
	 * @param argList
	 *            The argList of the event that contains additional data or an
	 *            ECG MicroSensorDataType
	 * @return "true" if the event's data is syntactically correct and "false"
	 *         otherwise
	 */
	public boolean doCommand(Date timeStamp, String commandType, List argList)
	{
		_logger.entering(this.getClass().getName(), "doCommand");

		boolean result;

		if (this._acceptEvents == false)
		{
			return false;
		}

		if (commandType != null && commandType.equals(ECG_LAB_PATH))
		{

			_logger.log(Level.FINE, "SensorShell is receiving the path to the ECG Lab.");

			result = analysePath(argList);

			_logger.exiting(this.getClass().getName(), "doCommand");

			return result;

		}

		_logger.log(Level.FINE, "SensorShell is getting an event.");

		result = analyseEvent(timeStamp, commandType, argList);

		_logger.exiting(this.getClass().getName(), "doCommand");

		return result;

	}

	private boolean analyseEvent(Date timeStamp, String commandType, List argList)
	{
		_logger.entering(this.getClass().getName(), "analyseEvent");

		WellFormedEventPacket packet;

		try
		{
			packet = new WellFormedEventPacket(0, timeStamp, commandType, argList);
		}
		catch (IllegalEventParameterException e)
		{
			_logger.log(Level.FINE,"An error occured while reading an event from the sensor.");
						
			_logger.exiting(this.getClass().getName(), "analyseEvent");
			
			return false;
		}

		if (this._serverMode == ServerMode.REMOTE)
		{

			_logger.log(Level.FINE, "The SensorShell is operating in RemoteServer mode.");
			
			if (this._sendingThread != null)
			{
				// pass EventPacket to SendingThread
				try
				{
					this._sendingThread.addEventPacket(packet);
				}
				catch (EventPacketQueueOverflowException e)
				{
					_logger.log(Level.SEVERE, "A QueueOverflowException had occured.");

					this._acceptEvents = false;
				}

				_logger.log(Level.FINE, "Event packet passed to SendingThread.");

			}
			else
			{
				_logger.log(Level.WARNING,"The stream to the ECG Lab is not open. Maybe the SensdingThread is not started yet?");
			}
		}
		else
		{
			_logger.log(Level.FINE, "The SensorShell is operating in InlineServer mode.");
			
			if (this._toInlineServer != null)
			{
				try
				{
                    _logger.log(Level.FINE, "if (this._toInlineServer != null)");
                    
					this._toInlineServer.flush();
					
					_logger.log(Level.FINE, "this._toInlineServer.flush();");

					this._toInlineServer.writeObject(packet);
					
					_logger.log(Level.FINE, "this._toInlineServer.writeObject(packet);");

					this._toInlineServer.flush();

					_logger.log(Level.FINE, "this._toInlineServer.flush();");
					
					this._count++;
					
					_logger.log(Level.FINE, "this._count++;");
					
					if(this._count == RESET_COUNT)
					{
						_logger.log(Level.FINE, "if(this._count == RESET_COUNT)");
						
						this._toInlineServer.reset();
						
						_logger.log(Level.FINE, "this._toInlineServer.reset();");
						
						this._count = 0;
						
						_logger.log(Level.FINE, "The stream to the ECG Lab has been reset.");
					}
					
					_logger.log(Level.FINE, "An event has been passed to the InlineServer.");

				}
				catch (IOException e)
				{
					_logger.log(Level.SEVERE, "An error occured while sending an event to the InlineServer.");
					
					_logger.log(Level.SEVERE, e.getMessage());
                    
                    _inlineServer.stopECGServer();
					
				}
			}
			else
			{
				_logger.log(Level.WARNING,"The stream to the ECG Lab is not open. Maybe the InlineServer is not started yet?");
			}
		}

		_logger.exiting(this.getClass().getName(), "analyseEvent");

		return true;
	}

	private boolean analysePath(List argList)
	{
		_logger.entering(this.getClass().getName(), "analysePath");

		if (this.ecgEclipseSensorPath == null)
		{
			if (argList == null)
			{
				_logger.log(Level.FINEST, "argList is null");

				_logger.exiting(this.getClass().getName(), "analysePath");

				return false;
			}

			this.ecgEclipseSensorPath = (String) argList.get(0);

			_logger.log(Level.INFO, "SensorShell got ECG Lab path to: " + this.ecgEclipseSensorPath);

		}

		_logger.exiting(this.getClass().getName(), "analysePath");

		return true;
	}

	// Here begins the list of methods that are only implemented for
	// compatibility issues to HackyStat sensors

	/**
	 * This method returns the SensorProperties that are declared in the
	 * "sensor.properties" file.
	 * 
	 * @return The SensorProperties
	 */
	public SensorProperties getSensorProperties()
	{
		_logger.entering(this.getClass().getName(), "getSensorProperties");

		_logger.exiting(this.getClass().getName(), "getSensorProperties");

		return this._properties;
	}

	/**
	 * This method is not implemented and only declared for compatibility
	 * reasons.
	 * 
	 * @return Is allways true to to tell the hackyStat sensors sending was
	 *         successfull
	 */
	public boolean send()
	{
		_logger.entering(this.getClass().getName(), "send");

		_logger.exiting(this.getClass().getName(), "send");

		return true;
	}

	/**
	 * This method is not implemented and only declared for compatibility
	 * reasons.
	 * 
	 * @param str
	 *            not used
	 */
	public void println(@SuppressWarnings("unused")
	String str)
	{
		_logger.entering(this.getClass().getName(), "println");

		_logger.exiting(this.getClass().getName(), "println");

		return;
	}

	/**
	 * This method is not implemented and only declared for compatibility
	 * reasons.
	 * 
	 * @return Is always true to tell HacyStat sensors that the server is
	 *         pingable
	 */
	public boolean isServerPingable()
	{
		_logger.entering(this.getClass().getName(), "isServerPingable");

		_logger.exiting(this.getClass().getName(), "isServerPingable");

		return true;
	}

	/**
	 * This method is not implemented and only declared for compatibility
	 * reasons.
	 * 
	 * @param milliSecondsToWait
	 *            Is not used
	 * @return s always true to tell HacyStat sensors that the server is
	 *         pingable
	 */
	public boolean isServerPingable(@SuppressWarnings("unused")
	int milliSecondsToWait)
	{
		_logger.entering(this.getClass().getName(), "isServerPingable");

		_logger.exiting(this.getClass().getName(), "isServerPingable");

		return true;
	}

	/**
	 * This method is not implemented and only declared for compatibility
	 * reasons.
	 * 
	 * @return An empty String
	 */
	public String getResultMessage()
	{
		_logger.entering(this.getClass().getName(), "getResultMessage");

		_logger.exiting(this.getClass().getName(), "getResultMessage");

		return "";
	}

	// End

	private void print(String line)
	{

		_logger.entering(this.getClass().getName(), "print");

		if (this._interactive)
		{
			System.out.print(line);
		}

		_logger.exiting(this.getClass().getName(), "print");
	}

	private String readLine()
	{
		_logger.entering(this.getClass().getName(), "readLine");

		try
		{
			String line = this._bufferedReader.readLine();

			_logger.log(Level.INFO, line + this._cr);

			_logger.exiting(this.getClass().getName(), "readLine");

			return line;
		}
		catch (IOException e)
		{
			_logger.exiting(this.getClass().getName(), "readLine");

			return "quit";

		}

	}

	private void printPrompt()
	{
		_logger.entering(this.getClass().getName(), "printPrompt");

		this.print(this._prompt);

		_logger.exiting(this.getClass().getName(), "printPrompt");
	}

	private void quit()
	{
		_logger.entering(this.getClass().getName(), "quit");

		_logger.log(Level.INFO, "Exiting SensorShell...");

		_logger.exiting(this.getClass().getName(), "quit");

		System.exit(0);

	}

	/**
	 * The main method makes this class a process that continuously reads from
	 * standard-input. Every string that is passed to its standard-input is
	 * handled as recorded event data.
	 * 
	 * @param args
	 *            The first parameter shall be the tool name string and the
	 *            second shall be the path to the "sensor.properties" file.
	 */
	public static void main(String args[])
	{

		_logger.entering(SensorShell.class.getName(), "main");

		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

		_logger.log(Level.INFO, "Registered DefaultExceptionHandelr");

		if ((args.length == 1) && (args[0].equalsIgnoreCase("-help")))
		{
			_logger.log(Level.INFO, "Help requested by user");

			System.out.println("java -jar sensorshell.jar [toolname] [sensor.properties]");

			_logger.exiting(SensorShell.class.getName(), "main");

			return;
		}

		SensorProperties sensorProperties;

		if (args.length >= 2)
		{
			sensorProperties = new SensorProperties("Shell", new File(args[1]));

			_logger.log(Level.INFO, "Loaded SensorProperties from file.");

			if (!sensorProperties.isFileAvailable())
			{
				System.out.println("Could not find sensor.properties file. ");

				System.out.println("Expected in: " + sensorProperties.getAbsolutePath());

				_logger.exiting(SensorShell.class.getName(), "main");

				return;
			}
		}
		else
		{
			sensorProperties = new SensorProperties("ECG SensorShell");

			_logger.log(Level.INFO, "Loaded default SensorProperties.");
		}

		boolean interactive = true;

		SensorShell shell = new SensorShell(sensorProperties, interactive, "");

		_logger.log(Level.INFO, "Created ECG SensorShell.");

		while (true)
		{

			shell.printPrompt();

			String inputString = shell.readLine();

			// Quit if necessary.
			if (inputString != null && inputString.equalsIgnoreCase("quit"))
			{
				shell.quit();

				return;
			}

			StringTokenizer tokenizer = new StringTokenizer(inputString,
					shell._delimiter);

			int numTokens = tokenizer.countTokens();

			if (numTokens == 0)
			{
				continue;
			}

			String commandName = tokenizer.nextToken();

			ArrayList<String> argList = new ArrayList<String>();

			while (tokenizer.hasMoreElements())
			{
				argList.add(tokenizer.nextToken());
			}

			shell.doCommand(new Date(), commandName, argList);

			_logger.exiting(SensorShell.class.getName(), "main");
		}

	}

	private static class StreamGobbler extends Thread
	{
		private InputStream _inputStream;

		private String _type;

		StreamGobbler(InputStream is, String type)
		{
			_logger.entering(this.getClass().getName(), "StreamGobbler");

			this._inputStream = is;

			this._type = type;

			_logger.exiting(this.getClass().getName(), "StreamGobbler");
		}

		@Override
		public void run()
		{
			_logger.entering(this.getClass().getName(), "run");

			try
			{
				InputStreamReader isr = new InputStreamReader(this._inputStream);

				BufferedReader br = new BufferedReader(isr);

				String line = null;

				while ((line = br.readLine()) != null)
				{
					 //SensorShell._logger.log(Level.INFO, this._type + ">" + line);
				}
			}
			catch (IOException e)
			{
				SensorShell._logger.log(Level.SEVERE, "An error occured while reading from the ECG Lab process.");

				SensorShell._logger.log(Level.SEVERE, e.getMessage());
			}

			_logger.exiting(this.getClass().getName(), "run");
		}

	}

	private static class DefaultExceptionHandler implements UncaughtExceptionHandler
	{
		public void uncaughtException(Thread t, Throwable e)
		{
			_logger.entering(this.getClass().getName(), "uncaughtException");

			System.out.println("Please report at www.electrocodeogram.org.");

			System.out.println("An uncaught RuntimeException had occured:");

			System.out.println("Thread:" + t.getName());

			System.out.println("Class: " + t.getClass());

			System.out.println("State: " + t.getState());

			System.out.println("Message: " + e.getMessage());

			System.out.println("StackTrace: ");

			e.printStackTrace();

			_logger.exiting(this.getClass().getName(), "uncaughtException");

		}

	}

	private static class InlineServer extends Thread
	{
		private static final int WAIT_FOR_PATH_DELAY = 1000;

		private Process _ecgLab;

		private SensorShell _shell;

		public InlineServer(SensorShell shell)
		{
			_logger.entering(this.getClass().getName(), "InlineServer");

			this._shell = shell;

			_logger.exiting(this.getClass().getName(), "InlineServer");
		}

		public void stopECGServer()
		{
			if (this._ecgLab == null)
			{
				_logger.log(Level.WARNING, "The ECG Lab process in null.");

				return;
			}
			try
			{
				if (this._shell._toInlineServer == null)
				{

					this._shell._toInlineServer = new ObjectOutputStream(
							this._ecgLab.getOutputStream());

					_logger.log(Level.INFO, "The stream to the ECG Lab has been reopened.");
				}

				this._shell._toInlineServer.writeObject(new String("quit"));

				_logger.log(Level.INFO, "The command \"quit\" has been sent to the ECG Lab.");

				this._shell._toInlineServer.flush();

				this._shell._toInlineServer.close();

			}
			catch (IOException e)
			{
				_logger.log(Level.SEVERE,"An error occured while sending the quit command to the ECG Lab.");
			}
            finally
            {
                if(this._shell._toInlineServer != null)
                {
                    try
                    {
                        this._shell._toInlineServer.close();
                        
                        _logger.log(Level.SEVERE,"Stream to ECG Lab has been closed.");
                    }
                    catch(IOException e)
                    {
                        _logger.log(Level.SEVERE,"Unable to clear the stream to the ECG Lab.");
                    }
                }
                
                this._ecgLab.destroy();
                
                _logger.log(Level.SEVERE,"The ECG Lab process has got the kill signal.");
            }
		}

		public void run()
		{
			_logger.entering(this.getClass().getName(), "run");

			while (this._shell.ecgEclipseSensorPath == null)
			{
				try
				{
					SensorShell._logger.log(Level.WARNING, "Path to ECG Lab not received yet. Waiting...");

					Thread.sleep(WAIT_FOR_PATH_DELAY);
				}
				catch (InterruptedException e)
				{
					SensorShell._logger.log(Level.SEVERE, "Path to ECG Lab not received yet.");

					SensorShell._logger.log(Level.SEVERE, "Unable to start ECG Lab as InlineServer.");

					_logger.exiting(this.getClass().getName(), "run");

					return;
				}
			}

			try
			{

				if (this._shell.ecgEclipseSensorPath != null && !this._shell.ecgEclipseSensorPath.equals(""))
				{

					String osName = System.getProperty("os.name");
					
					if(osName == null || osName.equals(""))
					{
						_logger.log(Level.SEVERE,"The operating system name could not be read from the system environment. Aborting InlineServer startup...");
						
						_logger.exiting(this.getClass().getName(),"run");
						
						return;
					}
					
					_logger.log(Level.FINE,"The operating system name is: " + osName);
					
					ProcessBuilder pb = null;
					
					File ecgDir = null;
					
					if(osName.startsWith("Windows"))
					{
						_logger.log(Level.INFO,"The operating system is a windows system.");
						
						pb = new ProcessBuilder("cmd.exe", "/C",
						"ecg.bat");
						
						ecgDir = new File(this._shell.ecgEclipseSensorPath);

						SensorShell._logger.log(Level.FINE, "ECG Lab directory is: " + ecgDir.getAbsolutePath());
						
					}
					else if(osName.startsWith("Linux"))
					{
						_logger.log(Level.INFO,"The operating system is a linux system.");
						
						pb = new ProcessBuilder("sh" , "./ecg.sh");
						
						ecgDir = new File(this._shell.ecgEclipseSensorPath);

						SensorShell._logger.log(Level.FINE, "ECG Lab directory is: " + ecgDir.getAbsolutePath());

					}
					else
					{
						_logger.log(Level.SEVERE,"The operating system is an unknown system. Aborting InlineServer startup...");
						
						return;
					}
					
					
					pb.directory(ecgDir);
					
					this._ecgLab = pb.start();

					SensorShell._logger.log(Level.INFO, "ECG Lab process started.");

					this._shell._toInlineServer = new ObjectOutputStream(this._ecgLab.getOutputStream());
					
					if(this._shell._toInlineServer == null)
					{
						_logger.log(Level.INFO,"Could not create stream to InlineServer.");
					}
					else
					{
						_logger.log(Level.INFO,"Stream to InlineServer created.");
					}

					StreamGobbler errorGobbler = new StreamGobbler(
							this._ecgLab.getErrorStream(), "ERROR");

					StreamGobbler outputGobbler = new StreamGobbler(
							this._ecgLab.getInputStream(), "OUTPUT");

					errorGobbler.start();

					outputGobbler.start();

					this._shell._serverMode = ServerMode.INLINE;
				}
				else
				{
					SensorShell._logger.log(Level.SEVERE, "The ECG Lab path is null or empty.");
				}
			}
			catch (IOException e)
			{
				SensorShell._logger.log(Level.SEVERE, "Unable to start ECG Lab.");

				SensorShell._logger.log(Level.SEVERE, e.getMessage());
			}

			_logger.exiting(this.getClass().getName(), "run");
		}

		/**
		 * @see java.lang.Object#finalize()
		 */
		@Override
		protected void finalize() throws Throwable
		{

			_logger.log(Level.INFO, "Finalizing InlineServer...");

			try
			{
				stopECGServer();
			}
			finally
			{
				super.finalize();
			}

		}
	}

}
