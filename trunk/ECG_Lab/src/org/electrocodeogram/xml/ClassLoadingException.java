/*
 * Class: ClassLoadingException
 * Version: 1.0
 * Date: 18.10.2005
 * By: Frank@Schlesinger.com
 */
package org.electrocodeogram.xml;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.logging.LogHelper;

/**
 * If a module class is loaded while parsing <em>ModuleSetup</em> or
 * <em>ModuleProperties</em> files and an error occurs, this
 * <code>Exception</code> is thrown.
 */
public class ClassLoadingException extends Exception {

    /**
     * This is the logger.
     */
    private static Logger logger = LogHelper
        .createLogger(ClassLoadingException.class.getName());

    /**
     * This is the <em>Serialization</em> id.
     */
    private static final long serialVersionUID = 2292155480118662068L;

    /**
     * This creates the <code>Exception</code>.
     */
    public ClassLoadingException() {
        logger.entering(this.getClass().getName(), "ClassLoadingException");

        logger.log(Level.SEVERE, "An ClassLoadingException occured");

        logger.log(Level.SEVERE, this.getMessage());

        logger.exiting(this.getClass().getName(), "ClassLoadingException");
    }
}
