package com.wiyuka.fluxUI.renderer;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public class UIPool {
    private final World world;
    private final Location anchorLoc;
    private final Map<String, List<TextDisplay>> entityCache = new HashMap<>();
    private final Set<String> activeNodesThisFrame = new HashSet<>();

    private static Matrix4f getShearMatrix(float xy, float xz, float yx, float yz, float zx, float zy) {
        return new Matrix4f(
                1f, xy, xz, 0f,
                yx, 1f, yz, 0f,
                zx, zy, 1f, 0f,
                0f, 0f, 0f, 1f
        );
    }

    public void destroy() {
        for (List<TextDisplay> displays : entityCache.values()) {
            displays.forEach(Display::remove);
        }
        entityCache.clear();
        activeNodesThisFrame.clear();
    }

    private static final Matrix4f UNIT_SQUARE_TRI = new Matrix4f()
            .translate(-0.1f + 0.5f, -0.5f + 0.5f, 0f)
            .scale(8.0f, 3.63f, 1f);

    private static final Matrix4f UNIT_SQUARE_RECT = new Matrix4f()
            .translate(-0.1f + 0.5f, -0.5f + 0.5f, 0f)
            .scale(8.08f, 3.66f, 1f);

    private static final Matrix4f[] UNIT_TRIANGLES = new Matrix4f[3];

    static {
        UNIT_TRIANGLES[0] = new Matrix4f().scale(0.5f).mul(new Matrix4f(UNIT_SQUARE_TRI));

        float offset = 1;
        Matrix4f shearYX = getShearMatrix(0f, 0f, -offset, 0f, 0f, 0f);
        UNIT_TRIANGLES[1] = new Matrix4f().scale(0.5f).translate(1f, 0f, 0f).mul(shearYX).mul(new Matrix4f(UNIT_SQUARE_TRI));

        Matrix4f shearXY = getShearMatrix(-1f, 0f, 0f, 0f, 0f, 0f);
        UNIT_TRIANGLES[2] = new Matrix4f().scale(0.5f).translate(0f, 1f, 0f).mul(shearXY).mul(new Matrix4f(UNIT_SQUARE_TRI));
    }

    public UIPool(Location anchorLoc) {
        this.world = anchorLoc.getWorld();
        Location pureLoc = anchorLoc.clone();
        pureLoc.setYaw(0f);
        pureLoc.setPitch(0f);
        this.anchorLoc = pureLoc;
    }

    public void drawText(String id, String text, Matrix4f worldTransform, int opacity, int interpTicks) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);
        TextDisplay display = displays.get(0);
        display.setText(text);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setTextOpacity((byte) Math.max(0, Math.min(255, opacity)));

        applyTransformAndColor(display, worldTransform, Color.fromARGB(0, 0, 0, 0), interpTicks, false);
    }
    public void drawText(String id, String text, Matrix4f worldTransform, int opacity, int interpTicks, TextDisplay.TextAlignment textAlignment) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);
        TextDisplay display = displays.get(0);
        display.setText(text);
        display.setAlignment(textAlignment);
        display.setTextOpacity((byte) Math.max(0, Math.min(255, opacity)));
        applyTransformAndColor(display, worldTransform, Color.fromARGB(0, 0, 0, 0), interpTicks, false);
    }

    public void beginFrame() {
        activeNodesThisFrame.clear();
    }

    public void drawRect(String id, Matrix4f worldTransform, Color color, int interpTicks) {
        activeNodesThisFrame.add(id);
        List<TextDisplay> displays = getOrSpawnEntities(id, 1);

        Matrix4f finalMat = new Matrix4f(worldTransform).mul(UNIT_SQUARE_RECT);
        applyTransformAndColor(displays.get(0), finalMat, color, interpTicks, false);
    }

    public void drawTriangle(String id, Vector3f point1, Vector3f point2, Vector3f point3, Matrix4f worldBaseMatrix, Color color, int interpTicks) {
        activeNodesThisFrame.add(id);

        Vector3f p2 = new Vector3f(point2).sub(point1);
        Vector3f p3 = new Vector3f(point3).sub(point1);

        Vector3f zAxis = new Vector3f(p2).cross(p3).normalize();
        Vector3f xAxis = new Vector3f(p2).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        float width = p2.length();
        float height = new Vector3f(p3).dot(yAxis);
        float p3Width = new Vector3f(p3).dot(xAxis);

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();
        float shear = width == 0 ? 0 : p3Width / width;

        Matrix4f shearMat = getShearMatrix(0f, 0f, shear, 0f, 0f, 0f);

        Matrix4f localTransform = new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(width, height, 1f)
                .mul(shearMat);

        Matrix4f finalTransform = new Matrix4f(worldBaseMatrix).mul(localTransform);

        List<TextDisplay> displays = getOrSpawnEntities(id, 3);

        for (int i = 0; i < 3; i++) {
            TextDisplay display = displays.get(i);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setText(" ");

            Matrix4f pieceMat = new Matrix4f(finalTransform).mul(UNIT_TRIANGLES[i]);
            applyTransformAndColor(display, pieceMat, color, interpTicks, true);
        }
    }

    public void endFrame() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, List<TextDisplay>> entry : entityCache.entrySet()) {
            if (!activeNodesThisFrame.contains(entry.getKey())) {
                entry.getValue().forEach(Display::remove);
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(entityCache::remove);
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

    private void applyTransformAndColor(TextDisplay display, Matrix4f targetMatrix, Color color, int interpTicks, boolean useZHack) {
        display.setBackgroundColor(color);

        if (interpTicks > 0) {
            display.setInterpolationDuration(interpTicks);
        }

        Transformation oldTransformation = display.getTransformation();
        display.setTransformationMatrix(targetMatrix);
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
}