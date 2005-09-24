package org.electrocodeogram.module.intermediate;

import org.electrocodeogram.event.TypedValidEventPacket;
import org.electrocodeogram.module.Module;

/**
 * This abstract class shall be subclassed for every new intermediate module.
 * The abstract method analyse is to be implemented to do the actual analysis that is
 * required by the module.
 */
public abstract class IntermediateModule extends Module implements IIntermediateModule
{

	/**
	 * If an intermediate module is operating in annotation mode, the AnnotationStyle
	 * tells when the module will send its annotation events.
	 *
	 */
	public enum AnnotationStyle
	{
		/**
		 * The module will first send an annotation event(s) and then send the original event(s). 
		 */
		PRE_ANNOTATION,

		/**
		 * The module will first send the original event(s) and then send the an annotation event(s). 
		 */
		POST_ANNOTATION
	}

	/**
	 * 
	 * The ProcessingMode defines whether the module operates as an annotator or as a filter.
	 *
	 */
	public enum ProcessingMode
	{
		/**
		 * The module operates as an annotator.
		 */
		ANNOTATOR,

		/**
		 * The module operates as a filter.
		 */
		FILTER
	}

	private String $separator = "";

	private ProcessingMode $processingMode;

	private AnnotationStyle $annotationStyle;

	/**
	 * This creates a new IntermediateModule with the given processing mode.
	 * @param moduleClassId Is the id of the module class as registered with the ModuleRegistry
	 * @param name The name given to this moduule instance
	 */
	public IntermediateModule(String moduleClassId, String name)
	{
		super(ModuleType.INTERMEDIATE_MODULE, moduleClassId, name);

		this.$processingMode = ProcessingMode.ANNOTATOR;

		this.$annotationStyle = AnnotationStyle.POST_ANNOTATION;

		initialize();

	}

	/**
	 * This method returns the annotation style that is set for the module.
	 * @return The annotation style
	 */
	public AnnotationStyle getAnnnotationStyle()
	{
		return this.$annotationStyle;
	}

	/**
	 * This method is used to set the annotation style of the module to the given annotation style.
	 * @param annotationStyle Is the new annotation style of the module 
	 */
	public void setAnnnotationStyle(AnnotationStyle annotationStyle)
	{
		this.$annotationStyle = annotationStyle;
	}

	/**
	 * This returns the processing mode the module is operating in.
	 * @return The processing mode
	 */
	public ProcessingMode getProcessingMode()
	{
		return this.$processingMode;
	}

	/**
	 * This sets the processing mode of the module to the given mode.
	 * @param processingMode Is the new processing mode of the module.
	 */
	public void setProcessingMode(ProcessingMode processingMode)
	{
		this.$processingMode = processingMode;
	}

	/**
	 * This method returns the separator string that this module uses.
	 * @return The separator string
	 */
	public String getSeparator()
	{
		return this.$separator;
	}

	/**
	 * This sets the separator string of the module to the given string value.
	 * @param separator Is the string value to use a the new separator string
	 */
	public void setSeparator(String separator)
	{
		this.$separator = separator;
	}

	/**
	 * @see org.electrocodeogram.module.Module#receiveEventPacket(org.electrocodeogram.event.TypedValidEventPacket)
	 * In addition to its superclass method this method gets the analysis result events of the module
	 * and sends them according to the processing mode and annotation style of the module.
	 */
	@Override
	public final void receiveEventPacket(TypedValidEventPacket eventPacket)
	{

		if (this.$processingMode == ProcessingMode.ANNOTATOR)
		{
			TypedValidEventPacket resultPacket = getAnalysisResult(eventPacket);

			if (this.$annotationStyle == AnnotationStyle.PRE_ANNOTATION)
			{
				sendEventPacket(resultPacket);
				sendEventPacket(eventPacket);
			}
			else
			{
				sendEventPacket(eventPacket);
				sendEventPacket(resultPacket);
			}
		}
		else
		{
			TypedValidEventPacket resultPacket = getAnalysisResult(eventPacket);

			sendEventPacket(resultPacket);
		}
	}

	private TypedValidEventPacket getAnalysisResult(TypedValidEventPacket eventPacket)
	{
		return analyse(eventPacket);
	}

	/**
	 * This method is to be implemented by all subclassing intermediate modules.
	 * For any given input event it shall compute and return an output event.
	 * That is the analysis result.
	 * @param eventPacket Is the original incoming event data
	 * @return The data of an event that is a result of the analysis
	 */
	public abstract TypedValidEventPacket analyse(TypedValidEventPacket eventPacket);

	/**
	 * @see org.electrocodeogram.module.Module#initialize()
	 */
	@Override
	public abstract void initialize();
}