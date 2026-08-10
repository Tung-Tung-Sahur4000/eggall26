package dev.shadmage.eggemall2.Events;

import dev.shadmage.eggemall.lib.Common;
import dev.shadmage.eggemall.lib.ItemUtil;
import dev.shadmage.eggemall.lib.PlayerUtil;
import dev.shadmage.eggemall.lib.RandomUtil;
import dev.shadmage.eggemall.lib.annotation.AutoRegister;
import dev.shadmage.eggemall.lib.remain.CompMaterial;
import dev.shadmage.eggemall.lib.remain.CompParticle;
import dev.shadmage.eggemall2.CustomEvents.EntityCaptureEvent;
import dev.shadmage.eggemall2.CustomEvents.EntityEscapeCaptureEvent;
import dev.shadmage.eggemall2.EggEmAllPlugin;
import dev.shadmage.eggemall2.Settings.Settings;
import dev.shadmage.eggemall2.Utils.ProcessPlaceholderMessages;
import dev.shadmage.eggemall2._external.StackingPlugins.StackingPluginAPI;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

@AutoRegister
public final class EggListener
implements Listener {
    private static final NamespacedKey EGGEMALL_ENTITY_DATA = new NamespacedKey((Plugin)EggEmAllPlugin.getInstance(), "EGGEMALL_ENTITY_DATA");

    @EventHandler
    public void onPlayerEggThrow(PlayerEggThrowEvent event) {
        if (EggEmAllPlugin.thrownEggs.contains(event.getEgg())) {
            event.setHatching(false);
            EggEmAllPlugin.thrownEggs.remove(event.getEgg());
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        final Projectile shot = event.getEntity();
        if (Settings.Particles.PLAYER_THROW_ONLY.booleanValue() && !(shot.getShooter() instanceof Player)) {
            return;
        }
        if (shot instanceof Egg) {
            String currentWorldName = shot.getWorld().getName();
            if (Settings.BlacklistWorlds.AS_WHITELIST.booleanValue() == Settings.BlacklistWorlds.WORLDS.contains(currentWorldName) && Settings.Particles.EGG_TRAILS.booleanValue()) {
                new BukkitRunnable(){

                    public void run() {
                        if (!shot.isValid() || shot.isOnGround() || shot.isInWater()) {
                            this.cancel();
                            return;
                        }
                        CompParticle.SPELL_WITCH.spawn(shot.getLocation());
                    }
                }.runTaskTimer((Plugin)EggEmAllPlugin.getInstance(), 0L, 1L);
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onEntityHitByEgg(EntityDamageEvent event) {
        StackingPluginAPI stackerPluginAPI;
        Common.setTellPrefix(Settings.CHAT_PREFIX);
        Entity targetEntity = event.getEntity();
        String groupPermission = EggEmAllPlugin.catchableMobs.getCatchPermission(targetEntity);
        String mobSpecificPermission = "eggemall.catchmob." + targetEntity.getName();
        if (!(event instanceof EntityDamageByEntityEvent)) {
            return;
        }
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent)event;
        Entity entity = damageEvent.getDamager();
        if (!(entity instanceof Egg)) {
            return;
        }
        Egg egg = (Egg)entity;
        EntityCaptureEvent entityCaptureEvent = new EntityCaptureEvent(targetEntity, egg);
        EntityEscapeCaptureEvent entityEscapeEvent = new EntityEscapeCaptureEvent(targetEntity, egg);
        if (!Settings.BlacklistWorlds.AS_WHITELIST.booleanValue() && Settings.BlacklistWorlds.WORLDS.contains(egg.getWorld().getName())) {
            ProjectileSource projectileSource;
            if (Settings.Messages.BLACKLISTED_WORLD.length() > 0 && (projectileSource = egg.getShooter()) instanceof Player) {
                Player player = (Player)projectileSource;
                Common.tell((CommandSender)player, ProcessPlaceholderMessages.ReplacePlaceholders(Settings.Messages.BLACKLISTED_WORLD, targetEntity));
            }
            return;
        }
        if (!EggEmAllPlugin.catchableMobs.isCatchable(targetEntity)) {
            ProjectileSource projectileSource;
            if (Settings.Messages.NOT_CATCHABLE.length() > 0 && (projectileSource = egg.getShooter()) instanceof Player) {
                Player player = (Player)projectileSource;
                Common.tell((CommandSender)player, ProcessPlaceholderMessages.ReplacePlaceholders(Settings.Messages.NOT_CATCHABLE, targetEntity));
            }
            return;
        }
        if (Settings.CatchChance.SPAWN_CHICKEN_ON_FAIL.booleanValue()) {
            EggEmAllPlugin.thrownEggs.add(egg);
        }
        if (Settings.Restrictions.PREVENT_CATCHING_BABIES.booleanValue() && targetEntity instanceof Ageable && !((Ageable)targetEntity).isAdult()) {
            ProjectileSource projectileSource;
            if (Settings.Messages.NO_BABIES.length() > 0 && (projectileSource = egg.getShooter()) instanceof Player) {
                Player player = (Player)projectileSource;
                Common.tell((CommandSender)player, Settings.Messages.NO_BABIES);
            }
            return;
        }
        if (Settings.Restrictions.PREVENT_CATCHING_TAMED.booleanValue() && targetEntity instanceof Tameable && ((Tameable)targetEntity).isTamed()) {
            ProjectileSource projectileSource;
            if (Settings.Messages.NO_TAMED.length() > 0 && (projectileSource = egg.getShooter()) instanceof Player) {
                Player player = (Player)projectileSource;
                Common.tell((CommandSender)player, Settings.Messages.NO_TAMED);
            }
            return;
        }
        if (Settings.Restrictions.PREVENT_CATCHING_SHEARED_SHEEP.booleanValue() && targetEntity instanceof Sheep && ((Sheep)targetEntity).isSheared()) {
            ProjectileSource projectileSource;
            if (Settings.Messages.NO_SHEARED_SHEEP.length() > 0 && (projectileSource = egg.getShooter()) instanceof Player) {
                Player player = (Player)projectileSource;
                Common.tell((CommandSender)player, Settings.Messages.NO_SHEARED_SHEEP);
            }
            return;
        }
        if (Settings.Restrictions.PREVENT_CATCHING_NAMED_ENTITIES.booleanValue() && targetEntity.getCustomName() != null) {
            ProjectileSource projectileSource;
            if (Settings.Messages.NO_NAMED_ENTITIES.length() > 0 && (projectileSource = egg.getShooter()) instanceof Player) {
                Player player = (Player)projectileSource;
                Common.tell((CommandSender)player, Settings.Messages.NO_NAMED_ENTITIES);
            }
            return;
        }
        EggEmAllPlugin.getInstance().getServer().getPluginManager().callEvent((Event)entityCaptureEvent);
        if (entityCaptureEvent.isCancelled()) {
            return;
        }
        ProjectileSource projectileSource = egg.getShooter();
        if (!(projectileSource instanceof Player)) {
            if (Settings.Restrictions.ONLY_ALLOW_PLAYER_THROWN_EGGS.booleanValue()) {
                return;
            }
            if (!RandomUtil.chance(Settings.CatchChance.CHANCE_PERCENTAGE)) {
                EggEmAllPlugin.getInstance().getServer().getPluginManager().callEvent((Event)entityEscapeEvent);
                return;
            }
        } else {
            Player player = (Player)projectileSource;
            if (Settings.Restrictions.REQUIRE_PERMISSIONS.booleanValue() && !player.hasPermission(groupPermission) && !player.hasPermission(mobSpecificPermission)) {
                if (Settings.Messages.NO_PERMISSION.length() > 0) {
                    Common.tell((CommandSender)player, ProcessPlaceholderMessages.ReplacePlaceholders(Settings.Messages.NO_PERMISSION, targetEntity));
                }
                return;
            }
            if (RandomUtil.chance(Settings.CatchChance.CHANCE_PERCENTAGE)) {
                if (Settings.Messages.CATCH_SUCCESS.length() > 0) {
                    Common.tell((CommandSender)player, ProcessPlaceholderMessages.ReplacePlaceholders(Settings.Messages.CATCH_SUCCESS, targetEntity));
                }
            } else {
                EggEmAllPlugin.getInstance().getServer().getPluginManager().callEvent((Event)entityEscapeEvent);
                if (Settings.Messages.CATCH_FAILED_CHANCE.length() > 0) {
                    Common.tell((CommandSender)player, ProcessPlaceholderMessages.ReplacePlaceholders(Settings.Messages.CATCH_FAILED_CHANCE, targetEntity));
                }
                return;
            }
        }
        if (Settings.Particles.EXPLOSION_ON_SUCCESS.booleanValue()) {
            CompParticle.EXPLOSION_LARGE.spawn(targetEntity.getLocation());
        }
        // Clone mode: the egg is a copy and the mob is left standing where it is, so the
        // original must keep its inventory rather than have it dropped on the floor
        boolean keepOriginal = Settings.Restrictions.KEEP_ORIGINAL_ON_CATCH.booleanValue();
        ItemStack eggStack = EggEmAllPlugin.catchableMobs.getSpawnEgg(targetEntity);
        if (!keepOriginal && !Settings.EntityInventories.ERASE_ENTITY_INVENTORY.booleanValue() && targetEntity instanceof InventoryHolder) {
            ItemStack[] items;
            for (ItemStack itemStack : items = ((InventoryHolder)targetEntity).getInventory().getContents()) {
                if (itemStack == null) continue;
                targetEntity.getWorld().dropItemNaturally(targetEntity.getLocation(), itemStack);
            }
        }
        String entitySnapshot = targetEntity.createSnapshot().getAsString();
        ItemMeta meta = eggStack.getItemMeta();
        if (meta != null) {
            ProjectileSource projectileSource2 = egg.getShooter();
            if (projectileSource2 instanceof Player) {
                Player player = (Player)projectileSource2;
                if (Settings.CatchChance.ADD_LORE_TO_EGG.booleanValue()) {
                    List<String> newLore = this.replacePlaceholders(Settings.CatchChance.LORE_LINES, targetEntity, player);
                    meta.setLore(newLore);
                }
            }
            if (Settings.NBT.MAINTAIN_ENTITY_DATA.booleanValue()) {
                meta.getPersistentDataContainer().set(EGGEMALL_ENTITY_DATA, PersistentDataType.STRING, entitySnapshot);
            }
            eggStack.setItemMeta(meta);
        }
        if (!keepOriginal && ((stackerPluginAPI = EggEmAllPlugin.getInstance().getStackingPlugin()) == null || !stackerPluginAPI.isStackedEntity(targetEntity) || !stackerPluginAPI.unstackEntity(targetEntity))) {
            targetEntity.remove();
        }
        targetEntity.getWorld().dropItem(targetEntity.getLocation(), eggStack);
        if (!EggEmAllPlugin.thrownEggs.contains(egg)) {
            EggEmAllPlugin.thrownEggs.add(egg);
        }
    }

    private List<String> replacePlaceholders(List<String> loreLines, Entity entity, Player player) {
        ArrayList<String> newLore = new ArrayList<String>();
        for (String line : loreLines) {
            Villager villager;
            line = line.replace("{entity_name}", entity.getName());
            line = line.replace("{entity}", ItemUtil.bountifyCapitalized(entity.getType().toString()));
            line = line.replace("{player}", player.getName());
            line = entity instanceof Villager ? ((villager = (Villager)entity).getProfession() != Villager.Profession.NONE ? line.replace("{profession}", ItemUtil.bountifyCapitalized(villager.getProfession().toString())) : line.replace("{profession}", "")) : line.replace("{profession}", "");
            newLore.add(Common.colorize(line));
        }
        return newLore;
    }

    @EventHandler
    public void onEntityEscapeCapture(EntityEscapeCaptureEvent event) {
        if (Settings.CatchChance.REMOVE_ENTITY_ON_FAIL_CHANCE.booleanValue()) {
            event.getEntity().remove();
            if (Settings.Particles.SMOKE_ON_ESCAPE.booleanValue()) {
                CompParticle.SMOKE_LARGE.spawn(event.getEntity().getLocation());
            }
        }
    }

    @EventHandler
    public void itemuse(PlayerInteractEvent e) {
        ItemStack item;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getItem() != null && CompMaterial.isMonsterEgg((item = e.getItem()).getType()) && Settings.NBT.MAINTAIN_ENTITY_DATA.booleanValue() && item.getPersistentDataContainer().has(EGGEMALL_ENTITY_DATA, PersistentDataType.STRING)) {
            String snapshotString = (String)item.getPersistentDataContainer().get(EGGEMALL_ENTITY_DATA, PersistentDataType.STRING);
            EntitySnapshot snapshot = Bukkit.getEntityFactory().createEntitySnapshot(snapshotString);
            Location loc = e.getClickedBlock().getLocation().clone().add(0.5, 1.0, 0.5);
            while (!CompMaterial.isAir(loc.getBlock()) && !CompMaterial.isAir(loc.getBlock().getRelative(BlockFace.UP))) {
                loc = loc.add(0.0, 1.0, 0.0);
            }
            snapshot.createEntity(loc);
            PlayerUtil.takeOnePiece(e.getPlayer(), item);
            e.setCancelled(true);
        }
    }
}

