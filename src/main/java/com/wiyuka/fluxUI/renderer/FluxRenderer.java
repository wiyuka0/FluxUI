package com.wiyuka.fluxUI.renderer;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class FluxRenderer {
    private final Map<String, UIPool> activeScreens = new HashMap<>();
    private final Map<String, Location> screenLocations = new HashMap<>();
    private final Set<String> screensRenderedThisTick = new HashSet<>();

    private UIPool currentPool;
    private final Deque<String> idStack = new ArrayDeque<>();
    private final Deque<Matrix4f> matrixStack = new ArrayDeque<>();

    private int currentInterpTicks = 0;
    private float microZOffset = 0.0f;
    private static final float MICRO_Z_STEP = 0.0001f;
    private static final float MAX_INTERACT_DISTANCE = 10.0f;

    private final Consumer<FluxRenderer> renderLogic;
    private Vector3f currentAnchor = null;

    private static class InputState {
        Vector3f rayOrigin;
        Vector3f rayDir;
        boolean clicking;
    }
    private final Map<UUID, InputState> playerInputs = new HashMap<>();

    public FluxRenderer(Consumer<FluxRenderer> renderLogic) {
        this.renderLogic = renderLogic;
    }

    public void destroy() {
        for (UIPool pool : activeScreens.values()) {
            pool.destroy();
        }
        activeScreens.clear();
        screenLocations.clear();
        playerInputs.clear();
    }

    public void tick() {
        beginTick();
        if (renderLogic != null) {
            try {
                renderLogic.accept(this);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                idStack.clear();
                matrixStack.clear();
                currentPool = null;
            }
        }
        endTick();
        consumeClicks();
    }

    private void beginTick() {
        screensRenderedThisTick.clear();
        for (UIPool pool : activeScreens.values()) {
            pool.beginFrame();
        }
    }

    private void endTick() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, UIPool> entry : activeScreens.entrySet()) {
            if (screensRenderedThisTick.contains(entry.getKey())) {
                entry.getValue().endFrame();
            } else {
                entry.getValue().destroy();
                toRemove.add(entry.getKey());
            }
        }
        toRemove.forEach(id -> {
            activeScreens.remove(id);
            screenLocations.remove(id);
        });
    }

    public void updatePlayerRay(Player player) {
        if (player == null || !player.isOnline()) return;
        Location eyeLoc = player.getEyeLocation();
        InputState state = playerInputs.computeIfAbsent(player.getUniqueId(), k -> new InputState());
        state.rayOrigin = new Vector3f((float) eyeLoc.getX(), (float) eyeLoc.getY(), (float) eyeLoc.getZ());
        state.rayDir = new Vector3f((float) eyeLoc.getDirection().getX(), (float) eyeLoc.getDirection().getY(), (float) eyeLoc.getDirection().getZ());
    }

    public void registerPlayerClick(Player player) {
        if (player == null) return;
        InputState state = playerInputs.get(player.getUniqueId());
        if (state != null) {
            state.clicking = true;
        }
    }

    public void removePlayer(Player player) {
        if (player != null) playerInputs.remove(player.getUniqueId());
    }

    private void consumeClicks() {
        playerInputs.values().forEach(state -> state.clicking = false);
    }

    public boolean area(String id) {
        idStack.push(id);
        return true;
    }

    public void endArea() {
        if (!idStack.isEmpty()) idStack.pop();
    }

    public boolean screen(Location loc, Vector3f xAxis, Vector3f yAxis, Vector3f zAxis, String screenId) {
        idStack.clear();
        matrixStack.clear();

        idStack.push(screenId);
        Location originalLoc = screenLocations.get(screenId);

        if (originalLoc == null || originalLoc.getWorld() != loc.getWorld() || originalLoc.distanceSquared(loc) > 4096.0) {
            if (originalLoc != null) {
                UIPool oldPool = activeScreens.remove(screenId);
                if (oldPool != null) oldPool.destroy();
            }
            originalLoc = loc.clone();
            screenLocations.put(screenId, originalLoc);
            activeScreens.put(screenId, new UIPool(originalLoc));
        }
        currentAnchor = new Vector3f((float) originalLoc.getX(), (float) originalLoc.getY(), (float) originalLoc.getZ());

        UIPool pool = activeScreens.get(screenId);
        screensRenderedThisTick.add(screenId);
        currentPool = pool;

        Vector3f delta = new Vector3f(
                (float) (loc.getX() - originalLoc.getX()),
                (float) (loc.getY() - originalLoc.getY()),
                (float) (loc.getZ() - originalLoc.getZ())
        );

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();
        Matrix4f baseTransform = new Matrix4f().translate(delta).rotate(rotation);

        matrixStack.push(baseTransform);
        microZOffset = 0.0f;

        return true;
    }

    public void endScreen() {
        currentPool = null;
        idStack.clear();
        matrixStack.clear();
    }

    public void rotateZ(float angleDegrees) { if (!matrixStack.isEmpty()) matrixStack.peek().rotateZ((float) Math.toRadians(angleDegrees)); }
    public void rotateX(float angleDegrees) { if (!matrixStack.isEmpty()) matrixStack.peek().rotateX((float) Math.toRadians(angleDegrees)); }
    public void rotateY(float angleDegrees) { if (!matrixStack.isEmpty()) matrixStack.peek().rotateY((float) Math.toRadians(angleDegrees)); }
    public void pushMatrix() { if (!matrixStack.isEmpty()) matrixStack.push(new Matrix4f(matrixStack.peek())); }
    public void popMatrix() { if (!matrixStack.isEmpty()) matrixStack.pop(); }
    public void translate(float x, float y, float z) { if (!matrixStack.isEmpty()) matrixStack.peek().translate(x, y, z); }
    public void scale(float x, float y, float z) { if (!matrixStack.isEmpty()) matrixStack.peek().scale(x, y, z); }
    public void interpolation(int ticks) { currentInterpTicks = ticks; }
    public void skew(float angleX, float angleY) {
        if (matrixStack.isEmpty()) return;
        float tanX = (float) Math.tan(Math.toRadians(angleX));
        float tanY = (float) Math.tan(Math.toRadians(angleY));

        Matrix4f shearMat = new Matrix4f(
                1f,   tanY, 0f, 0f,
                tanX, 1f,   0f, 0f,
                0f,   0f,   1f, 0f,
                0f,   0f,   0f, 1f
        );

        matrixStack.peek().mul(shearMat);
    }

    public void drawAbsRect(String id, float x, float y, float w, float h, Color color) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Matrix4f localTransform = new Matrix4f().translate(x, y - h, safeZ).scale(w, h, 1f);
        Matrix4f finalWorldMatrix = new Matrix4f(matrixStack.peek()).mul(localTransform);
        currentPool.drawRect(genFullId(id), finalWorldMatrix, color, currentInterpTicks);
    }

    public void drawAbsTriangle(String id, float x1, float y1, float x2, float y2, float x3, float y3, Color color) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Vector3f p1 = new Vector3f(x1, y1, safeZ);
        Vector3f p2 = new Vector3f(x2, y2, safeZ);
        Vector3f p3 = new Vector3f(x3, y3, safeZ);
        currentPool.drawTriangle(genFullId(id), p1, p2, p3, matrixStack.peek(), color, currentInterpTicks);
    }

    public void text(String id, String text, float scale) { text(id, text, scale, 255); }

    public void text(String id, String text, float scale, int opacity) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Matrix4f localTransform = new Matrix4f().translate(0, 0, safeZ).scale(scale, scale, scale);
        Matrix4f finalWorldMatrix = new Matrix4f(matrixStack.peek()).mul(localTransform);
        currentPool.drawText(genFullId(id), text, finalWorldMatrix, opacity, currentInterpTicks);
    }

    public void text(String id, String text, float scale, int opacity, TextDisplay.TextAlignment alignment) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Matrix4f localTransform = new Matrix4f().translate(0, 0, safeZ).scale(scale, scale, scale);
        Matrix4f finalWorldMatrix = new Matrix4f(matrixStack.peek()).mul(localTransform);
        currentPool.drawText(genFullId(id), text, finalWorldMatrix, opacity, currentInterpTicks, alignment);
    }

    public void textAbs(String id, String text, float x, float y, float scale, int opacity, TextDisplay.TextAlignment align) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Matrix4f localTransform = new Matrix4f().translate(x, y, safeZ).scale(scale, scale, scale);
        Matrix4f finalWorldMatrix = new Matrix4f(matrixStack.peek()).mul(localTransform);
        currentPool.drawText(genFullId(id), text, finalWorldMatrix, opacity, currentInterpTicks, align);
    }

    public void rect(String id, float width, float height, Color color) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Matrix4f localTransform = new Matrix4f().translate(0, 0, safeZ).scale(width, height, 1f);
        Matrix4f finalWorldMatrix = new Matrix4f(matrixStack.peek()).mul(localTransform);
        currentPool.drawRect(genFullId(id), finalWorldMatrix, color, currentInterpTicks);
    }

    public void triangle(String id, Vector3f p1, Vector3f p2, Vector3f p3, Color color) {
        if (currentPool == null || matrixStack.isEmpty()) return;
        float safeZ = getAndAdvanceMicroZ();
        Vector3f op1 = new Vector3f(p1).add(0, 0, safeZ);
        Vector3f op2 = new Vector3f(p2).add(0, 0, safeZ);
        Vector3f op3 = new Vector3f(p3).add(0, 0, safeZ);
        currentPool.drawTriangle(genFullId(id), op1, op2, op3, matrixStack.peek(), color, currentInterpTicks);
    }

    public Set<Player> getHoveringPlayers(float width, float height) {
        Set<Player> hoveredPlayers = new HashSet<>();
        if (matrixStack.isEmpty() || currentAnchor == null) return hoveredPlayers;

        Matrix4f currentTransform = matrixStack.peek();
        Matrix4f inverseMatrix = new Matrix4f(currentTransform).invertAffine();

        Iterator<Map.Entry<UUID, InputState>> it = playerInputs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, InputState> entry = it.next();
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            InputState input = entry.getValue();
            if (input.rayOrigin == null || input.rayDir == null) continue;

            Vector3f relativeRayOrigin = new Vector3f(input.rayOrigin).sub(currentAnchor);
            Vector3f localOrigin = relativeRayOrigin.mulPosition(inverseMatrix, new Vector3f());
            Vector3f localDir = input.rayDir.mulDirection(inverseMatrix, new Vector3f());

            if (!(Math.abs(localDir.z) > 1e-6f)) continue;
            float t = -localOrigin.z / localDir.z;
            if (!(t > 0)) continue;

            Vector3f localHitPoint = new Vector3f(localDir).mul(t).add(localOrigin);

            if (localHitPoint.x >= 0 && localHitPoint.x <= width && localHitPoint.y >= 0 && localHitPoint.y <= height) {
                Vector3f relativeHitPoint = localHitPoint.mulPosition(currentTransform, new Vector3f());
                float distanceSq = relativeHitPoint.distanceSquared(relativeRayOrigin);
                if (distanceSq <= MAX_INTERACT_DISTANCE * MAX_INTERACT_DISTANCE) {
                    hoveredPlayers.add(player);
                }
            }
        }
        return hoveredPlayers;
    }

    public boolean isHovered(Player player, float width, float height) {
        if (player == null) return false;
        return getHoveringPlayers(width, height).contains(player);
    }

    public boolean isHovered(float width, float height) {
        return !getHoveringPlayers(width, height).isEmpty();
    }

    public Set<Player> hitbox(float width, float height) {
        Set<Player> hovering = getHoveringPlayers(width, height);
        Set<Player> clicking = new HashSet<>();
        for (Player p : hovering) {
            InputState state = playerInputs.get(p.getUniqueId());
            if (state != null && state.clicking) {
                clicking.add(p);
            }
        }
        return clicking;
    }

    private String genFullId(String componentId) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = idStack.descendingIterator();
        while (it.hasNext()) sb.append(it.next()).append("/");
        return sb.append(componentId).toString();
    }

    private float getAndAdvanceMicroZ() {
        float z = microZOffset;
        microZOffset += MICRO_Z_STEP;
        return z;
    }
}