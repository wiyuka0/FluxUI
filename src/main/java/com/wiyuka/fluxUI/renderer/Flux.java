package com.wiyuka.fluxUI.renderer;

import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.*;
import java.util.function.Consumer;

public class Flux {
    public record FluxColor(int a, int r, int g, int b) {
        public static FluxColor fromARGB(int a, int r, int g, int b) {
                return new FluxColor(a, r, g, b);
            }
        }
    public record FluxLocation(String world, double x, double y, double z) {
        public double distanceSquared(FluxLocation other) {
                if (!this.world.equals(other.world)) return Double.MAX_VALUE;
                double dx = this.x - other.x;
                double dy = this.y - other.y;
                double dz = this.z - other.z;
                return dx * dx + dy * dy + dz * dz;
            }
        }
    public enum FluxTextAlignment {
        LEFT, CENTER, RIGHT
    }

    // ==========================================
    // 内部系统
    // ==========================================

    private static class FluxLayout {
        private final FluxRenderer layoutFlux;

        private final Vector2f layoutWindowPos = new Vector2f();
        private float layoutCursorX = 0f;
        private float layoutCursorY = 0f;
        private float layoutCurrLineHeight = 0f;
        private boolean layoutIsSameLine = false;
        private boolean layoutIsFirstItemOnLine = true;

        private float layoutContentMaxX = 0f;
        private float layoutContentMaxY = 0f;
        private float layoutAutoWindowWidth = 4.0f;
        private float layoutAutoWindowHeight = 3.0f;

        public final float layoutTEXT_SCALE = 0.45f;
        public final float layoutFRAME_HEIGHT = 0.22f;

        public final Vector2f layoutWINDOW_PADDING = new Vector2f(0.15f, 0.15f);
        public final Vector2f layoutITEM_SPACING = new Vector2f(0.15f, 0.05f);
        public final Vector2f layoutFRAME_PADDING = new Vector2f(0.15f, 0.0f);

        public final float layoutTITLE_BAR_HEIGHT = 0.3f;
        public final float layoutTEXT_OFFSET_Y = -0.05f;

        private final float layoutTEXT_WIDTH_RATIO = 0.028f;

        public final FluxColor layoutColWindowBg = FluxColor.fromARGB(240, 15, 15, 15);
        public final FluxColor layoutColTitleBg = FluxColor.fromARGB(255, 45, 60, 90);
        public final FluxColor layoutColFrameBg = FluxColor.fromARGB(255, 30, 45, 70);
        public final FluxColor layoutColSliderGrab = FluxColor.fromARGB(255, 66, 150, 250);
        public final FluxColor layoutColButton = FluxColor.fromARGB(255, 41, 74, 122);

        public FluxLayout(FluxRenderer layoutFlux) {
            this.layoutFlux = layoutFlux;
        }

        public void layoutBeginWindow(String title, float startX, float startY) {
            layoutWindowPos.set(startX, startY);

            layoutFlux.renderer_drawAbsRect("win_bg_" + title, startX, startY, layoutAutoWindowWidth, layoutAutoWindowHeight, layoutColWindowBg);
            layoutFlux.renderer_drawAbsRect("win_titlebg_" + title, startX, startY, layoutAutoWindowWidth, layoutTITLE_BAR_HEIGHT, layoutColTitleBg);

            layoutDrawTextLeft("win_title_txt_" + title, "▼ " + title, startX + 0.08f, startY - layoutTITLE_BAR_HEIGHT / 2f, layoutTEXT_SCALE);

            layoutCursorX = layoutWINDOW_PADDING.x;
            layoutCursorY = layoutTITLE_BAR_HEIGHT + layoutWINDOW_PADDING.y;
            layoutCurrLineHeight = 0f;
            layoutIsSameLine = false;
            layoutIsFirstItemOnLine = true;

            layoutContentMaxX = layoutCalcTextWidth("▼ " + title, layoutTEXT_SCALE) + 0.3f;
            layoutContentMaxY = layoutCursorY;
        }

        public void layoutEndWindow() {
            layoutAutoWindowWidth = layoutContentMaxX + layoutWINDOW_PADDING.x;
            layoutAutoWindowHeight = layoutContentMaxY + layoutWINDOW_PADDING.y;
        }

        public void layoutSameLine() {
            layoutIsSameLine = true;
        }

        public static class LayoutItemBounds {
            public float layoutItemBoundsAbsX, layoutItemBoundsAbsY, layoutItemBoundsW, layoutItemBoundsH;
            public LayoutItemBounds(float layoutItemBoundsAbsX, float layoutItemBoundsAbsY, float layoutItemBoundsW, float layoutItemBoundsH) {
                this.layoutItemBoundsAbsX = layoutItemBoundsAbsX; this.layoutItemBoundsAbsY = layoutItemBoundsAbsY; this.layoutItemBoundsW = layoutItemBoundsW; this.layoutItemBoundsH = layoutItemBoundsH;
            }
        }

        public LayoutItemBounds layoutAllocateSpace(float width, float height) {
            if (!layoutIsSameLine) {
                layoutCursorX = layoutWINDOW_PADDING.x;
                if (!layoutIsFirstItemOnLine) {
                    layoutCursorY += layoutCurrLineHeight + layoutITEM_SPACING.y;
                }
                layoutCurrLineHeight = 0f;
            } else {
                layoutCursorX += layoutITEM_SPACING.x;
            }

            float absX = layoutWindowPos.x + layoutCursorX;
            float absY = layoutWindowPos.y - layoutCursorY;

            layoutCurrLineHeight = Math.max(layoutCurrLineHeight, height);
            layoutCursorX += width;

            layoutContentMaxX = Math.max(layoutContentMaxX, layoutCursorX);
            layoutContentMaxY = Math.max(layoutContentMaxY, layoutCursorY + layoutCurrLineHeight);

            layoutIsSameLine = false;
            layoutIsFirstItemOnLine = false;
            return new LayoutItemBounds(absX, absY, width, height);
        }

        public void layoutDrawTextLeft(String id, String text, float absLeftX, float absCenterY, float scale) {
            float textWidth = layoutCalcTextWidth(text, scale);
            layoutFlux.renderer_textAbs(id, text, absLeftX + textWidth / 2f, absCenterY + layoutTEXT_OFFSET_Y, scale, 255, FluxTextAlignment.CENTER);
        }

        public void layoutDrawTextCenter(String id, String text, float absCenterX, float absCenterY, float scale) {
            layoutFlux.renderer_textAbs(id, text, absCenterX, absCenterY + layoutTEXT_OFFSET_Y, scale, 255, FluxTextAlignment.CENTER);
        }

        public float layoutCalcTextWidth(String text, float scale) {
            if (text == null || text.isEmpty()) return 0;

            int pixels = 0;
            boolean isBold = false;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '§' && i + 1 < text.length()) {
                    char formatCode = Character.toLowerCase(text.charAt(i + 1));
                    if (formatCode == 'l') {
                        isBold = true;
                    } else if ((formatCode >= '0' && formatCode <= '9') ||
                            (formatCode >= 'a' && formatCode <= 'f') ||
                            formatCode == 'r') {
                        isBold = false;
                    }
                    i++;
                    continue;
                }

                int charWidth = 0;
                if (c > 255) {
                    charWidth = 9;
                } else {
                    switch (c) {
                        case '!': case '\'': case ',': case '.': case ':': case ';': case 'i': case '|':
                            charWidth = 2; break;
                        case '`': case 'l':
                            charWidth = 3; break;
                        case ' ': case '"': case '(': case ')': case '*': case 'I': case '[': case ']': case 't': case '{': case '}':
                            charWidth = 4; break;
                        case '<': case '>': case 'f': case 'k':
                            charWidth = 5; break;
                        case '@': case '~':
                            charWidth = 7; break;
                        default:
                            charWidth = 6; break;
                    }
                }

                if (isBold) {
                    charWidth += 1;
                }

                pixels += charWidth;
            }

            if (pixels > 0) {
                pixels -= 1;
            }

            return pixels * layoutTEXT_WIDTH_RATIO * scale;
        }
    }
    private static class FluxRenderer {
        private final Map<String, UIPool> renderer_activeScreens = new HashMap<>();
        private final Map<String, FluxLocation> renderer_screenLocations = new HashMap<>();
        private final Set<String> renderer_screensRenderedThisTick = new HashSet<>();

        private UIPool renderer_currentPool;
        private final Deque<String> renderer_idStack = new ArrayDeque<>();
        private final Deque<Matrix4d> renderer_matrixStack = new ArrayDeque<>();

        private int renderer_currentInterpTicks = 0;
        private float renderer_microZOffset = 0.0f;
        private static final float renderer_MICRO_Z_STEP = 0.0001f;
        private static final float renderer_MAX_INTERACT_DISTANCE = 10.0f;

        private final Consumer<FluxRenderer> renderer_renderLogic;
        private Vector3d renderer_currentAnchor = null;

        private static class RendererInputState {
            Vector3d renderer_rayOrigin;
            Vector3d renderer_rayDir;
            boolean renderer_clicking;
        }
        private final Map<UUID, RendererInputState> renderer_playerInputs = new HashMap<>();

        public FluxRenderer(Consumer<FluxRenderer> renderer_renderLogic) {
            this.renderer_renderLogic = renderer_renderLogic;
        }

        public void renderer_destroy() {
            for (UIPool pool : renderer_activeScreens.values()) {
                pool.pool_destroy();
            }
            renderer_activeScreens.clear();
            renderer_screenLocations.clear();
            renderer_playerInputs.clear();
        }

        public void renderer_tick() {
            renderer_beginTick();
            if (renderer_renderLogic != null) {
                try {
                    renderer_renderLogic.accept(this);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    renderer_idStack.clear();
                    renderer_matrixStack.clear();
                    renderer_currentPool = null;
                }
            }
            renderer_endTick();
            renderer_consumeClicks();
        }

        private void renderer_beginTick() {
            renderer_screensRenderedThisTick.clear();
            for (UIPool pool : renderer_activeScreens.values()) {
                pool.pool_beginFrame();
            }
        }

        private void renderer_endTick() {
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, UIPool> entry : renderer_activeScreens.entrySet()) {
                if (renderer_screensRenderedThisTick.contains(entry.getKey())) {
                    entry.getValue().pool_endFrame();
                } else {
                    entry.getValue().pool_destroy();
                    toRemove.add(entry.getKey());
                }
            }
            toRemove.forEach(id -> {
                renderer_activeScreens.remove(id);
                renderer_screenLocations.remove(id);
            });
        }

        public void renderer_updatePlayerRay(UUID playerId, Vector3d origin, Vector3d direction) {
            if (playerId == null) return;
            RendererInputState state = renderer_playerInputs.computeIfAbsent(playerId, k -> new RendererInputState());
            state.renderer_rayOrigin = origin;
            state.renderer_rayDir = direction;
        }

        public void renderer_registerPlayerClick(UUID playerId) {
            if (playerId == null) return;
            RendererInputState state = renderer_playerInputs.get(playerId);
            if (state != null) {
                state.renderer_clicking = true;
            }
        }

        public void renderer_removePlayer(UUID playerId) {
            if (playerId != null) renderer_playerInputs.remove(playerId);
        }

        private void renderer_consumeClicks() {
            renderer_playerInputs.values().forEach(state -> state.renderer_clicking = false);
        }

        public boolean renderer_area(String id) {
            renderer_idStack.push(id);
            return true;
        }

        public void renderer_endArea() {
            if (!renderer_idStack.isEmpty()) renderer_idStack.pop();
        }

        public boolean renderer_screen(FluxLocation loc, Vector3d xAxis, Vector3d yAxis, Vector3d zAxis, String screenId) {
            renderer_idStack.clear();
            renderer_matrixStack.clear();

            renderer_idStack.push(screenId);
            FluxLocation originalLoc = renderer_screenLocations.get(screenId);

            if (originalLoc == null || !originalLoc.world.equals(loc.world) || originalLoc.distanceSquared(loc) > 4096.0f) {
                if (originalLoc != null) {
                    UIPool oldPool = renderer_activeScreens.remove(screenId);
                    if (oldPool != null) oldPool.pool_destroy();
                }
                originalLoc = new FluxLocation(loc.world, loc.x, loc.y, loc.z);
                renderer_screenLocations.put(screenId, originalLoc);
                renderer_activeScreens.put(screenId, new UIPool(originalLoc));
            }
            renderer_currentAnchor = new Vector3d(originalLoc.x, originalLoc.y, originalLoc.z);

            UIPool pool = renderer_activeScreens.get(screenId);
            renderer_screensRenderedThisTick.add(screenId);
            renderer_currentPool = pool;

            Vector3d delta = new Vector3d(
                    loc.x - originalLoc.x,
                    loc.y - originalLoc.y,
                    loc.z - originalLoc.z
            );

            Quaterniond rotation = new Quaterniond().lookAlong(new Vector3d(zAxis).mul(-1f), yAxis).conjugate();
            Matrix4d baseTransform = new Matrix4d().translate(delta).rotate(rotation);

            renderer_matrixStack.push(baseTransform);
            renderer_microZOffset = 0.0f;

            return true;
        }

        public void renderer_endScreen() {
            renderer_currentPool = null;
            renderer_idStack.clear();
            renderer_matrixStack.clear();
        }

        public void renderer_rotateZ(float angleDegrees) { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.peek().rotateZ((float) Math.toRadians(angleDegrees)); }
        public void renderer_rotateX(float angleDegrees) { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.peek().rotateX((float) Math.toRadians(angleDegrees)); }
        public void renderer_rotateY(float angleDegrees) { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.peek().rotateY((float) Math.toRadians(angleDegrees)); }
        public void renderer_pushMatrix() { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.push(new Matrix4d(renderer_matrixStack.peek())); }
        public void renderer_popMatrix() { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.pop(); }
        public void renderer_translate(float x, float y, float z) { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.peek().translate(x, y, z); }
        public void renderer_scale(float x, float y, float z) { if (!renderer_matrixStack.isEmpty()) renderer_matrixStack.peek().scale(x, y, z); }
        public void renderer_interpolation(int ticks) { renderer_currentInterpTicks = ticks; }
        public void renderer_skew(float angleX, float angleY) {
            if (renderer_matrixStack.isEmpty()) return;
            float tanX = (float) Math.tan(Math.toRadians(angleX));
            float tanY = (float) Math.tan(Math.toRadians(angleY));

            Matrix4d shearMat = new Matrix4d(
                    1f,   tanY, 0f, 0f,
                    tanX, 1f,   0f, 0f,
                    0f,   0f,   1f, 0f,
                    0f,   0f,   0f, 1f
            );

            renderer_matrixStack.peek().mul(shearMat);
        }

        public void renderer_drawAbsRect(String id, float x, float y, float w, float h, FluxColor color) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Matrix4d localTransform = new Matrix4d().translate(x, y - h, safeZ).scale(w, h, 1f);
            Matrix4d finalWorldMatrix = new Matrix4d(renderer_matrixStack.peek()).mul(localTransform);
            renderer_currentPool.pool_drawRect(renderer_genFullId(id), finalWorldMatrix, color, renderer_currentInterpTicks);
        }

        public void renderer_drawAbsTriangle(String id, float x1, float y1, float x2, float y2, float x3, float y3, FluxColor color) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Vector3d p1 = new Vector3d(x1, y1, safeZ);
            Vector3d p2 = new Vector3d(x2, y2, safeZ);
            Vector3d p3 = new Vector3d(x3, y3, safeZ);
            renderer_currentPool.pool_drawTriangle(renderer_genFullId(id), p1, p2, p3, renderer_matrixStack.peek(), color, renderer_currentInterpTicks);
        }

        public void renderer_text(String id, String text, float scale) { renderer_text(id, text, scale, 255); }

        public void renderer_text(String id, String text, float scale, int opacity) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Matrix4d localTransform = new Matrix4d().translate(0, 0, safeZ).scale(scale, scale, scale);
            Matrix4d finalWorldMatrix = new Matrix4d(renderer_matrixStack.peek()).mul(localTransform);
            renderer_currentPool.pool_drawText(renderer_genFullId(id), text, finalWorldMatrix, opacity, renderer_currentInterpTicks);
        }

        public void renderer_text(String id, String text, float scale, int opacity, FluxTextAlignment alignment) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Matrix4d localTransform = new Matrix4d().translate(0, 0, safeZ).scale(scale, scale, scale);
            Matrix4d finalWorldMatrix = new Matrix4d(renderer_matrixStack.peek()).mul(localTransform);
            renderer_currentPool.pool_drawText(renderer_genFullId(id), text, finalWorldMatrix, opacity, renderer_currentInterpTicks, alignment);
        }

        public void renderer_textAbs(String id, String text, float x, float y, float scale, int opacity, FluxTextAlignment align) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Matrix4d localTransform = new Matrix4d().translate(x, y, safeZ).scale(scale, scale, scale);
            Matrix4d finalWorldMatrix = new Matrix4d(renderer_matrixStack.peek()).mul(localTransform);
            renderer_currentPool.pool_drawText(renderer_genFullId(id), text, finalWorldMatrix, opacity, renderer_currentInterpTicks, align);
        }

        public void renderer_rect(String id, float width, float height, FluxColor color) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Matrix4d localTransform = new Matrix4d().translate(0, 0, safeZ).scale(width, height, 1f);
            Matrix4d finalWorldMatrix = new Matrix4d(renderer_matrixStack.peek()).mul(localTransform);
            renderer_currentPool.pool_drawRect(renderer_genFullId(id), finalWorldMatrix, color, renderer_currentInterpTicks);
        }

        public void renderer_triangle(String id, Vector3d p1, Vector3d p2, Vector3d p3, FluxColor color) {
            if (renderer_currentPool == null || renderer_matrixStack.isEmpty()) return;
            float safeZ = renderer_getAndAdvanceMicroZ();
            Vector3d op1 = new Vector3d(p1).add(0, 0, safeZ);
            Vector3d op2 = new Vector3d(p2).add(0, 0, safeZ);
            Vector3d op3 = new Vector3d(p3).add(0, 0, safeZ);
            renderer_currentPool.pool_drawTriangle(renderer_genFullId(id), op1, op2, op3, renderer_matrixStack.peek(), color, renderer_currentInterpTicks);
        }

        public Set<UUID> renderer_getHoveringPlayers(float width, float height) {
            Set<UUID> hoveredPlayers = new HashSet<>();
            if (renderer_matrixStack.isEmpty() || renderer_currentAnchor == null) return hoveredPlayers;

            Matrix4d currentTransform = renderer_matrixStack.peek();
            Matrix4d inverseMatrix = new Matrix4d(currentTransform).invertAffine();

            for (Map.Entry<UUID, RendererInputState> entry : renderer_playerInputs.entrySet()) {
                UUID playerId = entry.getKey();
                RendererInputState input = entry.getValue();

                if (input.renderer_rayOrigin == null || input.renderer_rayDir == null) continue;

                Vector3d relativeRayOrigin = new Vector3d(input.renderer_rayOrigin).sub(renderer_currentAnchor);
                Vector3d localOrigin = relativeRayOrigin.mulPosition(inverseMatrix, new Vector3d());
                Vector3d localDir = input.renderer_rayDir.mulDirection(inverseMatrix, new Vector3d());

                if (!(Math.abs(localDir.z) > 1e-6f)) continue;
                float t = (float) (-localOrigin.z / localDir.z);
                if (!(t > 0)) continue;

                Vector3d localHitPoint = new Vector3d(localDir).mul(t).add(localOrigin);

                if (localHitPoint.x >= 0 && localHitPoint.x <= width && localHitPoint.y >= 0 && localHitPoint.y <= height) {
                    Vector3d relativeHitPoint = localHitPoint.mulPosition(currentTransform, new Vector3d());
                    float distanceSq = (float) relativeHitPoint.distanceSquared(relativeRayOrigin);
                    if (distanceSq <= renderer_MAX_INTERACT_DISTANCE * renderer_MAX_INTERACT_DISTANCE) {
                        hoveredPlayers.add(playerId);
                    }
                }
            }
            return hoveredPlayers;
        }

        public boolean renderer_isHovered(UUID playerId, float width, float height) {
            if (playerId == null) return false;
            return renderer_getHoveringPlayers(width, height).contains(playerId);
        }

        public boolean renderer_isHovered(float width, float height) {
            return !renderer_getHoveringPlayers(width, height).isEmpty();
        }

        public Set<UUID> renderer_hitbox(float width, float height) {
            Set<UUID> hovering = renderer_getHoveringPlayers(width, height);
            Set<UUID> clicking = new HashSet<>();
            for (UUID p : hovering) {
                RendererInputState state = renderer_playerInputs.get(p);
                if (state != null && state.renderer_clicking) {
                    clicking.add(p);
                }
            }
            return clicking;
        }

        private String renderer_genFullId(String componentId) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> it = renderer_idStack.descendingIterator();
            while (it.hasNext()) sb.append(it.next()).append("/");
            return sb.append(componentId).toString();
        }

        private float renderer_getAndAdvanceMicroZ() {
            float z = renderer_microZOffset;
            renderer_microZOffset += renderer_MICRO_Z_STEP;
            return z;
        }
    }
    private static class FluxControllers {
        private final FluxRenderer controllerFlux;
        private final FluxLayout controllerLayout;
        private UUID controllerPlayerId;

        public FluxControllers(FluxRenderer controllerFlux, FluxLayout controllerLayout, UUID controllerPlayerId) {
            this.controllerFlux = controllerFlux;
            this.controllerLayout = controllerLayout;
            this.controllerPlayerId = controllerPlayerId;
        }

        public void controllerSetPlayer(UUID playerId) {
            this.controllerPlayerId = playerId;
        }

        public void controllerText(String id, String text) {
            float textWidth = controllerLayout.layoutCalcTextWidth(text, controllerLayout.layoutTEXT_SCALE);
            FluxLayout.LayoutItemBounds bounds = controllerLayout.layoutAllocateSpace(textWidth, controllerLayout.layoutFRAME_HEIGHT);
            controllerLayout.layoutDrawTextLeft(id, text, bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH / 2f, controllerLayout.layoutTEXT_SCALE);
        }

        public boolean controllerButton(String id, String text) {
            float textWidth = controllerLayout.layoutCalcTextWidth(text, controllerLayout.layoutTEXT_SCALE);
            float width = textWidth + (controllerLayout.layoutFRAME_PADDING.x * 2);

            FluxLayout.LayoutItemBounds bounds = controllerLayout.layoutAllocateSpace(width, controllerLayout.layoutFRAME_HEIGHT);

            controllerFlux.renderer_drawAbsRect(id + "_bg", bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY, bounds.layoutItemBoundsW, bounds.layoutItemBoundsH, controllerLayout.layoutColButton);
            controllerLayout.layoutDrawTextCenter(id + "_txt", text, bounds.layoutItemBoundsAbsX + bounds.layoutItemBoundsW / 2f, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH / 2f, controllerLayout.layoutTEXT_SCALE);

            controllerFlux.renderer_pushMatrix();
            controllerFlux.renderer_translate(bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH, 0);

            boolean isHovered = controllerFlux.renderer_isHovered(controllerPlayerId, bounds.layoutItemBoundsW, bounds.layoutItemBoundsH);
            FluxColor hoverOverlay = isHovered ? FluxColor.fromARGB(50, 255, 255, 255) : FluxColor.fromARGB(0, 0, 0, 0);
            controllerFlux.renderer_rect(id + "_hover", bounds.layoutItemBoundsW, bounds.layoutItemBoundsH, hoverOverlay);

            boolean clicked = controllerFlux.renderer_hitbox(bounds.layoutItemBoundsW, bounds.layoutItemBoundsH).contains(controllerPlayerId);
            controllerFlux.renderer_popMatrix();

            return clicked;
        }

        public boolean controllerButtonAbs(String id, String text, float x, float y) {
            float textWidth = controllerLayout.layoutCalcTextWidth(text, controllerLayout.layoutTEXT_SCALE);
            float width = textWidth + (controllerLayout.layoutFRAME_PADDING.x * 2);
            float height = controllerLayout.layoutFRAME_HEIGHT;

            controllerFlux.renderer_pushMatrix();
            controllerFlux.renderer_translate(x, y, 0);

            controllerFlux.renderer_rect(id + "_bg", width, height, controllerLayout.layoutColButton);
            controllerFlux.renderer_textAbs(id + "_txt", text, width / 2f, -height / 2f, controllerLayout.layoutTEXT_SCALE, 255, FluxTextAlignment.CENTER);

            boolean isHovered = controllerFlux.renderer_isHovered(controllerPlayerId, width, height);
            if (isHovered) {
                controllerFlux.renderer_rect(id + "_hover", width, height, FluxColor.fromARGB(50, 255, 255, 255));
            }
            boolean clicked = controllerFlux.renderer_hitbox(width, height).contains(controllerPlayerId);

            controllerFlux.renderer_popMatrix();
            return clicked;
        }

        public boolean controllerCheckboxAbs(String id, String label, boolean state, float x, float y) {
            float boxSize = controllerLayout.layoutFRAME_HEIGHT * 0.85f;
            float textWidth = controllerLayout.layoutCalcTextWidth(label, controllerLayout.layoutTEXT_SCALE);
            float totalWidth = boxSize + controllerLayout.layoutITEM_SPACING.x + textWidth;
            float height = controllerLayout.layoutFRAME_HEIGHT;
            float boxOffsetY = (height - boxSize) / 2f;
            controllerFlux.renderer_drawAbsRect(id + "_bg", x, y - boxOffsetY, boxSize, boxSize, controllerLayout.layoutColFrameBg);
            if (state) {
                float pad = 0.04f;
                controllerFlux.renderer_drawAbsRect(id + "_tick", x + pad, y - boxOffsetY - pad, boxSize - pad * 2, boxSize - pad * 2, controllerLayout.layoutColSliderGrab);
            }
            controllerLayout.layoutDrawTextLeft(id + "_txt", label, x + boxSize + controllerLayout.layoutITEM_SPACING.x, y - height / 2f, controllerLayout.layoutTEXT_SCALE);
            controllerFlux.renderer_pushMatrix();
            controllerFlux.renderer_translate(x, y - height, 0);
            boolean isHovered = controllerFlux.renderer_isHovered(controllerPlayerId, totalWidth, height);
            FluxColor hoverOverlay = isHovered ? FluxColor.fromARGB(30, 255, 255, 255) : FluxColor.fromARGB(0, 0, 0, 0);
            controllerFlux.renderer_rect(id + "_hover", totalWidth, height, hoverOverlay);
            if (controllerFlux.renderer_hitbox(totalWidth, height).contains(controllerPlayerId)) state = !state;
            controllerFlux.renderer_popMatrix();
            return state;
        }

        public float ControllerSliderFloatAbs(String id, String label, float value, float min, float max, float x, float y) {
            float trackW = 2.5f;
            float height = controllerLayout.layoutFRAME_HEIGHT;

            controllerFlux.renderer_drawAbsRect(id + "_bg", x, y, trackW, height, controllerLayout.layoutColFrameBg);

            float percent = Math.max(0, Math.min(1, (value - min) / (max - min)));
            float fillW = percent * trackW;
            if (fillW > 0) {
                controllerFlux.renderer_drawAbsRect(id + "_fill", x, y, fillW, height, controllerLayout.layoutColSliderGrab);
            }
            String valText = String.format("%.3f", value);
            controllerLayout.layoutDrawTextCenter(id + "_val", valText, x + trackW / 2f, y - height / 2f, controllerLayout.layoutTEXT_SCALE);
            controllerLayout.layoutDrawTextLeft(id + "_txt", label, x + trackW + controllerLayout.layoutITEM_SPACING.x, y - height / 2f, controllerLayout.layoutTEXT_SCALE);

            controllerFlux.renderer_pushMatrix();
            controllerFlux.renderer_translate(x, y - height, 0);

            boolean isHovered = controllerFlux.renderer_isHovered(controllerPlayerId, trackW, height);
            if (isHovered) {
                controllerFlux.renderer_rect(id + "_hover", trackW, height, FluxColor.fromARGB(30, 255, 255, 255));
            }
            if (controllerFlux.renderer_hitbox(trackW / 2, height).contains(controllerPlayerId)) {
                value = Math.max(min, value - (max - min) * 0.05f);
            }

            controllerFlux.renderer_translate(trackW / 2, 0, 0);
            if (controllerFlux.renderer_hitbox(trackW / 2, height).contains(controllerPlayerId)) {
                value = Math.min(max, value + (max - min) * 0.05f);
            }

            controllerFlux.renderer_popMatrix();
            return value;
        }

        public boolean controllerCheckbox(String id, String label, boolean state) {
            float boxSize = controllerLayout.layoutFRAME_HEIGHT * 0.85f;
            float textWidth = controllerLayout.layoutCalcTextWidth(label, controllerLayout.layoutTEXT_SCALE);

            FluxLayout.LayoutItemBounds bounds = controllerLayout.layoutAllocateSpace(boxSize + controllerLayout.layoutITEM_SPACING.x + textWidth, controllerLayout.layoutFRAME_HEIGHT);
            float boxOffsetY = (controllerLayout.layoutFRAME_HEIGHT - boxSize) / 2f;

            controllerFlux.renderer_drawAbsRect(id + "_bg", bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY - boxOffsetY, boxSize, boxSize, controllerLayout.layoutColFrameBg);

            if (state) {
                float pad = 0.04f;
                controllerFlux.renderer_drawAbsRect(id + "_tick", bounds.layoutItemBoundsAbsX + pad, bounds.layoutItemBoundsAbsY - boxOffsetY - pad, boxSize - pad * 2, boxSize - pad * 2, controllerLayout.layoutColSliderGrab);
            }

            controllerLayout.layoutDrawTextLeft(id + "_txt", label, bounds.layoutItemBoundsAbsX + boxSize + controllerLayout.layoutITEM_SPACING.x, bounds.layoutItemBoundsAbsY - controllerLayout.layoutFRAME_HEIGHT / 2f, controllerLayout.layoutTEXT_SCALE);

            controllerFlux.renderer_pushMatrix();
            controllerFlux.renderer_translate(bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH, 0);

            boolean isHovered = controllerFlux.renderer_isHovered(controllerPlayerId, bounds.layoutItemBoundsW, bounds.layoutItemBoundsH);
            FluxColor hoverOverlay = isHovered ? FluxColor.fromARGB(30, 255, 255, 255) : FluxColor.fromARGB(0, 0, 0, 0);
            controllerFlux.renderer_rect(id + "_hover", bounds.layoutItemBoundsW, bounds.layoutItemBoundsH, hoverOverlay);

            if (controllerFlux.renderer_hitbox(bounds.layoutItemBoundsW, bounds.layoutItemBoundsH).contains(controllerPlayerId)) {
                state = !state;
            }
            controllerFlux.renderer_popMatrix();

            return state;
        }

        public float controllerSliderFloat(String id, String label, float value, float min, float max) {
            float trackW = 2.5f;
            float textWidth = controllerLayout.layoutCalcTextWidth(label, controllerLayout.layoutTEXT_SCALE);

            FluxLayout.LayoutItemBounds bounds = controllerLayout.layoutAllocateSpace(trackW + controllerLayout.layoutITEM_SPACING.x + textWidth, controllerLayout.layoutFRAME_HEIGHT);

            controllerFlux.renderer_drawAbsRect(id + "_bg", bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY, trackW, bounds.layoutItemBoundsH, controllerLayout.layoutColFrameBg);

            float percent = Math.max(0, Math.min(1, (value - min) / (max - min)));
            float fillW = percent * trackW;
            if (fillW > 0) {
                controllerFlux.renderer_drawAbsRect(id + "_fill", bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY, fillW, bounds.layoutItemBoundsH, controllerLayout.layoutColSliderGrab);
            }

            String valText = String.format("%.3f", value);
            controllerLayout.layoutDrawTextCenter(id + "_val", valText, bounds.layoutItemBoundsAbsX + trackW / 2f, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH / 2f, controllerLayout.layoutTEXT_SCALE);
            controllerLayout.layoutDrawTextLeft(id + "_txt", label, bounds.layoutItemBoundsAbsX + trackW + controllerLayout.layoutITEM_SPACING.x, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH / 2f, controllerLayout.layoutTEXT_SCALE);

            controllerFlux.renderer_pushMatrix();
            controllerFlux.renderer_translate(bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY - bounds.layoutItemBoundsH, 0);
            if (controllerFlux.renderer_hitbox(trackW / 2, bounds.layoutItemBoundsH).contains(controllerPlayerId)) {
                value = Math.max(min, value - (max - min) * 0.05f);
            }
            controllerFlux.renderer_translate(trackW / 2, 0, 0);
            if (controllerFlux.renderer_hitbox(trackW / 2, bounds.layoutItemBoundsH).contains(controllerPlayerId)) {
                value = Math.min(max, value + (max - min) * 0.05f);
            }
            controllerFlux.renderer_popMatrix();

            return value;
        }

        public void controllerColorEdit3(String id, String label, FluxColor color) {
            float boxSize = controllerLayout.layoutFRAME_HEIGHT;
            float textWidth = controllerLayout.layoutCalcTextWidth(label, controllerLayout.layoutTEXT_SCALE);

            FluxLayout.LayoutItemBounds bounds = controllerLayout.layoutAllocateSpace(boxSize + controllerLayout.layoutITEM_SPACING.x + textWidth, controllerLayout.layoutFRAME_HEIGHT);

            float boxOffsetY = (controllerLayout.layoutFRAME_HEIGHT - boxSize) / 2f;
            controllerFlux.renderer_drawAbsRect(id + "_preview", bounds.layoutItemBoundsAbsX, bounds.layoutItemBoundsAbsY - boxOffsetY, boxSize, boxSize, color);

            controllerLayout.layoutDrawTextLeft(id + "_txt", label, bounds.layoutItemBoundsAbsX + boxSize + controllerLayout.layoutITEM_SPACING.x, bounds.layoutItemBoundsAbsY - controllerLayout.layoutFRAME_HEIGHT / 2f, controllerLayout.layoutTEXT_SCALE);
        }
    }

    public static interface PoolImpl {
        void poolBeginFrame();
        void poolEndFrame();
        void poolDestroy();
        void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks);
        void poolDrawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment);
        void poolDrawRect(String id, Matrix4d worldTransform, Flux.FluxColor color, int interpTicks);
        void poolDrawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor color, int interpTicks);
    }

    public static void setPoolFactory(UIPool.poolFactory f) {
        UIPool.setPoolFactory(f);
    }
    public static class UIPool {
        public interface poolFactory {
            PoolImpl pool_create(Flux.FluxLocation location);
        }

        private static poolFactory pool_factory;

        /**
         * 在插件启动时 (onEnable) 调用此方法注入 Bukkit 实现
         */
        private static void setPoolFactory(poolFactory f) {
            pool_factory = f;
        }

        // ==========================================
        // 代理逻辑
        // ==========================================

        private final PoolImpl pool_impl;

        public UIPool(Flux.FluxLocation location) {
            if (pool_factory == null) {
                throw new IllegalStateException("UIPool factory is not set! Please call UIPool.setFactory() before using Flux.");
            }
            this.pool_impl = pool_factory.pool_create(location);
        }

        public void pool_beginFrame() { pool_impl.poolBeginFrame(); }
        public void pool_endFrame() { pool_impl.poolEndFrame(); }
        public void pool_destroy() { pool_impl.poolDestroy(); }

        public void pool_drawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks) {
            pool_impl.poolDrawText(id, text, worldTransform, opacity, interpTicks);
        }

        public void pool_drawText(String id, String text, Matrix4d worldTransform, int opacity, int interpTicks, Flux.FluxTextAlignment alignment) {
            pool_impl.poolDrawText(id, text, worldTransform, opacity, interpTicks, alignment);
        }

        public void pool_drawRect(String id, Matrix4d worldTransform, Flux.FluxColor color, int interpTicks) {
            pool_impl.poolDrawRect(id, worldTransform, color, interpTicks);
        }

        public void pool_drawTriangle(String id, Vector3d point1, Vector3d point2, Vector3d point3, Matrix4d worldBaseMatrix, Flux.FluxColor color, int interpTicks) {
            pool_impl.poolDrawTriangle(id, point1, point2, point3, worldBaseMatrix, color, interpTicks);
        }
    }

    private final FluxRenderer renderer;
    private final FluxLayout layout;
    private final FluxControllers controllers;

    public Flux(UUID targetPlayerId, Consumer<Flux> renderLogic) {
        this.renderer = new FluxRenderer(r -> {
            if (renderLogic != null) renderLogic.accept(this);
        });
        this.layout = new FluxLayout(this.renderer);
        this.controllers = new FluxControllers(this.renderer, this.layout, targetPlayerId);
    }

    public void setTargetPlayer(UUID playerId) {
        this.controllers.controllerSetPlayer(playerId);
    }

    // ==========================================
    // 生命周期与输入系统
    // ==========================================
    public void tick() { renderer.renderer_tick(); }
    public void destroy() { renderer.renderer_destroy(); }

    /**
     * 更新玩家射线 (由外部平台传入)
     * @param playerId 玩家 UUID
     * @param origin 射线起点 (如玩家眼睛坐标)
     * @param direction 射线方向
     */
    public void updatePlayerRay(UUID playerId, Vector3d origin, Vector3d direction) {
        renderer.renderer_updatePlayerRay(playerId, origin, direction);
    }

    public void registerPlayerClick(UUID playerId) { renderer.renderer_registerPlayerClick(playerId); }
    public void removePlayer(UUID playerId) { renderer.renderer_removePlayer(playerId); }

    // ==========================================
    // 屏幕与矩阵操作
    // ==========================================
    public boolean screen(FluxLocation loc, Vector3d xAxis, Vector3d yAxis, Vector3d zAxis, String screenId) { return renderer.renderer_screen(loc, xAxis, yAxis, zAxis, screenId); }
    public void endScreen() { renderer.renderer_endScreen(); }
    public boolean area(String id) { return renderer.renderer_area(id); }
    public void endArea() { renderer.renderer_endArea(); }
    public void pushMatrix() { renderer.renderer_pushMatrix(); }
    public void popMatrix() { renderer.renderer_popMatrix(); }
    public void translate(float x, float y, float z) { renderer.renderer_translate(x, y, z); }
    public void scale(float x, float y, float z) { renderer.renderer_scale(x, y, z); }
    public void rotateX(float angle) { renderer.renderer_rotateX(angle); }
    public void rotateY(float angle) { renderer.renderer_rotateY(angle); }
    public void rotateZ(float angle) { renderer.renderer_rotateZ(angle); }
    public void skew(float angleX, float angleY) { renderer.renderer_skew(angleX, angleY); }
    public void interpolation(int ticks) { renderer.renderer_interpolation(ticks); }

    // ==========================================
    // 窗口与排版系统
    // ==========================================
    public void beginWindow(String title, float startX, float startY) { layout.layoutBeginWindow(title, startX, startY); }
    public void endWindow() { layout.layoutEndWindow(); }
    public void sameLine() { layout.layoutSameLine(); }

    // ==========================================
    // 交互控件
    // ==========================================
    public void     text        (String id, String text) { controllers.controllerText(id, text); }
    public boolean  button      (String id, String text) { return controllers.controllerButton(id, text); }
    public boolean  checkbox    (String id, String label, boolean state) { return controllers.controllerCheckbox(id, label, state); }
    public void     colorEdit3  (String id, String label, FluxColor color)   { controllers.controllerColorEdit3(id, label, color); }
    public float    sliderFloat (String id, String label, float value, float min, float max) { return controllers.controllerSliderFloat(id, label, value, min, max); }

    // ==========================================
    // 基础图形与文本
    // ==========================================
    public void text            (String id, String text,    float scale)                    { renderer.renderer_text(id, text, scale); }
    public void text            (String id, String text,    float scale,    int opacity)    { renderer.renderer_text(id, text, scale, opacity); }
    public boolean buttonAbs    (String id, String text,    float x,        float y)        { return controllers.controllerButtonAbs(id, text, x, y);}
    public void rect            (String id, float width,    float height,   FluxColor color)    { renderer.renderer_rect(id, width, height, color); }

    public boolean checkboxAbs  (String id, String label,   boolean state,  float x,        float y)                            { return controllers.controllerCheckboxAbs(id, label, state, x, y); }
    public void triangle        (String id, Vector3d p1,    Vector3d p2,    Vector3d p3,    FluxColor color)                        { renderer.renderer_triangle(id, p1, p2, p3, color); }
    public void text            (String id, String text,    float scale,    int opacity,    FluxTextAlignment align)    { renderer.renderer_text(id, text, scale, opacity, align); }
    public void drawAbsRect     (String id, float x,        float y,        float w,        float h,            FluxColor color)    { renderer.renderer_drawAbsRect(id, x, y, w, h, color); }
    public void textAbs         (String id, String text,    float x,        float y,        float scale,        int opacity,    FluxTextAlignment align) { renderer.renderer_textAbs(id, text, x, y, scale, opacity, align); }
    public void drawAbsTriangle (String id, float x1,       float y1,       float x2,       float y2,           float x3,       float y3, FluxColor color) { renderer.renderer_drawAbsTriangle(id, x1, y1, x2, y2, x3, y3, color); }
    public float sliderFloatAbs (String id, String label,   float value,    float min,      float max,          float x,        float y) { return controllers.ControllerSliderFloatAbs(id, label, value, min, max, x, y); }

    // ==========================================
    // 底层碰撞检测
    // ==========================================
    public Set<UUID>    getHoveringPlayers  (float width,   float height) { return renderer.renderer_getHoveringPlayers(width, height); }
    public Set<UUID>    hitbox              (float width,   float height) { return renderer.renderer_hitbox(width, height); }
    public boolean      isHovered           (float width,   float height) { return renderer.renderer_isHovered(width, height); }
    public boolean      isHovered           (UUID playerId, float width, float height) { return renderer.renderer_isHovered(playerId, width, height); }
}