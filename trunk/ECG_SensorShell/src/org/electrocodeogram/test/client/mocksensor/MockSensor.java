package org.electrocodeogram.test.client.mocksensor;

import java.util.Date;

import org.electrocodeogram.event.EventPacket;
import org.hackystat.kernel.admin.SensorProperties;
import org.hackystat.kernel.shell.SensorShell;

/**
 * This is the ECG TestSensor used for automated JUnit tests. It is capable of
 * generating valid and different kinds of invalid EventPackets and it defines
 * methods to send these EventPackets.
 */
public class MockSensor
{

    private SensorShell shell = null;

    private SensorProperties properties = null;
    
    protected Date sendingTime = null;
    
    

    /**
     * Creates a TestSensor instance and initializes it with a SensorShell.
     *
     */
    public MockSensor()
    {
        this.properties = new SensorProperties("TestSensor");

        this.shell = new SensorShell(this.properties, false);
    }

   
    
    /**
     * This method passes a single given EventPacket to the ECG SensorShell.
     * 
     * @param eventPacket
     *            The EventPacket to pass
     * @return The result as given by the ECG SensorShell. "true" means the
     *         EventPacket is syntactically valid and accepted. "false" means
     *         the EventPacket is syntactically invalid and not accepted.
     */
    public boolean sendEvent(EventPacket eventPacket)
    {
        this.sendingTime = new Date();
        
        return this.shell.doCommand(eventPacket.getTimeStamp(), eventPacket.getSensorDataType(), eventPacket.getArglist());
    }

    public Date getSendingTime()
    {
        Date toReturn = this.sendingTime;
        
        this.sendingTime = null;
        
        return toReturn;
    }

}
