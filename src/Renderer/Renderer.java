package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;

import java.util.Vector;
import Constants.Common;

public class Renderer {

    // Variables
    public static final int BACKGROUND_COLOR = 0xFF000000;
    private int width;
    private int height;
    private long precalc_halfW_Q24_8;
    private long precalc_halfH_Q24_8;
    private final long Z_NEAR_Q24_8;
    private final long Z_FAR_Q24_8;
    // Reusable primitives
	private final int[] reusableScreenCoords0 = new int[3];
	private final int[] reusableScreenCoords1 = new int[3];
    private Vector renderables;
    private int[] frameBuffer;
    private long[] origin;
    private long[] centerCam;
    // Scratch buffers (not pooled)
    private long[] scratch3a = new long[3];
    private long[] scratch3b = new long[3];
    private long[] scratch4a = new long[4];
    private long[] scratch4b = new long[4];


    public Renderer() {
        this.width = SharedData.display_width;
        this.height = SharedData.display_height;
        this.frameBuffer = new int[width * height];
        this.renderables = new Vector();
        // Z_NEAR and Z_FAR converted to fixed-point long values.
        this.Z_NEAR_Q24_8 = FixedBaseMath.toFixed(Common.Z_NEAR);
        this.Z_FAR_Q24_8 = FixedBaseMath.toFixed(Common.Z_FAR);
    }

    public void setRenderables(Vector renderables, long halfWidth, long halfHeight) {
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

    public void renderScene(Graphics g, long[] viewMatrix) {
        // For each renderable, acquire temporary matrices, use them for rendering,
        // then immediately release them.
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            // Acquire a temporary local transform matrix from the pool.
            long[] local = getLocalTransform(obj);
            // Multiply the view matrix by the local transform.
            long[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

            if (obj.material == null) {
                return;
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
        // Start with an identity matrix.
        long[] local = FixedMatMath.createIdentity4x4();
        long[] temp;
        long[] m;

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

    private void drawEdges(long[] finalM, SceneObject obj) {
        Material mat = obj.material;
        int[][] edges = obj.model.edges;
        long[][] verts = obj.model.vertices;

        long nearQ = mat.nearMarginQ;
        long farQ = mat.farMarginQ;
        long fadeNearQ = mat.fadeDistanceNearQ;
        long fadeFarQ = mat.fadeDistanceFarQ;
        int nearColor = mat.colorNear;
        int farColor = mat.colorFar;
        int ditherLevel = mat.ditherLevel;
        int shape = mat.primitiveShape;

        // Compute the object's center in camera space.
        origin = new long[]{0, 0, 0, FixedBaseMath.toFixed(1.0f)};
        centerCam = new long[4];
        FixedMatMath.transformPoint(finalM, origin, centerCam);
        long centerCamZ = centerCam[2];

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            FixedMatMath.transformPoint(finalM, verts[i0], scratch4a);
            FixedMatMath.transformPoint(finalM, verts[i1], scratch4b);

			int[] screenP0 = projectPointToScreen(scratch3a, scratch4a, reusableScreenCoords0);
			int[] screenP1 = projectPointToScreen(scratch3b, scratch4b, reusableScreenCoords1);
            if (screenP0 == null || screenP1 == null) {
                continue; // clipped
            }

            long distA = scratch4a[2];
            long distB = scratch4b[2];
            long distMid = FixedBaseMath.fixedDiv(FixedBaseMath.fixedAdd(distA, distB), FixedBaseMath.toFixed(2.0f));
            int alpha = RenderEffects.computeFadeAlpha(distMid, nearQ, farQ, fadeNearQ, fadeFarQ);
            if (alpha <= 0) {
                continue;
            }

            int blendedRGB = RenderEffects.interpolateColor(distMid, nearQ, farQ, nearColor, farColor);
            int alpha_orig = (blendedRGB >> 24) & 0xFF;
            int alpha_combined = (alpha_orig * alpha) / 255;

            int localAlpha0 = RenderEffects.computeLocalAlphaFromCameraSpace(scratch4a[2], centerCamZ, obj.model.boundingSphereRadius);
            int localAlpha1 = RenderEffects.computeLocalAlphaFromCameraSpace(scratch4b[2], centerCamZ, obj.model.boundingSphereRadius);
            int localAlpha = (localAlpha0 + localAlpha1) / 2;
            alpha_combined = (alpha_combined * localAlpha) / 255;

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

    private void drawVertices(long[] finalM, SceneObject obj) {
        Material mat = obj.material;
        if (mat == null) {
            return;
        }
        long nearQ = mat.nearMarginQ;
        long farQ = mat.farMarginQ;
        long fadeNearQ = mat.fadeDistanceNearQ;
        long fadeFarQ = mat.fadeDistanceFarQ;
        int nearColor = mat.colorNear;
        int farColor = mat.colorFar;
        int shape = mat.primitiveShape;

        long[][] verts = obj.model.vertices;
        for (int v = 0; v < verts.length; v++) {
            FixedMatMath.transformPoint(finalM, verts[v], scratch4a);
            int[] screenV = projectPointToScreen(scratch3a, scratch4a, reusableScreenCoords0);
            if (screenV == null) {
                continue;
            }
            int sx = screenV[0];
            int sy = screenV[1];
            long dist = scratch4a[2];
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

	private int[] projectPointToScreen(long[] scratch, long[] p, int[] reusableBuffer) {
		if (p[3] <= 0) {
			return null;
		}
		long x = FixedBaseMath.fixedDiv(p[0], p[3]);
		long y = FixedBaseMath.fixedDiv(p[1], p[3]);
		long z = p[2];
		int sx = FixedBaseMath.toInt(FixedBaseMath.fixedAdd(precalc_halfW_Q24_8,
						  FixedBaseMath.fixedMul(x, precalc_halfW_Q24_8)));
		int sy = FixedBaseMath.toInt(FixedBaseMath.fixedAdd(precalc_halfH_Q24_8,
						  FixedBaseMath.fixedMul(y, precalc_halfH_Q24_8)));
		long z_mapped = FixedBaseMath.fixedDiv(
				FixedBaseMath.fixedSub(Z_FAR_Q24_8, z),
				FixedBaseMath.fixedSub(Z_FAR_Q24_8, Z_NEAR_Q24_8));
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
