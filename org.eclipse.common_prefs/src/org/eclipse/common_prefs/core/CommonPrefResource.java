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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.common_prefs.StartupPlugin;
import org.eclipse.core.runtime.Platform;


/**
 * Class for holding a ref to a general preference file. The type can be
 * regular file in local file system, or a URL ref.
 * 
 * Added code for fixing network issue in eclipse 3.5
 * 
 * @author Domenic Alessi
 *
 */
public class CommonPrefResource {
	protected String resourceName;
	protected boolean isFile;
	protected long lastModified;
	protected boolean exists;
	protected File file;
	protected URL url;

	
	/**
	 * Create a new CommonPrefResource based on input. Note that we throw
	 * an exception if we can't initialize correctly. This since having a
	 * separate call to init() might be forgotten, so better handle in
	 * constructor.
	 * 
	 * @param resourceName File or URL
	 * @param fileHint Hint that resource name is a file to avoid expensive URL checking
	 * @param shouldExist If resource needs to exist
	 * @throws Exception 
	 */
	public CommonPrefResource(String resourceName, boolean fileHint, boolean shouldExist)
		throws Exception {
		this.resourceName = resourceName.trim();
		isFile = false;
		lastModified = -1;
		
		boolean initOK = false;
		if (fileHint)
			initOK = this.initFile(shouldExist);
		else 
			initOK = this.init(shouldExist);
		if (!initOK)
			throw new IOException("Failed to access " + this.resourceName);
	}
	
	/**
	 * Initialize the resource. Depending on type, cache some attributes.
	 * For performance reasons the lastModification time is cached for URL
	 * refs, so later - in normal case, i.e. when having an initialized ws
	 * and not modified URL ref, we will not need to reread the URL.
	 * 
	 * @param log
	 * @return
	 * @throws Exception 
	 */
	private boolean init(boolean shouldExist) throws Exception {
		
		// Expand any variables in the filename
		if (!expandVariable())
			return false;
		
		// This is not that elegant code, but the org.eclipse.core.filesystem.URIUtil.toURI
		// from the string returned wrong path (appended the install path), and java.net.URI
		// couldn't handle spaces. And the URL.getPath() will not remove the initial / ..
		// So doing it the crude way here.
		if (resourceName.startsWith("file:")) {
			String value = resourceName;
			if (Platform.getOS().compareToIgnoreCase(Platform.OS_WIN32) == 0)
				resourceName = value.substring(6); // On win32 the prefix is file:/<abs path>
			else
				resourceName = value.substring(5); // On *ix the prefix is file:<abs path>
		}
		
		// Check if we have a correct URL
		try {
			url = new URL(resourceName);
		} catch (MalformedURLException e) {
		}
		
		// If found a correct http(s) ref, handle here
		if (url != null) {
			if (url.getProtocol().compareTo("http") == 0
					|| url.getProtocol().compareTo("https") == 0) {
				isFile = false;

				HttpURLConnection httpCon = null;
				try {
					httpCon = (HttpURLConnection) url.openConnection();
					httpCon.setConnectTimeout(StartupPlugin.URL_CONNECT_TIMEOUT);
					httpCon.connect();

					
					// If getting a response to this request we assume this is an existing ref
					// If not getting a response, we still don't get an exception, but 0 as value
					lastModified = httpCon.getLastModified();
					/** 
					 * FA 2014-02-21: Bug - http pref on github has "lastModified == 0"
					 * fix by setting exist = true if lastModified >= 0 ...
					 */
					exists = (lastModified >= 0);
				} catch (Exception e) {
					lastModified = 0;
					exists = false;
				}

				// If we should exist, fail if not existing
				if (shouldExist)
					return exists;
				else
					return true;
			}
		}
		return initFile(shouldExist);
	}
	
	// Handle file initialization
	private boolean initFile(boolean shouldExist) throws Exception {
		File file = new File(resourceName);
		exists = file.exists();
		if (!exists) {
			// If file is not found and is not absolute, look for it relative to
			// the configuration location. If not found there, bail.
			if (!file.isAbsolute()) {
				String currResourceName = resourceName;
				resourceName = StartupPlugin.getConfigurationLocation() + File.separator + resourceName;
				exists = initFile(shouldExist);
				if (!exists) {
					if (shouldExist)
						throw new Exception("Preferences file " + resourceName + " not found.");
					else {
						// Restore original name since none of locations where valid
						resourceName = currResourceName;
						return true;
					}
				} else
					return true;
			} else
				return false;
		}		
		
		lastModified = file.lastModified();
		
		isFile = true;
		return true;
	}
	
	private boolean expandVariable() throws Exception {
		if (resourceName.charAt(0) == '$') {
			int inxEnd = resourceName.indexOf('}');
			String key = resourceName.substring(2, inxEnd);
			String value = System.getProperty(key);
			if (value != null && value.length() > 0) {
				resourceName = value + resourceName.substring(inxEnd + 1);
			} else {
				// Incorrect property format or missing property. Return raw filename to log error
				throw new Exception("System variable '" + key + "' in preferences file " + 
						resourceName + " entry not correctly defined.");
			}
		}
		return true;
	}
	
	public boolean exists() {
		return exists;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public long getLastModified() {
		return lastModified;
	}	
	
	/**
	 * Note that the caller is responsible for proper closing of the stream
	 * 
	 * @return
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException {
		if (isFile) {
			if (file == null)
				file = new File(resourceName);
			return new FileInputStream(file);
		} else {
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.setConnectTimeout(StartupPlugin.URL_CONNECT_TIMEOUT);
			httpCon.setReadTimeout(StartupPlugin.URL_READ_TIMEOUT);
			httpCon.connect();
		
			return httpCon.getInputStream();
		}
	}
	
	
	/**
	 * ADD THIS METHOD FOR TESTING WITH ECLIPSE 3.5
	 * 
	 * @return file
	 * @throws IOException
	 */
	public File getFile() {
		
		if (isFile) {
			if (file == null)
				file = new File(resourceName);
			return file;
		}
		else
			return null;
	}
	
	
	/**
	 * ADD THIS METHOD FOR TESTING
	 * 
	 * @return
	 * @throws IOException
	 */
	/*public File getOutputFile() throws IOException {
		
		OutputStream outputStream = null;
		if (resourceName != null) {
			String[] tokens = null;
			if (resourceName.startsWith("http")) {
				String[] headTokens = resourceName.split("//");
				String[] tailTokens = headTokens[headTokens.length-1].split("/");
				
			}
			else
				tokens = resourceName.split("\\\\");
			
			StringBuilder buffer = new StringBuilder(tokens[tokens.length-1].trim());
			buffer.insert(0, "eclipse3.5_");
			tokens[tokens.length-1] = buffer.toString();
			StringBuilder tempBuffer = new StringBuilder();
			for (int index = 0; index <= tokens.length-1; index++) {
				if (index == tokens.length-1)
					tempBuffer.append(tokens[index]);
				else
					tempBuffer.append(tokens[index]).append(File.separator);
			}
			String tempResourceName = tempBuffer.toString();
			File file = new File(tempResourceName);
			
			return file;
		}
		else {
			throw new IOException("Failed to access " + this.resourceName);
		}
		
	}*/
	
	
}
