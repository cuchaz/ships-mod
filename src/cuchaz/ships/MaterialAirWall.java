package cuchaz.ships;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.MaterialTransparent;

public class MaterialAirWall extends MaterialTransparent
{
	public MaterialAirWall( MapColor color )
	{
		super( color );
	}

	@Override
	public boolean blocksMovement( )
    {
		// block movement so water won't flow into this material
        return true;
    }
}
