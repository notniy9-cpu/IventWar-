package org.Main.iventWar;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class NuclearBombAnimation {
    private final IventWar plugin;
    private final Location startLocation;
    private final Location targetLocation;
    private final ArmorStand bombStand;
    private final int customModelData;
    private final int totalTicks;
    private int ticksPassed = 0;
    private final double startY;
    private final double endY;
    private boolean isRunning = false;
    private BukkitRunnable animationTask;
    private Runnable onComplete;
    private final float explosionPower;

    public NuclearBombAnimation(IventWar plugin, Location center, int customModelData, float explosionPower) {
        this.plugin = plugin;
        this.customModelData = customModelData;
        this.explosionPower = explosionPower;
        this.startLocation = center.clone().add(0, 30, 0);
        this.targetLocation = center.clone();
        this.targetLocation.setY(center.getWorld().getHighestBlockYAt(center) + 1);
        this.startY = startLocation.getY();
        this.endY = targetLocation.getY();
        this.totalTicks = 60;

        World world = startLocation.getWorld();
        this.bombStand = world.spawn(startLocation, ArmorStand.class);
        bombStand.setVisible(false);
        bombStand.setGravity(false);
        bombStand.setMarker(true);
        bombStand.setBasePlate(false);
        bombStand.setArms(false);
        bombStand.setHelmet(createBombItem());
        bombStand.setHeadPose(new EulerAngle(0, 0, 0));
    }

    private ItemStack createBombItem() {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    public void startAnimation(Runnable onComplete) {
        if (isRunning) return;
        this.isRunning = true;
        this.onComplete = onComplete;

        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning || bombStand == null || bombStand.isDead()) {
                    cancel();
                    finish();
                    return;
                }
                ticksPassed++;
                double progress = (double) ticksPassed / totalTicks;
                if (progress > 1.0) progress = 1.0;

                double y = startY + (endY - startY) * progress * progress;
                bombStand.teleport(new Location(bombStand.getWorld(), startLocation.getX(), y, startLocation.getZ()));

                double angle = progress * 2 * Math.PI * 3;
                bombStand.setHeadPose(new EulerAngle(0, angle, 0));

                bombStand.getWorld().spawnParticle(Particle.FLAME, bombStand.getLocation().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.01);
                bombStand.getWorld().spawnParticle(Particle.SMOKE_LARGE, bombStand.getLocation().add(0, 0.5, 0), 3, 0.3, 0.3, 0.3, 0.01);

                if (ticksPassed % 5 == 0) {
                    float pitch = 0.5f + (float) progress * 0.5f;
                    bombStand.getWorld().playSound(bombStand.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.3f, pitch);
                }

                if (ticksPassed >= totalTicks) {
                    // Бомба достигла земли – взрыв!
                    explode();
                    cancel();
                    finish();
                }
            }
        };
        animationTask.runTaskTimer(plugin, 0L, 1L);
    }

    private void explode() {
        Location loc = bombStand.getLocation();
        World world = loc.getWorld();

        bombStand.remove();

        // Главный взрыв
        world.createExplosion(loc, explosionPower);
        world.spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
        world.spawnParticle(Particle.CLOUD, loc, 100, 0.5, 0.5, 0.5, 0.1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);

        // Вторичные взрывы (для усиления эффекта)
        int secondaryCount = Math.min((int) (explosionPower / 2), 20);
        for (int i = 0; i < secondaryCount; i++) {
            double angle = Math.random() * 2 * Math.PI;
            double dist = 5 + Math.random() * 15;
            double x = loc.getX() + dist * Math.cos(angle);
            double z = loc.getZ() + dist * Math.sin(angle);
            Location secLoc = new Location(world, x, loc.getY(), z);
            world.createExplosion(secLoc, explosionPower * 0.3f);
            world.spawnParticle(Particle.EXPLOSION_NORMAL, secLoc, 20);
        }
    }

    private void finish() {
        isRunning = false;
        if (onComplete != null) {
            onComplete.run();
        }
    }

    public void cancel() {
        if (isRunning) {
            isRunning = false;
            if (animationTask != null && !animationTask.isCancelled()) {
                animationTask.cancel();
            }
            if (bombStand != null && !bombStand.isDead()) {
                bombStand.remove();
            }
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}