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

package org.eclipse.common_prefs.exportWizard;

import org.eclipse.common_prefs.StartupPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;


/**
 * Wizard to export selected preferences. The common export preferences wizard, see
 * org.eclipse.ui.internal.wizards.preferences.WizardPreferencesExportPage1, has
 * options to export either all preferences or only the ones implementing the
 * org.eclipse.ui.preferenceTransfer extension point.<br>
 * Not many plug-ins do this. Hence this augmented wizard that allows export of selected
 * preferences. It also gives an option to filter out default values.
 * 
 * @author Domenic Alessi
 *
 */
public class CommonPrefsExportWizard extends Wizard implements IExportWizard {

	private static String DESC_WIZABAN_EXPORT = "exportpref_wiz.png"; //$NON-NLS-1$
	
	protected CommonPrefsExportPage pageOne;	
	
	@Override
	public boolean performFinish() {
		return pageOne.performFinish();
	}

    public void addPages() {
        super.addPages(); 
        addPage(pageOne); 
    }
	
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
		setWindowTitle("Common Preference Export Wizard"); //NON-NLS-1
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(StartupPlugin.getImage(DESC_WIZABAN_EXPORT));

		pageOne = new CommonPrefsExportPage(
				"Export Preferences",
				"Export Preferences",
				null);
	}
}
