package Renderer;

import javax.microedition.lcdui.Graphics;
import FixedMath.*;
import java.util.Vector;

public class Renderer {

    private int[] frameBuffer;
    private int[] occlusionBuffer;
    private int width;
    private int height;
    private Vector renderables;

    // Pre-calculated values passed from Scene
    private int precalc_halfW_Q24_8;
    private int precalc_halfH_Q24_8;

    public Renderer() {
        this.width = SharedData.display_width;
        this.height = SharedData.display_height;
        this.frameBuffer = new int[width * height];
        this.occlusionBuffer = new int[width * height];
        this.renderables = new Vector();
    }

    // Updated setRenderables to receive pre-calculated values
    public void setRenderables(Vector renderables, int halfWidth, int halfHeight) {
        this.renderables = renderables;
        this.precalc_halfW_Q24_8 = halfWidth;
        this.precalc_halfH_Q24_8 = halfHeight;
    }

    public void clearBuffers() {
        // Clear framebuffer to black
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = 0x000000; // Black

        }

        // Clear occlusion buffer (if you decide to use it later)
        for (int i = 0; i < occlusionBuffer.length; i++) {
            occlusionBuffer[i] = 0; // 0 means empty

        }
    }

    public void renderScene(Graphics g, int[] viewMatrix) {
        clearBuffers();

        // Reset the background 
        g.setColor(0x00000000); // Use black for background

        g.fillRect(0, 0, width, height);

        // Iterate through renderable objects
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);

            // Perform object-level culling using bounding box
            if (isObjectVisible(obj, viewMatrix)) {

                // Build the local transform => TRS => 4x4
                int[] local = FixedMatMath.createIdentity4x4();
                local = FixedMatMath.multiply4x4(local, FixedMatMath.createTranslation4x4(obj.tx, obj.ty, obj.tz));
                local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationZ4x4(obj.rotZ));
                local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationY4x4(obj.rotY));
                local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationX4x4(obj.rotX));
                local = FixedMatMath.multiply4x4(local, FixedMatMath.createScale4x4(obj.scale, obj.scale, obj.scale));

                // Combine with the “viewMatrix” (which has camera & perspective)
                int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

                // Draw edges
                drawEdges(finalMatrix, obj.model, g);
            }
        }

        // Flush framebuffer to the screen
        g.drawRGB(frameBuffer, 0, width, 0, 0, width, height, true);
    }

    // Updated to use pre-calculated half width and height
    private void drawEdges(int[] finalM, ModelQ24_8 model, Graphics g) {
        int[][] edges = model.edges;
        int[][] verts = model.vertices;

        for (int i = 0; i < edges.length; i++) {
            int i0 = edges[i][0];
            int i1 = edges[i][1];

            // transform each vertex => (x', y', z', w')
            int[] p0 = transformPointQ24_8(finalM, verts[i0]);
            int[] p1 = transformPointQ24_8(finalM, verts[i1]);

            // skip if w=0
            if (p0[3] == 0 || p1[3] == 0) {
                continue;
            }

            // Perspective division and map to screen space
            // Using FixedBaseMath.div_lut (which you renamed from q24_8_div)
            int sx0 = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfW_Q24_8, FixedBaseMath.q24_8_mul(FixedBaseMath.q24_8_div(p0[0], p0[3]), precalc_halfW_Q24_8)));
            int sy0 = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfH_Q24_8, FixedBaseMath.q24_8_mul(FixedBaseMath.q24_8_div(p0[1], p0[3]), precalc_halfH_Q24_8)));
            int sx1 = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfW_Q24_8, FixedBaseMath.q24_8_mul(FixedBaseMath.q24_8_div(p1[0], p1[3]), precalc_halfW_Q24_8)));
            int sy1 = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfH_Q24_8, FixedBaseMath.q24_8_mul(FixedBaseMath.q24_8_div(p1[1], p1[3]), precalc_halfH_Q24_8)));

            // Clipping (TODO: Implement proper clipping later)

            // Draw the edge
            drawLine(sx0, sy0, sx1, sy1, 0xFFFF00FF); // Opaque magenta

        }
    }

    private int[] transformPointQ24_8(int[] m4x4, int[] xyz) {
        int[] out4 = new int[4];
        for (int row = 0; row < 4; row++) {
            long sum = 0;
            for (int col = 0; col < 3; col++) {
                sum += (long) m4x4[row * 4 + col] * (long) xyz[col];
            }
            // w=1
            sum += (long) m4x4[row * 4 + 3] * (long) Constants.Common.ONE_POS;
            out4[row] = (int) (sum >> FixedBaseMath.Q24_8_SHIFT);
        }
        return out4;
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        // Optimization 1: Early exit for horizontal/vertical lines
        if (dx == 0) {
            // Vertical line
            for (int y = y0; y != y1 + sy; y += sy) {
                if (y >= 0 && y < height) {
                    int index = y * width + x0;
                    if (x0 >= 0 && x0 < width) {
                        frameBuffer[index] = color;
                    }
                }
            }
            return;
        }

        if (dy == 0) {
            // Horizontal line
            for (int x = x0; x != x1 + sx; x += sx) {
                if (x >= 0 && x < width) {
                    int index = y0 * width + x;
                    if (y0 >= 0 && y0 < height) {
                        frameBuffer[index] = color;
                    }
                }
            }
            return;
        }

        // Optimization 2: Bresenham's algorithm with reduced branching
        while (true) {
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                frameBuffer[y0 * width + x0] = color;
            }

            if (x0 == x1 && y0 == y1) {
                break;
            }

            int e2 = err << 1; // Multiply by 2 using left shift

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

private boolean isObjectVisible(SceneObject obj, int[] viewMatrix) {
    // 1. Get the object's center in world space
    int centerX = obj.tx;
    int centerY = obj.ty;
    int centerZ = obj.tz;

    // 2. Transform the center to view space
    int[] centerView = transformPointQ24_8(viewMatrix, new int[] { centerX, centerY, centerZ, FixedBaseMath.toQ24_8(1f) });

    // 3. Get the object's bounding sphere radius
    int radius = obj.model.boundingSphereRadius;

    // 4. Perform a simplified view frustum check

    // Check against the near and far planes
    int nearPlaneZ = FixedBaseMath.toQ24_8(Constants.Common.Z_NEAR);
    int farPlaneZ = FixedBaseMath.toQ24_8(Constants.Common.Z_FAR);

    if (centerView[2] - radius > farPlaneZ) {
        return false; // Object is behind the far plane
    }

    if (centerView[2] + radius < nearPlaneZ) {
        return false; // Object is in front of the near plane
    }

    // 5. Project the center point to screen space
    int[] screenPoint = projectPointToScreen(centerView);

    if (screenPoint == null) {
        return false; // Object is not projectable (e.g., w <= 0)
    }

    int screenX = screenPoint[0];
    int screenY = screenPoint[1];

    // 6. Check against screen bounds using the pre-calculated screen dimensions
    int screenRadius = FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(radius, FixedBaseMath.q24_8_div(FixedBaseMath.toQ24_8(1f), Math.abs(centerView[3]))));

    if (screenX + screenRadius < 0 || screenX - screenRadius > SharedData.display_width ||
        screenY + screenRadius < 0 || screenY - screenRadius > SharedData.display_height) {
        return false; // Object is outside the screen bounds
    }

    return true; // Object is potentially visible
}

private int[] projectPointToScreen(int[] p) {
    // Skip if w is too small or negative
    if (p[3] <= 0) { // Use <= for robustness
        return null;
    }

    // Perspective division
    int x = FixedBaseMath.q24_8_div(p[0], p[3]);
    int y = FixedBaseMath.q24_8_div(p[1], p[3]);

    // Map x, y to screen.
    int sx = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfW_Q24_8, FixedBaseMath.q24_8_mul(x, precalc_halfW_Q24_8)));
    int sy = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfH_Q24_8, FixedBaseMath.q24_8_mul(y, precalc_halfH_Q24_8)));

    return new int[]{sx, sy};
}

}