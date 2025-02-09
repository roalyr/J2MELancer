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
            if (obj.material != null && obj.material.renderType == 0) {
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
        int fadeQ = mat.fadeDistanceQ24_8;
        int nearColor = mat.colorNear;
        int farColor = mat.colorFar;
        float exponent = mat.colorExponent;

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            FixedMatMath.transformPoint(finalM, verts[i0], scratch4a);
            FixedMatMath.transformPoint(finalM, verts[i1], scratch4b);

            screenP0 = projectPointToScreen(scratch3a, scratch4a);
            screenP1 = projectPointToScreen(scratch3b, scratch4b);

            if (screenP0 == null || screenP1 == null) {
                continue; // clipped

            }

            int distA = scratch4a[2];
            int distB = scratch4b[2];
            int distMid = FixedBaseMath.q24_8_div(
                    FixedBaseMath.q24_8_add(distA, distB),
                    FixedBaseMath.toQ24_8(2.0f));
            int alpha = RendererEffects.computeFadeAlpha(distMid, nearQ, farQ, fadeQ);
            if (alpha <= 0) {
                continue;
            }

            int blendedRGB = RendererEffects.interpolateColor(
                    distMid,
                    nearQ,
                    farQ,
                    nearColor,
                    farColor,
                    exponent);
            int r = (blendedRGB >> 16) & 0xFF;
            int g = (blendedRGB >> 8) & 0xFF;
            int b = (blendedRGB) & 0xFF;
            int finalColor = (alpha << 24) | (r << 16) | (g << 8) | b;

            drawLine(screenP0[0], screenP0[1], screenP1[0], screenP1[1], finalColor);
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int alpha = (color >> 24) & 0xFF;
 
        if ((x0 < 0 && x1 < 0) || (x0 >= width && x1 >= width) ||
                (y0 < 0 && y1 < 0) || (y0 >= height && y1 >= height)) {
            return;
        }
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;
        while (true) {
            if (x0 == x1 && y0 == y1) {
                break;
            }

            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                drawP(x0, y0, color, alpha);
            }

            //if (x0 > 0 && x0 < width-1 && y0 > 0 && y0 < height-1) {
            //    drawX(x0, y0, color, alpha);
            //}

            int e2 = err << 1;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void drawVertices(int[] finalM, SceneObject obj) {
        Material mat = obj.material;
        if (mat == null) {
            return;
        }
        int nearQ = mat.nearMarginQ24_8;
        int farQ = mat.farMarginQ24_8;
        int fadeQ = mat.fadeDistanceQ24_8;
        int nearColor = mat.colorNear;
        int farColor = mat.colorFar;
        float exponent = mat.colorExponent;
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
            int alpha = RendererEffects.computeFadeAlpha(dist, nearQ, farQ, fadeQ);
            if (alpha <= 0) {
                continue;
            }
            int blendedRGB = RendererEffects.interpolateColor(dist, nearQ, farQ, nearColor, farColor, exponent);
            int r = (blendedRGB >> 16) & 0xFF;
            int g = (blendedRGB >> 8) & 0xFF;
            int b = blendedRGB & 0xFF;
            int finalColor = (alpha << 24) | (r << 16) | (g << 8) | b;
            drawVertex(sx, sy, finalColor);
        }
    }

    private void drawVertex(int x, int y, int color) {
        int alpha = (color >> 24) & 0xFF;
        drawX(x, y, color, alpha);
    }

    private void drawX(int x, int y, int color, int alpha) {
        if ((x < 1) || (x > width) || (y < 1) || (y > height)) {
            return;
        }
        int index = (y - 1) * width + (x + 1);
        frameBuffer[index] = RendererEffects.blendPixel(frameBuffer[index], color, alpha);

        index = (y + 1) * width + (x + 1);
        frameBuffer[index] = RendererEffects.blendPixel(frameBuffer[index], color, alpha);

        index = (y + 1) * width + (x - 1);
        frameBuffer[index] = RendererEffects.blendPixel(frameBuffer[index], color, alpha);

        index = (y - 1) * width + (x - 1);
        frameBuffer[index] = RendererEffects.blendPixel(frameBuffer[index], color, alpha);

        index = (y) * width + (x);
        frameBuffer[index] = RendererEffects.blendPixel(frameBuffer[index], color, alpha);
    }
    
    private void drawP(int x, int y, int color, int alpha) {
        if ((x < 0) || (x >= width) || (y < 0) || (y >= height)) {
            return;
        }
        int index = (y) * width + (x);
        //if (frameBuffer[index] == BACKGROUND_COLOR){
        //    frameBuffer[index] = color;
        //} else {
            frameBuffer[index] = RendererEffects.blendPixel(frameBuffer[index], color, alpha);
        //}
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