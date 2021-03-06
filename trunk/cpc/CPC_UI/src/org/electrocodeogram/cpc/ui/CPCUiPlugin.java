package org.electrocodeogram.cpc.ui;


import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class CPCUiPlugin extends AbstractUIPlugin
{
	private static Log log = LogFactory.getLog(CPCUiPlugin.class);

	// The plug-in ID
	public static final String PLUGIN_ID = "org.electrocodeogram.cpc.ui";

	// The shared instance
	private static CPCUiPlugin plugin;

	//private static final String LOG_PROPERTIES_FILE = "logging.properties";
	//	private ILogManager logManager;

	/**
	 * The constructor
	 */
	public CPCUiPlugin()
	{
		plugin = this;

		//		configureLogging();
		log.info("using shared logging");

		log.trace("trace enabled");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CPCUiPlugin getDefault()
	{
		return plugin;
	}

	//	public static ILogManager getLogManager()
	//	{
	//		return getDefault().logManager;
	//	}

	/**
	 * Creates a descriptor for the given icon file.
	 * 
	 * @param fileName
	 * @return
	 */
	public static ImageDescriptor getImageDescriptor(String fileName)
	{
		String iconPath = "icons/";
		//		try
		//		{
		//URL installURL = getDefault().getDescriptor().getInstallURL();
		//URL url = new URL(installURL, iconPath + fileName);
		URL url = getDefault().getBundle().getEntry(iconPath + fileName);
		//TODO: test this ^
		return ImageDescriptor.createFromURL(url);
		//		}
		//		catch (MalformedURLException e)
		//		{
		//			// should not happen
		//			log.error("getImageDescriptor() - unable to load image - fileName: " + fileName);
		//			return ImageDescriptor.getMissingImageDescriptor();
		//		}
	}

	//	private void setupListeners()
	//	{
	//		if (log.isTraceEnabled())
	//			log.trace("setupListeners()");
	//
	//		IEventHubRegistry registry = CPCCorePlugin.getEventHubRegistry();
	//		registry.subscribe(CloneEvent.class, new CloneEventListener());
	//		registry.subscribe(EclipseEditorPartEvent.class, new EditorPartListener());
	//	}

	//	private void configureLogging()
	//	{
	// we can either use a shared ECGEclipseCorePlugin logger or get our own
	// stand alone logger.

	// for now we use a shared logger.
	//		logManager = ECGEclipseCorePlugin.getDefault().getPluginLogManager();
	//		log = this.logManager.getLogger(CPCUiPlugin.class.getName());
	//		log.info("using shared logging");

	// standalone logger
	/*
	try
	{
		URL url = getBundle().getEntry("/" + LOG_PROPERTIES_FILE);
		InputStream propertiesInputStream = url.openStream();
		if (propertiesInputStream != null)
		{
			Properties props = new Properties();
			props.load(propertiesInputStream);
			propertiesInputStream.close();
			this.logManager = new PluginLogManager(this, props);

			log = this.logManager.getLogger(CPCCorePlugin.class.getName());
			log.info("Logging Initialized");
		}
		else
		{
			System.err.println("ERROR - failed to load properties file - " + LOG_PROPERTIES_FILE);
		}
	}
	catch (Exception e)
	{
		String message = "Error while initializing log properties." + e.getMessage();
		System.err.println(message);

		IStatus status = new Status(IStatus.ERROR, getDefault().getBundle().getSymbolicName(), IStatus.ERROR,
				message, e);
		getLog().log(status);
		throw new RuntimeException("Error while initializing log properties.", e);
	}
	*/
	//	}
}
