/*******************************************************************************
 * Copyright (c) 2013 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.ships.core;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("cuchaz.ships.core")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({ "cuchaz.ships.core" })
@IFMLLoadingPlugin.SortingIndex(value = 1001)
public class CoreModPlugin implements IFMLLoadingPlugin {
	
	public static Boolean isObfuscatedEnvironment = null;
	
	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "cuchaz.ships.core.CoreModTransformer" };
	}
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
	
	@Override
	public String getModContainerClass() {
		return "cuchaz.ships.core.ShipsCore";
	}
	
	@Override
	public String getSetupClass() {
		// implement this if we want to get launcher classloader directly
		return null;
	}
	
	@Override
	public void injectData(Map<String,Object> data) {
		// data keys: mcLocation, coremodList, runtimeDeobfuscationEnabled, coremodLocation
		
		// are we running in an obfuscated environment?
		isObfuscatedEnvironment = (Boolean)data.get("runtimeDeobfuscationEnabled");
	}
}
