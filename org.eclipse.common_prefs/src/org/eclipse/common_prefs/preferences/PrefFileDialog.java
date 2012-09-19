/*******************************************************************************
 * Copyright (c) 2011 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ericsson - initial API and implementation
 *******************************************************************************/

package org.eclipse.common_prefs.preferences;

import java.io.File;

import org.eclipse.common_prefs.StartupPlugin;
import org.eclipse.common_prefs.core.CommonPrefEPFResource;
import org.eclipse.common_prefs.core.CommonPrefEPFResources;
import org.eclipse.common_prefs.exportWizard.CommonPrefsFileHistory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


public class PrefFileDialog extends Dialog {

	public enum PrefFileDialogType {PREF_FILE_ADD, PREF_FILE_EDIT};
	private static int BROWSE_BUTTON_ID = 100; // Any number
	
	PrefFileDialogType dialogType = null;
	
    private String title;
    private String message;
    private String value = "";//$NON-NLS-1$
    private CommonPrefEPFResource selectedPrefFile = null;
    private CommonPrefEPFResource addedPrefFile = null;

    private Button okButton;
    private Text text;
    private Text errorMessageText;
    private String errorMessage;
    
    private Button validateButton;
	
    public PrefFileDialog(Shell parentShell,
    		PrefFileDialogType type,
    		CommonPrefEPFResource selectedPrefFile) {
        super(parentShell);
        
        this.message = "Type the location of a preference file:";
        this.dialogType = type;
        this.selectedPrefFile = selectedPrefFile;
        if (type == PrefFileDialogType.PREF_FILE_ADD) {
        	this.title = "Add Preference File";
        	
			// Get initial dir value
			String dirName = CommonPrefsFileHistory.getSuggestedDefault();
			if (dirName != null && dirName.length() > 0) {
				int separator= dirName.lastIndexOf(File.separatorChar); //$NON-NLS-1$
				if (separator != -1) {
					dirName = dirName.substring(0, separator);
				}
			} else {
				dirName = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
			}        	
			this.value = dirName;
        	
        } else {  	
        	this.title = "Edit Preference File";
        	
        	// Get initial dir value
        	this.value = selectedPrefFile.getResourceName();        	
        }
    }

    protected Button getOkButton() {
        return okButton;
    }

    public CommonPrefEPFResource getValue() {
        return addedPrefFile;
    }    
    
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            if (!validateInput())
            	return;
            value = text.getText();
            
        } else if (buttonId == BROWSE_BUTTON_ID){
			FileDialog fd = new FileDialog(getShell());
			fd.setText("Please select the preference file");
			fd.setFilterPath(value);
			fd.setFilterExtensions(new String[] {"*.epf", "*"}); //$NON-NLS-1$
			fd.setFilterNames(new String[] {"Preference Files (*.epf)", "All Files (*.*)"});				
			
			String f = fd.open();
			if (f != null) {
				text.setText(f);
			}
			
        } else {	
            value = null;
        }
        super.buttonPressed(buttonId);
    }

    protected boolean validateInput() {
        String errorMessage = null;
        addedPrefFile = null;
      
        // Validate the input string ends with .epf 
        String resourceName = text.getText();
        int resLen = resourceName.length();
        if (resLen < 4) {
        	setErrorMessage("Too short input value. Filename needs to end with .epf.");
        	return false;
        }
        if (resourceName.substring(resLen - 4).compareToIgnoreCase(".epf") != 0) {
        	setErrorMessage("Input value suffix should be .epf.");
        	return false;        	
        }
        
        // Validate  
        if (dialogType == PrefFileDialogType.PREF_FILE_ADD) {
			CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
			CommonPrefEPFResource pf = prefFiles.getPrefFile(text.getText());
			if (pf != null) {
				errorMessage = "The selected preference file is already in the list.";
			} else {
				try {
					addedPrefFile = prefFiles.addPrefFile(selectedPrefFile, text.getText(), false, false);
					if (validateButton.getSelection() && !addedPrefFile.exists()) {
						prefFiles.removePrefFile(addedPrefFile);
						errorMessage = "Input resource can not be accessed.";
					}
				} catch (Exception e) {
					errorMessage = "Invalid input: " + e.getMessage();
				}      
			}
        } else if (dialogType == PrefFileDialogType.PREF_FILE_EDIT){
			CommonPrefEPFResources prefFiles = StartupPlugin.getDefault().getCommonPrefFiles(null);
			CommonPrefEPFResource pf = prefFiles.getPrefFile(text.getText());
			if (pf != null && pf != selectedPrefFile) {
				errorMessage = "A preference file with selected location is already in the list.";
			} else {
				try {
					addedPrefFile = prefFiles.addPrefFile(selectedPrefFile, text.getText(), false, false);
					if (validateButton.getSelection() && !addedPrefFile.exists()) {
						prefFiles.removePrefFile(addedPrefFile);
						errorMessage = "Input resource can not be accessed.";
					} else					
						prefFiles.removePrefFile(selectedPrefFile);

				} catch (Exception e) {
					errorMessage = "Invalid input: " + e.getMessage();
				}
			}
        } else
        	return false;
        
        setErrorMessage(errorMessage);
        
        // Remember value only if the selected resource exists
        if (addedPrefFile != null && addedPrefFile.exists())
        	CommonPrefsFileHistory.remember(addedPrefFile.getResourceName());
        
        return (errorMessage == null);
    } 
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (title != null) {
			shell.setText(title);
		}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        okButton = createButton(parent, IDialogConstants.OK_ID,
                IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID,
                IDialogConstants.CANCEL_LABEL, false);
        
        // create a Browse button
        createButton(parent, BROWSE_BUTTON_ID, "Browse...", false);       
        
        text.setFocus();
        if (value != null) {
            text.setText(value);
            text.selectAll();
        }
    }

    /*
     * (non-Javadoc) Method declared on Dialog.
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        if (message != null) {
            Label label = new Label(composite, SWT.WRAP);
            label.setText(message);
            GridData data = new GridData(GridData.GRAB_HORIZONTAL
                    | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.VERTICAL_ALIGN_CENTER);
            data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
            label.setLayoutData(data);
            label.setFont(parent.getFont());
        }
        
        text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL));      
        text.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
            	setErrorMessage(null);
            }
        });        
        
        errorMessageText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
        errorMessageText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL));
        errorMessageText.setBackground(errorMessageText.getDisplay()
                .getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        setErrorMessage(errorMessage);
        
        // create a Validate checkbox to allow non accessible resource input
        // for example when adding a http address which isn't accessible
		validateButton = new Button(composite, SWT.CHECK);
		validateButton.setText("Validate that the resource exist.");
		validateButton.setSelection(true);
		GridData gdlb = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		validateButton.setLayoutData(gdlb);
		validateButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// When changing state of the validate button, also
				// allow user to click OK again.
	    		Control button = getButton(IDialogConstants.OK_ID);
	    		if (button != null) {
	    			button.setEnabled(true);
	    		}
			}
		});	
        
        applyDialogFont(composite);
        return composite;
    }

    /**
     * Sets or clears the error message.
     * If not <code>null</code>, the OK button is disabled.
     * 
     * @param errorMessage
     *            the error message, or <code>null</code> to clear
     * @since 3.0
     */
    public void setErrorMessage(String errorMessage) {
    	this.errorMessage = errorMessage;
    	if (errorMessageText != null && !errorMessageText.isDisposed()) {
    		errorMessageText.setText(errorMessage == null ? " \n " : errorMessage); //$NON-NLS-1$
    		// Disable the error message text control if there is no error, or
    		// no error text (empty or whitespace only).  Hide it also to avoid
    		// color change.
    		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130281
    		boolean hasError = errorMessage != null && (StringConverter.removeWhiteSpaces(errorMessage)).length() > 0;
    		errorMessageText.setEnabled(hasError);
    		errorMessageText.setVisible(hasError);
    		errorMessageText.getParent().update();
    		// Access the ok button by id, in case clients have overridden button creation.
    		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=113643
    		Control button = getButton(IDialogConstants.OK_ID);
    		if (button != null) {
    			button.setEnabled(errorMessage == null);
    		}
    	}
    }     
}
