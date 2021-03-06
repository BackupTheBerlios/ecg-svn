package de.fu_berlin.inf.focustracker.ui.preferences;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ProbabilityFieldEditor extends StringFieldEditor{
	
    private double minValidValue = 0;

    private double maxValidValue = 1.0d;

    private static final int DEFAULT_TEXT_LIMIT = 10;

    /**
     * Creates a new probability field editor 
     */
    protected ProbabilityFieldEditor() {
    }

    /**
     * Creates an probability field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public ProbabilityFieldEditor(String name, String labelText, Composite parent) {
        this(name, labelText, parent, DEFAULT_TEXT_LIMIT);
    }

    /**
     * Creates an probability field editor.
     * 
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     * @param textLimit the maximum number of characters in the text.
     */
    public ProbabilityFieldEditor(String name, String labelText, Composite parent,
            int textLimit) {
        init(name, labelText);
        setTextLimit(textLimit);
        setEmptyStringAllowed(false);
        setErrorMessage("Value must be numeric and between 0 and 1!");//$NON-NLS-1$
        createControl(parent);
    }

    /**
     * @see org.eclipse.jface.preference.StringFieldEditor#checkState()
     */
    protected boolean checkState() {

        Text text = getTextControl();

        if (text == null) {
			return false;
		}

        String numberString = text.getText();
        try {
            double number = Double.valueOf(numberString).doubleValue();
            if (number >= minValidValue && number <= maxValidValue) {
				clearErrorMessage();
				return true;
			}
            
			showErrorMessage();
			return false;
			
        } catch (NumberFormatException e1) {
            showErrorMessage();
        }

        return false;
    }

    /**
     * @see org.eclipse.jface.preference.StringFieldEditor#doLoad()
     */
    protected void doLoad() {
        Text text = getTextControl();
        if (text != null) {
            double value = getPreferenceStore().getDouble(getPreferenceName());
            text.setText("" + value);//$NON-NLS-1$
        }

    }

    /**
     * @see org.eclipse.jface.preference.StringFieldEditor#doLoadDefault()
     */
    protected void doLoadDefault() {
        Text text = getTextControl();
        if (text != null) {
            double value = getPreferenceStore().getDefaultDouble(getPreferenceName());
            text.setText("" + value);//$NON-NLS-1$
        }
        valueChanged();
    }

    /**
     * @see org.eclipse.jface.preference.StringFieldEditor#doStore()
     */
    protected void doStore() {
        Text text = getTextControl();
        if (text != null) {
            Double i = new Double(text.getText());
            getPreferenceStore().setValue(getPreferenceName(), i.doubleValue());
        }
    }

    /**
     * Returns this field editor's current value as an integer.
     *
     * @return the value
     * @exception NumberFormatException if the <code>String</code> does not
     *   contain a parsable integer
     */
    public double getDoubleValue() throws NumberFormatException {
        return new Double(getStringValue()).doubleValue();
    }
}
