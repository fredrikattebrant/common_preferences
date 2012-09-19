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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public static final String PREF_ENABLE_READ = "read_common_prefs"; //$NON-NLS-1$
	public static final String PREF_SHOW_CURRENT = "show_current_prefs"; //$NON-NLS-1$	
	public static final String PREF_WS_INITIALIZED = "ws_initialized"; //$NON-NLS-1$
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = StartupPlugin.getDefault().getPreferenceStore();
		store.setDefault(PREF_WS_INITIALIZED, false);
		store.setDefault(PREF_ENABLE_READ, true);
		store.setDefault(PREF_SHOW_CURRENT, true);
	}
}	
