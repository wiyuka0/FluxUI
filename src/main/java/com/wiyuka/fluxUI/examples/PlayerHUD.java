package com.wiyuka.fluxUI.examples;

import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.entity.Player;
import org.joml.Vector3d;

public class PlayerHUD extends BaseEffect {

    private boolean showDetails = false;
    private float uiScale = 0.6f;

    public PlayerHUD(Player player) {
        super(player);
    }

    @Override
    protected void render(Flux ui) {
        Flux.FluxLocation attachedLoc = new Flux.FluxLocation(
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ(),
                player.getUniqueId() // 绑定实体骑乘位，Flux会将该实体骑乘到目标玩家上
        );

        Vector3d xAxis = new Vector3d(1, 0, 0);
        Vector3d yAxis = new Vector3d(0, 1, 0);
        Vector3d zAxis = new Vector3d(0, 0, 1);

        if (ui.screen(attachedLoc, xAxis, yAxis, zAxis, "player_hud_screen")) {

            ui.pushMatrix();

            ui.translate(0f, -0.1f, -2.5f);

            ui.zStep(0.0001f);

            ui.beginWindow("Player HUD", -1.5f, 1.0f, Flux.FluxBillboard.CENTER);

            // 文本使用 CENTER Billboard
            ui.text("§l玩家 " + player.getName(), Flux.FluxBillboard.CENTER);
            ui.text("当前生命: §c" + String.format("%.1f", player.getHealth()), Flux.FluxBillboard.CENTER);


            ui.endWindow();

            // 4. (可选) 绝对坐标画一个准星，同样传入 CENTER
            // ui.drawAbsRect("crosshair_h", -0.05f, 0.005f, 0.1f, 0.01f, Flux.FluxColor.fromARGB(200, 0, 255, 0), Flux.FluxBillboard.CENTER);
            // ui.drawAbsRect("crosshair_v", -0.005f, 0.05f, 0.01f, 0.1f, Flux.FluxColor.fromARGB(200, 0, 255, 0), Flux.FluxBillboard.CENTER);

            ui.popMatrix();
            ui.endScreen();
        }
    }
}