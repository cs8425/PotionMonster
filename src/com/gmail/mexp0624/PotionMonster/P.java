package com.gmail.mexp0624.PotionMonster;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

public class P extends JavaPlugin implements Listener
{
	public static P pl;
	FileConfiguration config;
	Random random = new Random();
	HashMap<String, List<effect>> affect = new HashMap();
	HashMap<String, Integer> respawn = new HashMap();
  
	public void onEnable() {
		pl = this;
		Bukkit.getPluginManager().registerEvents(this, this);

		loadConfig();
	}
  
	public void onDisable() {
		HandlerList.unregisterAll((org.bukkit.plugin.java.JavaPlugin)pl);
		this.affect.clear();
		this.respawn.clear();
	}
  
	public void loadConfig() {
		reloadConfig();
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			if (!configFile.getParentFile().mkdirs()) {
				getLogger().warning("Could not create config.yml directory.");
			}
			saveDefaultConfig();
		}
		this.config = getConfig();
		EntityType[] arrayOfEntityType;
		int j = (arrayOfEntityType = EntityType.values()).length;
		for (int i = 0; i < j; i++) {
			EntityType ent = arrayOfEntityType[i];
			String mob = getMob(ent);
			if (mob != null) {
				System.out.println("[addType]: " + mob);
				this.affect.put(mob, parseList(mob));
				this.respawn.put(mob, Integer.valueOf(this.config.getInt(mob + ".respawn", 0)));
			}
		}
	}
  
	List<effect> parseList(String mobName) {
		List<String> tmplist = this.config.getStringList(mobName + ".conf");
		List<effect> mobEff = new ArrayList();
		for (String tmp : tmplist) {
			effect eff = parseLine(tmp);
			if (eff != null) {
				mobEff.add(eff);
			}
		}
		return mobEff;
	}
  
	effect parseLine(String str) {
		String[] effs = str.split(":");

		effect out = null;
		if (effs.length == 2) {
			try {
				boolean show = false;
				byte type = 'X';
				if (effs[0].contains("/")) {
					String[] types = effs[0].split("/");
					if (types.length == 2) {
						if (types[1].contains("T")) { // toggle
							type = 'T';
						} else if (types[1].contains("1")) { // force change
							type = '1';
						} else if (types[1].contains("0")) { // force cancel
							type = '0';
						} else if (types[1].contains("X")) { // don't care
							type = 'X';
						}
						effs[0] = types[0];
						if (types[1].contains("@")) { // show Potion Effect
							System.out.println("[eff][" + (char)type + "]@P: " + effs[0] + "/10000");
							show = true;
						} else {
							System.out.println("[eff][" + (char)type + "]_P: " + effs[0] + "/10000");
						}
					}
				} else {
					System.out.println("[eff][X]_P: " + effs[0] + "/10000");
				}
				out = new effect(Short.parseShort(effs[0]), type);
				String[] parms = effs[1].split(";");
				String[] arrayOfString1;
				int j = (arrayOfString1 = parms).length;
				for (int i = 0; i < j; i++) {
					String parm = arrayOfString1[i];
					String[] eff = parm.split(",");
					if (eff.length == 2) {
						try {
							System.out.println("[eff] " + eff[0] + " @ " + eff[1]);

							out.add(new PotionEffect(PotionEffectType.getByName(eff[0].toUpperCase()), Integer.MAX_VALUE, Integer.valueOf(eff[1]).intValue(), show, show));
						}
						catch (NumberFormatException localNumberFormatException) {}
					}
				}
			}
			catch (NumberFormatException localNumberFormatException1) {}
		}
		return out;
	}
  
	public String getMob(EntityType e) {
		switch (e) {
		case ZOMBIE: 
			return "ZOMBIE";
		case CREEPER: 
			return "CREEPER";
		case SKELETON: 
			return "SKELETON";
		case SPIDER: 
			return "SPIDER";
		case CAVE_SPIDER: 
			return "CAVE_SPIDER";
		case SLIME: 
			return "SLIME";
		case WITCH: 
			return "WITCH";
		case ENDERMAN: 
			return "ENDERMAN";
		case PIG_ZOMBIE: 
			return "PIG_ZOMBIE";
		case BLAZE: 
			return "BLAZE";
		case ENDER_DRAGON: 
			return "ENDER_DRAGON";
		case GHAST: 
			return "GHAST";
		case GUARDIAN: 
			return "GUARDIAN";
		case SILVERFISH: 
			return "SILVERFISH";
		case WITHER: 
			return "WITHER";
		case WITHER_SKULL: 
			return "WITHER_SKULL";
		case OCELOT: 
			return "OCELOT";
		case WOLF: 
			return "WOLF";
		case IRON_GOLEM: 
			return "IRON_GOLEM";
		case SNOWMAN: 
			return "SNOWMAN";
		}
		return null;
	}
  
	public void transType(LivingEntity ent, EntityType et, byte type) {
		boolean val = false;
		if (type == 'T') { // toggle
			switch (et) {
			case ZOMBIE: 
			case PIG_ZOMBIE: 
				val = !((Zombie)ent).isBaby();
				break;
			case CREEPER: 
				val = !((Creeper)ent).isPowered();
				break;
			case SKELETON: 
				val = ((Skeleton)ent).getSkeletonType() == Skeleton.SkeletonType.NORMAL;
				break;
			}
		} else if (type == '1') { // force change
			val = true;
		} else if (type == '0') { // force cancel
			val = false;
		} else {
			return;
		}

		switch (et) {
		case ZOMBIE: 
		case PIG_ZOMBIE: 
			((Zombie)ent).setBaby(val);
			break;
		case CREEPER: 
			((Creeper)ent).setPowered(val);
			break;
		case SKELETON: 
			((Skeleton)ent).setSkeletonType(val ? Skeleton.SkeletonType.WITHER : Skeleton.SkeletonType.NORMAL);
			break;
		}
	}
  
	@EventHandler
	public void onSpawn(CreatureSpawnEvent e) {
		String mob = getMob(e.getEntityType());
		LivingEntity ent = e.getEntity();
		if (mob != null) {
			List<effect> efflist = (List)this.affect.get(mob);
			if (efflist != null) {
				int i = 0;
				int selected = this.random.nextInt(10000);
				for (effect conf : efflist) {
					i += conf.P;
					if (i > selected) {
						transType(ent, e.getEntityType(), conf.type);
						List<PotionEffect> toAdd = conf.eff;
						for (PotionEffect fx : toAdd) {
							ent.addPotionEffect(fx);
						}
						break;
					}
				}
			}
		}
	}
  
	@EventHandler
	public void onMobDeath(EntityDeathEvent e) {
		String mob = getMob(e.getEntityType());
		if (mob != null) {
			Integer p = (Integer)this.respawn.get(mob);
			if ((p != null) && (p.intValue() > this.random.nextInt(10000))) {
				final LivingEntity ent = e.getEntity();

				pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
					public void run() {
						ent.getWorld().spawn(ent.getLocation(), ent.getClass());
					}
				}, 12L);
			}
		}
	}
  
	static class effect {
		public final short P;
		public final List<PotionEffect> eff;
		public final byte type;

		effect(short P, byte type) {
			this.P = P;
			this.type = type;
			this.eff = new ArrayList();
		}

		public void add(PotionEffect ef) {
			this.eff.add(ef);
		}
	}
}

