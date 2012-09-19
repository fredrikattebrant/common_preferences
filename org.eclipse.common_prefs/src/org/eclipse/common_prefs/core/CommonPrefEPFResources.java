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

package org.eclipse.common_prefs.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.common_prefs.StartupPlugin;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;


/**
 * The class holds the ordered list of Preference Files. Implements logic to
 * load and save the preferences from storage. Note that the preference file
 * entries are read both from the config.ini, and from the user's preferences.
 * <p>
 * The entries in the config.ini are regarded as read-only. If user adds any
 * entries in addition to the ones defined in the config.ini file, these are
 * stored as follows:
 * <p>
 * <code>
 * ericsson.common_prefs.<no> =
 * 		[[init|force]|<filepath>] |
 * 		[config|ericsson.common_prefs.<no>]
 * </code>
 * <p>
 * A sample below:
 * <p>
 * <code>
 * 		ericsson.common_prefs.0 = init|/local/share/common.epf <br>
 * 		ericsson.common_prefs.1 = config|ericsson.common_prefs.0 <br>
 * 		ericsson.common_prefs.2 = force|C:/local/myPref.epf <br>
 * </code>
 * <p>
 * And the config.ini file then (likely) contains entry, e.g.:
 * <p>
 * </code>
 * 		ericsson.common_prefs.0 = init|&lt some path ...&gt
 * <code>
 * 
 * Added code for fixing network issue in eclipse 3.5
 * 
 * @author Domenic Alessi
 * 
 */
public class CommonPrefEPFResources {

	private static final String COMMON_PREFIX_READ = "ericsson.common_prefs.";
	
	/**
	 * Max no of pref files to be defined. Reason for defining this is to allow a chain
	 * with holes, i.e. having <key>.0, <key>.5 and <key>.10 defined in config.ini allowing
	 * for user addition in between.
	 */
	private static final int MAX_PREF_FILES = 32;
	
	// private CommonPrefFile[] prefFiles;
	private ArrayList<CommonPrefEPFResource> prefFiles;
	
	
	public CommonPrefEPFResources() {
		super();
		prefFiles = new ArrayList<CommonPrefEPFResource>();
	}

	/**
	 * An entry in the .ini or .ref file with structure according to
	 * documentation of the CommonPrefEPFResources class.
	 * 
	 * @author Domenic Alessi
	 *
	 */
	private class CommonPrefRefEntry {
		private static final String COMMON_PREFIX_INIT = "init";
		private static final String COMMON_PREFIX_FORCE = "force";
		private static final String COMMON_PREFIX_CONFIG = "config";
		
		private static final String COMMON_PREFIX_SPLIT = "|";
		private static final String COMMON_PREFIX_SPLIT_REGEXP = "\\|"; // Regexp so need to add "\\"		
		
		public String value;
		public boolean isForce;
		public boolean isConfig;
		
		public CommonPrefRefEntry(String entry, String configKey) throws Exception {
			String[] entries = entry.split(COMMON_PREFIX_SPLIT_REGEXP, 2);
			if (entries.length != 2) {
				throw new Exception("Incorrect no of arguments in entry '" + entry + 
						"' with preference key '" + configKey + "'.");
			}
			String key = entries[0].trim();
			value = entries[1].trim();

			isForce = (key.compareTo(COMMON_PREFIX_FORCE) == 0);
			if (!isForce) {
				isConfig = (key.compareTo(COMMON_PREFIX_CONFIG) == 0);
				if (!isConfig && key.compareTo(COMMON_PREFIX_INIT) != 0)
					throw new Exception("Incorrect key '" + key + "' in entry '" + entry + 
						"' with preference key '" + configKey + "'.");
			}					
		}
		
		public CommonPrefRefEntry(CommonPrefEPFResource pf) {
			if (pf.isConfig()) {
				value = COMMON_PREFIX_CONFIG;
				value += COMMON_PREFIX_SPLIT;				
				value += pf.getConfigKey();
			} else {				
				value = (pf.isForce()? 
						COMMON_PREFIX_FORCE :
						COMMON_PREFIX_INIT);
				value += COMMON_PREFIX_SPLIT;
				value += pf.getResourceName();
			}			
		}
	}
	
	/**
	 * Return number of pref files
	 * 
	 * @return
	 */
	public int length() {
		return prefFiles.size();
	}
	
	/**
	 * Move the item at inx one step up/down in the list.
	 * If noMove, only test if it's possible to move and return
	 * 
	 * @param pfMov - item to move
	 * @param moveDown - moveDown = to higher index in list
	 * @param noMove - test only if possible to move
	 * @return
	 */
	private boolean move(CommonPrefEPFResource pfMov, boolean moveDown, boolean noMove) {
		if (prefFiles.size() < 2)
			return false;
		
		int inx = prefFiles.indexOf(pfMov);
		if (inx < 0)
			return false; // Inconsistent state
		
		if (moveDown) {
			if (inx < prefFiles.size() - 1) {
				if (!noMove) {
					prefFiles.remove(inx);
					prefFiles.add(inx + 1, pfMov);
				}
				return true;
			} else
				return false;
		} else {
			if (inx > 0) {
				if (!noMove) {
					prefFiles.remove(inx);
					prefFiles.add(inx - 1, pfMov);
				}
				return true;
			} else
				return false;			
		}
	}	
	
	/**
	 * Add a common pref file to the list. Assume variables are checked.
	 * 
	 * @param afterPf
	 * @param filePath
	 * @param isForce
	 * @return
	 * @throws Exception 
	 */
	private CommonPrefEPFResource add(CommonPrefEPFResource afterPf, String filePath, boolean isForce) throws Exception {
		
		CommonPrefEPFResource pfNew = new CommonPrefEPFResource(filePath, null, isForce);
		
		int inx = (afterPf == null)? prefFiles.size() : prefFiles.indexOf(afterPf);
		if (inx < 0)
			return null; // Inconsistent state
		prefFiles.add(inx, pfNew);
		
		return pfNew;
	}
	
	/**
	 * If no user entries added, no need to save anything. If a user entry added,
	 * save all entries to preserve user defined order.
	 */
	public void save(){		
		// Temporary comment this code for testing with eclipse 3.5
		/*Preferences store = new Preferences();*/
		// End
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		Properties properties = new Properties();
		// END
		
		// Check if any user entries present
		boolean hasUserEntries = false;
		for (CommonPrefEPFResource pf : prefFiles) {
			if (!pf.isConfig()) {
				hasUserEntries = true;
				break;
			}
		}
		
		// Iterate over all entries since if an item that was is in the store was
		// removed we should put null in the store to mark as removed.
		for (int i = 0; i < MAX_PREF_FILES; i++) {
			String inxKey = new Integer(i).toString();
			String initKey = COMMON_PREFIX_READ + inxKey;
			
			// If no user entries or index out of bounds, remove entry if existing
			if (!hasUserEntries || i > prefFiles.size() - 1) {
				// Temporary comment this code for testing with eclipse 3.5
				/*if (store.contains(initKey)) 
					store.setToDefault(initKey);*/ // Remove entry
				// End
				
				// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
				if (properties.containsKey(initKey))
					setToDefault(initKey, properties);
				// END
				continue;
			}
			
			// Add entry - either referring to config, or direct entry
			CommonPrefRefEntry prefEntry = new CommonPrefRefEntry(prefFiles.get(i));
			// Temporary comment this code for testing with eclipse 3.5
			/*store.setValue(initKey, prefEntry.value);*/
			// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			if ( (initKey != null) && (prefEntry.value != null) )
				properties.put(initKey, prefEntry.value);
			// END
		}
		// Temporary comment this code for testing with eclipse 3.5
		/*CommonPrefsHelper.saveUserPreferencesEntries(store);*/
		// End
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		CommonPrefsHelper.saveUserPreferencesEntries(properties);
		// END
	}
	
	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 */
	public void setToDefault(String key, Properties properties) {
		
		if (key != null) {
			properties.remove(key);
			properties.setProperty(key, null);
		}
		
	}
	
	
	
	/**
	 * Retrieve the file locations for the preference files. For more info on
	 * format, please see comment in {@link CommonPrefEPFResources}
	 */
	public void load(MultiStatus status) {
		
		// Create a list with entries from the local preferences and the items read from
		// the config.ini file. For the entries from the config.ini file we need to save
		// the key, so we can write this when saving.
	
		// 1. Retrieve the keys from the config.ini file and add them first in list
		// Also check for any local refs added, should be treated as config entries.
		
		// Temporary comment out this code for testing the code below with eclipse 3.5
		//Preferences localRefs = CommonPrefsHelper.readLocalPreferencesEntries(status);
		// End
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		Properties localRefs = CommonPrefsHelper.readLocalPreferencesEntries(status);
		// END
		
		for (int i = 0; i < MAX_PREF_FILES; i++) {
			
			String inxKey = new Integer(i).toString();
			String configKey = COMMON_PREFIX_READ + inxKey;
			
			// Temporary comment out this code for testing the code below with eclipse 3.5
			//String entry = (localRefs != null)? localRefs.getString(configKey): ""; // returns value or ""
			// End
			
			// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
			String entry = (localRefs != null)? localRefs.getProperty(configKey): ""; // returns value or ""
			// END
			
			if (entry.length() == 0) {
				entry = System.getProperty(configKey); // returns value or null
				if (entry == null || entry.length() == 0)
					continue;
			}
			
			CommonPrefEPFResource pf = null;
			try {
				CommonPrefRefEntry prefEntry = new CommonPrefRefEntry(entry, configKey);
				pf = new CommonPrefEPFResource(prefEntry.value, configKey, prefEntry.isForce);
				
				if (!pf.exists() && status != null)
					status.add(new Status(
							Status.WARNING,
							StartupPlugin.PLUGIN_ID,
							"Failed to access " + pf.getResourceName()));
			} catch (Exception e) {
				if (status != null)
					status.add(new Status(Status.WARNING, StartupPlugin.PLUGIN_ID, e.getMessage(), e));
				continue;
			}			
			
			
			prefFiles.add(pf);
		}
		
		// 2. Add the keys from the user's preferences. If a "config" entry is found, that
		// entry will be moved to the position of the user pref found.
		
		// Temporary comment out this code for testing the code below with eclipse 3.5
		//Preferences store = CommonPrefsHelper.readUserPreferencesEntries(status);	
		// End
		
		// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
		Properties store = CommonPrefsHelper.readUserPreferencesEntries(status);	
		// END
		
		if (store != null) {
			
			for (int i = 0; i < MAX_PREF_FILES; i++) {
				String inxKey = new Integer(i).toString();
				String initKey = COMMON_PREFIX_READ + inxKey;
				
				// Temporary comment this code for testing with eclipse 3.5 
				/*if (!store.contains(initKey))
					continue;
				String entry = store.getString(initKey);*/
				// End temporary
				
				// ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
				if (!store.containsKey(initKey))
					continue;
				
				String entry = store.getProperty(initKey);
				// END
	
				CommonPrefEPFResource pf = null;
				try {
					CommonPrefRefEntry prefEntry = new CommonPrefRefEntry(entry, initKey);
					
					// If the type is "config", then find the already added entry and move it
					// to the end of the list. OK not to find - means config.ini key is removed
					if (prefEntry.isConfig) {
						for (CommonPrefEPFResource pfRef : prefFiles) {
							if (pfRef.isConfig() && pfRef.getConfigKey().compareTo(prefEntry.value) == 0) {
								prefFiles.remove(pfRef);
								prefFiles.add(pfRef);
								break; // exit loop
							}	
						}
						continue; // config.ini key not found. Bail out				
					} 					
					
					pf = new CommonPrefEPFResource(prefEntry.value, null, prefEntry.isForce);
					if (!pf.exists() && status != null)
						status.add(new Status(
								Status.WARNING,
								StartupPlugin.PLUGIN_ID,
								"Failed to access " + pf.getResourceName()));					
				} catch (Exception e) {
					if (status != null)
						status.add(new Status(Status.WARNING, StartupPlugin.PLUGIN_ID, e.getMessage(), e));
					continue;
				}				
	
				prefFiles.add(pf);
			}
		}
	}
	
	
	
	
	
	/**
	 * Clear all user entries from the preference store, i.e. all preference
	 * files added through the GUI. Entries defined in the config.ini and any
	 * "default" user preference file will also remain
	 */
	public void removeUserEntries() {
		// Remove in reverse order
		int last = prefFiles.size() - 1;
		for (int i = last; i > 0; i--) {
			CommonPrefEPFResource pf = prefFiles.get(i);
			if (!pf.isConfig())
				prefFiles.remove(i);
		}

		// Save keys to persist state
		save();
	}
	
	/**
	 * Create a CommonPrefFile and add to the list. Note that all "init" keys are stored
	 * on even slots, and "force" keys on odd - so each entry has a unique position in the
	 * list.
	 * <br><br>
	 * The entries from the config.ini file are read only, and are thus fixed in position
	 * in the list as well. User entries are stored as preferences, and are allowed to be
	 * at all other positions in the list
	 * 
	 * @param afterEntry - put the entry after this, if null - put last
	 * @param filePath - should have value > ""
	 * @param isForce 
	 * @param isConfig
	 * @return CommonPrefFile or null
	 * @throws Exception 
	 */
	public CommonPrefEPFResource addPrefFile(
			CommonPrefEPFResource afterEntry,
			String filePath,
			boolean isForce,
			boolean isConfig) throws Exception {
		
		if (filePath == null || filePath.length() == 0)
			return null;
		
		return add(afterEntry, filePath, isForce);
	}
	
	/**
	 * Return a matching {@link CommonPrefEPFResource} based on resourcePath. If no entry found,
	 * return null.
	 * 
	 * @param resourcePath

	 * @return CommonPrefFile or null
	 */
	public CommonPrefEPFResource getPrefFile(String resourcePath) {
		if (resourcePath == null || resourcePath.length() == 0)
			return null;
		
		for (CommonPrefEPFResource pf : prefFiles) {
			
			if (pf.getResourceName().compareToIgnoreCase(resourcePath) == 0) {
				
				return pf;
			}
		}
		
		return null;
	}
	
	/**
	 * Remove the entry. If element not found, or can't be removed since being
	 * defined in a config.ini file, return false
	 * 
	 * @param pfRem
	 * @return boolean
	 */
	public boolean removePrefFile(CommonPrefEPFResource pfRem) {
		// If no entry or entry is in config.ini, we can't move or remove item
		if (pfRem == null || pfRem.isConfig())
			return false;
		
		return prefFiles.remove(pfRem);
	}
	
	/**
	 * Move the selected file one step up/down in the list. If the element can't 
	 * be moved (top/bottom at list or config.ini element) return false.
	 * 
	 * @param pfMov
	 * @param moveDown
	 * @return boolean
	 */
	public boolean movePrefFile(CommonPrefEPFResource pfMov, boolean moveDown) {
		return move(pfMov, moveDown, false);
	}
	
	/**
	 * Test if it's possible to move the selected item one step in the list.
	 * 
	 * @param pfMov
	 * @param moveDown
	 * @return boolean
	 */
	public boolean canMovePrefFile(CommonPrefEPFResource pfMov, boolean moveDown) {
		return move(pfMov, moveDown, true);
	}
	
	/**
	 * Implementation of Iterator interface. The remove() method
	 * should not be called here - thus not implemented.
	 * 
	 * @return
	 */
	class CommonPrefFileIterator implements Iterator<CommonPrefEPFResource> {
		private int curIndex = 0;
		public boolean hasNext() {
			return (curIndex < prefFiles.size());
		}
	
		public CommonPrefEPFResource next() {
			return prefFiles.get(curIndex++);
		}
	
		public void remove() {
		}
	}
	
	public Iterator<CommonPrefEPFResource> iterator() {
		return new CommonPrefFileIterator();
	}
}
