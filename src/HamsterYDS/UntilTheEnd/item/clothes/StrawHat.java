package HamsterYDS.UntilTheEnd.item.clothes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import HamsterYDS.UntilTheEnd.api.UntilTheEndApi;
import HamsterYDS.UntilTheEnd.cap.hum.Humidity;
import HamsterYDS.UntilTheEnd.guide.CraftGuide;
import HamsterYDS.UntilTheEnd.item.ItemLoader;
import HamsterYDS.UntilTheEnd.item.ItemProvider;
import HamsterYDS.UntilTheEnd.item.materials.Rope;

/**
 * @author 南外丶仓鼠
 * @version V5.1.1
 */
public class StrawHat implements Listener{
	public static ItemStack item;
	public static NamespacedKey nsk=new NamespacedKey(Humidity.plugin,"ute.strawhat");
	public StrawHat() {		
		ShapelessRecipe recipe=new ShapelessRecipe(nsk,item);
		recipe.addIngredient(6,Material.LEAVES);
		recipe.addIngredient(2,Rope.item.getType());
		Bukkit.addRecipe(recipe);
		ItemLoader.plugin.getServer().getPluginManager().registerEvents(this,ItemLoader.plugin);
		ItemProvider.addItem(this.getClass(),item);

		Inventory inv=CraftGuide.getCraftInventory();
		inv.setItem(11,item);
		inv.setItem(14,Rope.item);
		inv.setItem(15,new ItemStack(Material.LEAVES,6));
		inv.setItem(16,Rope.item);
		UntilTheEndApi.GuideApi.addItemCraftInv("§6草帽",inv);
		CraftGuide.addItem("§6衣物",item);
	}
	
	@EventHandler public void onCraft(CraftItemEvent event) {
		ItemStack item=event.getRecipe().getResult();
        item.setAmount(1);
        if (item.equals(this.item)) {
            if (!event.getInventory().containsAtLeast(Rope.item,2)) {
                event.setCancelled(true);
            }
        }
	}
}
