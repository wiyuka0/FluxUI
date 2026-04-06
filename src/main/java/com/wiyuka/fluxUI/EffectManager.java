package com.wiyuka.fluxUI;

import com.wiyuka.fluxUI.examples.BaseEffect;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class EffectManager implements Listener {

    private final Map<UUID, BaseEffect> activeEffects = new HashMap<>();
    private final Map<String, Function<Player, BaseEffect>> effectRegistry = new HashMap<>();

    public EffectManager(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                activeEffects.entrySet().removeIf(entry -> {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p == null || !p.isOnline()) {
                        entry.getValue().stop();
                        return true;
                    }
                    entry.getValue().tick();
                    return false;
                });
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void registerEffect(String name, Function<Player, BaseEffect> constructor) {
        effectRegistry.put(name.toLowerCase(), constructor);
    }

    public boolean playEffect(Player player, String name, String[] args) {
        stopEffect(player);
        Function<Player, BaseEffect> constructor = effectRegistry.get(name.toLowerCase());
        if (constructor == null) return false;

        BaseEffect effect = constructor.apply(player);
        effect.setArgs(args);
        activeEffects.put(player.getUniqueId(), effect);
        return true;
    }

    public void stopEffect(Player player) {
        BaseEffect effect = activeEffects.remove(player.getUniqueId());
        if (effect != null) {
            effect.stop();
        }
    }

    public boolean hasEffect(Player player) {
        return activeEffects.containsKey(player.getUniqueId());
    }

    public Iterable<String> getAvailableEffects() {
        return effectRegistry.keySet();
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action.isLeftClick() || action.isRightClick()) {
            BaseEffect effect = activeEffects.get(event.getPlayer().getUniqueId());
            if (effect != null) {
                effect.onClick();
            }
        }
    }

    public void cleanupAll() {
        for (BaseEffect effect : activeEffects.values()) {
            effect.stop();
        }
        activeEffects.clear();
    }
}