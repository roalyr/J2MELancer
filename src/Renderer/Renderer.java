package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;
import java.util.Vector;
import java.util.Hashtable;
import Constants.Common;

public class Renderer {

    private final int BACKGROUND_COLOR = 0xFF000000;
    private int[] frameBuffer;
    private int width;
    private int height;
    private Vector renderables;
    private int precalc_halfW_Q24_8;
    private int precalc_halfH_Q24_8;
    private final int Z_NEAR_Q24_8;
    private final int Z_FAR_Q24_8;
    private int[] scratch4a = new int[4];
    private int[] scratch4b = new int[4];
    private Hashtable localTransformCache = new Hashtable();
    private long fpsStartTime;
    private int framesRendered;
    private int currentFPS;
    // Vertex template for drawing fuzzy vertices.
    private final int TEMPLATE_SIZE = 16;
    private final double TEMPLATE_EXP = 2.0;
    private final int[] vertexTemplate = createFuzzyCircleTemplate(TEMPLATE_SIZE, TEMPLATE_EXP);

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
        int localWidth = width, localHeight = height;
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, localWidth, localHeight);
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = BACKGROUND_COLOR;
        }
    }

    public void renderSceneEdges(Graphics g, int[] viewMatrix) {
        int localWidth = width, localHeight = height;
        //for (int i = 0; i < renderables.size(); i++) {
        //    SceneObject obj = (SceneObject) renderables.elementAt(i);
        //    obj.depth = calculateObjectDepth(obj, viewMatrix);
        //}
        //sortRenderablesByDepth();
        
        //localTransformCache.clear();

        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            int[] local = getLocalTransform(obj);
            int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);
            drawEdges(finalMatrix, obj.model);
        }
        g.drawRGB(frameBuffer, 0, localWidth, 0, 0, localWidth, localHeight, true);
        updateFPS();
        printFPS(g);
    }

    public void renderSceneVertices(Graphics g, int[] viewMatrix) {
        int localWidth = width, localHeight = height;
        //for (int i = 0; i < renderables.size(); i++) {
        //    SceneObject obj = (SceneObject) renderables.elementAt(i);
        //    obj.depth = calculateObjectDepth(obj, viewMatrix);
        //}
        //sortRenderablesByDepth();
        
        //localTransformCache.clear();

        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            int[] local = getLocalTransform(obj);
            int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);
            drawVertices(finalMatrix, obj.model);
        }
        g.drawRGB(frameBuffer, 0, localWidth, 0, 0, localWidth, localHeight, true);
        //updateFPS();
        //printFPS(g);
    }

    private void updateFPS() {
        framesRendered++;
        long currentTime = System.currentTimeMillis();
        if (currentTime - fpsStartTime >= 1000) {
            currentFPS = framesRendered;
            framesRendered = 0;
            fpsStartTime = currentTime;
        }
    }

    private void printFPS(Graphics g) {
        g.setColor(0xFFFFFFFF);
        String fpsText = "FPS: " + currentFPS + " Renderables: " + renderables.size();
        g.drawString(fpsText, 2, 2, Graphics.TOP | Graphics.LEFT);
    }

    private int[] getLocalTransform(SceneObject obj) {
        if (localTransformCache.containsKey(obj)) {
            return (int[]) localTransformCache.get(obj);
        }
        int[] local = FixedMatMath.createIdentity4x4();
        local = FixedMatMath.multiply4x4(local, FixedMatMath.createTranslation4x4(obj.tx, obj.ty, obj.tz));
        local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationZ4x4(obj.rotZ));
        local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationY4x4(obj.rotY));
        local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationX4x4(obj.rotX));
        local = FixedMatMath.multiply4x4(local, FixedMatMath.createScale4x4(obj.scale, obj.scale, obj.scale));
        localTransformCache.put(obj, local);
        return local;
    }

    private int calculateObjectDepth(SceneObject obj, int[] viewMatrix) {
        int[] worldCenter = new int[]{obj.tx, obj.ty, obj.tz, FixedBaseMath.toQ24_8(1.0f)};
        FixedMatMath.transformPoint(viewMatrix, worldCenter, scratch4a);
        return scratch4a[2];
    }

    private void sortRenderablesByDepth() {
        int n = renderables.size();
        if (n <= 1) {
            return;
        }
        SceneObject[] arr = new SceneObject[n];
        for (int i = 0; i < n; i++) {
            arr[i] = (SceneObject) renderables.elementAt(i);
        }
        for (int i = 1; i < n; i++) {
            SceneObject key = arr[i];
            int left = 0, right = i;
            while (left < right) {
                int mid = (left + right) / 2;
                if (arr[mid].depth < key.depth) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }
            for (int j = i; j > left; j--) {
                arr[j] = arr[j - 1];
            }
            arr[left] = key;
        }
        renderables.removeAllElements();
        for (int i = 0; i < n; i++) {
            renderables.addElement(arr[i]);
        }
    }

    private void drawEdges(int[] finalM, ModelQ24_8 model) {
        int nearPlaneZ = FixedBaseMath.toQ24_8(0f);
        int farPlaneZ = FixedBaseMath.toQ24_8(1f);
        int nearColor = 0xFFFF00FF;
        int farColor = 0x1100FF00;
        float exponent = 5.0f;
        int[][] edges = model.edges;
        int[][] verts = model.vertices;

        // Draw edges
        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            FixedMatMath.transformPoint(finalM, verts[i0], scratch4a);
            FixedMatMath.transformPoint(finalM, verts[i1], scratch4b);
            if (scratch4a[3] == 0 || scratch4b[3] == 0) {
                continue;
            }
            int[] screenP0 = projectPointToScreen(scratch4a);
            int[] screenP1 = projectPointToScreen(scratch4b);
            if (screenP0 == null || screenP1 == null) {
                continue;
            }
            int z = FixedBaseMath.q24_8_add(screenP0[2], screenP1[2]);
            z = FixedBaseMath.q24_8_div(z, FixedBaseMath.toQ24_8(2f));
            int blendedColor = interpolateColor(z, nearPlaneZ, farPlaneZ, nearColor, farColor, exponent);
            drawLine(screenP0[0], screenP0[1], screenP1[0], screenP1[1], blendedColor);
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int localWidth = width;
        int localHeight = height;
        if (color == BACKGROUND_COLOR) {
            return;
        }
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
                frameBuffer[index] = blendPixel(frameBuffer[index], color, (color >> 24) & 0xFF);
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

    private void drawVertices(int[] finalM, ModelQ24_8 model) {
        int nearPlaneZ = FixedBaseMath.toQ24_8(0f);
        int farPlaneZ = FixedBaseMath.toQ24_8(1f);
        int nearColor = 0xFFFF0000;
        int farColor = 0x110000FF;
        float exponent = 5.0f;
        int[][] verts = model.vertices;

        // Draw vertices
        for (int v = 0; v < verts.length; v++) {
            FixedMatMath.transformPoint(finalM, verts[v], scratch4a);
            int[] screenV = projectPointToScreen(scratch4a);
            if (screenV == null) {
                continue;
            }
            int z = screenV[2];
            int blendedColor = interpolateColor(z, nearPlaneZ, farPlaneZ, nearColor, farColor, exponent);
            drawVertex(screenV[0], screenV[1], blendedColor);
        }
    }

    private void drawVertex(int x, int y, int color) {
        final int templateSize = TEMPLATE_SIZE;
        final int halfTemplate = templateSize / 2;
        int startX = x - halfTemplate;
        int startY = y - halfTemplate;

        int srcAlpha = (color >> 24) & 0xFF;
        int srcR = (color >> 16) & 0xFF;
        int srcG = (color >> 8) & 0xFF;
        int srcB = color & 0xFF;

        for (int ty = 0; ty < templateSize; ty++) {
            int targetY = startY + ty;
            if (targetY < 0 || targetY >= height) {
                continue;
            }
            int offsetY = targetY * width;
            for (int tx = 0; tx < templateSize; tx++) {
                int targetX = startX + tx;
                if (targetX < 0 || targetX >= width) {
                    continue;
                }
                int templatePixel = vertexTemplate[ty * templateSize + tx];
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
        // Convert center and radius to Q24.8 fixed-point.
        int center_fixed = FixedBaseMath.toQ24_8((templateSize - 1) / 2.0f);
        int radius_fixed = FixedBaseMath.toQ24_8(templateSize / 2.0f);
        int one_fixed = FixedBaseMath.toQ24_8(1.0f);

        // Loop over each pixel in the template.
        for (int y = 0; y < templateSize; y++) {
            for (int x = 0; x < templateSize; x++) {
                // Convert pixel coordinates to Q24.8.
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
                    // normalized = distance / radius in Q24.8.
                    int normalized = FixedBaseMath.q24_8_div(distance_fixed, radius_fixed);
                    // Compute inverse = 1 - normalized.
                    int inv = one_fixed - normalized;
                    // Apply power: (inv)^exponent.
                    int powered = FixedBaseMath.pow(inv, (float) exponent);
                    // Multiply by 255 (in fixed-point) to get alpha.
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

    private int[] projectPointToScreen(int[] p) {
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
        return new int[]{sx, sy, z_mapped};
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

    private int interpolateColor(int z, int z0, int z1, int color0, int color1, float exponent) {
        if (z0 == z1) {
            return color1;
        }
        int zRange = FixedBaseMath.q24_8_sub(z1, z0);
        int zDist = FixedBaseMath.q24_8_sub(z, z0);
        int zRatio = FixedBaseMath.q24_8_div(zDist, zRange);
        if (zRatio < 0) {
            zRatio = 0;
        }
        if (zRatio > FixedBaseMath.toQ24_8(1f)) {
            zRatio = FixedBaseMath.toQ24_8(1f);
        }
        // final float exponent = 3.0f;  // Adjust exponent as needed

        int adjustedRatio = FixedBaseMath.pow(zRatio, exponent);

        // Reverse the gradient: start at color1 and interpolate to color0.
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a0 = (color0 >> 24) & 0xFF;
        int r0 = (color0 >> 16) & 0xFF;
        int g0 = (color0 >> 8) & 0xFF;
        int b0 = color0 & 0xFF;

        int a = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(a1),
                FixedBaseMath.q24_8_mul(
                FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(a0), FixedBaseMath.toQ24_8(a1)),
                adjustedRatio)));
        int r = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(r1),
                FixedBaseMath.q24_8_mul(
                FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(r0), FixedBaseMath.toQ24_8(r1)),
                adjustedRatio)));
        int g = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(g1),
                FixedBaseMath.q24_8_mul(
                FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(g0), FixedBaseMath.toQ24_8(g1)),
                adjustedRatio)));
        int b = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(b1),
                FixedBaseMath.q24_8_mul(
                FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(b0), FixedBaseMath.toQ24_8(b1)),
                adjustedRatio)));

        if (a < 0) {
            a = 0;
        } else if (a > 255) {
            a = 255;
        }
        if (r < 0) {
            r = 0;
        } else if (r > 255) {
            r = 255;
        }
        if (g < 0) {
            g = 0;
        } else if (g > 255) {
            g = 255;
        }
        if (b < 0) {
            b = 0;
        } else if (b > 255) {
            b = 255;
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}