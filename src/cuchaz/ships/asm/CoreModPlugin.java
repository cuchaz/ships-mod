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
package cuchaz.ships.asm;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.Name;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@Name("cuchaz.ships.core")
@MCVersion("1.7.10")
@TransformerExclusions({ "cuchaz.ships.asm" })
public class CoreModPlugin implements IFMLLoadingPlugin {
	
	public static Boolean isObfuscatedEnvironment = null;
	
	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "cuchaz.ships.asm.CoreModTransformer" };
	}
	
	@Override
	public String getAccessTransformerClass() {
		return null;
	}
	
	@Override
	public String getModContainerClass() {
		return "cuchaz.ships.Ships";
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
