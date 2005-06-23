package org.electrocodeogram;

import java.util.ArrayList;

/**
 * This class contains methods to access the protected fields of the SendingThread
 * for testing purposes. It is used by the automated tests.
 */
public class SendingThreadTest
{

    private SendingThread sendingThread = null;
    
    /**
     * Thie creates a new SendingThreadTest object.
     *
     */
    public SendingThreadTest()
    {
        this.sendingThread = SendingThread.getInstance();
    }
    
    /**
     * This method returns the current size of the EventPacketQeueu in the SendingThread
     * @return The current size of the EventPacketQeueu in the SendingThread
     */
    public int getBufferSize()
    {
        ArrayList<EventPacket> bufferCopy = createBufferCopy();
        
        return bufferCopy.size();
    }
    
    /**
     * This method tests wether the size of the EventPacketQueue in the SendingThread is equal to the given size. 
     * @param size The size to test against
     * @return "true", if the size is equal and "false" if not
     */
    public boolean testBufferSize(int size)
    {
        ArrayList<EventPacket> bufferCopy = createBufferCopy();
        
        if(bufferCopy.size() == size)
        {
            return true;
        }
        
        return false;
        
    }
 
    /**
     * This method tests wether the state beeing connected of the SendingThread is equal to the state within a period of given connection trials. 
     * @param connected The state to etst against
     * @param trials The number of trials to reach that state
     * @return "true", if the state is equal and "false" if not
     */
    public boolean testConnection(boolean connected, int trials)
    {
        int connectionTrialOffset = this.sendingThread.connectionTrials;
        
        int connectionTrialDelta = 0; 
        
        while(connectionTrialDelta > trials  && this.sendingThread.connectedFlag != connected)
        {
            connectionTrialDelta = this.sendingThread.connectionTrials - connectionTrialOffset;
        }
        
        if(this.sendingThread.connectedFlag == connected)
        {
            return true;
        }
       
        return false;
       
        
    }
    
    /**
     * This method tests wether the tailmost EventPacket of the EventPacketQueue in the SendingThread is equal to the given EventPacket.
     * @param eventPacket The EventPacket to test against
     * @return "true" if it is equal nad "false" if not
     */
    public boolean testLastElement(TestEventPacket eventPacket)
    {
        ArrayList<EventPacket> bufferCopy = createBufferCopy();
        
        EventPacket lastAdded = bufferCopy.get(bufferCopy.size()-1);
        
        if(lastAdded.getSourceId() == eventPacket.getSourceId() && lastAdded.getTimeStamp().equals(eventPacket.getTimeStamp()) && lastAdded.getHsCommandName().equals(eventPacket.getHsCommandName()) && lastAdded.getArglist().equals(eventPacket.getArglist()))
        {
            return true;
        }
        
        return false;
        
    }
    
    private ArrayList<EventPacket> createBufferCopy()
    {
        ArrayList<EventPacket> bufferCopy = new ArrayList<EventPacket>(); 
        
        for(EventPacket elem : this.sendingThread.queue)
        {
            bufferCopy.add(elem);
        }
        return bufferCopy;
    }
    
    /**
     * This method returns the current value for the connection delay in the SendingThread
     * @return The current value for the connection delay in the SendingThread
     */
    public int getConnectionDelay()
    {
        return this.sendingThread.connectionDelay;
    }
    
    /**
     * This method returns the current number of connection trials in the SendingThread
     * @return The current number of connection trials in the SendingThread
     */
    public int getConnectionTrials()
    {
        return this.sendingThread.connectionTrials;
    }
}
