/*
 * (c) Freie Universitšt Berlin - AG Software Engineering - 2006
 */
package org.electrocodeogram.module.intermediate.implementation;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.logging.LogHelper;

/**
 * An EpisodeRecognizerManager manages multiple EpisodeRecognizer instances
 * of a single kind. Usually, at leat one recognizer in its initial state
 * needs to be available for any new event because many potential episodes may
 * be possible in parallel. Each EpisodeRecognizerIntermediateModule has one
 * Manager which manages exactly one kind of EpisodeRecognizer. 
 *
 */
public class EpisodeRecognizerManager {

	/**
	 * Log instance. 
	 */
	private static Logger logger = LogHelper
    .createLogger(EpisodeRecognizerIntermediateModule.class.getName());

	/**
	 * List of recognizers which are active and may emit episodes.
	 */
	private ArrayList<EpisodeRecognizer> recognizers = null;
	
	/**
	 * String of the class name (without package) for the EpisodeRecognizer 
     * kind.
	 */
	private Class recognizerClass = null;
	
	/**
	 * Holds a fresh EpisodeRecognizer in initial state.
	 */
	private EpisodeRecognizer freeRecognizer = null;
	
	/**
	 * Reference to the EpisodeRecognizerIntermediateModule. 
	 */
	private EpisodeRecognizerIntermediateModule module = null; 
	
    
	/**
     * Standard Constructor.
     * 
	 * @param module the creator
	 * @param classname class name of the EpisodeRecognizer without package path
	 */
	public EpisodeRecognizerManager(EpisodeRecognizerIntermediateModule module, String classname) {

		try {
			recognizerClass = module.getClass().getClassLoader().loadClass(classname);
		} catch (ClassNotFoundException e) {
	        logger.log(Level.SEVERE, "Couldn't load Recognizer " + classname);
			e.printStackTrace();
		}
		
		this.recognizers = new ArrayList<EpisodeRecognizer>();
		this.module = module;
		
	}
	
	/**
     * @see org.electrocodeogram.module.intermediate.implementation.IntermediateModule#analyse(org.electrocodeogram.event.ValidEventPacket)
     * 
	 * @param packet
	 * @return
	 */
	public ValidEventPacket analyse(ValidEventPacket packet) {

		ValidEventPacket event = null;

		ValidEventPacket recognizerEvent = null;
		
		if (freeRecognizer == null && recognizerClass != null) {
			try {
				freeRecognizer = (EpisodeRecognizer) recognizerClass.newInstance();
			} catch (InstantiationException e) {
                logger.log(Level.SEVERE, "Couldn't create Recognizer instance of kind " 
                        + recognizerClass.getName());
				e.printStackTrace();
			} catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, "Couldn't access Recognizer instance of kind " 
                        + recognizerClass.getName());
				e.printStackTrace();
			}
		}			

		for (ListIterator<EpisodeRecognizer> i = recognizers.listIterator(); i.hasNext(); ) {
	    
			EpisodeRecognizer recognizer = i.next();
			
			// Do it for active recognizers!
			recognizerEvent = recognizer.analyse(packet, this.module.getMinDuration());
			
			if (event != null && recognizerEvent != null) {
		        logger.log(Level.WARNING, "double episode in EpisodeRecognizer (used!) at time " + recognizerEvent.getTimeStamp());
                logger.log(Level.WARNING, "   old: " + event.toString());                
                logger.log(Level.WARNING, "   new: " + recognizerEvent.toString());                
            }

			if (recognizerEvent != null) {
				event = recognizerEvent;
            }
			
			if (recognizer.isInFinalState()) {
				i.remove();
            }
		}

		// Do it for the free recognizer
		recognizerEvent = freeRecognizer.analyse(packet, this.module.getMinDuration());

        // Look for existing recognizer with equal state. If, forget the free
        // one. "equal" depends on type of recognizer. It should be true if both
        // would react equally on the next events so one of them is
        for (ListIterator<EpisodeRecognizer> i = recognizers.listIterator(); i.hasNext(); ) {
                EpisodeRecognizer recognizer = i.next();
                if (freeRecognizer.equals(recognizer)) {
                    freeRecognizer = null;
                    recognizerEvent = null;
                    break;
                }
        }
        
		if (event != null && recognizerEvent != null) {
            // This warning indicates that there should be a way to emit
            // more than one episode from a single event coming in
	        logger.log(Level.WARNING, "double episode in EpisodeRecognizer (free!) at time " + recognizerEvent.getTimeStamp());
            logger.log(Level.WARNING, "   old: " + event.toString());                
            logger.log(Level.WARNING, "   new: " + recognizerEvent.toString());                
        }

		if (recognizerEvent != null) {
			event = recognizerEvent;
        }

		if (freeRecognizer != null && !freeRecognizer.isInInitialState()) {
			if (!freeRecognizer.isInFinalState())
				recognizers.add(freeRecognizer);
			freeRecognizer = null;
		}

		logger.log(Level.FINE, "number of recogs: " + recognizers.size() + " at time " + packet.getTimeStamp());
		logger.log(Level.FINE, "  they are: " + recognizers.toString());			
		
        return event;

    }
	
}
