package com.wiyuka.fluxUI.renderer;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.*;

import java.lang.Math;
import java.util.*;

public class BukkitUIPool implements Flux.PoolImpl {
    private final World world;
    private final Location anchorLoc;
    private final Flux.FluxLocation fluxLoc; // 新增：保存 fluxLoc 以便获取 attachedEntity
    private final Map<String, List<TextDisplay>> entityCache = new HashMap<>();
    private final Set<String> activeNodesThisFrame = new HashSet<>();

    // 新增：变换缓存，与 Fabric 保持一致
    private final Map<TextDisplay, Transformation> transformCache = new WeakHashMap<>();

    private static Matrix4d getShearMatrix(float xy, float xz, float yx, float yz, float zx, float zy) {
        return new Matrix4d(
                1f, xy, xz, 0f,
                yx, 1f, yz, 0f,
                zx, zy, 1f, 0f,
                0f, 0f, 0f, 1f
        );
    }

    private static final Matrix4d UNIT_SQUARE_TRI = new Matrix4d()
            .translate(-0.1f + 0.5f, -0.5f + 0.5f, 0f)
            .scale(8.0f, 3.63f, 1f);

    private static final Matrix4d UNIT_SQUARE_RECT = new Matrix4d()
            .translate(-0.1f + 0.5f, -0.5f + 0.5f, 0f)
            .scale(8.08f, 3.66f, 1f);

    private static final Matrix4d[] UNIT_TRIANGLES = new Matrix4d[3];

    static {
        UNIT_TRIANGLES[0] = new Matrix4d().scale(0.5f).mul(new Matrix4d(UNIT_SQUARE_TRI));

        float offset = 1;
        Matrix4d shearYX = getShearMatrix(0f, 0f, -offset, 0f, 0f, 0f);
        UNIT_TRIANGLES[1] = new Matrix4d().scale(0.5f).translate(1f, 0f, 0f).mul(shearYX).mul(new Matrix4d(UNIT_SQUARE_TRI));

        Matrix4d shearXY = getShearMatrix(-1f, 0f, 0f, 0f, 0f, 0f);
        UNIT_TRIANGLES[2] = new Matrix4d().scale(0.5f).translate(0f, 1f, 0f).mul(shearXY).mul(new Matrix4d(UNIT_SQUARE_TRI));
    }

    public BukkitUIPool(Flux.FluxLocation fluxLoc) {
        this.fluxLoc = fluxLoc; // 保存 fluxLoc
        this.world = Bukkit.getWorld(fluxLoc.world());
        if (this.world == null) {
            throw new IllegalArgumentException("World not found: " + fluxLoc.world());
        }
        this.anchorLoc = new Location(this.world, fluxLoc.x(), fluxLoc.y(), fluxLoc.z(), 0f, 0f);
    }

    @Override
    public void poolDestroy() {
        for (List<TextDisplay> displays : entityCache.values()) {
            displays.forEach(Display::remove);
        }
        entityCache.clear();
        activeNodesThisFrame.clear();
        transformCache.clear(); // 新增：清理缓存
    }

    @Override
    public void poolBeginFrame() {
        activeNodesThisFrame.clear();
    }

    @Override
    public void poolEndFrame() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, List<TextDisplay>> entry : entityCache.entrySet()) {
            if (!activeNodesThisFrame.contains(entry.getKey())) {
                entry.getValue().forEach(Display::remove);
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(entityCache::remove);
    }

    // 新增：带 Billboard 的方法签名
    @Override
    public void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxBillboard billboard) {
        drawTextInternal(id, text, worldTransform, opacity, interpTicks, TextDisplay.TextAlignment.CENTER, billboard);
    }

    // 新增：带 Billboard 的方法签名
    @Override
    public void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment, Flux.FluxBillboard billboard) {
        drawTextInternal(id, text, worldTransform, opacity, interpTicks, convertAlignment(alignment), billboard);
    }

    private void drawTextInternal(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, TextDisplay.TextAlignment alignment, Flux.FluxBillboard billboard) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);
        TextDisplay display = displays.get(0);

        // 字体逻辑：原生 Bukkit 没有直接的 setFont 方法。
        // 如果你的服务器运行的是 Paper，建议在这里使用 Adventure API (display.text(Component...)) 来设置字体。
        // 这里保留与 Fabric 类似的判断逻辑作为占位/参考。
        boolean isCustomIcon = false;
        for (char c : text.toCharArray()) {
            if (c >= '\uE000') {
                isCustomIcon = true;
                break;
            }
        }

        /*
         * Paper/Adventure API 示例:
         * net.kyori.adventure.text.Component comp = net.kyori.adventure.text.Component.text(text);
         * if (isCustomIcon) {
         *     comp = comp.font(net.kyori.adventure.key.Key.key("hgwar", "ui_pixel_font"));
         * }
         * display.text(comp);
         */
        display.setText(text); // 原生 Bukkit 降级方案

        display.setAlignment(alignment);
        display.setTextOpacity((byte) Math.max(0, Math.min(255, opacity)));

        applyTransformAndColor(display, worldTransform, Color.fromARGB(0, 0, 0, 0), interpTicks, false, billboard);
    }

    // 新增：带 Billboard 的方法签名
    @Override
    public void poolDrawRect(String id, Matrix4d worldTransform, Flux.FluxColor fluxColor, int interpTicks, Flux.FluxBillboard billboard) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);

        Matrix4d finalMat = new Matrix4d(worldTransform).mul(UNIT_SQUARE_RECT);
        applyTransformAndColor(displays.get(0), finalMat, convertColor(fluxColor), interpTicks, false, billboard);
    }

    // 新增：带 Billboard 的方法签名
    @Override
    public void poolDrawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor fluxColor, int interpTicks, Flux.FluxBillboard billboard) {
        activeNodesThisFrame.add(id);

        Vector3d p2 = new Vector3d(point2).sub(point1);
        Vector3d p3 = new Vector3d(point3).sub(point1);

        Vector3d zAxis = new Vector3d(p2).cross(p3).normalize();
        Vector3d xAxis = new Vector3d(p2).normalize();
        Vector3d yAxis = new Vector3d(zAxis).cross(xAxis).normalize();

        float width = (float) p2.length();
        float height = (float) new Vector3d(p3).dot(yAxis);
        float p3Width = (float) new Vector3d(p3).dot(xAxis);

        Quaternionf rotation = new Quaternionf().lookAlong(toVector3f(new Vector3d(zAxis).mul(-1f)), toVector3f(yAxis)).conjugate();
        float shear = width == 0 ? 0 : p3Width / width;

        Matrix4d shearMat = getShearMatrix(0f, 0f, shear, 0f, 0f, 0f);

        Matrix4d localTransform = new Matrix4d()
                .translate(point1)
                .rotate(rotation)
                .scale(width, height, 1f)
                .mul(shearMat);

        Matrix4d finalTransform = new Matrix4d(worldBaseMatrix).mul(localTransform);

        List<TextDisplay> displays = getOrSpawnEntities(id, 3);
        Color bukkitColor = convertColor(fluxColor);

        for (int i = 0; i < 3; i++) {
            TextDisplay display = displays.get(i);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setText(" ");

            Matrix4d pieceMat = new Matrix4d(finalTransform).mul(UNIT_TRIANGLES[i]);
            applyTransformAndColor(display, pieceMat, bukkitColor, interpTicks, true, billboard);
        }
    }

    private List<TextDisplay> getOrSpawnEntities(String id, int requiredCount) {
        List<TextDisplay> list = entityCache.get(id);
        if (list == null || list.size() != requiredCount) {
            if (list != null) list.forEach(Display::remove);
            list = new ArrayList<>();
            for (int i = 0; i < requiredCount; i++) {
                TextDisplay display = (TextDisplay) world.spawnEntity(anchorLoc, EntityType.TEXT_DISPLAY);

                // 新增：实体骑乘逻辑 (Entity Riding)
                if (fluxLoc.attachedEntity() != null) {
                    Entity target = Bukkit.getEntity(fluxLoc.attachedEntity());
                    if (target != null) {
                        target.addPassenger(display);
                    }
                }

                display.setText(" ");
                display.setBillboard(Display.Billboard.FIXED);
                display.setShadowed(false);
                display.setDefaultBackground(false);
                display.setAlignment(TextDisplay.TextAlignment.LEFT);
                display.setBrightness(new Display.Brightness(15, 15));

                list.add(display);

                // 新增：初始化 Transform 缓存
                transformCache.put(display, display.getTransformation());
            }
            entityCache.put(id, list);
        }
        return list;
    }

    // 新增：Billboard 转换方法
    private Display.Billboard convertBillboard(Flux.FluxBillboard billboard) {
        if (billboard == null) return Display.Billboard.FIXED;
        switch (billboard) {
            case CENTER: return Display.Billboard.CENTER;
            case HORIZONTAL: return Display.Billboard.HORIZONTAL;
            case VERTICAL: return Display.Billboard.VERTICAL;
            case FIXED:
            default: return Display.Billboard.FIXED;
        }
    }

    // 更新：加入 billboard 和 transformCache 逻辑
    private void applyTransformAndColor(TextDisplay display, Matrix4d targetMatrix, Color color, int interpTicks, boolean useZHack, Flux.FluxBillboard billboard) {
        display.setBackgroundColor(color);
        display.setBillboard(convertBillboard(billboard)); // 应用 Billboard

        if (interpTicks > 0) {
            display.setInterpolationDuration(interpTicks);
        }

        // 使用缓存获取旧的 Transformation
        Transformation oldTransformation = transformCache.getOrDefault(display, display.getTransformation());

        // Bukkit 的 setTransformationMatrix 会自动帮我们计算出 Translation, Scale, Left/Right Rotation
        display.setTransformationMatrix(toMatrix4f(targetMatrix));
        Transformation newTransformation = display.getTransformation();

        if (useZHack) {
            Quaternionf oldRightRot = oldTransformation.getRightRotation();
            Quaternionf newRightRot = newTransformation.getRightRotation();

            Quaternionf rightRotationChange = new Quaternionf(oldRightRot).difference(newRightRot);
            Vector3f euler = rightRotationChange.getEulerAnglesXYZ(new Vector3f());

            if (Math.abs(euler.z) >= Math.toRadians(45.0)) {
                float sign = Math.signum(euler.z);
                float rot = (float) Math.toRadians(-90.0) * sign;

                Quaternionf leftRot = newTransformation.getLeftRotation();
                leftRot.rotateZ(-rot);

                Vector3f scale = newTransformation.getScale();
                scale.set(scale.y, scale.x, scale.z);

                Quaternionf rightRot = newTransformation.getRightRotation();
                rightRot.rotateZ(rot);

                newTransformation = new Transformation(
                        newTransformation.getTranslation(),
                        leftRot,
                        scale,
                        rightRot
                );
                display.setTransformation(newTransformation);
            }
        }

        // 更新缓存
        transformCache.put(display, newTransformation);

        if (!oldTransformation.equals(newTransformation)) {
            display.setInterpolationDelay(0);
        }
    }

    // ==========================================
    // 数据转换工具
    // ==========================================

    private Color convertColor(Flux.FluxColor color) {
        return Color.fromARGB(color.a(), color.r(), color.g(), color.b());
    }

    private TextDisplay.TextAlignment convertAlignment(Flux.FluxTextAlignment alignment) {
        switch (alignment) {
            case LEFT: return TextDisplay.TextAlignment.LEFT;
            case RIGHT: return TextDisplay.TextAlignment.RIGHT;
            case CENTER:
            default: return TextDisplay.TextAlignment.CENTER;
        }
    }

    private Matrix4f toMatrix4f(Matrix4d matrix) {
        Matrix4f result = new Matrix4f();

        result.m00((float) matrix.m00());
        result.m01((float) matrix.m01());
        result.m02((float) matrix.m02());
        result.m03((float) matrix.m03());

        result.m10((float) matrix.m10());
        result.m11((float) matrix.m11());
        result.m12((float) matrix.m12());
        result.m13((float) matrix.m13());

        result.m20((float) matrix.m20());
        result.m21((float) matrix.m21());
        result.m22((float) matrix.m22());
        result.m23((float) matrix.m23());

        result.m30((float) matrix.m30());
        result.m31((float) matrix.m31());
        result.m32((float) matrix.m32());
        result.m33((float) matrix.m33());

        return result;
    }

    private Vector3f toVector3f(Vector3d v) {
        return new Vector3f(
                (float) v.x(),
                (float) v.y(),
                (float) v.z()
        );
    }
}