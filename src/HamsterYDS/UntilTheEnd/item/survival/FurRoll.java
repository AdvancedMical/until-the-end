package HamsterYDS.UntilTheEnd.item.survival;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import HamsterYDS.UntilTheEnd.api.UntilTheEndApi.BlockApi;
import HamsterYDS.UntilTheEnd.item.ItemManager;
import HamsterYDS.UntilTheEnd.player.PlayerManager;
import HamsterYDS.UntilTheEnd.player.death.DeathCause;
import HamsterYDS.UntilTheEnd.player.death.DeathMessage;

public class FurRoll implements Listener{
	public FurRoll() {		
		HashMap<ItemStack,Integer> materials=new HashMap<ItemStack,Integer>();
		materials.put(ItemManager.namesAndItems.get("§6稻草卷"),1);
		materials.put(ItemManager.namesAndItems.get("§6兔毛"),3);
		materials.put(ItemManager.namesAndItems.get("§6绳子"),1);
		ItemManager.registerRecipe(materials,ItemManager.namesAndItems.get("§6毛皮卷"),"§6生存");
		ItemManager.plugin.getServer().getPluginManager().registerEvents(this,ItemManager.plugin);
	}
	@EventHandler public void onClick(PlayerInteractEvent event) {
		Player player=event.getPlayer();
		if(event.getAction()!=Action.RIGHT_CLICK_AIR) return;
		ItemStack item=player.getItemInHand().clone();
		if(item==null) return;
		item.setAmount(1);
		if(item.equals(ItemManager.namesAndItems.get("§6毛皮卷"))) {
			event.setCancelled(true);
			if(player.getWorld().getEnvironment()!=Environment.NORMAL) {
				player.sendMessage("§6[§cUntilTheEnd§6]§r 非主世界不可使用此物品！");
				player.getWorld().createExplosion(player.getLocation(),3);
				if(player.isDead()) DeathMessage.causes.put(player.getName(),DeathCause.INVALIDSLEEPNESS);
				return;
			}
			if(player.getWorld().getTime()>=23000||player.getWorld().getTime()<=16000) {
				player.sendMessage("§6[§cUntilTheEnd§6]§r 白天不可使用此物品！");
				return;
			}
			if(Math.random()<=0.3) {
				ItemStack itemr=player.getItemInHand();
				itemr.setAmount(itemr.getAmount()-1);
			}
			Location loc=player.getLocation();
			new BukkitRunnable() {
				@Override
				public void run() {
					player.removePotionEffect(PotionEffectType.BLINDNESS);
					player.removePotionEffect(PotionEffectType.NIGHT_VISION);
					player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,50,1));
					player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,50,1));
					if(Math.random()<=0.1) {
						if(player.getHealth()+1<player.getMaxHealth())
							player.setHealth(player.getHealth()+1); 
						PlayerManager.change(player.getName(),"san",1); 
					}
					if(Math.random()<=0.07) {
						if(player.getFoodLevel()>=1) player.setFoodLevel(player.getFoodLevel()-1);
						else cancel();
					}
					if(!BlockApi.locToStr(player.getLocation()).equalsIgnoreCase(BlockApi.locToStr(loc))) cancel();
				}
			}.runTaskTimer(ItemManager.plugin,0L,1L); 
		}
	}
}