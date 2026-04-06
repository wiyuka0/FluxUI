package com.wiyuka.fluxUI;

import com.wiyuka.fluxUI.examples.*;
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
        EffectManager effectManager = new EffectManager(this);
        getCommand("flux").setExecutor(new FluxCommand(effectManager));
        effectManager.registerEffect("simple_window", SimpleWindowGUI::new);
        effectManager.registerEffect("flux", FluxLogoEffect::new);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
