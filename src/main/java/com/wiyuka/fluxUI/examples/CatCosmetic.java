package com.wiyuka.fluxUI.examples;

import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class CatCosmetic extends BaseEffect {

    // ==========================================
    // 外观颜色库
    // ==========================================
    private final Flux.FluxColor FUR_COLOR    = Flux.FluxColor.fromARGB(255, 35, 35, 40);    // 黑猫毛色
    private final Flux.FluxColor INNER_COLOR  = Flux.FluxColor.fromARGB(255, 255, 140, 170); // 粉色内耳
    private final Flux.FluxColor TIP_COLOR    = Flux.FluxColor.fromARGB(255, 240, 240, 240); // 尾巴尖白毛
    private final Flux.FluxColor CHOKER_COLOR = Flux.FluxColor.fromARGB(255, 20, 20, 20);    // 项圈黑色
    private final Flux.FluxColor BELL_COLOR   = Flux.FluxColor.fromARGB(255, 255, 215, 0);   // 铃铛金色

    public CatCosmetic(Player player) {
        super(player);
    }

    @Override
    protected void render(Flux flux) {
        long t = ticks;
        Location loc = player.getLocation();

        // 构建标准的右手坐标系 (画布永远垂直于地面，跟随玩家身体旋转)
        Vector forward = loc.getDirection().setY(0).normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = new Vector(0, 1, 0);
        Vector backward = forward.clone().multiply(-1.0);

        Vector3f xAxis = new Vector3f((float) right.getX(), (float) right.getY(), (float) right.getZ());
        Vector3f yAxis = new Vector3f((float) up.getX(), (float) up.getY(), (float) up.getZ());
        Vector3f zAxis = new Vector3f((float) backward.getX(), (float) backward.getY(), (float) backward.getZ());

        flux.screen(FluxUtil.locationToFlux(loc), toVector3d(xAxis), toVector3d(yAxis), toVector3d(zAxis), "cat_cosmetic_" + player.getName());

        double velocity = player.getVelocity().setY(0).length();

        // 1. 渲染动态猫耳 (头部)
        drawCatEars(t, loc.getPitch(), flux);

        // 2. 渲染物理摇摆猫尾 (腰部后方)
        drawCatTail(t, velocity, flux);

        // 3. 渲染项圈与动态铃铛 (脖子前方)
        drawCollarAndBell(t, velocity, flux);

        // 4. 渲染二次元情绪反馈 (头顶/脸侧)
        drawAnimeExpressions(t, flux);

        flux.endScreen();
    }

    // ==========================================
    // 模块 1：动态猫耳
    // ==========================================
    private void drawCatEars(long t, float headPitch, Flux flux) {
        flux.pushMatrix();
        flux.translate(0, 1.6f, -0.05f); // 眼睛高度，稍微靠前
        flux.rotateX(-headPitch);        // 跟随低头/抬头
        flux.translate(0, 0.3f, 0);      // 爬到头顶

        boolean twitching = (t % 120 < 10); // 偶尔抖耳
        float twitchAngle = twitching ? (float)Math.sin(t * 1.5) * 15f : 0f;

        // 左耳
        flux.pushMatrix();
        flux.translate(-0.18f, 0, 0);
        flux.rotateZ(-15f + twitchAngle);
        drawEar("left_ear", flux);
        flux.popMatrix();

        // 右耳
        flux.pushMatrix();
        flux.translate(0.18f, 0, 0);
        flux.rotateZ(15f - twitchAngle);
        drawEar("right_ear", flux);
        flux.popMatrix();

        flux.popMatrix();
    }

    private void drawEar(String id, Flux flux) {
        Vector3f p1 = new Vector3f(0, 0.2f, 0);
        Vector3f p2 = new Vector3f(-0.08f, 0, 0);
        Vector3f p3 = new Vector3f(0.08f, 0, 0);

        Vector3f in1 = new Vector3f(0, 0.15f, -0.01f);
        Vector3f in2 = new Vector3f(-0.05f, 0.02f, -0.01f);
        Vector3f in3 = new Vector3f(0.05f, 0.02f, -0.01f);

        drawDoubleSidedTriangle(id + "_fur", p1, p2, p3, FUR_COLOR, flux);
        drawDoubleSidedTriangle(id + "_inner", in1, in2, in3, INNER_COLOR, flux);
    }

    // ==========================================
    // 模块 2：物理摇摆猫尾
    // ==========================================
    private void drawCatTail(long t, double velocity, Flux flux) {
        flux.pushMatrix();
        flux.translate(0, 0.8f, 0.15f); // 腰部，靠后 (+Z)

        float liftAngle = (float) Math.min(velocity * 150f, 60f);
        flux.rotateX(-30f - liftAngle); // 基础下垂 + 跑动翘起

        float swishSpeed = velocity > 0.05 ? 0.8f : 0.1f;
        float swishAmount = velocity > 0.05 ? 20f : 10f;

        int segments = 8;
        float segLength = 0.1f;

        for (int i = 0; i < segments; i++) {
            float curveX = (float)Math.sin(t * 0.05 - i * 0.5) * 5f;
            float curveZ = (float)Math.sin(t * swishSpeed - i * 0.4) * swishAmount;

            flux.rotateX(curveX);
            flux.rotateZ(curveZ);
            flux.translate(0, -segLength / 2, 0);

            float width = 0.06f - (i * 0.005f);
            Color color = (i >= segments - 2) ? TIP_COLOR : FUR_COLOR;

            flux.pushMatrix();
            flux.rect("tail_" + i + "_a", width, segLength, Flux.FluxColor.fromARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue()));
            flux.rotateY(90f);
            flux.rect("tail_" + i + "_b", width, segLength, Flux.FluxColor.fromARGB(color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue()));
            flux.popMatrix();

            flux.translate(0, -segLength / 2, 0);
        }
        flux.popMatrix();
    }

    // ==========================================
    // 模块 3：项圈与动态铃铛
    // ==========================================
    private void drawCollarAndBell(long t, double velocity, Flux flux) {
        flux.pushMatrix();
        // 移到脖子前方 (Y=1.4, Z=-0.15 靠前)
        flux.translate(0, 1.4f, -0.15f);

        // 1. 画一个黑色的细项圈
        flux.pushMatrix();
        flux.rect("choker", 0.15f, 0.02f, CHOKER_COLOR);
        flux.popMatrix();

        // 2. 画金铃铛
        flux.pushMatrix();
        flux.translate(0, -0.03f, -0.01f); // 挂在项圈下面一点点

        // 铃铛的物理晃动 (跑动时剧烈晃动，站立时微微呼吸)
        float bellJingle = velocity > 0.05 ? (float)Math.sin(t * 1.5) * 30f : (float)Math.sin(t * 0.1) * 5f;
        flux.rotateZ(bellJingle);

        // 用十字交叉的矩形模拟立体的铃铛
        float bellSize = 0.04f;
        flux.rect("bell_a", bellSize, bellSize, BELL_COLOR);
        flux.rotateY(90f);
        flux.rect("bell_b", bellSize, bellSize, BELL_COLOR);

        flux.popMatrix();
        flux.popMatrix();
    }

    // ==========================================
    // 模块 4：二次元情绪反馈 (Anime Expressions)
    // ==========================================
    private void drawAnimeExpressions(long t, Flux flux) {
        flux.pushMatrix();

        // 移到头部中心
        flux.translate(0, 1.8f, 0);

        // 情绪 1：潜行卖萌 (飘粉色爱心)
        if (player.isSneaking()) {
            // 利用 t 取模做一个循环上升的动画 (周期 40 ticks)
            float progress = (t % 40) / 40.0f;
            float heartY = progress * 0.6f; // 向上飘 0.6 格
            int opacity = (int)(255 * (1.0f - progress)); // 越往上越透明

            flux.pushMatrix();
            // 爱心稍微偏右一点飘起，并且带有左右摇摆的微调
            float sway = (float)Math.sin(progress * Math.PI * 2) * 0.1f;
            flux.translate(0.2f + sway, heartY, 0);

            // 绘制爱心文本
            flux.text("emote_heart", "❤", 0.15f, opacity);
            flux.popMatrix();
        }

        // 情绪 2：残血流汗 (巨大蓝色汗滴)
        if (player.getHealth() <= 10.0) {
            flux.pushMatrix();
            // 定位到额头左侧
            flux.translate(-0.25f, 0.1f, -0.1f);

            // 汗滴的上下微抖动画
            float sweatBounce = (float)Math.sin(t * 0.3) * 0.02f;
            flux.translate(0, sweatBounce, 0);

            // 绘制汗滴文本
            flux.text("emote_sweat", "💧", 0.15f, 200);
            flux.popMatrix();
        }

        flux.popMatrix();
    }

    // ==========================================
    // 辅助工具
    // ==========================================
    private void drawDoubleSidedTriangle(String id, Vector3f p1, Vector3f p2, Vector3f p3, Color color, Flux flux) {
        flux.triangle(id + "_front", p1, p2, p3, color);
        flux.triangle(id + "_back", p1, p3, p2, color);
    }
}