package com.github.tomhallman.mist.preferences.fieldeditors;

import static com.github.tomhallman.mist.util.ui.GridDataUtil.applyGridData;
import static com.github.tomhallman.mist.util.ui.GridDataUtil.onGridData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A field editor for displaying labels not associated with other widgets.
 * 
 * @see https://www.eclipse.org/articles/Article-Field-Editors/field_editors.html
 */
class LabelFieldEditor extends FieldEditor {
    private static Logger log = LogManager.getLogger();

    private Label label;

    // All labels can use the same preference name since they don't
    // store any preference.
    public LabelFieldEditor(String value, Composite parent) {
        super("label", value, parent);
        log.trace("LabelFieldEditor({},{})", value, parent);
    }

    // Adjusts the field editor to be displayed correctly
    // for the given number of columns.
    protected void adjustForNumColumns(int numColumns) {
        onGridData(label).horizontalSpan(numColumns);
    }

    // Fills the field editor's controls into the given parent.
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        label = getLabelControl(parent);

        applyGridData(label).horizontalSpan(numColumns).horizontalAlignment(SWT.FILL).verticalAlignment(SWT.CENTER);
        // grabExcessHorizontalSpace = false;
        // grabExcessVerticalSpace = false;
    }

    // Returns the number of controls in the field editor.
    public int getNumberOfControls() {
        return 1;
    }

    // Labels do not persist any preferences, so these methods are empty.
    protected void doLoad() {
    }

    protected void doLoadDefault() {
    }

    protected void doStore() {
    }
}
