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

/**
 * @author Domenic Alessi
 *
 */
public class NetworkPrefResource {
	
	private String host;
	
	private int port;
	
	private String type;
	
	private boolean hasAuthenticate;
	
	/**
	 * 
	 */
	public NetworkPrefResource() {
		// TODO Auto-generated constructor stub
		
	}
	
	public NetworkPrefResource(String host, int port, String type, boolean hasAuthenticate) {
		this.host = host;
		this.port = port;
		this.type = type;
		this.hasAuthenticate = hasAuthenticate;
		
	}
	
	public String getHost() {
		
		return host;
	}
	
	public int getPort() {
		
		return port;
	}
	
	public String getType() {
		
		return type;
	}
	
	public boolean getAuthenticate() {
		
		return hasAuthenticate;
	}
	
}
