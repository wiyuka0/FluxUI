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
        effectManager.registerEffect("seraphim", SeraphimEffect::new);
        effectManager.registerEffect("lotus", LotusEffect::new);
        effectManager.registerEffect("corridor", CorridorEffect::new);
        effectManager.registerEffect("cannon", CannonEffect::new);
        effectManager.registerEffect("astral_cataclysm", AstralCataclysmEffect::new);
        effectManager.registerEffect("resupply", ResupplyEffect::new);
        effectManager.registerEffect("test", TestFluxTriangleEffect::new);
        effectManager.registerEffect("stellar_engine", StellarEngineEffect::new);
        effectManager.registerEffect("blender", BlenderUIEffect::new);
        effectManager.registerEffect("chronos_domain", ChronosDomainEffect::new);
        effectManager.registerEffect("divine_ophanim", DivineOphanimEffect::new);
        effectManager.registerEffect("nuke", TacticalNukeEffect::new);
        effectManager.registerEffect("fire", FireEffect::new);
        effectManager.registerEffect("retro", LowPolyRetroEffect::new);
        effectManager.registerEffect("hud", SmartPistolHUD::new);
        effectManager.registerEffect("crysis", CrysisHUD::new);
        effectManager.registerEffect("cat", CatCosmetic::new);
        effectManager.registerEffect("td", HapticFeedbackModule::new);
        effectManager.registerEffect("gag", GagModule::new);
        effectManager.registerEffect("paint", PaintGUI::new);
        effectManager.registerEffect("test_layout", HelloWorldGUI::new);
        effectManager.registerEffect("simple_window", SimpleWindowGUI::new);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
