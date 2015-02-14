/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.ships;

public class Bits {
	
	public static int packUnsigned(int unsigned, int size, int offset) {
		int result = unsigned & getMask(size);
		return result << offset;
	}
	
	public static int unpackUnsigned(int packed, int size, int offset) {
		packed = packed >> offset;
		return packed & getMask(size);
	}
	
	public static int getMask(int size) {
		// mask sizes of 0 and 32 are not allowed
		// we could allow them, but I don't want to add conditionals so this method stays very fast
		assert (size > 0 && size < 32);
		return 0xffffffff >>> (32 - size);
	}
	
	public static int getMaxUnsigned(int size) {
		return (1 << size) - 1;
	}
}
