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

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.common_prefs.core.CommonPrefEPFResources;
import org.eclipse.common_prefs.core.NetworkPrefResources;
import org.eclipse.common_prefs.exportWizard.CommonPrefsExportPage;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The activator class controls the plug-in life cycle
 */
/**
 * Fixing network issue for Eclipse 3.5
 * 
 * @author Domenic Alessi
 *
 */
public class StartupPlugin extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "org.eclipse.common_prefs"; //$NON-NLS-1$
	private static StartupPlugin instance;

	public static final String DEFAULT_USER_INI_FILE = "common_preferences.ini";
	public static final String DEFAULT_USER_DEF_FILE = "default_preferences.epf";
	public static final String COMMON_PREF_OUT	= "common_preferences_out.txt";
	
	public static final String LOCAL_PREF_FILE_REF = ".ref";

	public static final String EXP_TREE_PLUGIN_IMG = "plugins.gif";//$NON-NLS-1$
	public static final String EXP_TREE_PLUGIN_DIFF_IMG = "plugins_diff.gif";//$NON-NLS-1$
	public static final String EXP_TREE_PLUGIN_SAME_IMG = "plugins_same.gif";//$NON-NLS-1$
	public static final String EXP_TREE_PLUGIN_ADDED_IMG = "plugins_added.gif";//$NON-NLS-1$
	public static final String EXP_TREE_CONFIG_IMG = "eclipse.gif";//$NON-NLS-1$
	public static final String EXP_TREE_SAME_VAL_IMG = "same_value.gif";//$NON-NLS-1$
	public static final String EXP_TREE_DIFF_VAL_IMG = "diff_value.gif";//$NON-NLS-1$
	public static final String EXP_TREE_DIFF_FILE_VAL_IMG = "diff_file_value.gif";//$NON-NLS-1$
	public static final String EXP_TREE_ADDED_VAL_IMG = "added_value.gif";//$NON-NLS-1$

	public static final String PREF_FILE_EXIST_IMG = "preferences_exist.gif";//$NON-NLS-1$
	public static final String PREF_FILE_NOEXIST_IMG = "preferences_no_exist.gif";//$NON-NLS-1$
	
	public static final int URL_CONNECT_TIMEOUT = 500; // 1000; // milliseconds
	public static final int URL_READ_TIMEOUT = 500; // 2000 // milliseconds
	
	private CommonPrefEPFResources commonPrefEPFResources;
	
	private ServiceTracker tracker;
	
	private NetworkPrefResources networkPrefResources;
	
	public StartupPlugin() {
		instance = this;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		
		reg.put(EXP_TREE_PLUGIN_IMG, getImage(EXP_TREE_PLUGIN_IMG));//$NON-NLS-1$
		reg.put(EXP_TREE_PLUGIN_DIFF_IMG, getImage(EXP_TREE_PLUGIN_DIFF_IMG));//$NON-NLS-1$
		reg.put(EXP_TREE_PLUGIN_SAME_IMG, getImage(EXP_TREE_PLUGIN_SAME_IMG));//$NON-NLS-1$		
		reg.put(EXP_TREE_PLUGIN_ADDED_IMG, getImage(EXP_TREE_PLUGIN_ADDED_IMG));//$NON-NLS-1$		
		reg.put(EXP_TREE_CONFIG_IMG, getImage(EXP_TREE_CONFIG_IMG));//$NON-NLS-1$
		reg.put(EXP_TREE_SAME_VAL_IMG, getImage(EXP_TREE_SAME_VAL_IMG));//$NON-NLS-1$
		reg.put(EXP_TREE_DIFF_VAL_IMG, getImage(EXP_TREE_DIFF_VAL_IMG));//$NON-NLS-1$						
		reg.put(EXP_TREE_DIFF_FILE_VAL_IMG, getImage(EXP_TREE_DIFF_FILE_VAL_IMG));//$NON-NLS-1$						
		reg.put(EXP_TREE_ADDED_VAL_IMG, getImage(EXP_TREE_ADDED_VAL_IMG));//$NON-NLS-1$		
		reg.put(PREF_FILE_EXIST_IMG, getImage(PREF_FILE_EXIST_IMG));//$NON-NLS-1$						
		reg.put(PREF_FILE_NOEXIST_IMG, getImage(PREF_FILE_NOEXIST_IMG));//$NON-NLS-1$			
	}	

	static public ImageDescriptor getImage(String imageId) {
		URL iconBaseURL = null;
		try {
			iconBaseURL= new URL(StartupPlugin.getDefault().getBundle().getEntry("/"), "icons/" ); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (MalformedURLException e) {
//			StartupPlugin.getDefault().log(e);
			return null;
		}
		try {
			URL u = new URL(iconBaseURL, imageId);
			return ImageDescriptor.createFromURL(u);
		} catch (MalformedURLException e) {
//			StartupPlugin.getDefault().log(e);
			return null;
		}		
	}	
	
    /**
     * Returns the shared instance
     *
     * @return StartupPlugin
     */
    public static StartupPlugin getDefault() {
        return instance;
    }
 
    /**
     * Get a plug-in folder in the configuration space. This is typically <eclipse install dir>/
     * configuration/org.eclipse.common_prefs 
     * 
     * @return
     */
    static public String getConfigurationLocation() {    
    	
    	Location loc = Platform.getConfigurationLocation();
    	if (loc == null) {
    		
    		return null; // Error
    	}
    	File file = new File(loc.getURL().getFile());
    	
    	return file.getAbsolutePath() + File.separator + PLUGIN_ID + File.separator;
    }
    
    /**
     * Return the ini file with user's preference file entries. Note that entries
     * might also be defined in the config.ini file, which is considered as read-only.
     * 
     * @return String
     */
    static public String getDefaultUserPrefIniFile() {
    	
    	return getConfigurationLocation() + DEFAULT_USER_INI_FILE;	
    }

    /**
     * Return the list of .ref files in the configuration directory
     * 
     * @return
     */
    static public File[] getLocalReferenceFiles() {
    	
    	File dir = new File(getConfigurationLocation());
    	File[] files = null;
    	if (dir.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return (name.endsWith(LOCAL_PREF_FILE_REF));
				}
			};
			
			files = dir.listFiles(filter);
			
    	}
    	return files;
    }
    
    /**
     * Return the location of the preference file saved automatically by the plug-in
     * on first startup of the workspace. This to save the default settings for the 
     * plug-ins, to enable the {@link CommonPrefsExportPage} to show a filtered view
     * with changes only.
     * 
     * @return IPath
     */
    static public IPath getDefaultPrefFile() {
    	
		IPath path = StartupPlugin.getDefault().getStateLocation();
		path = path.append(File.separator);
		path = path.append(StartupPlugin.DEFAULT_USER_DEF_FILE);
		return path;
    }
    
    
    /**
     * ADD THIS CODE FOR TESTING WITH ECLIPSE 3.5
     * 
     */
    static public String commonPrefOutput() {
    	String outFile = getConfigurationLocation() + COMMON_PREF_OUT;
    	return outFile;
    }
    
    /**
     * Return the single instance of the CommonPrefFiles. If status is
     * set it will be used to report any issues when loading. OK to pass
     * null, and no reporting will be done.
     * 
     * @param status
     * @return
     */
    public CommonPrefEPFResources getCommonPrefFiles(MultiStatus status) {
    	
    	if (commonPrefEPFResources == null) {
    		commonPrefEPFResources = new CommonPrefEPFResources();
    		commonPrefEPFResources.load(status);
    	}
    	return commonPrefEPFResources;
    }
    
    /**
     * Clear the initialized and possibly changed values. On next read  - i.e. access
     * to the {@link StartupPlugin#getCommonPrefFiles()} the values from disk will
     * be re-read.
     */
    public void clearCommonPrefFiles() {
    	commonPrefEPFResources = null;
    }
    
    
    /**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 * @return
	 */
	public IProxyService getProxyService() {
        return (IProxyService) tracker.getService();
	}
	
	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 */
	public NetworkPrefResources getNetworkPrefResources() {
		return networkPrefResources;
	}


	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		 tracker = new ServiceTracker(getBundle().getBundleContext(), IProxyService.class.getName(), null);
         tracker.open();
         
         networkPrefResources = new NetworkPrefResources();
         networkPrefResources.loadNetworkSettings();

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		instance = null;
		super.stop(context);
	}

	/**
	 * Log the status message to the plug-ins log.
	 * 
	 * @param severity
	 * @param message
	 * @param e
	 */
	public static void log(int severity, String message, Throwable e) {
		getDefault().getLog().log(new Status(severity, PLUGIN_ID, 0, message, e));
	}
	
	/**
	 * Log the status message to the plug-ins log.
	 * 
	 * @param s
	 */
	public static void log(IStatus s) {
		getDefault().getLog().log(s);
	}
	
	/**
	 * Log the status message to the plug-ins log.
	 * 
	 * @param e
	 */
	public void log(Throwable e) {
		log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "Error", e)); //$NON-NLS-1$
	}
}
