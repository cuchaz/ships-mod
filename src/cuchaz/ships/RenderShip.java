package cuchaz.ships;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

public class RenderShip extends Render
{
	@Override
	public void doRender( Entity entity, double x, double y, double z, float yaw, float partialTickTime )
	{
		doRender( (EntityShip)entity, x, y, z, yaw, partialTickTime );
	}
	
	public void doRender( EntityShip ship, double x, double y, double z, float yaw, float partialTickTime )
	{
		// UNDONE: render the ship
	}
}
