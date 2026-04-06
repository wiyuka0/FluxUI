package com.wiyuka.fluxUI.renderer;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.*;

import java.lang.Math;
import java.util.*;

public class BukkitUIPool implements Flux.PoolImpl {
    private final World world;
    private final Location anchorLoc;
    private final Map<String, List<TextDisplay>> entityCache = new HashMap<>();
    private final Set<String> activeNodesThisFrame = new HashSet<>();

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

    @Override
    public void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks) {
        drawTextInternal(id, text, worldTransform, opacity, interpTicks, TextDisplay.TextAlignment.CENTER);
    }

    @Override
    public void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment) {
        drawTextInternal(id, text, worldTransform, opacity, interpTicks, convertAlignment(alignment));
    }

    private void drawTextInternal(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, TextDisplay.TextAlignment alignment) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);
        TextDisplay display = displays.get(0);
        display.setText(text);
        display.setAlignment(alignment);
        display.setTextOpacity((byte) Math.max(0, Math.min(255, opacity)));

        applyTransformAndColor(display, worldTransform, Color.fromARGB(0, 0, 0, 0), interpTicks, false);
    }

    @Override
    public void poolDrawRect(String id, Matrix4d worldTransform, Flux.FluxColor fluxColor, int interpTicks) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);

        Matrix4d finalMat = new Matrix4d(worldTransform).mul(UNIT_SQUARE_RECT);
        applyTransformAndColor(displays.get(0), finalMat, convertColor(fluxColor), interpTicks, false);
    }

    @Override
    public void poolDrawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor fluxColor, int interpTicks) {
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
            applyTransformAndColor(display, pieceMat, bukkitColor, interpTicks, true);
        }
    }

    private List<TextDisplay> getOrSpawnEntities(String id, int requiredCount) {
        List<TextDisplay> list = entityCache.get(id);
        if (list == null || list.size() != requiredCount) {
            if (list != null) list.forEach(Display::remove);
            list = new ArrayList<>();
            for (int i = 0; i < requiredCount; i++) {
                TextDisplay display = (TextDisplay) world.spawnEntity(anchorLoc, EntityType.TEXT_DISPLAY);

                display.setText(" ");
                display.setBillboard(Display.Billboard.FIXED);
                display.setShadowed(false);
                display.setDefaultBackground(false);
                display.setAlignment(TextDisplay.TextAlignment.LEFT);
                display.setBrightness(new Display.Brightness(15, 15));

                list.add(display);
            }
            entityCache.put(id, list);
        }
        return list;
    }

    private void applyTransformAndColor(TextDisplay display, Matrix4d targetMatrix, Color color, int interpTicks, boolean useZHack) {
        display.setBackgroundColor(color);

        if (interpTicks > 0) {
            display.setInterpolationDuration(interpTicks);
        }

        Transformation oldTransformation = display.getTransformation();
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

                display.setTransformation(new Transformation(
                        newTransformation.getTranslation(),
                        leftRot,
                        scale,
                        rightRot
                ));
            }
        }

        if (!oldTransformation.equals(display.getTransformation())) {
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