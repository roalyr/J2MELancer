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
            int[] screenP0 = projectPointToScreen(p0);
            int[] screenP1 = projectPointToScreen(p1);

            // Check if points are projectable
            if (screenP0 == null || screenP1 == null) {
                continue;
            }

            // Draw the edge with depth-based alpha blending
            drawLine(screenP0[0], screenP0[1], screenP0[2], screenP1[0], screenP1[1], screenP1[2], 0xFFFF00FF);
        }
    }

    private int[] projectPointToScreen(int[] p) {
        // Skip if w is too small or negative
        if (p[3] <= 0) {
            return null;
        }

        // Perspective division
        int x = FixedBaseMath.q24_8_div(p[0], p[3]);
        int y = FixedBaseMath.q24_8_div(p[1], p[3]);
        int z = p[2];

        // Map x, y to screen.
        int sx = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfW_Q24_8, FixedBaseMath.q24_8_mul(x, precalc_halfW_Q24_8)));
        int sy = FixedBaseMath.toInt(FixedBaseMath.q24_8_add(precalc_halfH_Q24_8, FixedBaseMath.q24_8_mul(y, precalc_halfH_Q24_8)));

        // Map z to 0-1 range
        int z_mapped = FixedBaseMath.q24_8_div(
                FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(Constants.Common.Z_FAR), z),
                FixedBaseMath.q24_8_sub(FixedBaseMath.toQ24_8(Constants.Common.Z_FAR), FixedBaseMath.toQ24_8(Constants.Common.Z_NEAR)));

        // Clamp z_mapped to the 0-1 range in Q24.8 format
        if (z_mapped < 0) {
            z_mapped = 0;
        } else if (z_mapped > FixedBaseMath.toQ24_8(1f)) {
            z_mapped = FixedBaseMath.toQ24_8(1f);
        }

        return new int[]{sx, sy, z_mapped};
    }

    private void drawLine(int x0, int y0, int z0, int x1, int y1, int z1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        // Linearly interpolate z-values in Q24.8 format
        int dz = z1 - z0;
        int z = z0;
        int dz_step = 0;

        if (dx > dy && dx != 0) {
            dz_step = dz / dx;
        } else if (dy != 0) {
            dz_step = dz / dy;
        }

        // Convert near and far plane distances to Q24.8 for alpha calculation
        int nearPlaneZ = 0;
        int farPlaneZ = FixedBaseMath.toQ24_8(1f);

        // Alpha threshold (you can adjust this value as needed)
        // TODO: Move to common constants as well
        int alphaThreshold = FixedBaseMath.toQ24_8(0.1f); // e.g., discard pixels with alpha < 0.05 (in Q24.8 format)

        int alpha = calculateAlpha(z, nearPlaneZ, farPlaneZ);

        // Early exit for horizontal/vertical lines
        if (dx == 0) {
            // Vertical line
            for (int y = y0; y != y1 + sy; y += sy) {
                if (y >= 0 && y < height && x0 >= 0 && x0 < width) {
           
                    // Check alpha against threshold
                    if (alpha > alphaThreshold) {
                        // Blend color with the alpha value
                        int blendedColor = (alpha << 24) | (color & 0x00FFFFFF);
                        frameBuffer[y * width + x0] = blendedColor;
                    }
                }
                z += dz_step;
            }
            return;
        }

        if (dy == 0) {
            // Horizontal line
            for (int x = x0; x != x1 + sx; x += sx) {
                if (x >= 0 && x < width && y0 >= 0 && y0 < height) {
                    // Check alpha against threshold
                    if (alpha > alphaThreshold) {
                        // Blend color with the alpha value
                        int blendedColor = (alpha << 24) | (color & 0x00FFFFFF);
                        frameBuffer[y0 * width + x] = blendedColor;
                    }
                }
                z += dz_step;
            }
            return;
        }

        // Bresenham's algorithm
        while (true) {
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                // Check alpha against threshold
                if (alpha > alphaThreshold) {
                    // Blend color with the alpha value
                    int blendedColor = (alpha << 24) | (color & 0x00FFFFFF);
                    frameBuffer[y0 * width + x0] = blendedColor;
                }
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
            z += dz_step;
        }
    }

    private int calculateAlpha_(int z, int nearZ, int farZ) {
        // Clamp z to the 0-1 range (representing near to far)
        if (z < nearZ) {
            z = nearZ;
        } else if (z > farZ) {
            z = farZ;
        }

        // Calculate alpha using linear interpolation
        int zRange = FixedBaseMath.q24_8_sub(farZ, nearZ); // farZ - nearZ

        int zDist = FixedBaseMath.q24_8_sub(z, nearZ); // z - nearZ

        // division result will be up to 1.0 in Q24.8
        int zRatio = FixedBaseMath.q24_8_div(zDist, zRange); // (z - nearZ) / (farZ - nearZ)

        // Scale zRatio to 0-255 range for alpha
        int alpha = FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(zRatio, FixedBaseMath.toQ24_8(255f)));

        // Invert
        // alpha = 255 - alpha;

        // Ensure alpha is within the 0-255 range
        if (alpha < 0) {
            alpha = 0;
        }
        if (alpha > 255) {
            alpha = 255;
        }
        return alpha;
    }

    private int calculateAlpha(int z, int nearZ, int farZ) {
        // Clamp z to the 0-1 range (representing near to far)
        // TODO: add threshold value
        if (z < nearZ) {
            z = nearZ;
        } else if (z > farZ) {
            z = farZ;
        }

        // Calculate zRatio using linear interpolation, result is up to 1.0 in Q24.8
        int zRatio = FixedBaseMath.q24_8_div(FixedBaseMath.q24_8_sub(z, nearZ), FixedBaseMath.q24_8_sub(farZ, nearZ));

        // Power function: alpha = 1 - (zRatio ^ power)
        float power = 2.5f; // Adjust this value to control the curve (higher values = steeper drop-off)

        int oneQ24_8 = FixedBaseMath.toQ24_8(1f);
        int alpha = FixedBaseMath.q24_8_sub(oneQ24_8, FixedBaseMath.pow(zRatio, power));

        // Scale alpha to 0-255 range
        alpha = FixedBaseMath.toInt(FixedBaseMath.q24_8_mul(alpha, FixedBaseMath.toQ24_8(255f)));

        // Invert
        alpha = 255 - alpha;

        // Ensure alpha is within the 0-255 range
        if (alpha < 0) {
            alpha = 0;
        }
        if (alpha > 255) {
            alpha = 255;
        }

        return alpha;
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

    private boolean isObjectVisible(SceneObject obj, int[] viewMatrix) {
        // 1. Get the object's center in world space
        int centerX = obj.tx;
        int centerY = obj.ty;
        int centerZ = obj.tz;

        // 2. Transform the center to view space
        int[] centerView = transformPointQ24_8(viewMatrix, new int[]{centerX, centerY, centerZ, FixedBaseMath.toQ24_8(1f)});

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
}