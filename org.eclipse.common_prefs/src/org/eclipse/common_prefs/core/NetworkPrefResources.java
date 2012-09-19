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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Domenic Alessi
 *
 */
public class NetworkPrefResources {
	
	public static final String HAS_MIGRATED = "org.eclipse.core.net.hasMigrated";
	
	public static final String NON_PROXIED_HOST = "nonProxiedHosts";
	
	public static final String PROXIES_ENABLE = "proxiesEnabled";
	
	public static final String PORT = "port";
	
	public static final String HAS_AUTHORIZED = "hasAuth";
	
	public static final String HOST = "host";
	
	public static final String ORG_ECLIPSE_CORE_NET = "org.eclipse.core.net";
	
	public static final String HTTP = "HTTP";
	 
	public static final String HTTPS = "HTTPS";

	private Map<String, NetworkPrefResource> prefProxiesNetworkMap;
	
	private Map<String, NetworkNonProxiesResource> prefNonProxiesNetworkMap;
	
	/**
	 * 
	 */
	public NetworkPrefResources() {
		// TODO Auto-generated constructor stub
		super();
		prefProxiesNetworkMap = new HashMap<String, NetworkPrefResource>();
		prefNonProxiesNetworkMap = new HashMap<String, NetworkNonProxiesResource>();
	}
	
	
	public Map<String, NetworkPrefResource> getNetworkPreferencesMap() {
		
		return prefProxiesNetworkMap;
	}
	
	public Map<String, NetworkNonProxiesResource> getNetworkNonProxiesPreferencesMap() {
		
		return prefNonProxiesNetworkMap;
	}
	
	private void addNetworkResource(NetworkPrefResource proxiesResource, NetworkNonProxiesResource nonProxiesResource) {
		
		if (proxiesResource != null) {
			if (proxiesResource.getType() == null)
				return;
		
			//addNetwork(networkResource);
			addProxiesNetwork(proxiesResource);
		}
		else
			addNonProxiesNetwork(nonProxiesResource);
	}
	
	
	private void addProxiesNetwork(NetworkPrefResource netResource) {
		
		String type = netResource.getType();
		if (type.trim().equals(""))
			return;
		
		prefProxiesNetworkMap.put(type, netResource);
		
	}
	
	
	private void addNonProxiesNetwork(NetworkNonProxiesResource nonProxiesResource) {
		
		prefNonProxiesNetworkMap.put(ORG_ECLIPSE_CORE_NET, nonProxiesResource);
	}
	
	
	public void loadNetworkSettings() {
		
		CommonPrefsHelper commonPrefsHelper = new CommonPrefsHelper();
		String[] types = commonPrefsHelper.getProxyType();
		String[] hosts = commonPrefsHelper.getProxyHost();
		int[] ports = commonPrefsHelper.getProxyPort();
		boolean[] authenticate = commonPrefsHelper.getAuthenticate();
		boolean isProxiesEnable = commonPrefsHelper.getProxiesEnable();
		String nonProxies = buildNonProxiesHost(commonPrefsHelper);
		
		
		for (int i = 0; i < types.length; i++) {
			NetworkPrefResource networkProxiesSettings = new NetworkPrefResource(hosts[i], ports[i], types[i], authenticate[i]);
			addNetworkResource(networkProxiesSettings, null);
		}
		
		NetworkNonProxiesResource nonProxiesResource = new NetworkNonProxiesResource(nonProxies, isProxiesEnable);
		addNetworkResource(null, nonProxiesResource);
		
	}
	
	
	private String buildNonProxiesHost(CommonPrefsHelper commonPrefsHelper) {
		
		return commonPrefsHelper.getNonProxiesHost();
	}
	
	

}
