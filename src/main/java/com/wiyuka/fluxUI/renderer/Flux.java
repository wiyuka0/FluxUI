package com.wiyuka.fluxUI.renderer;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;

import java.util.Set;
import java.util.function.Consumer;

public class Flux {
    public final FluxRenderer renderer;
    public final FluxLayout layout;
    public final FluxControllers controllers;

    public Flux(Player player, Consumer<Flux> renderLogic) {
        this.renderer = new FluxRenderer(r -> {
            if (renderLogic != null) renderLogic.accept(this);
        });
        this.layout = new FluxLayout(this.renderer);
        this.controllers = new FluxControllers(this.renderer, this.layout, player);
    }

    public void setTargetPlayer(Player player) {
        this.controllers.setPlayer(player);
    }

    // ==========================================
    // 生命周期与输入系统
    // ==========================================
    public void tick() { renderer.tick(); }
    public void destroy() { renderer.destroy(); }
    public void updatePlayerRay(Player player) { renderer.updatePlayerRay(player); }
    public void registerPlayerClick(Player player) { renderer.registerPlayerClick(player); }
    public void removePlayer(Player player) { renderer.removePlayer(player); }

    // ==========================================
    // 屏幕与矩阵操作
    // ==========================================
    public boolean screen(Location loc, Vector3f xAxis, Vector3f yAxis, Vector3f zAxis, String screenId) { return renderer.screen(loc, xAxis, yAxis, zAxis, screenId); }
    public void endScreen() { renderer.endScreen(); }
    public boolean area(String id) { return renderer.area(id); }
    public void endArea() { renderer.endArea(); }
    public void pushMatrix() { renderer.pushMatrix(); }
    public void popMatrix() { renderer.popMatrix(); }
    public void translate(float x, float y, float z) { renderer.translate(x, y, z); }
    public void scale(float x, float y, float z) { renderer.scale(x, y, z); }
    public void rotateX(float angle) { renderer.rotateX(angle); }
    public void rotateY(float angle) { renderer.rotateY(angle); }
    public void rotateZ(float angle) { renderer.rotateZ(angle); }
    public void skew(float angleX, float angleY) { renderer.skew(angleX, angleY); }
    public void interpolation(int ticks) { renderer.interpolation(ticks); }

    // ==========================================
    // 窗口与排版系统
    // ==========================================
    public void beginWindow(String title, float startX, float startY) { layout.beginWindow(title, startX, startY); }
    public void endWindow() { layout.endWindow(); }
    public void sameLine() { layout.sameLine(); }

    // ==========================================
    // 交互控件
    // ==========================================
    public void     text        (String id, String text) { controllers.text(id, text); }
    public boolean  button      (String id, String text) { return controllers.button(id, text); }
    public boolean  checkbox    (String id, String label, boolean state) { return controllers.checkbox(id, label, state); }
    public void     colorEdit3  (String id, String label, Color color)   { controllers.colorEdit3(id, label, color); }
    public float    sliderFloat (String id, String label, float value, float min, float max) { return controllers.sliderFloat(id, label, value, min, max); }

    // ==========================================
    // 基础图形与文本
    // ==========================================
    public void text            (String id, String text,    float scale)                    { renderer.text(id, text, scale); }
    public void text            (String id, String text,    float scale,    int opacity)    { renderer.text(id, text, scale, opacity); }
    public boolean buttonAbs    (String id, String text,    float x,        float y)        { return controllers.buttonAbs(id, text, x, y);}
    public void rect            (String id, float width,    float height,   Color color)    { renderer.rect(id, width, height, color); }

    public boolean checkboxAbs  (String id, String label,   boolean state,  float x,        float y)                            { return controllers.checkboxAbs(id, label, state, x, y); }
    public void triangle        (String id, Vector3f p1,    Vector3f p2,    Vector3f p3,    Color color)                        { renderer.triangle(id, p1, p2, p3, color); }
    public void text            (String id, String text,    float scale,    int opacity,    TextDisplay.TextAlignment align)    { renderer.text(id, text, scale, opacity, align); }
    public void drawAbsRect     (String id, float x,        float y,        float w,        float h,            Color color)    { renderer.drawAbsRect(id, x, y, w, h, color); }
    public void textAbs         (String id, String text,    float x,        float y,        float scale,        int opacity,    TextDisplay.TextAlignment align) { renderer.textAbs(id, text, x, y, scale, opacity, align); }
    public void drawAbsTriangle (String id, float x1,       float y1,       float x2,       float y2,           float x3,       float y3, Color color) { renderer.drawAbsTriangle(id, x1, y1, x2, y2, x3, y3, color); }
    public float sliderFloatAbs (String id, String label,   float value,    float min,      float max,          float x,        float y) { return controllers.sliderFloatAbs(id, label, value, min, max, x, y); }
    // ==========================================
    // 底层碰撞检测
    // ==========================================
    public Set<Player>  getHoveringPlayers  (float width,   float height) { return renderer.getHoveringPlayers(width, height); }
    public Set<Player>  hitbox              (float width,   float height) { return renderer.hitbox(width, height); }
    public boolean      isHovered           (float width,   float height) { return renderer.isHovered(width, height); }
    public boolean      isHovered           (Player player, float width, float height) { return renderer.isHovered(player, width, height); }
}