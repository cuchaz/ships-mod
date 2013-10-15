package net.minecraft.entity;

public class EntityAccessor
{
	// access protected methods of classes by package-injection
	public static void updateFallState( Entity entity, double dy, boolean isOnGround )
	{
		entity.updateFallState( dy, isOnGround );
	}
}
