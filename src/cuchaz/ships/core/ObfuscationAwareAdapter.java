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

import java.lang.reflect.Field;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class ObfuscationAwareAdapter extends ClassVisitor {
	
	private boolean m_isObfuscatedEnvironment;
	
	public ObfuscationAwareAdapter(int api, ClassVisitor cv, boolean isObfuscatedEnvironment) {
		super(api, cv);
		
		m_isObfuscatedEnvironment = isObfuscatedEnvironment;
	}
	
	public ClassVisitor getPreviousClassVisitor() {
		return cv;
	}
	
	public void setPreviousClassVisitor(ClassVisitor val) {
		cv = val;
	}
	
	protected String getRuntimeMethodName(String runtimeClassName, String clearMethodName, String idMethodName) {
		if (m_isObfuscatedEnvironment) {
			return methodMapReverseLookup(getMethodMap(runtimeClassName), idMethodName);
		} else {
			return clearMethodName;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,String> getMethodMap(String obfuscatedClassName) {
		// sadly, the method maps are private
		// let's fix that
		try {
			Field field = FMLDeobfuscatingRemapper.class.getDeclaredField("methodNameMaps");
			field.setAccessible(true);
			
			return ((Map<String,Map<String,String>>)field.get(FMLDeobfuscatingRemapper.INSTANCE)).get(obfuscatedClassName);
		} catch (Exception ex) {
			throw new Error("Unable to access FML's deobfuscation mappings!", ex);
		}
	}
	
	private String methodMapReverseLookup(Map<String,String> methodMap, String idMethodName) {
		// did we not get a method map? just pass through the method name
		if (methodMap == null) {
			return idMethodName;
		}
		
		// methodNameMaps = Map<obfuscated class name,Map<obfuscated method name + signature,id method name>>
		for (Map.Entry<String,String> entry : methodMap.entrySet()) {
			if (entry.getValue().equals(idMethodName)) {
				String obfuscatedName = entry.getKey();
				
				// chop off the signature
				// ie, turn "a(III)V" into just "a"
				
				return obfuscatedName.substring(0, obfuscatedName.indexOf("("));
			}
		}
		
		// no method was found
		// return empty string so it fails comparisons with expected values, but doesn't throw exceptions
		return "";
	}
}
