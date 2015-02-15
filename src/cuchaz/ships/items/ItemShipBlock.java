package cuchaz.ships.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cuchaz.ships.ShipType;

public class ItemShipBlock extends ItemBlock {
	
	public ItemShipBlock(Block block) {
		super(block);
		setMaxDamage(0);
		setHasSubtypes(true);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int damage) {
		return this.field_150939_a.func_149735_b(2, damage);
	}
	
	public int getMetadata(int meta) {
		return meta;
	}
	
	public String getUnlocalizedName(ItemStack itemStack) {
		return super.getUnlocalizedName() + "." + ShipType.getByMeta(itemStack.getItemDamage()).name().toLowerCase();
	}
}
