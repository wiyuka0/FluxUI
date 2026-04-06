package com.wiyuka.fluxUI.examples;

import com.wiyuka.fluxUI.renderer.Flux;
import com.wiyuka.fluxUI.RenderUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

public class FluxLogoEffect extends BaseEffect {

    private final Location centerLoc;
    private final Vector3f xAxis = new Vector3f(1, 0, 0);
    private final Vector3f yAxis = new Vector3f(0, 1, 0);
    private final Vector3f zAxis = new Vector3f(0, 0, 1);

    public FluxLogoEffect(Player player) {
        super(player);
        this.centerLoc = player.getLocation().clone().add(player.getLocation().getDirection().multiply(6.0));
        this.centerLoc.add(0, 1.5, 0);
    }

    @Override
    protected void render(Flux flux) {
        long t = ticks;

        Color cyanCore = Color.fromARGB(255, 0, 255, 255);       // 青色核心
        Color cyanGlow = Color.fromARGB(120, 0, 200, 255);       // 青色光晕
        Color magentaCore = Color.fromARGB(255, 255, 0, 255);    // 品红核心
        Color magentaGlow = Color.fromARGB(120, 200, 0, 255);    // 品红光晕
        Color pureWhite = Color.fromARGB(255, 255, 255, 255);    // 纯白

        float floatY = (float) Math.sin(t * 0.05) * 0.4f;

        flux.screen(FluxUtil.locationToFlux(centerLoc), toVector3d(xAxis), toVector3d(yAxis), toVector3d(zAxis), "flux_logo_main");
        flux.translate(0, floatY, 0);

        float ringRadius = 4.5f;

        flux.pushMatrix();
        flux.rotateX(t * 2.5f);
        RenderUtils.drawFlatRing("gyro_x", ringRadius, ringRadius + 0.1f, 36, cyanGlow, flux);
        flux.popMatrix();

        flux.pushMatrix();
        flux.rotateY(t * 2.0f);
        RenderUtils.drawFlatRing("gyro_y", ringRadius + 0.2f, ringRadius + 0.3f, 36, magentaGlow, flux);
        flux.popMatrix();

        flux.pushMatrix();
        flux.rotateZ(t * 3.0f);
        RenderUtils.drawFlatRing("gyro_z", ringRadius + 0.4f, ringRadius + 0.5f, 12, pureWhite, flux);
        flux.popMatrix();

        flux.pushMatrix();
        flux.scale(5.0f, 5.0f, 5.0f);

        float textPulse = 1.0f + (float) Math.sin(t * 0.1) * 0.02f;
        flux.scale(textPulse, textPulse, textPulse);

        flux.pushMatrix();
        flux.translate(0.06f, -0.04f, -0.1f);
        flux.text("logo_shadow_magenta", "§d§lFLUX", 1.0f);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(-0.06f, 0.04f, -0.05f);
        flux.text("logo_shadow_cyan", "§b§lFLUX", 1.0f);
        flux.popMatrix();

        // 顶层：纯白主字
        flux.translate(0, 0, 0.05f);
        flux.text("logo_main_text", "§f§lFLUX", 1.0f);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(0, -1.8f, 0.2f);
        flux.scale(1.2f, 1.2f, 1.2f);
        // 闪烁效果
        if (t % 40 > 5) {
            flux.text("logo_subtitle", "§7I M G U I   E N G I N E", 1.0f);
        }
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(0, -3.5f, 0);
        flux.rotateX(90f);

        flux.pushMatrix();
        flux.rotateZ(t * 1.5f);
        RenderUtils.drawFlatRing("base_hex", 3.5f, 3.8f, 6, cyanCore, flux);
        flux.popMatrix();

        // 内层精密圆环 (逆时针旋转)
        flux.pushMatrix();
        flux.rotateZ(-t * 2.5f);
        RenderUtils.drawFlatRing("base_circle", 2.0f, 2.2f, 24, magentaCore, flux);
        // 底座中心点
        RenderUtils.drawFlatRing("base_center", 0.0f, 0.5f, 8, pureWhite, flux);
        flux.popMatrix();

        flux.popMatrix();

        int cubeCount = 4;
        for (int i = 0; i < cubeCount; i++) {
            flux.pushMatrix();

            float angle = t * 2.0f + (i * (360f / cubeCount));
            float rad = (float) Math.toRadians(angle);
            float orbitRadius = 5.5f;

            float px = (float) Math.cos(rad) * orbitRadius;
            float pz = (float) Math.sin(rad) * orbitRadius;
            float py = (float) Math.sin(t * 0.08f + i) * 2.5f;

            flux.translate(px, py, pz);

            flux.rotateX(t * 6f);
            flux.rotateY(t * 6f);

            Color cubeColor = (i % 2 == 0) ? cyanCore : magentaCore;
            RenderUtils.drawBox("data_cube_" + i, 0.4f, 0.4f, 0.4f, cubeColor, flux);

            flux.popMatrix();
        }

        flux.endScreen();
    }
}