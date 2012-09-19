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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Domenic Alessi
 *
 */
public class CommonPrefProjectPreference extends EclipsePreferences {

	/**
	 * 
	 */
	public CommonPrefProjectPreference() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 */
	public static void read(InputStream inputStream, Properties properties) throws  BackingStoreException, IOException {
		
		//Properties fromDisk = loadProperties(inputStream);
		loadProperties(inputStream, properties);
		//return fromDisk;
		        
	}
	
	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 */
	private static void loadProperties(InputStream inputStream, Properties result) throws BackingStoreException, IOException {
        
 		//Properties result = new Properties();
         InputStream input = null;
         try {
        	 input = new BufferedInputStream(inputStream);
             result.load(input);
         } finally {
        	 if (input != null) {
        		 input.close();
        		 input = null;
             //FileUtil.safeClose(input);
        	 }
         }
         //return result;
     }

	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 *  
	 */
	public static  IEclipsePreferences updatePreferences(InputStream inputStream, Properties properties, File file) throws CoreException,
																	IOException, BackingStoreException {
		        
		         // if we made it this far we are inside /project/.settings and might
		 // have a change to a preference file
		 IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IPath path = Path.fromOSString(file.getAbsolutePath()); 
		IFile iFile= workspace.getRoot().getFileForLocation(path); 
		
         String project = path.segment(0);
         String qualifier = path.removeFileExtension().lastSegment();
         IEclipsePreferences root = Platform.getPreferencesService().getRootNode();
         //EclipsePreferences node = (EclipsePreferences) root.node(InstanceScope.SCOPE).node(project).node(qualifier);
         EclipsePreferences node = (EclipsePreferences) root.node(InstanceScope.SCOPE);
         String  message = null;
         //try {
             if (!(node instanceof IEclipsePreferences))
                 return null;
             EclipsePreferences projectPrefs = node;
             
             //if (projectPrefs.isWriting)
             //    return;
             read(inputStream, properties);
             // make sure that we generate the appropriate resource change events
             // if encoding settings have changed
            // if (ResourcesPlugin.PI_RESOURCES.equals(qualifier))
            //     preferencesChanged(iFile.getProject());
         /*} catch (BackingStoreException e) {
             IStatus status = new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IStatus.ERROR, message, e);
             throw new CoreException(status);
         }*/
             return projectPrefs;
     }
	
	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 */
	 private static void preferencesChanged(IProject project) {
          Workspace workspace = ((Workspace) ResourcesPlugin.getWorkspace());
          workspace.getCharsetManager().projectPreferencesChanged(project);
          workspace.getContentDescriptionManager().projectPreferencesChanged(project);
	 }
	 
	 
    public static void doConvertFromProperties(EclipsePreferences node, Properties table, boolean notify) {
    	convertFromProperties(node, table, notify);
    }

}
