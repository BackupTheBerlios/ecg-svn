package org.electrocodeogram.module.intermediate.implementation.recognizers;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.event.IllegalEventParameterException;
import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.event.WellFormedEventPacket;
import org.electrocodeogram.logging.LogHelper;
import org.electrocodeogram.misc.xml.ECGParser;
import org.electrocodeogram.misc.xml.NodeException;
import org.electrocodeogram.module.intermediate.implementation.EpisodeRecognizer;
import org.electrocodeogram.module.intermediate.implementation.EpisodeRecognizerIntermediateModule;
import org.w3c.dom.Document;

public class WindowActiveEpisodeRecognizer implements EpisodeRecognizer {

    public enum WindowActiveEpisodeState {start, windowactive, stop}
	
	private static Logger logger = LogHelper
    .createLogger(EpisodeRecognizerIntermediateModule.class.getName());

	private WindowActiveEpisodeState state;
	
	private String activeWindow = null;
	
	private Date startDate = null;
	
	public static DateFormat dateFormat = DateFormat.getDateTimeInstance(
	        DateFormat.MEDIUM, DateFormat.MEDIUM);
		
	public WindowActiveEpisodeRecognizer() {
		state = WindowActiveEpisodeState.start;
	}
	
	public boolean isInInitialState() {
		return state == WindowActiveEpisodeState.start;
	}
	
	public boolean isInFinalState() {
		return state == WindowActiveEpisodeState.stop;
	}
	
    public ValidEventPacket analyse(ValidEventPacket packet, int id, long minDuration) {

		ValidEventPacket event = null;

		String msdt = packet.getMicroSensorDataType().getName();

		try {

			if (msdt.equals("msdt.window.xsd")) {

				Document document = packet.getDocument();
				Date timestamp = packet.getTimeStamp();

				String activity = ECGParser.getSingleNodeValue("activity", document);
				String windowname = ECGParser.getSingleNodeValue("windowname", document);
				
				if (state == WindowActiveEpisodeState.start && activity.equals("activated")) {
					state = WindowActiveEpisodeState.windowactive;
					activeWindow = windowname;
					startDate = timestamp;
				}
				else if (state == WindowActiveEpisodeState.windowactive && 
						activity.equals("deactivated") /*&& 
						windowname.equals(activeWindow)*/) {
					event = generateEpisode(id, "msdt.windowactive.xsd", minDuration,
							ECGParser.getSingleNodeValue("username", document),
							ECGParser.getSingleNodeValue("projectname", document),
							timestamp,
							startDate,
							activeWindow);
					activeWindow = null;
					startDate = null;
					state = WindowActiveEpisodeState.stop;
				}

			}

		} catch (NodeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        return event;

    }
	
	public String toString() {
		return activeWindow + " in state " + state;
	}

	private ValidEventPacket generateEpisode(int id, String msdt, long minDur,
			String username, String projectname, Date end, 
			Date begin, String filename) {

		ValidEventPacket event = null;
		long duration = end.getTime() - begin.getTime();
		
		if (duration < minDur)
			return null;

        String data = "<?xml version=\"1.0\"?><microActivity>";
		
		data += "<username>"  + username  + "</username>";

		data += "<projectname>" + projectname + "</projectname>";

		data += "<starttime>" + this.dateFormat.format(begin) + "</starttime>";

		data += "<duration>" + duration + "</duration>";

		data += "<resourcename>" + filename + "</resourcename>";

		data += "</microActivity>";

        logger.log(Level.FINE, "windowactive event created");
        logger.log(Level.FINE, data);

        String[] args = {WellFormedEventPacket.HACKYSTAT_ADD_COMMAND, msdt,
            data};

        try {
            event = new ValidEventPacket(id, end,
                WellFormedEventPacket.HACKYSTAT_ACTIVITY_STRING, Arrays
                    .asList(args));

        } catch (IllegalEventParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		
		return event;

	}
	

}
