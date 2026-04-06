package com.wiyuka.fluxUI.renderer;

import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

public class FluxControllers {
    private final FluxRenderer flux;
    private final FluxLayout layout;
    private Player player;

    public FluxControllers(FluxRenderer flux, FluxLayout layout, Player player) {
        this.flux = flux;
        this.layout = layout;
        this.player = player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void text(String id, String text) {
        float textWidth = layout.calcTextWidth(text, layout.TEXT_SCALE);
        FluxLayout.ItemBounds bounds = layout.allocateSpace(textWidth, layout.FRAME_HEIGHT);
        layout.drawTextLeft(id, text, bounds.absX, bounds.absY - bounds.h / 2f, layout.TEXT_SCALE);
    }

    public boolean button(String id, String text) {
        float textWidth = layout.calcTextWidth(text, layout.TEXT_SCALE);
        float width = textWidth + (layout.FRAME_PADDING.x * 2);

        FluxLayout.ItemBounds bounds = layout.allocateSpace(width, layout.FRAME_HEIGHT);

        flux.drawAbsRect(id + "_bg", bounds.absX, bounds.absY, bounds.w, bounds.h, layout.colButton);
        layout.drawTextCenter(id + "_txt", text, bounds.absX + bounds.w / 2f, bounds.absY - bounds.h / 2f, layout.TEXT_SCALE);

        flux.pushMatrix();
        flux.translate(bounds.absX, bounds.absY - bounds.h, 0);

        boolean isHovered = flux.isHovered(player, bounds.w, bounds.h);
        Color hoverOverlay = isHovered ? Color.fromARGB(50, 255, 255, 255) : Color.fromARGB(0, 0, 0, 0);
        flux.rect(id + "_hover", bounds.w, bounds.h, hoverOverlay);

        boolean clicked = flux.hitbox(bounds.w, bounds.h).contains(player);
        flux.popMatrix();

        return clicked;
    }

    public boolean buttonAbs(String id, String text, float x, float y) {
        float textWidth = layout.calcTextWidth(text, layout.TEXT_SCALE);
        float width = textWidth + (layout.FRAME_PADDING.x * 2);
        float height = layout.FRAME_HEIGHT;

        flux.pushMatrix();
        flux.translate(x, y, 0);

        flux.rect(id + "_bg", width, height, layout.colButton);
        flux.textAbs(id + "_txt", text, width / 2f, -height / 2f, layout.TEXT_SCALE, 255, TextDisplay.TextAlignment.CENTER);

        boolean isHovered = flux.isHovered(player, width, height);
        if (isHovered) {
            flux.rect(id + "_hover", width, height, Color.fromARGB(50, 255, 255, 255));
        }
        boolean clicked = flux.hitbox(width, height).contains(player);

        flux.popMatrix();
        return clicked;
    }

    public boolean checkboxAbs(String id, String label, boolean state, float x, float y) {
        float boxSize = layout.FRAME_HEIGHT * 0.85f;
        float textWidth = layout.calcTextWidth(label, layout.TEXT_SCALE);
        float totalWidth = boxSize + layout.ITEM_SPACING.x + textWidth;
        float height = layout.FRAME_HEIGHT;
        float boxOffsetY = (height - boxSize) / 2f;
        flux.drawAbsRect(id + "_bg", x, y - boxOffsetY, boxSize, boxSize, layout.colFrameBg);
        if (state) {
            float pad = 0.04f;
            flux.drawAbsRect(id + "_tick", x + pad, y - boxOffsetY - pad, boxSize - pad * 2, boxSize - pad * 2, layout.colSliderGrab);
        }
        layout.drawTextLeft(id + "_txt", label, x + boxSize + layout.ITEM_SPACING.x, y - height / 2f, layout.TEXT_SCALE);
        flux.pushMatrix();
        flux.translate(x, y - height, 0);
        boolean isHovered = flux.isHovered(player, totalWidth, height);
        Color hoverOverlay = isHovered ? Color.fromARGB(30, 255, 255, 255) : Color.fromARGB(0, 0, 0, 0);
        flux.rect(id + "_hover", totalWidth, height, hoverOverlay);
        if (flux.hitbox(totalWidth, height).contains(player)) state = !state;
        flux.popMatrix();
        return state;
    }
    public float sliderFloatAbs(String id, String label, float value, float min, float max, float x, float y) {
        float trackW = 2.5f;
        float height = layout.FRAME_HEIGHT;

        flux.drawAbsRect(id + "_bg", x, y, trackW, height, layout.colFrameBg);

        float percent = Math.max(0, Math.min(1, (value - min) / (max - min)));
        float fillW = percent * trackW;
        if (fillW > 0) {
            flux.drawAbsRect(id + "_fill", x, y, fillW, height, layout.colSliderGrab);
        }
        String valText = String.format("%.3f", value);
        layout.drawTextCenter(id + "_val", valText, x + trackW / 2f, y - height / 2f, layout.TEXT_SCALE);
        layout.drawTextLeft(id + "_txt", label, x + trackW + layout.ITEM_SPACING.x, y - height / 2f, layout.TEXT_SCALE);

        flux.pushMatrix();
        flux.translate(x, y - height, 0);

        boolean isHovered = flux.isHovered(player, trackW, height);
        if (isHovered) {
            flux.rect(id + "_hover", trackW, height, Color.fromARGB(30, 255, 255, 255));
        }
        if (flux.hitbox(trackW / 2, height).contains(player)) {
            value = Math.max(min, value - (max - min) * 0.05f);
        }

        flux.translate(trackW / 2, 0, 0);
        if (flux.hitbox(trackW / 2, height).contains(player)) {
            value = Math.min(max, value + (max - min) * 0.05f);
        }

        flux.popMatrix();
        return value;
    }
    public boolean checkbox(String id, String label, boolean state) {
        float boxSize = layout.FRAME_HEIGHT * 0.85f;
        float textWidth = layout.calcTextWidth(label, layout.TEXT_SCALE);

        FluxLayout.ItemBounds bounds = layout.allocateSpace(boxSize + layout.ITEM_SPACING.x + textWidth, layout.FRAME_HEIGHT);
        float boxOffsetY = (layout.FRAME_HEIGHT - boxSize) / 2f;

        flux.drawAbsRect(id + "_bg", bounds.absX, bounds.absY - boxOffsetY, boxSize, boxSize, layout.colFrameBg);

        if (state) {
            float pad = 0.04f;
            flux.drawAbsRect(id + "_tick", bounds.absX + pad, bounds.absY - boxOffsetY - pad, boxSize - pad * 2, boxSize - pad * 2, layout.colSliderGrab);
        }

        layout.drawTextLeft(id + "_txt", label, bounds.absX + boxSize + layout.ITEM_SPACING.x, bounds.absY - layout.FRAME_HEIGHT / 2f, layout.TEXT_SCALE);

        flux.pushMatrix();
        flux.translate(bounds.absX, bounds.absY - bounds.h, 0);

        boolean isHovered = flux.isHovered(player, bounds.w, bounds.h);
        Color hoverOverlay = isHovered ? Color.fromARGB(30, 255, 255, 255) : Color.fromARGB(0, 0, 0, 0);
        flux.rect(id + "_hover", bounds.w, bounds.h, hoverOverlay);

        if (flux.hitbox(bounds.w, bounds.h).contains(player)) {
            state = !state;
        }
        flux.popMatrix();

        return state;
    }

    public float sliderFloat(String id, String label, float value, float min, float max) {
        float trackW = 2.5f;
        float textWidth = layout.calcTextWidth(label, layout.TEXT_SCALE);

        FluxLayout.ItemBounds bounds = layout.allocateSpace(trackW + layout.ITEM_SPACING.x + textWidth, layout.FRAME_HEIGHT);

        flux.drawAbsRect(id + "_bg", bounds.absX, bounds.absY, trackW, bounds.h, layout.colFrameBg);

        float percent = Math.max(0, Math.min(1, (value - min) / (max - min)));
        float fillW = percent * trackW;
        if (fillW > 0) {
            flux.drawAbsRect(id + "_fill", bounds.absX, bounds.absY, fillW, bounds.h, layout.colSliderGrab);
        }

        String valText = String.format("%.3f", value);
        layout.drawTextCenter(id + "_val", valText, bounds.absX + trackW / 2f, bounds.absY - bounds.h / 2f, layout.TEXT_SCALE);
        layout.drawTextLeft(id + "_txt", label, bounds.absX + trackW + layout.ITEM_SPACING.x, bounds.absY - bounds.h / 2f, layout.TEXT_SCALE);

        flux.pushMatrix();
        flux.translate(bounds.absX, bounds.absY - bounds.h, 0);
        if (flux.hitbox(trackW / 2, bounds.h).contains(player)) {
            value = Math.max(min, value - (max - min) * 0.05f);
        }
        flux.translate(trackW / 2, 0, 0);
        if (flux.hitbox(trackW / 2, bounds.h).contains(player)) {
            value = Math.min(max, value + (max - min) * 0.05f);
        }
        flux.popMatrix();

        return value;
    }

    public void colorEdit3(String id, String label, Color color) {
        float boxSize = layout.FRAME_HEIGHT;
        float textWidth = layout.calcTextWidth(label, layout.TEXT_SCALE);

        FluxLayout.ItemBounds bounds = layout.allocateSpace(boxSize + layout.ITEM_SPACING.x + textWidth, layout.FRAME_HEIGHT);

        float boxOffsetY = (layout.FRAME_HEIGHT - boxSize) / 2f;
        flux.drawAbsRect(id + "_preview", bounds.absX, bounds.absY - boxOffsetY, boxSize, boxSize, color);

        layout.drawTextLeft(id + "_txt", label, bounds.absX + boxSize + layout.ITEM_SPACING.x, bounds.absY - layout.FRAME_HEIGHT / 2f, layout.TEXT_SCALE);
    }
}