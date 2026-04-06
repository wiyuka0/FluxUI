package com.wiyuka.fluxUI;

import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.Color;
import org.joml.Vector3f;

public class RenderUtils {

    public static void drawBox(String id, float w, float h, float d, Color color, Flux flux) {
        float hw = w / 2, hh = h / 2, hd = d / 2;
        Color frontC = darken(color, 0.85), sideC = darken(color, 0.7), botC = darken(color, 0.5);

        flux.pushMatrix();
        flux.translate(-hw, -hh, hd);
        flux.rect(id + "_front", w, h, frontC);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(hw, -hh, -hd);
        flux.rotateY(180);
        flux.rect(id + "_back", w, h, frontC);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(-hw, hh, hd);
        flux.rotateX(-90);
        flux.rect(id + "_top", w, d, color);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(-hw, -hh, -hd);
        flux.rotateX(90);
        flux.rect(id + "_bottom", w, d, botC);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(hw, -hh, hd);
        flux.rotateY(90);
        flux.rect(id + "_right", d, h, sideC);
        flux.popMatrix();

        flux.pushMatrix();
        flux.translate(-hw, -hh, -hd);
        flux.rotateY(-90);
        flux.rect(id + "_left", d, h, sideC);
        flux.popMatrix();
    }

    public static void drawQuad(String id, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, Color color, Flux flux) {
        flux.triangle(id + "_f1", p1, p2, p3, color);
        flux.triangle(id + "_f2", p1, p3, p4, color);
        flux.triangle(id + "_b1", p1, p3, p2, color);
        flux.triangle(id + "_b2", p1, p4, p3, color);
    }

    public static Color lerpColor(Color c1, Color c2, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));

        int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);
        int r = (int) (c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);

        return Color.fromARGB(a, r, g, b);
    }

    public static void drawPetal(String id, float length, float width, float bend, Color color, Flux flux) {
        Vector3f root = new Vector3f(0, 0, 0);
        Vector3f midLeft = new Vector3f(-width / 2, length * 0.5f, bend);
        Vector3f midRight = new Vector3f(width / 2, length * 0.5f, bend);
        Vector3f tip = new Vector3f(0, length, bend * 1.5f);
        Color dark = darken(color, 0.7);
        drawQuad(id + "_base", root, midRight, midLeft, root, dark, flux);
        drawQuad(id + "_tip", midLeft, midRight, tip, tip, color, flux);
    }

    public static void drawPyramid(String id, float baseSize, float height, Color color, Flux flux) {
        float h = baseSize / 2;
        Vector3f top = new Vector3f(0, height, 0);
        Vector3f v1 = new Vector3f(-h, 0, -h), v2 = new Vector3f(h, 0, -h);
        Vector3f v3 = new Vector3f(h, 0, h),   v4 = new Vector3f(-h, 0, h);

        drawDoubleSidedTriangle(id+"_s1", top, v1, v2, color, flux);
        drawDoubleSidedTriangle(id+"_s2", top, v2, v3, darken(color, 0.8), flux);
        drawDoubleSidedTriangle(id+"_s3", top, v3, v4, darken(color, 0.6), flux);
        drawDoubleSidedTriangle(id+"_s4", top, v4, v1, darken(color, 0.4), flux);
        drawDoubleSidedTriangle(id+"_b1", v1, v4, v3, darken(color, 0.4), flux);
        drawDoubleSidedTriangle(id+"_b2", v1, v3, v2, darken(color, 0.4), flux);
    }

    public static void drawFeather(String id, float length, float width, Color color, Flux flux) {
        Vector3f root = new Vector3f(0, 0, 0);
        Vector3f tip = new Vector3f(length, 0, 0);
        Vector3f top = new Vector3f(length * 0.3f, width / 2, 0);
        Vector3f bottom = new Vector3f(length * 0.3f, -width / 2, 0);

        flux.triangle(id + "_f1", root, bottom, top, color); flux.triangle(id + "_b1", root, top, bottom, color);
        flux.triangle(id + "_f2", top, bottom, tip, color);  flux.triangle(id + "_b2", top, tip, bottom, color);
    }

    public static void drawOctahedron(String id, float w, float h, Color color, Flux flux) {
        Vector3f top = new Vector3f(0, h, 0), bottom = new Vector3f(0, -h, 0);
        Vector3f m1 = new Vector3f(w, 0, w), m2 = new Vector3f(-w, 0, w);
        Vector3f m3 = new Vector3f(-w, 0, -w), m4 = new Vector3f(w, 0, -w);

        Color c2 = darken(color, 0.85), c3 = darken(color, 0.7), c4 = darken(color, 0.55);

        drawDoubleSidedTriangle(id+"_t1", top, m1, m2, color, flux); drawDoubleSidedTriangle(id+"_t2", top, m2, m3, c2, flux);
        drawDoubleSidedTriangle(id+"_t3", top, m3, m4, c3, flux);    drawDoubleSidedTriangle(id+"_t4", top, m4, m1, c4, flux);
        drawDoubleSidedTriangle(id+"_b1", bottom, m2, m1, c2, flux); drawDoubleSidedTriangle(id+"_b2", bottom, m3, m2, c3, flux);
        drawDoubleSidedTriangle(id+"_b3", bottom, m4, m3, c4, flux); drawDoubleSidedTriangle(id+"_b4", bottom, m1, m4, color, flux);
    }

    public static void drawFlatRing(String id, float innerRadius, float outerRadius, int segments, Color color, Flux flux) {
        float angleStep = (float) (2 * Math.PI / segments);
        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep, a2 = (i + 1) * angleStep;
            Vector3f p1 = new Vector3f((float) Math.cos(a1) * innerRadius, (float) Math.sin(a1) * innerRadius, 0);
            Vector3f p2 = new Vector3f((float) Math.cos(a2) * innerRadius, (float) Math.sin(a2) * innerRadius, 0);
            Vector3f p3 = new Vector3f((float) Math.cos(a1) * outerRadius, (float) Math.sin(a1) * outerRadius, 0);
            Vector3f p4 = new Vector3f((float) Math.cos(a2) * outerRadius, (float) Math.sin(a2) * outerRadius, 0);
            flux.triangle(id + "_f1_" + i, p1, p3, p4, color); flux.triangle(id + "_b1_" + i, p1, p4, p3, color);
            flux.triangle(id + "_f2_" + i, p1, p4, p2, color); flux.triangle(id + "_b2_" + i, p1, p2, p4, color);
        }
    }

    private static void drawDoubleSidedTriangle(String id, Vector3f p1, Vector3f p2, Vector3f p3, Color color, Flux flux) {
        flux.triangle(id + "_front", p1, p2, p3, color);
        flux.triangle(id + "_back", p1, p3, p2, color);
    }

    public static Color darken(Color color, double factor) {
        return Color.fromARGB(color.getAlpha(),
                Math.min(255, Math.max(0, (int) (color.getRed() * factor))),
                Math.min(255, Math.max(0, (int) (color.getGreen() * factor))),
                Math.min(255, Math.max(0, (int) (color.getBlue() * factor))));
    }
}