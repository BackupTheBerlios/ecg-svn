package org.electrocodeogram.cpc.sensor;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.electrocodeogram.cpc.core.utils.CoreFileUtils;
import org.electrocodeogram.cpc.core.utils.CoreUtils;
import org.electrocodeogram.cpc.sensor.listener.CPCFileBufferListener;
import org.electrocodeogram.cpc.sensor.listener.CPCPartListener;
import org.electrocodeogram.cpc.sensor.listener.CPCResourceChangeListener;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class CPCSensorPlugin extends Plugin implements IStartup
{
	private static Log log = LogFactory.getLog(CPCSensorPlugin.class);

	// The plug-in ID
	public static final String PLUGIN_ID = "org.electrocodeogram.cpc.sensor";

	static public final int DIFFCHANGE_INTERVAL = 250;

	// The shared instance
	private static CPCSensorPlugin plugin;

	//private static final String LOG_PROPERTIES_FILE = "logging.properties";
	//	private ILogManager logManager;

	/**
	 * The constructor
	 */
	public CPCSensorPlugin()
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
	public static CPCSensorPlugin getDefault()
	{
		return plugin;
	}

	//	public static ILogManager getLogManager()
	//	{
	//		return getDefault().logManager;
	//	}

	public void earlyStartup()
	{
		//verify that we have not been deactivated by some other plugin
		if (CoreUtils.pluginIsDeactivated(PLUGIN_ID))
		{
			log.info("CPC Sensor was deactivated by a 3rd party plugin.");
			return;
		}

		if (Platform.isRunning())
			setupListeners();
		else
			log.info("earlyStartup(): not starting Eclipse event listeners in standalone mode.");
	}

	//TODO: those CPC parts which really need earlyStartup should all check if the user might
	//have disabled earlyStartup execution and warn the user that this will break things.
	/*
	public static boolean isEarlyStartupDisabled() {
		   String plugins = PlatformUI.getPreferenceStore().getString(
		      /*
		       * Copy constant out of internal Eclipse interface
		       * IPreferenceConstants.PLUGINS_NOT_ACTIVATED_ON_STARTUP
		       * so that we are not accessing internal type.
		       * /
		      "PLUGINS_NOT_ACTIVATED_ON_STARTUP");
		   return plugins.indexOf(FavortesPlugin.ID) != -1;
		}
	*/

	private void setupListeners()
	{
		if (log.isTraceEnabled())
			log.trace("setupListeners()");

		/*
		 * ECG Eclipse Sensor events.
		 */
		/*
		SimpleECGSensorConversionListener simpleConverter = new SimpleECGSensorConversionListener();

		MAEEventDispatcher dispatcher = ECGEclipseSensor.getInstance().getEventDispatcher();
		dispatcher.subscribe(TextOperationMicroActivityEvent.class, simpleConverter);
		dispatcher.subscribe(CodeDiffMicroActivityEvent.class, simpleConverter);
		dispatcher.subscribe(EditorMicroActivityEvent.class, simpleConverter);
		dispatcher.subscribe(ResourceMicroActivityEvent.class, simpleConverter);
		*/

		/*
		 * Direct Eclipse API hooks.
		 * 
		 * These are mainly for events which would not be generated by a normal ECG Eclipse Plugin
		 * because they are of no interest to ECG Lab. Other than that there may also be cases
		 * where performance considerations suggest omission of the additional layer of indirection
		 * which the ECG Eclipse Plugin represents. 
		 */

		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		//listen to file buffer state changes
		CPCFileBufferListener fileBufferListener = new CPCFileBufferListener();
		FileBuffers.getTextFileBufferManager().addFileBufferListener(fileBufferListener);
		log.trace("setupListeners() - added FileBufferListener.");

		// add the PartListener for listening on part events.
		IPartService partService = null;
		CPCPartListener partListener = new CPCPartListener();
		for (int i = 0; i < windows.length; i++)
		{
			partService = windows[i].getPartService();
			partService.addPartListener(partListener);
		}
		log.trace("setupListeners() - added PartListener.");

		//we want to be notified about all resource changes
		workspace
				.addResourceChangeListener(new CPCResourceChangeListener(), IResourceChangeEvent.POST_CHANGE /*| IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE*/);
		//TODO: the PRE_CLOSE and PRE_DELETE types are mainly geared towards project close and delete.
		//		such events would be potentially interesting for the ECG Lab too, so it is not clear whether we should
		//		handle them here or rather generate them in the ECG Eclipse Sensor and just convert them here.
		log.trace("setupListeners() - added ResourceChangeListener.");

		/*
		 * Send initial events on startup: which parts are open and which are activated
		 */
		for (int i = 0; i < windows.length; i++)
		{
			IWorkbenchPage page = windows[i].getActivePage();

			// send opened event on all views
			/*
			IViewReference[] views = page.getViewReferences();
			for (int j = 0; j < views.length; j++)
			{
				IViewPart view = views[j].getView(false);
				if (view != null)
				{
					// just call the related listener method
					partListener.partOpened(view);
				}
			}*/

			// send opened event on all editors
			IEditorReference[] editors = page.getEditorReferences();
			for (int j = 0; j < editors.length; j++)
			{
				IEditorPart editor = editors[j].getEditor(false);
				if (editor != null)
				{
					// just call the related listener method
					partListener.partOpened(editor);

					if (editor instanceof ITextEditor)
					{
						ITextEditor textEditor = (ITextEditor) editor;
						String location = CoreUtils.getLocationFromPart(textEditor);

						//make sure this file is located within the workspace, we're not interested in external files.
						if (CoreFileUtils.isFileLocatedInWorkspace(location))
						{
							//also notify the file buffer about a potentially missed file open event
							IDocumentProvider provider = textEditor.getDocumentProvider();
							IDocument document = provider.getDocument(textEditor.getEditorInput());
							fileBufferListener.initiallyOpenDocument(CoreUtils.getProjectnameFromLocation(location),
									CoreUtils.getProjectRelativePathFromLocation(location), document);
						}
						else
						{
							if (log.isDebugEnabled())
								log
										.debug("setupListeners() - not located within workspace, ignoring file: "
												+ location);
						}

					}
				}
			}

			// send event for activated part
			IWorkbenchPart activePart = page.getActivePart();
			partListener.partActivated(activePart);
		}

	}

	//	private void configureLogging()
	//	{
	// we can either use a shared ECGEclipseCorePlugin logger or get our own
	// stand alone logger.

	// for now we use a shared logger.
	//		logManager = ECGEclipseCorePlugin.getDefault().getPluginLogManager();
	//		log = this.logManager.getLogger(CPCSensorPlugin.class.getName());
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
