package org.electrocodeogram.cpc.core.api.hub.event;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This event is generated by the <em>CPC Sensor</em> module, whenever a file is opened or closed
 * in an editor or when an editor window gains or looses the input focus.
 * <br>
 * The event only contains information about the type of event. 
 * 
 * @author vw
 */
public class EclipseEditorPartEvent extends EclipseEvent
{
	private static Log log = LogFactory.getLog(EclipseEditorPartEvent.class);

	/**
	 * The type of an {@link EclipseEditorPartEvent}. 
	 */
	public enum Type
	{
		/**
		 * The file was just opened in an editor window.
		 */
		OPENED,

		/**
		 * The editor window for this file just obtained the input focus.
		 */
		ACTIVATED,

		/**
		 * The editor window for this file just lost the input focus.
		 */
		DEACTIVATED,

		/**
		 * The editor window for this file was just closed.
		 */
		CLOSED
	}

	protected Type type;

	/**
	 * Creates a new {@link EclipseEditorPartEvent} for the given
	 * user and project. 
	 * 
	 * @param user the current user, never null.
	 * @param project the project for the file affected by this event, never null.
	 */
	public EclipseEditorPartEvent(String user, String project)
	{
		super(user, project);

		log.trace("EclipseEditorPartEvent(...)");
	}

	/**
	 * Retrieves the type of this event.
	 * 
	 * @return the type of this editor part event, never null.
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Sets the type of this event.
	 * <p>
	 * This is a required value.
	 * 
	 * @param type the type of this editor part event, never null.
	 */
	public void setType(Type type)
	{
		if (log.isTraceEnabled())
			log.trace("setType(): " + type);
		assert (type != null);

		checkSeal();

		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * @see org.electrocodeogram.cpc.core.api.hub.event.EclipseEvent#isValid()
	 */
	@Override
	public boolean isValid()
	{
		if (type == null)
			return false;

		return super.isValid();
	}

	/*
	 * (non-Javadoc)
	 * @see org.electrocodeogram.cpc.core.api.hub.event.CPCEvent#toString()
	 */
	@Override
	public String toString()
	{
		return "EclipseEditorPartEvent[" + super.subToString() + ", type: " + type + "]";
	}

}
