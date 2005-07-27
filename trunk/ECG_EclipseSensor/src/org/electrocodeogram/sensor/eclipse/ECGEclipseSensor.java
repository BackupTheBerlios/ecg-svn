package org.electrocodeogram.sensor.eclipse;

import java.util.Arrays;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.hackystat.stdext.sensor.eclipse.EclipseSensor;
import org.hackystat.stdext.sensor.eclipse.EclipseSensorShell;

/**
 * This is the ECG EclipseSensor.
 * It uses the original HackyStat EclipseSensor and extends.
 * So all original HackyStat events are recorded along with any newly
 * introduced ECG events.
 */
public class ECGEclipseSensor
{

    private static ECGEclipseSensor theInstance = null;

    private EclipseSensor hackyEclipse = null;

    private boolean isEnabled = true;

    private EclipseSensorShell eclipseSensorShell;
    
    private ECGEclipseSensor()
    {
      
        this.hackyEclipse = EclipseSensor.getInstance();
        
        this.eclipseSensorShell = this.hackyEclipse.getEclipseSensorShell();
      
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        
        workspace.addResourceChangeListener(new ResourceChangeAdapter(), IResourceChangeEvent.POST_CHANGE);
    }
    
    /**
     * This return the singleton instance of the ECGEclipseSensor class.
     * @return The singleton instance of the ECGEclipseSensor class
     */
    public static ECGEclipseSensor getInstance()
    {
        if(theInstance == null)
        {
            theInstance = new ECGEclipseSensor();
        }
        
        return theInstance;
    }
 
    /**
     * This method takes the data of a recorded event and generates
     * and sends a HackyStat Activity event with the given event data.
     * @param microSensorDataType The name of the MicroSensorDataType of the event data
     * @param data Additional data
     */
    public void processActivity(String microSensorDataType, String data)
    {
        if (!this.isEnabled ) {
            return;
        }

        String[] args = { "add", microSensorDataType, data};
        
        this.eclipseSensorShell.doCommand("Activity", Arrays.asList(args));
    }
    
    private class ResourceChangeAdapter implements IResourceChangeListener
    {
        /**
         * Provides manipulation of IResourceChangeEvent instance due to implement
         * <code>IResourceChangeListener</code>. This method must not be called by client because it
         * is called by platform when resources are changed.
         * 
         * @param event A resource change event to describe changes to resources.
         */
        public void resourceChanged(IResourceChangeEvent event)
        {

            if (event.getType() != IResourceChangeEvent.POST_CHANGE)
                return;

            //debuglog("a resource has been changed");

            IResourceDelta $delta = event.getDelta();

            IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {

                public boolean visit(IResourceDelta delta)
                {

                    int kind = delta.getKind();
                    int flags = delta.getFlags();

                    switch (kind)
                    {
                    case IResourceDelta.ADDED:

                        processActivity("Resource added ", delta.getResource().getName());

                        if (flags == IResourceDelta.MOVED_FROM || flags == IResourceDelta.MOVED_TO) {
                            //eventlog("it has been moved");
                        }

                        break;

                    case IResourceDelta.REMOVED:

                        processActivity("Resource removed ", delta.getResource().getName());

                        if (flags == IResourceDelta.MOVED_FROM || flags == IResourceDelta.MOVED_TO) {
                            //eventlog("it has been moved");
                        }
                        break;

                    case IResourceDelta.CHANGED:

                        String name = delta.getResource().getName();

                        if (name != null)

                            processActivity("Resource changed ", delta.getResource().getName());

                        switch (flags)
                        {
                        case IResourceDelta.OPEN:

//                          if(delta.getResource().getType() == IResource.PROJECT)
//                          {
//                              this.activeProject = delta.getResource().getName();
//                          }
                            
                            processActivity("Resource opened or closed ", delta.getResource().getName());

                            break;

                        case IResourceDelta.CONTENT:

                            break;

                        case IResourceDelta.DESCRIPTION:

                            break;

                        case IResourceDelta.MARKERS:

                            break;

                        case IResourceDelta.TYPE:

                            break;

                        case IResourceDelta.SYNC:

                            break;

                        case IResourceDelta.REPLACED:

                            processActivity("Resource replaced", delta.getResource().getName());

                            break;

                        default:
                        //debuglog("a child resource must have been changed");
                        }

                        break;

                    }

                    return true;
                }

            };

           
                try {
                    $delta.accept(visitor);
                }
                catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
           
        }
    }
    
}
