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
 * Holding information about a resource (.epf) file
 * 
 * @author Domenic Alessi
 * @see CommonPrefEPFResources
 */
public class CommonPrefEPFResource extends CommonPrefResource {
	private boolean isForce;
	private String configKey;
	private boolean isSelected;

	public CommonPrefEPFResource(String resourceName,
			String configKey,
			boolean isForce) throws Exception {
		super(resourceName, false, false);
		this.configKey = configKey;
		this.isForce = isForce;
	}

	public String getConfigKey() {
		return configKey;
	}
	
	public boolean isConfig() {
		return (configKey != null);
	}

	public boolean isForce() {
		return isForce;
	}

	public void setIsForce(boolean isForce) {
		this.isForce = isForce;
	}	
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}
}
