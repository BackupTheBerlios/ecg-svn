package org.electrocodeogram.module.target;

import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.module.Module;


/**
 * This abstract class shall be subclasssed by all target modules that
 * are indended to rite out the event data they receive.
 * The abstract method write is to be implemented to do the actual writing
 *
 */
public abstract class EventWriter extends Module
{

   /**
    * This creates the module.
    *
    */
    public EventWriter()
    {
        super(ModuleType.TARGET_MODULE);
    }

    
    /**
     * @see org.electrocodeogram.module.Module#receiveEventPacket(org.electrocodeogram.event.ValidEventPacket)
     * In addition to its superclass method this methos writes out every event it
     * receives, by calling the module's write method.
     */
    @Override
    public void receiveEventPacket(ValidEventPacket eventPacket)
    {
        setChanged();
        
        notifyObservers(eventPacket);
       
        clearChanged();
        
        write(eventPacket); 
    }
    
   /**
    * This method is to be implemented to do the actual writng of incoming events. 
    * @param eventPacket Is the incoming event that is to be written out
    */
    public abstract void write(ValidEventPacket eventPacket);

}