package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;

import java.util.Vector;
import Constants.Common;

public class Renderer {

    // Variables
    private final int BACKGROUND_COLOR = 0xFF000000;
    private int width;
    private int height;
    private int precalc_halfW_Q24_8;
    private int precalc_halfH_Q24_8;
    private final int Z_NEAR_Q24_8;
    private final int Z_FAR_Q24_8;
    // Reusable primitives
    private Vector renderables;
    private int[] frameBuffer;
    private int[] origin;
    private int[] centerCam;
    // Scratch buffers (not pooled)
    private int[] scratch3a = new int[3];
    private int[] scratch3b = new int[3];
    private int[] scratch4a = new int[4];
    private int[] scratch4b = new int[4];
    private int[] screenP0;
    private int[] screenP1;

    public Renderer() {
        this.width = SharedData.display_width;
        this.height = SharedData.display_height;
        this.frameBuffer = new int[width * height];
        this.renderables = new Vector();
        this.Z_NEAR_Q24_8 = FixedBaseMath.toQ24_8(Common.Z_NEAR);
        this.Z_FAR_Q24_8 = FixedBaseMath.toQ24_8(Common.Z_FAR);

    }

    public void setRenderables(Vector renderables, int halfWidth, int halfHeight) {
        this.renderables = renderables;
        this.precalc_halfW_Q24_8 = halfWidth;
        this.precalc_halfH_Q24_8 = halfHeight;
    }

    public Vector getRenderables() {
        return renderables;
    }

    public void clearBuffers(Graphics g) {
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = BACKGROUND_COLOR;
        }
    }

    public void renderScene(Graphics g, int[] viewMatrix) {
        // For each renderable, acquire temporary matrices, use them for rendering,
        // then immediately release them.
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            // Acquire a temporary local transform matrix from the pool.
            int[] local = getLocalTransform(obj);  // uses the pool internally
            // Multiply the view matrix by the local transform.

            int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

            // Render edges or vertices using the computed final matrix.
            if (obj.material == null) {
                return;
            }

            if (obj.material.renderType == RenderEffects.TYPE_VERTICES) {
                drawVertices(finalMatrix, obj);
            } else {
                drawEdges(finalMatrix, obj);
            }

            // Release the temporary matrices back to the pool.
            FixedMatMath.releaseMatrix(local);
            FixedMatMath.releaseMatrix(finalMatrix);
        }
        g.drawRGB(frameBuffer, 0, width, 0, 0, width, height, true);
        SharedData.renderables_num = getRenderables().size();
    }

    private int[] getLocalTransform(SceneObject obj) {
        // Start with an identity matrix.
        int[] local = FixedMatMath.createIdentity4x4();
        int[] temp;
        int[] m;

        // Multiply with translation.
        m = FixedMatMath.createTranslation4x4(obj.tx, obj.ty, obj.tz);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Multiply with rotation about Z.
        m = FixedMatMath.createRotationZ4x4(obj.rotZ);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Multiply with rotation about Y.
        m = FixedMatMath.createRotationY4x4(obj.rotY);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Multiply with rotation about X.
        m = FixedMatMath.createRotationX4x4(obj.rotX);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        // Multiply with scale.
        m = FixedMatMath.createScale4x4(obj.scale, obj.scale, obj.scale);
        temp = FixedMatMath.multiply4x4(local, m);
        FixedMatMath.releaseMatrix(local);
        FixedMatMath.releaseMatrix(m);
        local = temp;

        return local;
    }

    private void drawEdges(int[] finalM, SceneObject obj) {
        Material mat = obj.material;
        int[][] edges = obj.model.edges;
        int[][] verts = obj.model.vertices;

        int nearQ = mat.nearMarginQ24_8;
        int farQ = mat.farMarginQ24_8;
        int fadeNearQ = mat.fadeDistanceNearQ24_8;
        int fadeFarQ = mat.fadeDistanceFarQ24_8;
        int nearColor = mat.colorNear;
        int farColor = mat.colorFar;
        int ditherLevel = mat.ditherLevel;
        int shape = mat.primitiveShape;

        // Compute the object's center in camera space.
        // Create a point for (0,0,0,1) in object space.
        origin = new int[]{0, 0, 0, FixedBaseMath.toQ24_8(1.0f)};
        // We allocate a temporary array for the transformed point.
        centerCam = new int[4];
        FixedMatMath.transformPoint(finalM, origin, centerCam);
        int centerCamZ = centerCam[2];

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            // Transform endpoints from object space to camera space.
            FixedMatMath.transformPoint(finalM, verts[i0], scratch4a);
            FixedMatMath.transformPoint(finalM, verts[i1], scratch4b);

            // Project the transformed points to screen.
            screenP0 = projectPointToScreen(scratch3a, scratch4a);
            screenP1 = projectPointToScreen(scratch3b, scratch4b);
            if (screenP0 == null || screenP1 == null) {
                continue; // clipped

            }

            // Compute a fade alpha based on the edge's midpoint distance (world depth)
            int distA = scratch4a[2];
            int distB = scratch4b[2];
            int distMid = FixedBaseMath.q24_8_div(
                    FixedBaseMath.q24_8_add(distA, distB),
                    FixedBaseMath.toQ24_8(2.0f));
            int alpha = RenderEffects.computeFadeAlpha(distMid, nearQ, farQ, fadeNearQ, fadeFarQ);
            if (alpha <= 0) {
                continue;
            }

            // Interpolate color based on depth.
            int blendedRGB = RenderEffects.interpolateColor(
                    distMid,
                    nearQ,
                    farQ,
                    nearColor,
                    farColor);
            int alpha_orig = (blendedRGB >> 24) & 0xFF;
            int alpha_combined = (alpha_orig * alpha) / 255;

            // --- Apply local fade based on camera-space depth relative to object center ---
            int localAlpha0 = RenderEffects.computeLocalAlphaFromCameraSpace(scratch4a[2], centerCamZ, obj.model.boundingSphereRadius);
            int localAlpha1 = RenderEffects.computeLocalAlphaFromCameraSpace(scratch4b[2], centerCamZ, obj.model.boundingSphereRadius);
            int localAlpha = (localAlpha0 + localAlpha1) / 2;
            alpha_combined = (alpha_combined * localAlpha) / 255;
            // --- End local alpha processing ---

            int r = (blendedRGB >> 16) & 0xFF;
            int g = (blendedRGB >> 8) & 0xFF;
            int b = blendedRGB & 0xFF;
            int finalColor = (alpha_combined << 24) | (r << 16) | (g << 8) | b;

            RenderLine.drawLineDither(shape, width, height, frameBuffer,
                    screenP0[0], screenP0[1],
                    screenP1[0], screenP1[1],
                    finalColor, ditherLevel);
        }
    }

    private void drawVertices(int[] finalM, SceneObject obj) {
        Material mat = obj.material;
        if (mat == null) {
            return;
        }
        int nearQ = mat.nearMarginQ24_8;
        int farQ = mat.farMarginQ24_8;
        int fadeNearQ = mat.fadeDistanceNearQ24_8;
        int fadeFarQ = mat.fadeDistanceFarQ24_8;
        int nearColor = mat.colorNear;
        int farColor = mat.colorFar;
        int shape = mat.primitiveShape;

        int[][] verts = obj.model.vertices;
        for (int v = 0; v < verts.length; v++) {
            FixedMatMath.transformPoint(finalM, verts[v], scratch4a);
            int[] screenV = projectPointToScreen(scratch3a, scratch4a);
            if (screenV == null) {
                continue;
            }
            int sx = screenV[0];
            int sy = screenV[1];
            int dist = scratch4a[2];
            int alpha = RenderEffects.computeFadeAlpha(dist, nearQ, farQ, fadeNearQ, fadeFarQ);
            if (alpha <= 0) {
                continue;
            }
            int blendedRGB = RenderEffects.interpolateColor(dist, nearQ, farQ, nearColor, farColor);
            int r = (blendedRGB >> 16) & 0xFF;
            int g = (blendedRGB >> 8) & 0xFF;
            int b = blendedRGB & 0xFF;
            int finalColor = (alpha << 24) | (r << 16) | (g << 8) | b;
            RenderVertex.drawVertex(shape, width, height, frameBuffer, sx, sy, finalColor);
        }
    }

    private int[] projectPointToScreen(int[] r, int[] p) {
        if (p[3] <= 0) {
            return null;
        }
        int x = FixedBaseMath.q24_8_div(p[0], p[3]);
        int y = FixedBaseMath.q24_8_div(p[1], p[3]);
        int z = p[2];
        int sx = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfW_Q24_8,
                FixedBaseMath.q24_8_mul(x, precalc_halfW_Q24_8)));
        int sy = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfH_Q24_8,
                FixedBaseMath.q24_8_mul(y, precalc_halfH_Q24_8)));
        int z_mapped = FixedBaseMath.q24_8_div(
                FixedBaseMath.q24_8_sub(Z_FAR_Q24_8, z),
                FixedBaseMath.q24_8_sub(Z_FAR_Q24_8, Z_NEAR_Q24_8));
        if (z_mapped < 0) {
            z_mapped = 0;
        } else if (z_mapped > FixedBaseMath.toQ24_8(1f)) {
            z_mapped = FixedBaseMath.toQ24_8(1f);
        }
        r[0] = sx;
        r[1] = sy;
        r[2] = z_mapped;
        return r;
    }
}