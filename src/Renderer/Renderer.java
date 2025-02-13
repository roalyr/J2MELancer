package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;
import java.util.Vector;
import Constants.Common;

public class Renderer {

    public static final int BACKGROUND_COLOR = 0xFF000000;

    private int width;
    private int height;

    // Pre-calculated half of screen width/height in fixed Q24.8
    private long precalc_halfW_Q24_8;
    private long precalc_halfH_Q24_8;

    // Clipping planes in Q24.8
    private final long Z_NEAR_Q24_8;
    private final long Z_FAR_Q24_8;

    private Vector renderables;

    // The main color buffer (ARGB)
    private int[] frameBuffer;

    // The new depth buffer, storing camera-space Z in fixed point
    private long[] depthBuffer;

    private long[] origin;
    private long[] centerCam;

    // Scratch arrays for transformations
    private long[] scratch3a = new long[3];
    private long[] scratch3b = new long[3];
    private long[] scratch4a = new long[4];
    private long[] scratch4b = new long[4];
    private int[] reusableScreenCoords0 = new int[3];
    private int[] reusableScreenCoords1 = new int[3];

    public Renderer() {
        this.width = SharedData.display_width;
        this.height = SharedData.display_height;

        this.frameBuffer = new int[width * height];
        // Create the depth buffer, same size
        this.depthBuffer = new long[width * height];

        this.renderables = new Vector();

        this.Z_NEAR_Q24_8 = FixedBaseMath.toFixed(Common.Z_NEAR);
        this.Z_FAR_Q24_8  = FixedBaseMath.toFixed(Common.Z_FAR);
    }

    public void setRenderables(Vector renderables, long halfWidth, long halfHeight) {
        this.renderables = renderables;
        this.precalc_halfW_Q24_8 = halfWidth;
        this.precalc_halfH_Q24_8 = halfHeight;
    }

    public Vector getRenderables() {
        return renderables;
    }

    /**
     * Clear color and depth buffers
     */
    public void clearBuffers(Graphics g) {
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);

        // Reset frameBuffer
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = BACKGROUND_COLOR;
        }

        // Reset depthBuffer to a very large value
        // so that any drawn fragment is guaranteed to be closer.
        for (int i = 0; i < depthBuffer.length; i++) {
            depthBuffer[i] = Long.MAX_VALUE;
        }
    }

    public void renderScene(Graphics g, long[] viewMatrix) {
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            long[] local = getLocalTransform(obj);
            long[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

            if (obj.material == null) {
                FixedMatMath.releaseMatrix(local);
                FixedMatMath.releaseMatrix(finalMatrix);
                continue;
            }

            if (obj.material.renderType == RenderEffects.TYPE_VERTICES) {
                drawVertices(finalMatrix, obj);
            } else {
                drawEdges(finalMatrix, obj);
            }

            FixedMatMath.releaseMatrix(local);
            FixedMatMath.releaseMatrix(finalMatrix);
        }

        g.drawRGB(frameBuffer, 0, width, 0, 0, width, height, true);
        SharedData.renderables_num = getRenderables().size();
    }

    private long[] getLocalTransform(SceneObject obj) {
        long[] local = FixedMatMath.createIdentity4x4();

        // Translate
        long[] m = FixedMatMath.createTranslation4x4(obj.tx, obj.ty, obj.tz);
        long[] temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Rotate Z
        m = FixedMatMath.createRotationZ4x4(obj.rotZ);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Rotate Y
        m = FixedMatMath.createRotationY4x4(obj.rotY);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Rotate X
        m = FixedMatMath.createRotationX4x4(obj.rotX);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Scale
        m = FixedMatMath.createScale4x4(obj.scale, obj.scale, obj.scale);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        return local;
    }

    private void drawEdges(long[] finalM, SceneObject obj) {
        Material mat = obj.material;
        int[][] edges = obj.model.edges;
        long[][] verts = obj.model.vertices;

        long nearQ = mat.nearMarginQ;
        long farQ = mat.farMarginQ;
        long fadeNearQ = mat.fadeDistanceNearQ;
        long fadeFarQ = mat.fadeDistanceFarQ;

        int nearColor = mat.colorNear;
        int farColor  = mat.colorFar;
        int ditherLevel = mat.ditherLevel;
        int shape = mat.primitiveShape;

        origin = new long[] {0, 0, 0, FixedBaseMath.toFixed(1.0f)};
        centerCam = new long[4];
        FixedMatMath.transformPoint(finalM, origin, centerCam);
        long centerCamZ = centerCam[2];

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            // Transform endpoints
            FixedMatMath.transformPoint(finalM, verts[i0], scratch4a);
            FixedMatMath.transformPoint(finalM, verts[i1], scratch4b);

            // Project each endpoint to screen (primarily for x/y).
            int[] screenP0 = projectPointToScreen(scratch3a, scratch4a, reusableScreenCoords0);
            int[] screenP1 = projectPointToScreen(scratch3b, scratch4b, reusableScreenCoords1);

            if (screenP0 == null || screenP1 == null) {
                continue; // behind the camera or invalid
            }

            // Camera-space z of endpoints
            long distA = scratch4a[2];
            long distB = scratch4b[2];
            // For fade color, we can use midpoint or any heuristic:
            long distMid = FixedBaseMath.fixedDiv(
                    FixedBaseMath.fixedAdd(distA, distB),
                    FixedBaseMath.toFixed(2.0f)
            );

            // Distance fade
            int alphaFade = RenderEffects.computeFadeAlpha(distMid, nearQ, farQ, fadeNearQ, fadeFarQ);
            if (alphaFade <= 0) {
                continue; // fully faded
            }
            // Interpolate color
            int blendedRGB = RenderEffects.interpolateColor(distMid, nearQ, farQ, nearColor, farColor);

            // Combine with local alpha
            int alphaOrig = (blendedRGB >>> 24) & 0xFF;
            int alphaCombined = (alphaOrig * alphaFade) >> 8;

            int localAlpha0 = RenderEffects.computeLocalAlphaFromCameraSpace(distA, centerCamZ, obj.model.boundingSphereRadius);
            int localAlpha1 = RenderEffects.computeLocalAlphaFromCameraSpace(distB, centerCamZ, obj.model.boundingSphereRadius);
            int localAlpha  = (localAlpha0 + localAlpha1) >> 1;

            alphaCombined = (alphaCombined * localAlpha) >> 8;
            if (alphaCombined > 255) alphaCombined = 255;

            int r = (blendedRGB >>> 16) & 0xFF;
            int g = (blendedRGB >>> 8)  & 0xFF;
            int b = (blendedRGB       )  & 0xFF;

            // Final ARGB color with "darkening alpha" but we won't blend on the framebuffer, just store
            int finalColor = (alphaCombined << 24) | (r << 16) | (g << 8) | b;

            // Draw the line with depth test, passing in the actual camera‐space Z for endpoints
            RenderLine.drawLineDither(
                    shape,
                    width, height,
                    frameBuffer,
                    depthBuffer, // pass depthBuffer
                    screenP0[0], screenP0[1], distA,
                    screenP1[0], screenP1[1], distB,
                    finalColor,
                    ditherLevel
            );
        }
    }

    private void drawVertices(long[] finalM, SceneObject obj) {
        Material mat = obj.material;
        if (mat == null) {
            return;
        }
        long nearQ = mat.nearMarginQ;
        long farQ  = mat.farMarginQ;
        long fadeNearQ = mat.fadeDistanceNearQ;
        long fadeFarQ  = mat.fadeDistanceFarQ;
        int nearColor = mat.colorNear;
        int farColor  = mat.colorFar;
        int shape     = mat.primitiveShape;

        long[][] verts = obj.model.vertices;

        for (int v = 0; v < verts.length; v++) {
            FixedMatMath.transformPoint(finalM, verts[v], scratch4a);

            int[] screenV = projectPointToScreen(scratch3a, scratch4a, reusableScreenCoords0);
            if (screenV == null) {
                continue;
            }

            long dist = scratch4a[2];
            int alphaFade = RenderEffects.computeFadeAlpha(dist, nearQ, farQ, fadeNearQ, fadeFarQ);
            if (alphaFade <= 0) {
                continue;
            }
            int blendedRGB = RenderEffects.interpolateColor(dist, nearQ, farQ, nearColor, farColor);

            // Combine final alpha
            int a = (blendedRGB >>> 24) & 0xFF;
            a = (a * alphaFade) >> 8;
            if (a > 255) a = 255;

            int r = (blendedRGB >>> 16) & 0xFF;
            int g = (blendedRGB >>>  8) & 0xFF;
            int b = (blendedRGB       ) & 0xFF;

            int finalColor = (a << 24) | (r << 16) | (g << 8) | b;

            // Draw the single vertex with depth test
            RenderVertex.drawVertex(
                    shape,
                    width, height,
                    frameBuffer,
                    depthBuffer,
                    screenV[0], screenV[1],
                    dist,         // pass the camera‐space Z
                    finalColor
            );
        }
    }

    /**
     * Returns screen coords [sx, sy, z_mapped] in reusableBuffer, or null if invalid.
     * We still compute a [0..1] z_mapped, but for actual depth testing, we use p[2] directly.
     */
    private int[] projectPointToScreen(long[] scratch, long[] p, int[] reusableBuffer) {
        if (p[3] <= 0) {
            return null; // behind camera in homogeneous sense
        }
        long x = FixedBaseMath.fixedDiv(p[0], p[3]);
        long y = FixedBaseMath.fixedDiv(p[1], p[3]);
        long z = p[2];

        long halfW = precalc_halfW_Q24_8;
        long halfH = precalc_halfH_Q24_8;

        long rx = FixedBaseMath.fixedAdd(halfW, FixedBaseMath.fixedMul(x, halfW));
        long ry = FixedBaseMath.fixedAdd(halfH, FixedBaseMath.fixedMul(y, halfH));

        int sx = FixedBaseMath.toInt(rx);
        int sy = FixedBaseMath.toInt(ry);

        // We also clamp normalized Z for 0..1 (for potential 2D debug or color interpolation):
        long den = FixedBaseMath.fixedSub(Z_FAR_Q24_8, Z_NEAR_Q24_8);
        if (den == 0) {
            return null;
        }
        long zs = FixedBaseMath.fixedSub(Z_FAR_Q24_8, z);
        long z_mapped = FixedBaseMath.fixedDiv(zs, den);
        if (z_mapped < 0) {
            z_mapped = 0;
        } else if (z_mapped > FixedBaseMath.toFixed(1f)) {
            z_mapped = FixedBaseMath.toFixed(1f);
        }

        reusableBuffer[0] = sx;
        reusableBuffer[1] = sy;
        reusableBuffer[2] = FixedBaseMath.toInt(z_mapped);
        return reusableBuffer;
    }
}
