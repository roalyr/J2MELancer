package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.FixedMatMath;
import FixedMath.FixedBaseMath;
import java.util.Vector;
import java.util.Hashtable;
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
    private long fpsStartTime;
    private int framesRendered;
    private int currentFPS;
    // Vertex template for drawing fuzzy vertices.
    private final int TEMPLATE_SIZE = 16;
    private final double TEMPLATE_EXP = 2.0;
    private final int[] vertexTemplate = createFuzzyCircleTemplate(TEMPLATE_SIZE, TEMPLATE_EXP);
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
        fpsStartTime = System.currentTimeMillis();
        framesRendered = 0;
        currentFPS = 0;
    }

    public void setRenderables(Vector renderables, int halfWidth, int halfHeight) {
        this.renderables = renderables;
        this.precalc_halfW_Q24_8 = halfWidth;
        this.precalc_halfH_Q24_8 = halfHeight;
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
    }

    public void updateFPS() {
        framesRendered++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - fpsStartTime >= 1000) {
            currentFPS = framesRendered;
            framesRendered = 0;
            fpsStartTime = currentTime;
        }
    }

    public void printFPS(Graphics g) {
        g.setColor(0xFFFFFFFF);
        String fpsText = "FPS: " + currentFPS + " Renderables: " + renderables.size();
        g.drawString(fpsText, 2, 2, Graphics.TOP | Graphics.LEFT);
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
            int alpha = computeFadeAlpha(distMid, nearQ, farQ, fadeQ);
            if (alpha <= 0) {
                continue;
            }

            int blendedRGB = interpolateColor(
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

    private static final int ALPHA_THRESHOLD = 10;

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int alpha = (color >> 24) & 0xFF;
        if (alpha < ALPHA_THRESHOLD) {
            return;
        }
        int localWidth = width;
        int localHeight = height;
        if ((x0 < 0 && x1 < 0) || (x0 >= localWidth && x1 >= localWidth) ||
            (y0 < 0 && y1 < 0) || (y0 >= localHeight && y1 >= localHeight)) {
            return;
        }
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;
        while (true) {
            if (x0 >= 0 && x0 < localWidth && y0 >= 0 && y0 < localHeight) {
                int index = y0 * localWidth + x0;
                frameBuffer[index] = blendPixel(frameBuffer[index], color, alpha);
            }
            if (x0 == x1 && y0 == y1) {
                break;
            }
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
            int alpha = computeFadeAlpha(dist, nearQ, farQ, fadeQ);
            if (alpha <= 0) {
                continue;
            }
            int blendedRGB = interpolateColor(dist, nearQ, farQ, nearColor, farColor, exponent);
            int r = (blendedRGB >> 16) & 0xFF;
            int g = (blendedRGB >> 8) & 0xFF;
            int b = blendedRGB & 0xFF;
            int finalColor = (alpha << 24) | (r << 16) | (g << 8) | b;
            drawVertex(sx, sy, finalColor);
        }
    }

    private void drawVertex(int x, int y, int color) {
        final int halfTemplate = TEMPLATE_SIZE / 2;
        int startX = x - halfTemplate;
        int startY = y - halfTemplate;
        int srcAlpha = (color >> 24) & 0xFF;
        int srcR = (color >> 16) & 0xFF;
        int srcG = (color >> 8) & 0xFF;
        int srcB = color & 0xFF;
        for (int ty = 0; ty < TEMPLATE_SIZE; ty++) {
            int targetY = startY + ty;
            if (targetY < 0 || targetY >= height) {
                continue;
            }
            int offsetY = targetY * width;
            for (int tx = 0; tx < TEMPLATE_SIZE; tx++) {
                int targetX = startX + tx;
                if (targetX < 0 || targetX >= width) {
                    continue;
                }
                int templatePixel = vertexTemplate[ty * TEMPLATE_SIZE + tx];
                int tplAlpha = (templatePixel >> 24) & 0xFF;
                if (tplAlpha == 0) {
                    continue;
                }
                int modAlpha = (tplAlpha * srcAlpha) / 255;
                int modR = (tplAlpha * srcR) / 255;
                int modG = (tplAlpha * srcG) / 255;
                int modB = (tplAlpha * srcB) / 255;
                int index = offsetY + targetX;
                int dstPixel = frameBuffer[index];
                frameBuffer[index] = blendPixel(dstPixel, (modAlpha << 24) | (modR << 16) | (modG << 8) | modB, srcAlpha);
            }
        }
    }

    private int[] createFuzzyCircleTemplate(int templateSize, double exponent) {
        int[] template = new int[templateSize * templateSize];
        int center_fixed = FixedBaseMath.toQ24_8((templateSize - 1) / 2.0f);
        int radius_fixed = FixedBaseMath.toQ24_8(templateSize / 2.0f);
        int one_fixed = FixedBaseMath.toQ24_8(1.0f);
        for (int y = 0; y < templateSize; y++) {
            for (int x = 0; x < templateSize; x++) {
                int x_fixed = x << FixedBaseMath.Q24_8_SHIFT;
                int y_fixed = y << FixedBaseMath.Q24_8_SHIFT;
                int dx_fixed = x_fixed - center_fixed;
                int dy_fixed = y_fixed - center_fixed;
                int dx2 = FixedBaseMath.q24_8_mul(dx_fixed, dx_fixed);
                int dy2 = FixedBaseMath.q24_8_mul(dy_fixed, dy_fixed);
                int distSq_fixed = dx2 + dy2;
                int distance_fixed = FixedBaseMath.sqrt(distSq_fixed);
                int alpha;
                if (distance_fixed > radius_fixed) {
                    alpha = 0;
                } else {
                    int normalized = FixedBaseMath.q24_8_div(distance_fixed, radius_fixed);
                    int inv = one_fixed - normalized;
                    int powered = FixedBaseMath.pow(inv, (float) exponent);
                    int alpha_fixed = FixedBaseMath.q24_8_mul(powered, FixedBaseMath.toQ24_8(255f));
                    alpha = FixedBaseMath.toInt(alpha_fixed);
                    if (alpha < 0) {
                        alpha = 0;
                    }
                    if (alpha > 255) {
                        alpha = 255;
                    }
                }
                template[y * templateSize + x] = (alpha << 24) | 0x00FFFFFF;
            }
        }
        return template;
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

    private int blendPixel(int dstPixel, int srcPixel, int srcMaxAlpha) {
        int dstA = (dstPixel >> 24) & 0xFF;
        int dstR = (dstPixel >> 16) & 0xFF;
        int dstG = (dstPixel >> 8) & 0xFF;
        int dstB = dstPixel & 0xFF;
        int srcA = (srcPixel >> 24) & 0xFF;
        int srcR = (srcPixel >> 16) & 0xFF;
        int srcG = (srcPixel >> 8) & 0xFF;
        int srcB = srcPixel & 0xFF;
        int sumA = dstA + srcA;
        int sumR = dstR + srcR;
        int sumG = dstG + srcG;
        int sumB = dstB + srcB;
        if (sumA > srcMaxAlpha) {
            float scale = srcMaxAlpha / (float) sumA;
            sumA = srcMaxAlpha;
            sumR = (int) (sumR * scale);
            sumG = (int) (sumG * scale);
            sumB = (int) (sumB * scale);
        }
        if (sumA > 255) {
            sumA = 255;
        }
        if (sumR > 255) {
            sumR = 255;
        }
        if (sumG > 255) {
            sumG = 255;
        }
        if (sumB > 255) {
            sumB = 255;
        }
        return (sumA << 24) | (sumR << 16) | (sumG << 8) | sumB;
    }

    private int interpolateColor(int z, int z0, int z1,
            int color0, int color1,
            float exponent) {
        int rangeQ = FixedBaseMath.q24_8_sub(z1, z0);
        if (rangeQ <= 0) {
            return color1;
        }
        int distQ = FixedBaseMath.q24_8_sub(z, z0);
        int ratioQ = FixedBaseMath.q24_8_div(distQ, rangeQ);
        if (ratioQ < 0) {
            ratioQ = 0;
        }
        int oneQ = FixedBaseMath.toQ24_8(1.0f);
        if (ratioQ > oneQ) {
            ratioQ = oneQ;
        }
        int adjustedRatioQ = FixedBaseMath.pow(ratioQ, exponent);
        int a0 = (color0 >> 24) & 0xFF;
        int r0 = (color0 >> 16) & 0xFF;
        int g0 = (color0 >> 8) & 0xFF;
        int b0 = color0 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int deltaA = a1 - a0;
        int deltaR = r1 - r0;
        int deltaG = g1 - g0;
        int deltaB = b1 - b0;
        int a = a0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaA), adjustedRatioQ));
        int r = r0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaR), adjustedRatioQ));
        int g = g0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaG), adjustedRatioQ));
        int b = b0 + FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(FixedBaseMath.toQ24_8(deltaB), adjustedRatioQ));
        if (a < 0) a = 0; else if (a > 255) a = 255;
        if (r < 0) r = 0; else if (r > 255) r = 255;
        if (g < 0) g = 0; else if (g > 255) g = 255;
        if (b < 0) b = 0; else if (b > 255) b = 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int computeFadeAlpha(int z, int nearQ, int farQ, int fadeQ) {
        if (z < nearQ || z > farQ) {
            return 0;
        }
        if (fadeQ <= 0) {
            return 255;
        }
        int nearPlusFade = FixedBaseMath.q24_8_add(nearQ, fadeQ);
        if (z <= nearPlusFade) {
            int dz = FixedBaseMath.q24_8_sub(z, nearQ);
            int ratioQ = FixedBaseMath.q24_8_div(dz, fadeQ);
            if (ratioQ < 0) ratioQ = 0;
            if (ratioQ > FixedBaseMath.toQ24_8(1.0f)) ratioQ = FixedBaseMath.toQ24_8(1.0f);
            return ratioQToAlpha(ratioQ);
        }
        int farMinusFade = FixedBaseMath.q24_8_sub(farQ, fadeQ);
        if (z >= farMinusFade) {
            int dz = FixedBaseMath.q24_8_sub(farQ, z);
            int ratioQ = FixedBaseMath.q24_8_div(dz, fadeQ);
            if (ratioQ < 0) ratioQ = 0;
            if (ratioQ > FixedBaseMath.toQ24_8(1.0f)) ratioQ = FixedBaseMath.toQ24_8(1.0f);
            return ratioQToAlpha(ratioQ);
        }
        return 255;
    }

    private int ratioQToAlpha(int ratioQ) {
        int alphaQ = FixedBaseMath.q24_8_mul(ratioQ, FixedBaseMath.toQ24_8(255.0f));
        int alphaI = FixedBaseMath.toInt(alphaQ);
        if (alphaI < 0) alphaI = 0;
        if (alphaI > 255) alphaI = 255;
        return alphaI;
    }
}