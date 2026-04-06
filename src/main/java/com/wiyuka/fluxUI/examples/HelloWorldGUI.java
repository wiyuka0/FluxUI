package com.wiyuka.fluxUI.examples;

import com.wiyuka.fluxUI.FluxUI;
import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class HelloWorldGUI extends BaseEffect implements Listener {

    private final Location centerLoc;
    private final Vector3f xAxis, yAxis, zAxis;

    // 移除了 private final FluxLayout layout;

    private boolean show_demo_window = true;
    private boolean show_another_window = false;
    private float f = 1.0f;
    private Color clear_color = Color.fromARGB(255, 114, 144, 154); // 恢复经典蓝灰色
    private int counter = 0;

    private long lastFrameTime = System.currentTimeMillis();
    private float fps = 60.0f;

    public HelloWorldGUI(Player player) {
        super(player);

        Location eye = player.getEyeLocation();
        Vector dir = eye.getDirection();
        dir.setY(0).normalize();

        this.centerLoc = eye.clone().add(dir.clone().multiply(3.5));
        this.zAxis = new Vector3f((float) -dir.getX(), 0, (float) -dir.getZ()).normalize();
        this.yAxis = new Vector3f(0, 1, 0);
        this.xAxis = new Vector3f(zAxis).cross(yAxis).normalize();

        // 不再需要实例化 layout，Flux 已经内部封装好了
        Bukkit.getPluginManager().registerEvents(this, FluxUI.getInstance());
    }

    @EventHandler
    public void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer().equals(this.player)) {
            event.setCancelled(true);
            this.onClick();
        }
    }

    @Override
    protected void render(Flux flux) {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;
        if (deltaTime > 0) fps = fps * 0.9f + (1.0f / deltaTime) * 0.1f;
        flux.screen(centerLoc, xAxis, yAxis, zAxis, "imgui_hello_world_screen");
        flux.pushMatrix();
        flux.translate(-2.0f, 1.5f, 0f);
        flux.beginWindow("Hello, world!", 0, 0);
        flux.text("txt_desc", "This is some useful text.");
        show_demo_window = flux.checkbox("chk_demo", "Demo Window", show_demo_window);
        show_another_window = flux.checkbox("chk_another", "Another Window", show_another_window);
        f = flux.sliderFloat("sld_f", "float", f, 0.0f, 1.0f);
        flux.colorEdit3("col_clear", "clear color", clear_color);
        if (flux.button("btn_counter", "Button")) counter++;
        flux.sameLine();
        flux.text("txt_cnt", "counter = " + counter);
        String fpsText = String.format("Application average %.1f ms/Frame", 1000.0f / fps);
        flux.text("txt_fps", fpsText);
        flux.endWindow();
        flux.popMatrix();
        flux.endScreen();
    }
}