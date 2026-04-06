package com.wiyuka.fluxUI;

import com.wiyuka.fluxUI.examples.*;
import com.wiyuka.fluxUI.renderer.BukkitUIPool;
import com.wiyuka.fluxUI.renderer.UIPool;
import org.bukkit.plugin.java.JavaPlugin;

public final class FluxUI extends JavaPlugin {
    private static FluxUI instance;

    public static FluxUI getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        // Plugin startup logic
        UIPool.setFactory(BukkitUIPool::new);
        EffectManager effectManager = new EffectManager(this);
        getCommand("flux").setExecutor(new FluxCommand(effectManager));
        effectManager.registerEffect("test_layout", HelloWorldGUI::new);
        effectManager.registerEffect("simple_window", SimpleWindowGUI::new);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
