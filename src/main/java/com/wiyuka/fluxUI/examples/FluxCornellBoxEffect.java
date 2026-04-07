package com.wiyuka.fluxUI.examples;

import com.wiyuka.embree.*;
import com.wiyuka.fluxUI.renderer.Flux;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class FluxCornellBoxEffect extends BaseEffect {

    private final Location centerLoc;
    private final Vector3f xAxis = new Vector3f(1, 0, 0);
    private final Vector3f yAxis = new Vector3f(0, 1, 0);
    private final Vector3f zAxis = new Vector3f(0, 0, 1);

    private final int FLOOR_RES = 55;
    private final int CUBE_RES = 3;
    private final int SAMPLES = 6400;
    private final int MAX_BOUNCES = 2;

    private EmbreeDevice embreeDevice;

    private final CubeState cube1 = new CubeState(new Vector3f(1, 1, 1), new Vector3f(1.0f, 0.3f, 0.3f));
    private final CubeState cube2 = new CubeState(new Vector3f(0.8f, 0.8f, 0.8f), new Vector3f(0.3f, 1.0f, 0.3f));
    private final CubeState cube3 = new CubeState(new Vector3f(0.6f, 1.2f, 0.6f), new Vector3f(0.3f, 0.5f, 1.0f));

    private final List<Vector3f> activePlayersRelFeet = new ArrayList<>();
    private final List<Vector3f> triangleMaterials = new ArrayList<>();

    public FluxCornellBoxEffect(Player player) {
        super(player);
        this.centerLoc = player.getLocation().clone().add(player.getLocation().getDirection().multiply(7.0));
        this.centerLoc.add(0, 1.5, 0);

        System.load("I:\\downloads\\embree-4.4.0\\bin\\embree4.dll");
        this.embreeDevice = new EmbreeDevice();
    }

    @Override
    protected void render(Flux flux) {
        long t = ticks;
        updatePhysics(t);

        EmbreeScene scene = new EmbreeScene(embreeDevice);
        EmbreeTriangleMesh mesh = buildDynamicMesh(t);
        scene.attachGeometry(mesh);
        scene.commit();

        flux.screen(FluxUtil.locationToFlux(centerLoc), toVector3d(xAxis), toVector3d(yAxis), toVector3d(zAxis), "embree_reality_pt");
        flux.pushMatrix();

        float pitchX = 20f;
        float yawY = t * 0.5f;

        flux.rotateX(pitchX);
        flux.rotateY(yawY);
        flux.scale(0.5f, 0.5f, 0.5f);

        drawDynamicPlaneConcurrent(flux, scene, "floor", new Vector3f(0, -1.5f, 0), 12f, 12f, -90, 0, FLOOR_RES, new Vector3f(0.9f, 0.9f, 0.9f));
        drawDynamicCubeUIConcurrent(flux, scene, "cube1", cube1);
        drawDynamicCubeUIConcurrent(flux, scene, "cube2", cube2);
        drawDynamicCubeUIConcurrent(flux, scene, "cube3", cube3);

        flux.pushMatrix();
        flux.translate(0, 4.8f, 0);
        flux.rotateX(90f);
        flux.translate(-1.5f, -1.5f, 0);
        flux.rect("light_source", 3.0f, 3.0f, new Flux.FluxColor(255, 255, 255, 255));
        flux.popMatrix();

        flux.popMatrix();
        flux.endScreen();

        scene.close();
        mesh.close();
    }

    private EmbreeTriangleMesh buildDynamicMesh(long t) {
        triangleMaterials.clear();
        Triangulator tri = new Triangulator();

        tri.addBox(new Vector3f(0, 4.8f, 0), new Vector3f(1.5f, 0.05f, 1.5f), 0, new Vector3f(25.0f, 25.0f, 25.0f));

        tri.addBox(new Vector3f(0, -1.6f, 0), new Vector3f(6.0f, 0.1f, 6.0f), 0, new Vector3f(0.9f, 0.9f, 0.9f));

        tri.addBox(cube1.pos, cube1.halfSize, cube1.yaw, cube1.color);
        tri.addBox(cube2.pos, cube2.halfSize, cube2.yaw, cube2.color);
        tri.addBox(cube3.pos, cube3.halfSize, cube3.yaw, cube3.color);

        for (Vector3f relFeet : activePlayersRelFeet) {
            float currentYaw = t * 0.5f;
            tri.addPlayerBox(relFeet, currentYaw);
        }

        EmbreeTriangleMesh mesh = new EmbreeTriangleMesh(embreeDevice);
        mesh.setVertices(tri.getVertices());
        mesh.setIndices(tri.getIndices());
        mesh.commit();
        return mesh;
    }

    private void updatePhysics(long t) {
        float time = t * 0.05f;
        cube1.pos.set((float)Math.cos(time)*3.5f, -0.5f, (float)Math.sin(time)*3.5f);
        cube1.yaw = t * 2f;
        cube2.pos.set(0, -0.7f + Math.abs((float)Math.sin(time * 2.5f)) * 3.0f, 0);
        cube2.yaw = t * 3f;
        float slide = (float)Math.sin(time * 1.5f) * 3f;
        cube3.pos.set(slide, -0.3f, -slide);
        cube3.yaw = -t * 4f;

        activePlayersRelFeet.clear();
        for (Player p : centerLoc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(centerLoc) < 144) {
                Location pLoc = p.getLocation();
                activePlayersRelFeet.add(new Vector3f(
                        (float)(pLoc.getX() - centerLoc.getX()),
                        (float)(pLoc.getY() - centerLoc.getY()),
                        (float)(pLoc.getZ() - centerLoc.getZ())
                ));
            }
        }
    }

    private void drawDynamicCubeUIConcurrent(Flux flux, EmbreeScene scene, String id, CubeState state) {
        float w = state.halfSize.x * 2f, h = state.halfSize.y * 2f, d = state.halfSize.z * 2f, yaw = state.yaw;
        Vector3f c = state.pos;
        Vector3f clr = state.color;
        drawDynamicPlaneConcurrent(flux, scene, id+"_f", new Vector3f(0, 0, state.halfSize.z).rotateY((float)Math.toRadians(yaw)).add(c), w, h, 0, yaw, CUBE_RES, clr);
        drawDynamicPlaneConcurrent(flux, scene, id+"_b", new Vector3f(0, 0, -state.halfSize.z).rotateY((float)Math.toRadians(yaw)).add(c), w, h, 0, yaw + 180, CUBE_RES, clr);
        drawDynamicPlaneConcurrent(flux, scene, id+"_r", new Vector3f(state.halfSize.x, 0, 0).rotateY((float)Math.toRadians(yaw)).add(c), d, h, 0, yaw + 90, CUBE_RES, clr);
        drawDynamicPlaneConcurrent(flux, scene, id+"_l", new Vector3f(-state.halfSize.x, 0, 0).rotateY((float)Math.toRadians(yaw)).add(c), d, h, 0, yaw - 90, CUBE_RES, clr);
        drawDynamicPlaneConcurrent(flux, scene, id+"_t", new Vector3f(0, state.halfSize.y, 0).add(c), w, d, -90, yaw, CUBE_RES, clr);
    }

    private void drawDynamicPlaneConcurrent(Flux flux, EmbreeScene scene, String id, Vector3f center, float width, float height, float pitchX, float yawY, int res, Vector3f baseColor) {
        int totalQuads = res * res;
        int[] resultColors = new int[totalQuads];

        float stepW = width / res, stepH = height / res;
        Vector3f normal = new Vector3f(0, 0, 1).rotateX((float) Math.toRadians(pitchX)).rotateY((float) Math.toRadians(yawY)).normalize();

        int cores = Runtime.getRuntime().availableProcessors();
        IntStream.range(0, cores).parallel().forEach(threadId -> {
            try (RayContext ctx = new RayContext()) {
                ThreadLocalRandom rand = ThreadLocalRandom.current();

                int startIdx = (totalQuads * threadId) / cores;
                int endIdx = (totalQuads * (threadId + 1)) / cores;

                for (int index = startIdx; index < endIdx; index++) {
                    int i = index / res;
                    int j = index % res;

                    float drawX = -width / 2.0f + i * stepW;
                    float drawY = -height / 2.0f + j * stepH;

                    Vector3f worldPos = new Vector3f(drawX + stepW / 2.0f, drawY + stepH / 2.0f, 0)
                            .rotateX((float) Math.toRadians(pitchX)).rotateY((float) Math.toRadians(yawY)).add(center);

                    Vector3f colorSum = new Vector3f();

                    for (int s = 0; s < SAMPLES; s++) {
                        Vector3f rayDir = cosineSampleHemisphere(normal, rand);
                        Vector3f rayOrigin = new Vector3f(worldPos).add(new Vector3f(normal).mul(0.001f));
                        Vector3f Li = traceEmbreeRay(ctx, scene, rand, rayOrigin, rayDir, 1);
                        colorSum.add(Li);
                    }

                    colorSum.div(SAMPLES);
                    colorSum.mul(Math.max(0f, baseColor.x), Math.max(0f, baseColor.y), Math.max(0f, baseColor.z));

                    float exposure = 0.8f;
                    colorSum.x = 1.0f - (float) Math.exp(-colorSum.x * exposure);
                    colorSum.y = 1.0f - (float) Math.exp(-colorSum.y * exposure);
                    colorSum.z = 1.0f - (float) Math.exp(-colorSum.z * exposure);

                    colorSum.x = (float) Math.pow(Math.max(0f, colorSum.x), 1.0 / 2.2);
                    colorSum.y = (float) Math.pow(Math.max(0f, colorSum.y), 1.0 / 2.2);
                    colorSum.z = (float) Math.pow(Math.max(0f, colorSum.z), 1.0 / 2.2);

                    int r = Math.max(0, Math.min(255, (int) (colorSum.x * 255)));
                    int g = Math.max(0, Math.min(255, (int) (colorSum.y * 255)));
                    int b = Math.max(0, Math.min(255, (int) (colorSum.z * 255)));

                    resultColors[index] = (r << 16) | (g << 8) | b;
                }
            }
        });

        flux.pushMatrix();
        flux.translate(center.x, center.y, center.z);
        if (yawY != 0) flux.rotateY(yawY);
        if (pitchX != 0) flux.rotateX(pitchX);

        float drawStepW = stepW * 1.03f, drawStepH = stepH * 1.03f;
        int quadId = 0;

        for (int index = 0; index < totalQuads; index++) {
            int i = index / res;
            int j = index % res;
            float drawX = -width / 2.0f + i * stepW;
            float drawY = -height / 2.0f + j * stepH;

            int bitColor = resultColors[index];
            int r = (bitColor >> 16) & 0xFF;
            int g = (bitColor >> 8) & 0xFF;
            int b = bitColor & 0xFF;

            flux.pushMatrix();
            flux.translate(drawX, drawY, 0);
            flux.rect(id + "_" + quadId++, drawStepW, drawStepH, new Flux.FluxColor(255, r, g, b));
            flux.popMatrix();
        }
        flux.popMatrix();
    }

    private Vector3f traceEmbreeRay(RayContext ctx, EmbreeScene scene, ThreadLocalRandom rand, Vector3f ro, Vector3f rd, int depth) {
        Hit hit = new Hit();
        Ray ray = new Ray(ro.x, ro.y, ro.z, rd.x, rd.y, rd.z);

        if (scene.intersect(ctx, ray, hit)) {
            Vector3f albedo = triangleMaterials.get(hit.primID);

            if (albedo.x > 5.0f) return new Vector3f(albedo);
            if (depth >= MAX_BOUNCES) return new Vector3f(0.015f, 0.015f, 0.02f);

            float distance = ray.tfar;
            Vector3f hitPos = new Vector3f(ro).add(new Vector3f(rd).mul(distance));
            Vector3f hitNormal = new Vector3f(hit.NgX, hit.NgY, hit.NgZ).normalize();

            if (hitNormal.dot(rd) > 0) hitNormal.mul(-1.0f);

            Vector3f nextDir = cosineSampleHemisphere(hitNormal, rand);
            Vector3f bounceRo = new Vector3f(hitPos).add(new Vector3f(hitNormal).mul(0.001f));

            Vector3f incomingLight = traceEmbreeRay(ctx, scene, rand, bounceRo, nextDir, depth + 1);

            return new Vector3f(albedo).mul(incomingLight);
        }

        float skyGradient = 0.5f * (rd.y + 1.0f);
        return new Vector3f(0.01f, 0.015f, 0.025f).mul(1.0f - skyGradient)
                .add(new Vector3f(0.04f, 0.06f, 0.1f).mul(skyGradient));
    }

    private Vector3f cosineSampleHemisphere(Vector3f n, ThreadLocalRandom rand) {
        float r1 = rand.nextFloat();
        float r2 = rand.nextFloat();
        float phi = (float) (2.0 * Math.PI * r1);
        float x = (float) (Math.cos(phi) * Math.sqrt(r2));
        float y = (float) (Math.sin(phi) * Math.sqrt(r2));
        float z = (float) Math.sqrt(1.0 - r2);

        Vector3f up = Math.abs(n.z) < 0.999 ? new Vector3f(0,0,1) : new Vector3f(1,0,0);
        Vector3f tangent = new Vector3f(up).cross(n).normalize();
        Vector3f bitangent = new Vector3f(n).cross(tangent);

        return new Vector3f()
                .add(new Vector3f(tangent).mul(x))
                .add(new Vector3f(bitangent).mul(y))
                .add(new Vector3f(n).mul(z)).normalize();
    }

    private class Triangulator {
        private final List<Float> vList = new ArrayList<>();
        private final List<Integer> iList = new ArrayList<>();
        private int currentVertex = 0;

        public void addBox(Vector3f center, Vector3f halfSize, float yaw, Vector3f colorAlbedo) {
            Vector3f[] v = new Vector3f[8];
            int idx = 0;
            for (float x : new float[]{-1, 1}) {
                for (float y : new float[]{-1, 1}) {
                    for (float z : new float[]{-1, 1}) {
                        Vector3f p = new Vector3f(x * halfSize.x, y * halfSize.y, z * halfSize.z);
                        if (yaw != 0) p.rotateY((float)Math.toRadians(yaw));
                        p.add(center);
                        vList.add(p.x); vList.add(p.y); vList.add(p.z);
                        v[idx++] = p;
                    }
                }
            }
            int[][] faces = {
                    {0,2,6,4}, {1,5,7,3}, {0,4,5,1}, {2,3,7,6}, {0,1,3,2}, {4,6,7,5}
            };
            for (int[] f : faces) {
                iList.add(currentVertex + f[0]); iList.add(currentVertex + f[1]); iList.add(currentVertex + f[2]);
                triangleMaterials.add(colorAlbedo);
                iList.add(currentVertex + f[0]); iList.add(currentVertex + f[2]); iList.add(currentVertex + f[3]);
                triangleMaterials.add(colorAlbedo);
            }
            currentVertex += 8;
        }

        public void addPlayerBox(Vector3f relFeet, float currentUIYaw) {
            float r = 0.3f;
            float h = 1.8f;

            int idx = 0;
            Vector3f[] v = new Vector3f[8];

            for (float dx : new float[]{-r, r}) {
                for (float dy : new float[]{0, h}) {
                    for (float dz : new float[]{-r, r}) {
                        Vector3f p = new Vector3f(relFeet.x + dx, relFeet.y + dy, relFeet.z + dz);

                        p.rotateX((float) Math.toRadians(-20f));
                        p.rotateY((float) Math.toRadians(-currentUIYaw));
                        p.mul(2.0f);

                        vList.add(p.x); vList.add(p.y); vList.add(p.z);
                        v[idx++] = p;
                    }
                }
            }

            Vector3f playerColor = new Vector3f(0.12f, 0.12f, 0.15f);

            int[][] faces = {
                    {0,2,6,4}, {1,5,7,3}, {0,4,5,1}, {2,3,7,6}, {0,1,3,2}, {4,6,7,5}
            };

            for (int[] f : faces) {
                iList.add(currentVertex + f[0]); iList.add(currentVertex + f[1]); iList.add(currentVertex + f[2]);
                triangleMaterials.add(playerColor);
                iList.add(currentVertex + f[0]); iList.add(currentVertex + f[2]); iList.add(currentVertex + f[3]);
                triangleMaterials.add(playerColor);
            }
            currentVertex += 8;
        }

        public float[] getVertices() {
            float[] arr = new float[vList.size()];
            for (int i=0; i<vList.size(); i++) arr[i] = vList.get(i);
            return arr;
        }
        public int[] getIndices() {
            int[] arr = new int[iList.size()];
            for (int i=0; i<iList.size(); i++) arr[i] = iList.get(i);
            return arr;
        }
    }

    public Vector3d toVector3d(Vector3f v) { return new Vector3d(v.x, v.y, v.z); }

    private static class CubeState {
        Vector3f pos = new Vector3f(), halfSize, color; float yaw = 0;
        public CubeState(Vector3f h, Vector3f c) { this.halfSize = h; this.color = c; }
    }
}