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
public class NetworkNonProxiesResource {

	private String nonProxiesHost;
	
	private boolean isProxiesEnable;
	
	/**
	 * 
	 */
	public NetworkNonProxiesResource() {
		// TODO Auto-generated constructor stub
	}
	
	public NetworkNonProxiesResource(String nonProxiesHost, boolean isProxiesEnable) {
		this.nonProxiesHost = nonProxiesHost;
		this.isProxiesEnable = isProxiesEnable;
	}
	
	
	public String getNonProxiesHost() {
		return nonProxiesHost;
	}
	
	public boolean getProxiesEnable() {
		return isProxiesEnable;
	}

}
