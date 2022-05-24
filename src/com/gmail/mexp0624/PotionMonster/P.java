package com.gmail.mexp0624.PotionMonster;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Bee;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataAdapterContext;

public class P extends JavaPlugin implements Listener {
	public static P pl;
	Logger logger = getLogger();
	FileConfiguration config;
	Random random = new Random();
	ConcurrentHashMap<EntityType, List<effect>> affect = new ConcurrentHashMap();
	ConcurrentHashMap<EntityType, Integer> respawn = new ConcurrentHashMap();

	// NamespacedKeys for PersistentDataContainer
	private final Map<String, NamespacedKey> nKey = new HashMap<>();

	// List<TargetChan> track = new ArrayList(); // wait for setable AI
	// ConcurrentHashMap<Entity, TargetChan> track = new ConcurrentHashMap(); //
	// wait for setable AI
	// ConcurrentHashMap<Entity, TargetChan> trackCarrier = new ConcurrentHashMap();
	// // wait for setable AI
	ConcurrentHashMap<UUID, UUID> track = new ConcurrentHashMap(); // passengerUUID, carrierUUID

	public P() {
		nKey.put("uuid", new NamespacedKey(this, "uuid"));
		nKey.put("carrier", new NamespacedKey(this, "carrier"));
	}

	public void onEnable() {
		pl = this;
		Bukkit.getPluginManager().registerEvents(this, this);

		loadConfig();
		// readFlyer();

		/*
		 * Bukkit.getServer().getScheduler().runTaskTimer(this, new Runnable() {
		 * 
		 * @Override
		 * public void run() {
		 * updateTarget();
		 * }
		 * }, 20, 10);
		 */
	}

	public void onDisable() {
		HandlerList.unregisterAll((org.bukkit.plugin.java.JavaPlugin) pl);
		this.affect.clear();
		this.respawn.clear();

		// saveFlyer(); // save first
		// this.track.clear();
	}

	// command
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> toreturn = new ArrayList<String>();
		if (!sender.hasPermission("potionmon.reload")) {
			return toreturn; // no permission, return empty
		}

		if (args.length == 1) {
			toreturn.add("reload");
		}
		return toreturn;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (!sender.hasPermission("potionmon.reload")) {
			return true; // no permission
		}
		if (args.length == 1) {
			if (args[0].equalsIgnoreCase("reload")) {
				this.onDisable();
				this.onEnable();
				sender.sendMessage(ChatColor.GOLD + "[PotionMonster]" + ChatColor.WHITE + " reloaded !");
				return true;
			}
			sender.sendMessage("There is no " + ChatColor.GOLD + args[0] + " command!");
			return true;
		}
		return false;
	}

	public void saveFlyer() {
		File cacheFd = new File(getDataFolder(), "cache.yml");
		YamlConfiguration list = new YamlConfiguration();

		List<String> outlist = new ArrayList<>();
		// this.track.forEach((ent, tchan) -> {
		// if (tchan == null) {
		// return;
		// }

		// final LivingEntity carrier = tchan.Carrier;
		// final LivingEntity passenger = tchan.Passenger;
		// if (carrier == null) {
		// return;
		// }
		// if (passenger == null) {
		// return;
		// }
		// outlist.add(String.format("%s:%s", carrier.getUniqueId(),
		// passenger.getUniqueId()));
		// });
		this.track.forEach((passengerUUID, carrierUUID) -> {
			if (carrierUUID == null) {
				return;
			}
			outlist.add(String.format("%s:%s", passengerUUID, carrierUUID));
		});
		list.set("UUID", outlist);

		try {
			list.save(cacheFd);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readFlyer() {
		File cacheFd = new File(getDataFolder(), "cache.yml");
		if (!cacheFd.exists()) {
			return;
		}
		YamlConfiguration list = YamlConfiguration.loadConfiguration(cacheFd);
		List<String> uuidList = list.getStringList("UUID");

		Server server = Bukkit.getServer();

		for (String argv : uuidList) {
			String[] args = argv.split(":");
			if (args.length != 2)
				continue;

			UUID passengerUUID = UUID.fromString(args[0]);
			UUID carrierUUID = UUID.fromString(args[1]);
			final LivingEntity passenger = (LivingEntity) server.getEntity(passengerUUID);
			final LivingEntity carrier = (LivingEntity) server.getEntity(carrierUUID);
			if (passenger == null) {
				continue;
			}
			if (carrier == null) {
				continue;
			}
			// this.track.put(passenger, new TargetChan((LivingEntity)carrier,
			// (LivingEntity)passenger));
			this.track.put(passengerUUID, carrierUUID);
		}
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
			String mob = ent.name();
			logger.log(Level.INFO, "[addType]: " + mob);
			if (!this.config.isConfigurationSection(mob))
				continue; // not define, skip
			this.affect.put(ent, parseList(mob));
			this.respawn.put(ent, Integer.valueOf(this.config.getInt(mob + ".respawn", 0)));
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
				boolean fly = false;
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
						if (types[1].contains("F")) { // fly
							fly = true;
						}
						effs[0] = types[0];
						if (types[1].contains("@")) { // show Potion Effect
							logger.log(Level.INFO, "[eff][" + (char) type + "]@P: " + effs[0] + "/10000");
							show = true;
						} else {
							logger.log(Level.INFO, "[eff][" + (char) type + "]_P: " + effs[0] + "/10000");
						}
					}
				} else {
					logger.log(Level.INFO, "[eff][X]_P: " + effs[0] + "/10000");
				}
				out = new effect(Short.parseShort(effs[0]), type, fly);
				String[] parms = effs[1].split(";");
				String[] arrayOfString1;
				int j = (arrayOfString1 = parms).length;
				for (int i = 0; i < j; i++) {
					String parm = arrayOfString1[i];
					String[] eff = parm.split(",");
					if (eff.length == 2) {
						try {
							logger.log(Level.INFO, "[eff] " + eff[0] + " @ " + eff[1]);

							out.add(new PotionEffect(PotionEffectType.getByName(eff[0].toUpperCase()),
									Integer.MAX_VALUE, Integer.valueOf(eff[1]).intValue(), show, show));
						} catch (NumberFormatException localNumberFormatException) {
						}
					}
				}
			} catch (NumberFormatException localNumberFormatException1) {
			}
		}
		return out;
	}

	LivingEntity transType(LivingEntity ent, EntityType et, byte type, boolean fly) {
		boolean val = false;
		if (type == 'T') { // toggle
			switch (et) {
				case ZOMBIE:
				case DROWNED:
				case HUSK:
				case ZOMBIFIED_PIGLIN:
				case PIGLIN:
				case PIGLIN_BRUTE:
					// case HOGLIN:
					// case ZOGLIN:
					val = ((Ageable) ent).isAdult();
					break;
				case CREEPER:
					val = !((Creeper) ent).isPowered();
					break;
				case SKELETON:
				case WITHER_SKELETON:
				case GUARDIAN:
				case ELDER_GUARDIAN:
					val = true;
					break;
			}
		} else if (type == '1') { // force change
			val = true;
		} else if (type == '0') { // force cancel
			val = false;
		} else {
			if (fly)
				this.setFly(ent);
			return ent;
		}

		switch (et) {
			case ZOMBIE:
			case DROWNED:
			case HUSK:
			case ZOMBIFIED_PIGLIN:
			case PIGLIN:
			case PIGLIN_BRUTE:
				if (val) {
					((Ageable) ent).setBaby();
				} else {
					((Ageable) ent).setAdult();
				}
				if (fly)
					this.setFly(ent);
				break;
			case CREEPER:
				((Creeper) ent).setPowered(val);
				break;
			case SKELETON:
				if (val)
					this.Respawn(ent, EntityType.WITHER_SKELETON, fly);
				break;
			case WITHER_SKELETON:
				if (val)
					this.Respawn(ent, EntityType.SKELETON, fly);
				break;
			case GUARDIAN:
				if (val)
					this.Respawn(ent, EntityType.ELDER_GUARDIAN, fly);
				break;
			case ELDER_GUARDIAN:
				if (val)
					this.Respawn(ent, EntityType.GUARDIAN, fly);
				break;
			default:
				if (fly)
					this.setFly(ent);
		}
		return ent;
	}

	public void Respawn(LivingEntity ent, EntityType newType, boolean fly) {
		final Vector vel = ent.getVelocity();
		final World ww = ent.getWorld();
		ent.remove();
		LivingEntity ent2 = (LivingEntity) ww.spawnEntity(ent.getLocation(), newType);
		ent2.setVelocity(vel);
		if (fly) {
			setFly(ent2);
		}
	}

	public void setFly(LivingEntity ent) {
		final Vector vel = ent.getVelocity();
		final World ww = ent.getWorld();
		// Mob bat = (Mob) ww.spawnEntity(ent.getLocation(), EntityType.BAT);
		Mob bat = (Mob) ww.spawnEntity(ent.getLocation(), EntityType.BEE);
		// Mob bat = (Mob) ww.spawnEntity(ent.getLocation(), EntityType.PARROT);
		// Mob bat = (Mob) ww.spawnEntity(ent.getLocation(), EntityType.PHANTOM);
		// Mob bat = (Mob) ww.spawnEntity(ent.getLocation(), EntityType.VEX);
		// bat.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new
		// AttributeModifier("hp", 20, AttributeModifier.Operation.ADD_NUMBER));
		bat.setVelocity(vel);
		bat.addPotionEffect(
				new PotionEffect(PotionEffectType.getByName("REGENERATION"), Integer.MAX_VALUE, 1, false, false));
		bat.addPotionEffect(
				new PotionEffect(PotionEffectType.getByName("HEALTH_BOOST"), Integer.MAX_VALUE, 5, false, false));
		bat.addPotionEffect(new PotionEffect(PotionEffectType.getByName("HEAL"), Integer.MAX_VALUE, 5, false, false));
		bat.addPotionEffect(
				new PotionEffect(PotionEffectType.getByName("FIRE_RESISTANCE"), Integer.MAX_VALUE, 0, false, false));
		// bat.addPotionEffect(new PotionEffect(PotionEffectType.getByName("SPEED"),
		// Integer.MAX_VALUE, 5, false, false));
		// TODO: other buff
		bat.setMetadata("PotionMonster-Carrier", new FixedMetadataValue(this, 1));

		// TODO: NOT RemoveWhenFarAway option
		// if (ent instanceof Monster) {
		// bat.setRemoveWhenFarAway(true);
		// }
		bat.setRemoveWhenFarAway(ent.getRemoveWhenFarAway());
		bat.addPassenger(ent);
		bat.setTarget(((Mob) ent).getTarget());

		// this.track.put(ent.getUniqueId(), bat.getUniqueId());
		// this.track.put(ent, new TargetChan((LivingEntity)bat, (LivingEntity)ent));
		// this.trackCarrier.put(bat, new TargetChan((Mob)bat, (Mob)ent));

		// bind two-way
		setCarrier(bat, ent);
		setCarrier(ent, bat);
	}

	@EventHandler
	public void onSpawn(CreatureSpawnEvent e) {
		EntityType mob = e.getEntityType();
		LivingEntity ent = e.getEntity();
		if (mob != null) {
			List<effect> efflist = (List) this.affect.get(mob);
			if (efflist != null) {
				int i = 0;
				int selected = this.random.nextInt(10000);
				for (effect conf : efflist) {
					i += conf.P;
					if (i > selected) {
						ent = transType(ent, e.getEntityType(), conf.type, conf.fly);
						ent.setMetadata("PotionMonster-buff", new FixedMetadataValue(this, selected));
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
		EntityType mob = e.getEntityType();
		if (mob != null) {
			Integer p = (Integer) this.respawn.get(mob);
			if ((p != null) && (p.intValue() > this.random.nextInt(10000))) {
				final LivingEntity ent = e.getEntity();
				final Vector vel = ent.getVelocity();

				pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
					public void run() {
						LivingEntity enty = ent.getWorld().spawn(ent.getLocation(), ent.getClass());
						enty.setVelocity(vel);
					}
				}, 12L);
			}
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		// Location l = event.getLocation();
		// World world = l.getWorld();
		Entity ent = event.getEntity();

		if (ent instanceof Creeper) {
			LivingEntity lent = (LivingEntity) ent;
			if (lent.hasMetadata("PotionMonster-buff")) {
				removePotionEffect(lent);
			}
		} else {
			// unhandled entity
			// event.setCancelled(true);
			return;
		}
	}

	public void removePotionEffect(LivingEntity ent) {
		for (PotionEffect fx : ent.getActivePotionEffects()) {
			ent.removePotionEffect(fx.getType());
		}
	}

	@EventHandler
	public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent e) {
		Entity ent = e.getEntity();
		// Entity carrier = ent.getVehicle();
		// if (carrier != null) {
		// // getLogger().info("[EntityRemoveFromWorldEvent]: " + e.toString() + " " +
		// carrier.toString());
		// if (carrier.hasMetadata("PotionMonster-Carrier")) {
		// pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
		// public void run() {
		// carrier.remove();
		// }
		// }, 4L);
		// }
		// // if (carrier instanceof Bee) {
		// // carrier.remove();
		// // }
		// }
		// UUID entUUID = ent.getUniqueId();
		// UUID carrierUUID = this.track.get(entUUID);
		UUID carrierUUID = getCarrier(ent);
		if (carrierUUID != null) {
			pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
				public void run() {
					final Server server = Bukkit.getServer();
					final LivingEntity carrier = (LivingEntity) server.getEntity(carrierUUID);
					if (carrier == null) {
						return;
					}
					carrier.remove();
				}
			}, 4L);
			// this.track.remove(entUUID);
		}

		// TargetChan tchan = this.track.get(ent);
		// if (tchan != null) {
		// //tchan.Carrier.remove(); // this cause
		// java.util.ConcurrentModificationException
		// pl.getServer().getScheduler().scheduleSyncDelayedTask(pl, new Runnable() {
		// public void run() {
		// tchan.Carrier.remove();
		// }
		// }, 4L);
		// this.track.remove(ent);
		// }
		// this.trackCarrier.remove(ent);
	}

	@EventHandler
	// public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent e) {
	public void onEntityTargetEvent(EntityTargetEvent e) {
		Entity ent = e.getEntity();
		/*
		 * Entity carrier = ent.getVehicle();
		 * if (carrier != null) {
		 * //getLogger().info("[EntityTargetEvent]: " + e.toString() + " " +
		 * carrier.toString());
		 * if (carrier instanceof Bee) {
		 * LivingEntity target = ((Mob) ent).getTarget();
		 * ((Mob) carrier).setTarget(target);
		 * }
		 * }
		 */
		// TargetChan tchan = this.track.get(ent);
		// if (tchan != null) {
		// tchan.update();
		// }

		// if (ent.hasMetadata("PotionMonster-Carrier")) {
		if (hasCarrier(ent)) {
			if (ent instanceof Bee) {
				Bee bee = (Bee) ent;
				bee.setAnger(0);
				bee.setHasNectar(true);
			}
			e.setCancelled(true);
		}

		/*
		 * tchan = this.trackCarrier.get(ent);
		 * if (tchan != null) {
		 * //e.setCancelled(true);
		 * tchan.reset();
		 * }
		 */
	}
	/*
	 * public void updateTarget() {
	 * this.track.forEach((ent, tchan) -> {
	 * if (tchan == null) {
	 * return;
	 * }
	 * tchan.update();
	 * });
	 * }
	 */

	@EventHandler
	public void onEntityDismountEvent(EntityDismountEvent e) {
		Entity ent = e.getEntity();
		Entity entDe = e.getDismounted();
		// if (ent.hasMetadata("PotionMonster-Carrier")) {
		// e.setCancelled(true);
		// }
		// if (entDe.hasMetadata("PotionMonster-Carrier")) {
		// e.setCancelled(true);
		// }
		if (hasCarrier(ent) || hasCarrier(entDe)) {
			e.setCancelled(true);
		}
	}

	protected boolean hasCarrier(Entity ent) {
		PersistentDataHolder dataHolder = null;
		try {
			dataHolder = (PersistentDataHolder) ent;
		} catch (ClassCastException e) {
		}
		if (dataHolder == null)
			return false;
		PersistentDataContainer dc = dataHolder.getPersistentDataContainer();
		Integer carr = dc.getOrDefault(nKey.get("carrier"), PersistentDataType.INTEGER, null);
		if (carr == null)
			return false;
		return true;
	}

	protected UUID getCarrier(Entity ent) {
		PersistentDataHolder dataHolder = null;
		try {
			dataHolder = (PersistentDataHolder) ent;
		} catch (ClassCastException e) {
		}
		if (dataHolder == null)
			return null;
		PersistentDataContainer dc = dataHolder.getPersistentDataContainer();
		return dc.getOrDefault(nKey.get("uuid"), new UUIDTagType(), null);
	}

	protected boolean setCarrier(Entity carrier, Entity passenger) {
		PersistentDataHolder dataHolder = null;
		try {
			dataHolder = (PersistentDataHolder) passenger;
		} catch (ClassCastException e) {
		}
		if (dataHolder != null) {
			PersistentDataContainer dc = dataHolder.getPersistentDataContainer();
			// dc.set(nKey.get("uuid"), PersistentDataType.STRING, uuid.toString());
			dc.set(nKey.get("uuid"), new UUIDTagType(), carrier.getUniqueId());
			dc.set(nKey.get("carrier"), PersistentDataType.INTEGER, 0x01);
			return true;
		}
		return false;
	}

	public boolean clear(PersistentDataContainer dc) {
		dc.remove(nKey.get("uuid"));
		dc.remove(nKey.get("carrier"));
		return true;
	}

	static class effect {
		public final short P;
		public final List<PotionEffect> eff;
		public final byte type;
		public final boolean fly;

		effect(short P, byte type, boolean fly) {
			this.P = P;
			this.type = type;
			this.fly = fly;
			this.eff = new ArrayList();
		}

		public void add(PotionEffect ef) {
			this.eff.add(ef);
		}
	}

	static class TargetChan {
		public final LivingEntity Carrier;
		public final LivingEntity Passenger;
		public LivingEntity Target;

		TargetChan(LivingEntity carrier, LivingEntity passenger) {
			this.Carrier = carrier;
			this.Passenger = passenger;
		}

		/*
		 * public void update() {
		 * this.Target = this.Passenger.getTarget();
		 * this.Carrier.setTarget(this.Target);
		 * }
		 * public void reset() {
		 * this.Carrier.setTarget(this.Target);
		 * }
		 */
	}

	public class UUIDTagType implements PersistentDataType<byte[], UUID> {
		@Override
		public Class<byte[]> getPrimitiveType() {
			return byte[].class;
		}

		@Override
		public Class<UUID> getComplexType() {
			return UUID.class;
		}

		@Override
		public byte[] toPrimitive(UUID uuid, PersistentDataAdapterContext context) {
			ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
			bb.putLong(uuid.getMostSignificantBits());
			bb.putLong(uuid.getLeastSignificantBits());
			return bb.array();
		}

		@Override
		public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
			ByteBuffer bb = ByteBuffer.wrap(primitive);
			long firstLong = bb.getLong();
			long secondLong = bb.getLong();
			return new UUID(firstLong, secondLong);
		}
	}
}
