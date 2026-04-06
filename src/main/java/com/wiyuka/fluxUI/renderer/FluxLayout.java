package com.wiyuka.fluxUI.renderer;

import org.bukkit.Color;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector2f;

public class FluxLayout {
    private final FluxRenderer flux;

    private final Vector2f windowPos = new Vector2f();
    private float cursorX = 0f;
    private float cursorY = 0f;
    private float currLineHeight = 0f;
    private boolean isSameLine = false;
    private boolean isFirstItemOnLine = true;

    private float contentMaxX = 0f;
    private float contentMaxY = 0f;
    private float autoWindowWidth = 4.0f;
    private float autoWindowHeight = 3.0f;

    public final float TEXT_SCALE = 0.45f;
    public final float FRAME_HEIGHT = 0.22f;

    public final Vector2f WINDOW_PADDING = new Vector2f(0.15f, 0.15f);
    public final Vector2f ITEM_SPACING = new Vector2f(0.15f, 0.05f);
    public final Vector2f FRAME_PADDING = new Vector2f(0.15f, 0.0f);

    public final float TITLE_BAR_HEIGHT = 0.3f;
    public final float TEXT_OFFSET_Y = -0.05f;

    private final float TEXT_WIDTH_RATIO = 0.028f;

    public final Color colWindowBg = Color.fromARGB(240, 15, 15, 15);
    public final Color colTitleBg = Color.fromARGB(255, 45, 60, 90);
    public final Color colFrameBg = Color.fromARGB(255, 30, 45, 70);
    public final Color colSliderGrab = Color.fromARGB(255, 66, 150, 250);
    public final Color colButton = Color.fromARGB(255, 41, 74, 122);

    public FluxLayout(FluxRenderer flux) {
        this.flux = flux;
    }

    public void beginWindow(String title, float startX, float startY) {
        windowPos.set(startX, startY);

        flux.drawAbsRect("win_bg_" + title, startX, startY, autoWindowWidth, autoWindowHeight, colWindowBg);
        flux.drawAbsRect("win_titlebg_" + title, startX, startY, autoWindowWidth, TITLE_BAR_HEIGHT, colTitleBg);

        drawTextLeft("win_title_txt_" + title, "▼ " + title, startX + 0.08f, startY - TITLE_BAR_HEIGHT / 2f, TEXT_SCALE);

        cursorX = WINDOW_PADDING.x;
        cursorY = TITLE_BAR_HEIGHT + WINDOW_PADDING.y;
        currLineHeight = 0f;
        isSameLine = false;
        isFirstItemOnLine = true;

        contentMaxX = calcTextWidth("▼ " + title, TEXT_SCALE) + 0.3f;
        contentMaxY = cursorY;
    }

    public void endWindow() {
        autoWindowWidth = contentMaxX + WINDOW_PADDING.x;
        autoWindowHeight = contentMaxY + WINDOW_PADDING.y;
    }

    public void sameLine() {
        isSameLine = true;
    }

    public static class ItemBounds {
        public float absX, absY, w, h;
        public ItemBounds(float absX, float absY, float w, float h) {
            this.absX = absX; this.absY = absY; this.w = w; this.h = h;
        }
    }

    public ItemBounds allocateSpace(float width, float height) {
        if (!isSameLine) {
            cursorX = WINDOW_PADDING.x;
            if (!isFirstItemOnLine) {
                cursorY += currLineHeight + ITEM_SPACING.y;
            }
            currLineHeight = 0f;
        } else {
            cursorX += ITEM_SPACING.x;
        }

        float absX = windowPos.x + cursorX;
        float absY = windowPos.y - cursorY;

        currLineHeight = Math.max(currLineHeight, height);
        cursorX += width;

        contentMaxX = Math.max(contentMaxX, cursorX);
        contentMaxY = Math.max(contentMaxY, cursorY + currLineHeight);

        isSameLine = false;
        isFirstItemOnLine = false;
        return new ItemBounds(absX, absY, width, height);
    }

    public void drawTextLeft(String id, String text, float absLeftX, float absCenterY, float scale) {
        float textWidth = calcTextWidth(text, scale);
        flux.textAbs(id, text, absLeftX + textWidth / 2f, absCenterY + TEXT_OFFSET_Y, scale, 255, TextDisplay.TextAlignment.CENTER);
    }

    public void drawTextCenter(String id, String text, float absCenterX, float absCenterY, float scale) {
        flux.textAbs(id, text, absCenterX, absCenterY + TEXT_OFFSET_Y, scale, 255, TextDisplay.TextAlignment.CENTER);
    }

    public float calcTextWidth(String text, float scale) {
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

        return pixels * TEXT_WIDTH_RATIO * scale;
    }
}