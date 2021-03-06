/*
 * Class: IntermediateModule
 * Version: 1.0
 * Date: 18.10.2005
 * By: Frank@Schlesinger.com
 */

package org.electrocodeogram.module.intermediate;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.logging.LogHelper;
import org.electrocodeogram.module.Module;
import org.electrocodeogram.modulepackage.ModuleProperty;
import org.electrocodeogram.modulepackage.ModulePropertyException;
import org.electrocodeogram.modulepackage.ModuleType;

/**
 * This abstract class needs to be subclassed for every new intermediate
 * module. The abstract method {@link #analyse(ValidEventPacket)} is to be implemented to do the
 * actual analysis that is required by the module.
 */
public abstract class IntermediateModule extends Module implements
    IIntermediateModule {

    /**
     * This is the default separator string.
     */
    private static final String DEFAULT_SEPARATOR = "";

    /**
     * This is the logger.
     */
    private static Logger logger = LogHelper
        .createLogger(IntermediateModule.class.getName());

    /**
     * If an <code>IntermediateModule</code> is operating in annotation
     * mode, the <em>AnnotationStyle</em> tells when the module will
     * send its annotation events.
     */
    public enum AnnotationStyle {
        /**
         * The module will first send annotation event(s) and then
         * send the original event(s).
         */
        PRE_ANNOTATION,

        /**
         * The module will first send the original event(s) and then
         * send the annotation event(s).
         */
        POST_ANNOTATION
    }

    /**
     * The <em>ProcessingMode</em> tells whether the module operates
     * as an annotator or as a filter.
     */
    public enum ProcessingMode {
        /**
         * The module operates as an annotator.
         */
        ANNOTATOR,

        /**
         * The module operates as a filter.
         */
        FILTER
    }

    /**
     * This is the currently used seperator string.
     */
    private String separator = DEFAULT_SEPARATOR;

    /**
     * This is the current <em>ProcessingMode</em>.
     */
    private ProcessingMode processingMode;

    /**
     * This is the current <em>AnnotationStyle</em>.
     */
    private AnnotationStyle annotationStyle;

    /**
     * Creates a new <em>IntermediateModule</em> with the given
     * <em>ProcessingMode</em>.
     * @param id
     *            Is the string id of the <em>ModulePackage</em>.
     * @param name
     *            The name to assign to this module
     */
    public IntermediateModule(final String id, final String name) {
        super(ModuleType.INTERMEDIATE_MODULE, id, name);

        logger.entering(this.getClass().getName(), "IntermediateModule",
            new Object[] {id, name});

        this.processingMode = ProcessingMode.ANNOTATOR;

        this.annotationStyle = AnnotationStyle.POST_ANNOTATION;

        initialize();

        logger.exiting(this.getClass().getName(), "IntermediateModule");

    }

    /**
     * @see org.electrocodeogram.module.Module#update()
     */
    @Override
    public abstract void update();

    /**
     * @see org.electrocodeogram.module.Module#propertyChanged(org.electrocodeogram.modulepackage.ModuleProperty)
     */
    @Override
    protected abstract void propertyChanged(ModuleProperty moduleProperty)
    throws ModulePropertyException;

    /**
     * This method returns the <em>AnnotationStyle</em> that is set
     * for the module.
     * @return The <em>AnnotationStyle</em> style
     */
    public final AnnotationStyle getAnnnotationStyle() {
        logger.entering(this.getClass().getName(), "getAnnnotationStyle");

        logger.exiting(this.getClass().getName(), "getAnnnotationStyle",
            this.annotationStyle);

        return this.annotationStyle;
    }

    /**
     * This method is used to set the <em>AnnotationStyle</em> of
     * the module to the given <em>AnnotationStyle</em>.
     * @param style
     *            Is the new <em>AnnotationStyle</em> of the module
     */
    public final void setAnnnotationStyle(final AnnotationStyle style) {
        logger.entering(this.getClass().getName(), "setAnnnotationStyle",
            new Object[] {style});

        if (style == null) {
            logger.log(Level.WARNING,
                "The parameter \"annotationStyle\" is null");

            logger.exiting(this.getClass().getName(), "setAnnnotationStyle");

            return;
        }

        this.annotationStyle = style;

        logger.exiting(this.getClass().getName(), "setAnnnotationStyle");
    }

    /**
     * This returns the <em>ProcessingMode</em>.
     * @return The <em>ProcessingMode</em>
     */
    public final ProcessingMode getProcessingMode() {
        logger.entering(this.getClass().getName(), "getProcessingMode");

        logger.exiting(this.getClass().getName(), "getProcessingMode",
            this.processingMode);

        return this.processingMode;

    }

    /**
     * This sets the <em>ProcessingMode</em> of the module to the
     * given mode.
     * @param mode
     *            Is the new <em>ProcessingMode</em> for the module
     */
    public final void setProcessingMode(final ProcessingMode mode) {
        logger.entering(this.getClass().getName(), "setProcessingMode",
            new Object[] {mode});

        if (mode == null) {
            logger.log(Level.WARNING,
                "The parameter \"processingMode\" is null");

            logger.exiting(this.getClass().getName(), "setProcessingMode");

            return;
        }

        this.processingMode = mode;

        logger.exiting(this.getClass().getName(), "setProcessingMode");

    }

    /**
     * This method returns the separator string that is
     * used by this module.
     * @return The separator string
     */
    public final String getSeparator() {
        logger.entering(this.getClass().getName(), "getSeparator");

        logger.exiting(this.getClass().getName(), "getSeparator",
            this.separator);

        return this.separator;
    }

    /**
     * This sets the separator string of the module to
     * the given string value.
     * @param string
     *            Is the string value to use a the new
     *            separator string
     */
    public final void setSeparator(final String string) {
        logger.entering(this.getClass().getName(), "setSeparator",
            new Object[] {string});

        if (string == null) {
            logger.log(Level.WARNING, "The parameter \"separator\" is null");

            logger.exiting(this.getClass().getName(), "setSeparator");

            return;
        }

        this.separator = string;

        logger.exiting(this.getClass().getName(), "setSeparator");
    }

    /**
     * @see org.electrocodeogram.module.Module#receiveEventPacket(org.electrocodeogram.event.ValidEventPacket)
     *      In addition to its superclass method this method gets the
     *      analysis result event of the module and sends them
     *      according to the processing mode and annotation style of
     *      the module.
     */
    @Override
    public final void receiveEventPacket(final ValidEventPacket eventPacket) {

        logger.entering(this.getClass().getName(), "receiveEventPacket",
            new Object[] {eventPacket});

        Collection<ValidEventPacket> resultPackets = getAnalysisResult(eventPacket);

        if (this.processingMode == ProcessingMode.ANNOTATOR 
                && this.annotationStyle != AnnotationStyle.PRE_ANNOTATION) {
                sendEventPacket(eventPacket);
        }
            
        if (resultPackets != null)
            for (ValidEventPacket packet : resultPackets)
                sendEventPacket(packet);

        if (this.processingMode == ProcessingMode.ANNOTATOR 
                && this.annotationStyle == AnnotationStyle.PRE_ANNOTATION) {
                sendEventPacket(eventPacket);
        }

        logger.exiting(this.getClass().getName(), "receiveEventPacket");
    }

    /**
     * Is used to call the actual analysis implementation.
     * @param eventPacket
     *            Is the event to be analysed
     * @return The event that is the result of the analysis
     */
    private Collection<ValidEventPacket> getAnalysisResult(
        final ValidEventPacket eventPacket) {
        logger.entering(this.getClass().getName(), "getAnalysisResult");

        Collection<ValidEventPacket> packets = analyse(eventPacket);

        logger.exiting(this.getClass().getName(), "getAnalysisResult", packets);

        return packets;
    }

    /**
     * This method is to be implemented by all subclassing
     * modules. For any given input event it
     * shall compute and return an output event. That is the analysis
     * result. If the module does not want to generate any output events
     * at the moment, <code>null</code> can be returned.
     * @param eventPacket
     *            Is the original incoming event
     * @return The data of a collection of events that is a result of the analysis
     */
    public abstract Collection<ValidEventPacket> analyse(ValidEventPacket eventPacket);

    /**
     * @see org.electrocodeogram.module.Module#initialize()
     */
    @Override
    public abstract void initialize();
}
