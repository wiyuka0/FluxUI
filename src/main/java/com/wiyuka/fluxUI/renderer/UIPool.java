package com.wiyuka.fluxUI.renderer;

import org.joml.Matrix4d;
import org.joml.Vector3d;

public class UIPool {

    // 定义底层实现的接口规范
    public interface Impl {
        void beginFrame();
        void endFrame();
        void destroy();
        void drawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks);
        void drawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment);
        void drawRect(String id, Matrix4d worldTransform, Flux.FluxColor color, int interpTicks);
        void drawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor color, int interpTicks);
    }

    public interface Factory {
        Impl create(Flux.FluxLocation location);
    }

    private static Factory factory;

    /**
     * 在插件启动时 (onEnable) 调用此方法注入 Bukkit 实现
     */
    public static void setFactory(Factory f) {
        factory = f;
    }

    // ==========================================
    // 代理逻辑
    // ==========================================

    private final Impl impl;

    public UIPool(Flux.FluxLocation location) {
        if (factory == null) {
            throw new IllegalStateException("UIPool factory is not set! Please call UIPool.setFactory() before using Flux.");
        }
        this.impl = factory.create(location);
    }

    public void beginFrame() { impl.beginFrame(); }
    public void endFrame() { impl.endFrame(); }
    public void destroy() { impl.destroy(); }

    public void drawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks) {
        impl.drawText(id, text, worldTransform, opacity, interpTicks);
    }

    public void drawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment) {
        impl.drawText(id, text, worldTransform, opacity, interpTicks, alignment);
    }

    public void drawRect(String id, Matrix4d worldTransform, Flux.FluxColor color, int interpTicks) {
        impl.drawRect(id, worldTransform, color, interpTicks);
    }

    public void drawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor color, int interpTicks) {
        impl.drawTriangle(id, point1, point2, point3, worldBaseMatrix, color, interpTicks);
    }
}