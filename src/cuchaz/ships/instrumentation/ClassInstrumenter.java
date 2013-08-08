package cuchaz.ships.instrumentation;

public class ClassInstrumenter
{
	public static <T> T instrument( T in )
	{
		// UNDONE: figure out how to use javassist to intercept TileEntityChest.isUsableByPlayer( EntityPlayer )
		return in;
	}
}
