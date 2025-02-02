package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;
import java.util.Vector;
import java.util.Hashtable;
import Constants.Common;

public class Renderer {

    // Dedicated framebuffer background color (opaque black)
    private final int BACKGROUND_COLOR = 0xFF000000;

    private int[] frameBuffer;
    private int width;
    private int height;
    private Vector renderables; // Expected to be already culled and filtered

    // Pre-calculated screen half-dimensions (in Q24.8)
    private int precalc_halfW_Q24_8;
    private int precalc_halfH_Q24_8;
    // Near and far plane values in Q24.8 (from Common)
    private final int Z_NEAR_Q24_8;
    private final int Z_FAR_Q24_8;
    // Reusable scratch arrays for transformations.
    private int[] scratch4a = new int[4];
    private int[] scratch4b = new int[4];
    // Cache for local transforms.
    private Hashtable localTransformCache = new Hashtable();

    // FPS calculation fields.
    private long fpsStartTime;
    private int framesRendered;
    private int currentFPS;

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

    public void clearBuffers() {
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = BACKGROUND_COLOR;
        }
    }

    public void renderScene(Graphics g, int[] viewMatrix) {
        clearBuffers();
        int localWidth = width;
        int localHeight = height;

        // Clear the drawing surface.
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, localWidth, localHeight);

        // Update each object's depth (assumed visible as provided by Scene).
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            obj.depth = calculateObjectDepth(obj, viewMatrix);
        }

        sortRenderablesByDepth();
        localTransformCache.clear();

        // Render each object.
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);
            int[] local = getLocalTransform(obj);
            int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);
            drawEdges(finalMatrix, obj.model, g);
        }

        // Blit framebuffer.
        g.drawRGB(frameBuffer, 0, localWidth, 0, 0, localWidth, localHeight, true);

        updateFPS();
        printFPS(g);
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
        int[] worldCenter = new int[]{
            obj.tx,
            obj.ty,
            obj.tz,
            FixedBaseMath.toQ24_8(1.0f)
        };
        FixedMatMath.transformPoint(viewMatrix, worldCenter, scratch4a);
        return scratch4a[2];
    }

    private void sortRenderablesByDepth() {
        int n = renderables.size();
        if (n <= 1) return;
        SceneObject[] arr = new SceneObject[n];
        for (int i = 0; i < n; i++) {
            arr[i] = (SceneObject) renderables.elementAt(i);
        }
        // Binary insertion sort in descending order.
        for (int i = 1; i < n; i++) {
            SceneObject key = arr[i];
            int left = 0, right = i;
            while (left < right) {
                int mid = (left + right) / 2;
                if (arr[mid].depth < key.depth)
                    right = mid;
                else
                    left = mid + 1;
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

    private void drawEdges(int[] finalM, ModelQ24_8 model, Graphics g) {
        int nearPlaneZ = 0;
        int farPlaneZ = FixedBaseMath.toQ24_8(1f);
        int nearColor = 0xFF000000;
        int farColor = 0xFF00FF00;
        int[][] edges = model.edges;
        int[][] verts = model.vertices;

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            FixedMatMath.transformPoint(finalM, verts[i0], scratch4a);
            FixedMatMath.transformPoint(finalM, verts[i1], scratch4b);

            if (scratch4a[3] == 0 || scratch4b[3] == 0)
                continue;

            int[] screenP0 = projectPointToScreen(scratch4a);
            int[] screenP1 = projectPointToScreen(scratch4b);
            if (screenP0 == null || screenP1 == null)
                continue;

            int z = FixedBaseMath.q24_8_add(screenP0[2], screenP1[2]);
            z = FixedBaseMath.q24_8_div(z, FixedBaseMath.toQ24_8(2f));

            int blendedColor = interpolateColor(z, nearPlaneZ, farPlaneZ, nearColor, farColor);
            drawLine(screenP0[0], screenP0[1], screenP1[0], screenP1[1], blendedColor);
        }
    }

/**
     * Draws a line using Bresenham's algorithm. Checks bounds per pixel and
     * skips drawing if the new color is nearly equal to the background.
     */
    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int localWidth = width;
        int localHeight = height;
        // If the drawing color equals the background, skip drawing.
        if (color == BACKGROUND_COLOR) return;
        
        // Early-out if entire line is off-screen.
        if ((x0 < 0 && x1 < 0) || (x0 >= localWidth && x1 >= localWidth) ||
            (y0 < 0 && y1 < 0) || (y0 >= localHeight && y1 >= localHeight)) {
            return;
        }
        
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;
        
        // Special-case for horizontal and vertical lines.
        if (dx == 0) {
            for (int y = y0; y != y1 + sy; y += sy) {
                if (x0 >= 0 && x0 < localWidth && y >= 0 && y < localHeight) {
                    int index = y * localWidth + x0;
                    
                        frameBuffer[index] = color;
                    
                }
            }
            return;
        }
        if (dy == 0) {
            for (int x = x0; x != x1 + sx; x += sx) {
                if (x >= 0 && x < localWidth && y0 >= 0 && y0 < localHeight) {
                    int index = y0 * localWidth + x;
                    
                        frameBuffer[index] = color;
                    
                }
            }
            return;
        }
        
        while (true) {
            if (x0 >= 0 && x0 < localWidth && y0 >= 0 && y0 < localHeight) {
                int index = y0 * localWidth + x0;
                
                    frameBuffer[index] = color;
                
            }
            if (x0 == x1 && y0 == y1) break;
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

    private int[] projectPointToScreen(int[] p) {
        if (p[3] <= 0) return null;
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
        if (z_mapped < 0)
            z_mapped = 0;
        else if (z_mapped > FixedBaseMath.toQ24_8(1f))
            z_mapped = FixedBaseMath.toQ24_8(1f);
        return new int[]{sx, sy, z_mapped};
    }

    private int interpolateColor(int z, int z0, int z1, int color0, int color1) {
        if (z0 == z1) return color0;
        int zRange = FixedBaseMath.q24_8_sub(z1, z0);
        int zDist = FixedBaseMath.q24_8_sub(z, z0);
        int zRatio = FixedBaseMath.q24_8_div(zDist, zRange);
        if (zRatio < 0) zRatio = 0;
        if (zRatio > FixedBaseMath.toQ24_8(1f)) zRatio = FixedBaseMath.toQ24_8(1f);

        int a0 = (color0 >> 24) & 0xFF;
        int r0 = (color0 >> 16) & 0xFF;
        int g0 = (color0 >> 8) & 0xFF;
        int b0 = color0 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(a0),
                FixedBaseMath.q24_8_mul(
                    FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(a1), FixedBaseMath.toQ24_8(a0)),
                    zRatio)));
        int r = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(r0),
                FixedBaseMath.q24_8_mul(
                    FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(r1), FixedBaseMath.toQ24_8(r0)),
                    zRatio)));
        int g = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(g0),
                FixedBaseMath.q24_8_mul(
                    FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(g1), FixedBaseMath.toQ24_8(g0)),
                    zRatio)));
        int b = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(
                FixedBaseMath.toQ24_8(b0),
                FixedBaseMath.q24_8_mul(
                    FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(b1), FixedBaseMath.toQ24_8(b0)),
                    zRatio)));

        if (a < 0) a = 0; else if (a > 255) a = 255;
        if (r < 0) r = 0; else if (r > 255) r = 255;
        if (g < 0) g = 0; else if (g > 255) g = 255;
        if (b < 0) b = 0; else if (b > 255) b = 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}