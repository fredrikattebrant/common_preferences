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

package org.eclipse.common_prefs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.common_prefs.core.CommonPrefsHelper;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


/**
 * The class will be instantiated on startup and handle the reading of 
 * preferences. 
 * 
 * @author Domenic Alessi
 *
 */
public class StartupClass implements IStartup {

	List<String> prefFiles = null;

	public StartupClass() {
		super();
		prefFiles = new ArrayList<String>();
	}
	
	/** 
	 * The method will check if common preference files are to be loaded. If so, they
	 * will be loaded depending on what type of flag is set.
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	public void earlyStartup() {
		
		// Check if common preference reading is enabled
		IPreferenceStore store = StartupPlugin.getDefault().getPreferenceStore();
		boolean readPrefs = true;
		if (store.contains(PreferenceInitializer.PREF_ENABLE_READ))
			readPrefs = store.getBoolean(PreferenceInitializer.PREF_ENABLE_READ);
		if (!readPrefs)
			return;
		
		/*
		 * Call to pref reading needs to be in context of code below since display
		 * etc needs to be up when calling to have listeners react properly on updates.
		 * 
		 * NOTE: Hence will not Common Prefs code affect headless invocations.
		 */
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final boolean wsIsInitialized = store.getBoolean(PreferenceInitializer.PREF_WS_INITIALIZED);
		workbench.getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
				if (window == null)
					return;

				// If debugging, record an entry in the log. Since also gives a
				// ms accurate timestamp it can be used for checking performance
				if (StartupPlugin.getDefault().isDebugging())
					StartupPlugin.log(new Status(
							IStatus.INFO,
							StartupPlugin.PLUGIN_ID,
							"Common Preferences Plugin earlyStartup called."));				
				
				if (!wsIsInitialized){
					
					CommonPrefsHelper.saveDefaultPreferences();
				}
				
				IStatus status = CommonPrefsHelper.loadPreferences();
				if (status.getChildren().length > 0)
					StartupPlugin.log(status);
				
				// If debugging, record an exit entry in the log
				if (StartupPlugin.getDefault().isDebugging())
					StartupPlugin.log(new Status(
							IStatus.INFO,
							StartupPlugin.PLUGIN_ID,
							"Common Preferences Plugin earlyStartup finished."));
			}
		});
	}
}
