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

    public Renderer() {
        this.width = SharedData.display_width;
        this.height = SharedData.display_height;
        this.frameBuffer = new int[width * height];
        this.occlusionBuffer = new int[width * height];
        this.renderables = new Vector();
    }

    public void setRenderables(Vector renderables) {
        this.renderables = renderables;
    }

    public void clearBuffers() {
        // Clear framebuffer to black
        for (int i = 0; i < frameBuffer.length; i++) {
            frameBuffer[i] = 0x000000; // Black

        }

        // Clear occlusion buffer
        for (int i = 0; i < occlusionBuffer.length; i++) {
            occlusionBuffer[i] = 0; // 0 means empty

        }
    }

    public void renderScene(Graphics g, int[] viewMatrix) {
        clearBuffers();

        // Sort objects by depth (closest to farthest)
        sortRenderablesByDepth(renderables);

        // TODO: 
        // Render objects to occlusion buffer and framebuffer in a single loop
        for (int i = 0; i < renderables.size(); i++) {
            SceneObject obj = (SceneObject) renderables.elementAt(i);

            if (renderObjectToOcclusionBuffer(obj, viewMatrix)) {
                renderObjectToFrameBuffer(obj, viewMatrix, g);
            }

        }


        // Flush framebuffer to the screen
        g.drawRGB(frameBuffer, 0, width, 0, 0, width, height, true);
    }

    private boolean renderObjectToOcclusionBuffer(SceneObject obj, int[] viewMatrix) {
    //System.out.println("renderObjectToOcclusionBuffer called for object: " + obj);

    // Project object's bounding box to screen space
    int[] screenRect = projectBoundingBoxToScreen(obj, viewMatrix);

    if (screenRect == null) {
        //System.out.println("----- Object off screen");
        return false; // Object is off-screen
    }

    int minX = screenRect[0];
    int minY = screenRect[1];
    int maxX = screenRect[2];
    int maxY = screenRect[3];

    //System.out.println("  Screen Rectangle: [" + minX + ", " + minY + ", " + maxX + ", " + maxY + "]");

    // Check if the object's bounding rectangle is fully covered
    boolean fullyOccluded = true;
    for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
            if (x >= 0 && x < width && y >= 0 && y < height) {
                if (occlusionBuffer[y * width + x] == 0) {
                    fullyOccluded = false;
                    //System.out.println("    Pixel (" + x + ", " + y + ") is not occluded.");
                    //break;
                }
            }
        }
        if (!fullyOccluded) {
            break;
        }
    }

    if (fullyOccluded) {
        //System.out.println("----- Object fully occluded");
        return false; // Skip rendering if fully occluded
    }

    // Fill the bounding rectangle in the occlusion buffer
    for (int y = Math.max(0, minY); y <= Math.min(height - 1, maxY); y++) {
        for (int x = Math.max(0, minX); x <= Math.min(width - 1, maxX); x++) {
            occlusionBuffer[y * width + x] = 1;
        }
    }

    //System.out.println("----- Object MAPPED on culling buffer");
    return true;
}

    private void renderObjectToFrameBuffer(SceneObject obj, int[] viewMatrix, Graphics g) {
        // Build the local transform => TRS => 4x4
        int[] local = FixedMatMath.createIdentity4x4();

        // T
        local = FixedMatMath.multiply4x4(local,
                FixedMatMath.createTranslation4x4(obj.tx, obj.ty, obj.tz));

        // Rz, Ry, Rx (whatever order you prefer)
        local = FixedMatMath.multiply4x4(local,
                FixedMatMath.createRotationZ4x4(obj.rotZ));
        local = FixedMatMath.multiply4x4(local,
                FixedMatMath.createRotationY4x4(obj.rotY));
        local = FixedMatMath.multiply4x4(local,
                FixedMatMath.createRotationX4x4(obj.rotX));

        // S
        local = FixedMatMath.multiply4x4(local,
                FixedMatMath.createScale4x4(obj.scale, obj.scale, obj.scale));

        // Combine with the “viewMatrix” (which has camera & perspective)
        int[] finalMatrix = FixedMatMath.multiply4x4(viewMatrix, local);

        // Draw edges
        drawEdges(finalMatrix, obj.model, g);
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
            float w0f = (float) p0[3];
            float w1f = (float) p1[3];

            float x0f = p0[0] / w0f;
            float y0f = p0[1] / w0f;
            float z0f = -p0[2]; // -Z is forward.

            float x1f = p1[0] / w1f;
            float y1f = p1[1] / w1f;
            float z1f = -p1[2]; // -Z is forward.

            // map x,y to screen
            int sx0 = (int) (SharedData.halfW + x0f * SharedData.halfW);
            int sy0 = (int) (SharedData.halfH - y0f * SharedData.halfH);
            int sx1 = (int) (SharedData.halfW + x1f * SharedData.halfW);
            int sy1 = (int) (SharedData.halfH - y1f * SharedData.halfH);


            // TODO: move this out into SharedData and update from within Scene.
            float nearPlaneZ = -FixedBaseMath.toQ24_8(Constants.Common.Z_NEAR);
            float farPlaneZ = -FixedBaseMath.toQ24_8(Constants.Common.Z_FAR);

            // near-plane cull
            if ((z0f > nearPlaneZ) || (z1f > nearPlaneZ)) {
                continue;
            }

            // far-plane cull
            if ((z0f < farPlaneZ) || (z1f < farPlaneZ)) {
                continue;
            }

            // X-planes cull (skip when both vertices are off)
            if ((sx0 < SharedData.leftPlaneX) || (sx1 > SharedData.rightPlaneX)) {
                continue;
            }

            // Y-planes cull (skip when both vertices are off)
            if ((sy0 < SharedData.upPlaneY) || (sy1 > SharedData.downPlaneY)) {
                continue;
            }

            //drawLine(sx0, sy0, sx1, sy1, 0xFF00FF); // Example color
            g.drawLine(sx0, sy0, sx1, sy1);
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
      //  System.out.println("drawLine: (" + x0 + ", " + y0 + ") to (" + x1 + ", " + y1 + ")");

        // ... (Clipping - Optional) ...

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int err = dx - dy;

        System.out.println("  dx=" + dx + ", dy=" + dy + ", sx=" + sx + ", sy=" + sy + ", err=" + err);

        while (true) {
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                if (occlusionBuffer[y0 * width + x0] == 0) {
                   // System.out.println("    Plotting pixel: (" + x0 + ", " + y0 + ")");
                    frameBuffer[y0 * width + x0] = color;
                } else {
                   // System.out.println("    Pixel occluded: (" + x0 + ", " + y0 + ")");
                    return;
                }
            }

            if (x0 == x1 && y0 == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
            System.out.println("  x0=" + x0 + ", y0=" + y0 + ", err=" + err);
        }
    }

    private int[] projectBoundingBoxToScreen(SceneObject obj, int[] viewMatrix) {
   // System.out.println("projectBoundingBoxToScreen called for object: " + obj);

    // Use the object's model's bounding box directly
    int[] bbox = new int[] {
        obj.model.minX, obj.model.minY, obj.model.minZ,
        obj.model.maxX, obj.model.maxY, obj.model.maxZ
    };

    // 1. Transform the bounding box corners to world space using object's transform
    int[] local = FixedMatMath.createIdentity4x4();
    local = FixedMatMath.multiply4x4(local, FixedMatMath.createTranslation4x4(obj.tx, obj.ty, obj.tz));
    local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationZ4x4(obj.rotZ));
    local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationY4x4(obj.rotY));
    local = FixedMatMath.multiply4x4(local, FixedMatMath.createRotationX4x4(obj.rotX));
    local = FixedMatMath.multiply4x4(local, FixedMatMath.createScale4x4(obj.scale, obj.scale, obj.scale));

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (int i = 0; i < 8; i++) {
        int[] corner = {
            (i & 1) == 0 ? bbox[0] : bbox[3], // x (min or max)
            (i & 2) == 0 ? bbox[1] : bbox[4], // y (min or max)
            (i & 4) == 0 ? bbox[2] : bbox[5]  // z (min or max)
        };

       // System.out.println("  Corner " + i + " (local): [" + corner[0] + ", " + corner[1] + ", " + corner[2] + "]");

        int[] worldCorner = transformPointQ24_8(local, corner);

       // System.out.println("  Corner " + i + " (world): [" + worldCorner[0] + ", " + worldCorner[1] + ", " + worldCorner[2] + ", " + worldCorner[3] + "]");

        // 2. Transform the world space corners to view space using viewMatrix
        int[] viewCorner = transformPointQ24_8(viewMatrix, worldCorner);

       // System.out.println("  Corner " + i + " (view): [" + viewCorner[0] + ", " + viewCorner[1] + ", " + viewCorner[2] + ", " + viewCorner[3] + "]");

        // 3. Project the view space corners to screen space
        int[] screenCorner = projectPointToScreen(viewCorner);

        // 4. Update min/max screen coordinates
        if (screenCorner != null) {
          //  System.out.println("  Corner " + i + " (screen): [" + screenCorner[0] + ", " + screenCorner[1] + "]");

            minX = Math.min(minX, screenCorner[0]);
            minY = Math.min(minY, screenCorner[1]);
            maxX = Math.max(maxX, screenCorner[0]);
            maxY = Math.max(maxY, screenCorner[1]);
        }
    }

    if (minX == Integer.MAX_VALUE || minY == Integer.MAX_VALUE || maxX == Integer.MIN_VALUE || maxY == Integer.MIN_VALUE) {
      //  System.out.println("  Object is completely off-screen or invalid projection.");
        return null;
    }
    
   // System.out.println("  Screen Bounding Box: [" + minX + ", " + minY + ", " + maxX + ", " + maxY + "]");

    return new int[]{minX, minY, maxX, maxY};
}

    private int[] projectPointToScreen(int[] p) {
        // Skip if w=0
        if (p[3] == 0) {
            return null;
        }

        float w = (float) p[3];
        float x = p[0] / w;
        float y = p[1] / w;
        float z = -p[2]; // -Z is forward

        // Far-plane cull
        float farPlaneZ = -FixedBaseMath.toQ24_8(Constants.Common.Z_FAR);
        if (z < farPlaneZ) {
            return null;
        }

        // Map x, y to screen
        int sx = (int) (SharedData.halfW + x * SharedData.halfW);
        int sy = (int) (SharedData.halfH - y * SharedData.halfH);

        // X-planes cull
        if (sx < SharedData.leftPlaneX || sx > SharedData.rightPlaneX) {
            return null;
        }

        // Y-planes cull
        if (sy < SharedData.upPlaneY || sy > SharedData.downPlaneY) {
            return null;
        }

        return new int[]{sx, sy};
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
            out4[row] = (int) (sum >> 8);
        }
        return out4;
    }

    // You'll need to implement this based on your depth calculation needs
    private void sortRenderablesByDepth(Vector renderables) {
        // A simple bubble sort for demonstration. 
        // Consider more efficient sorting algorithms if you have many objects.
        int n = renderables.size();
        boolean swapped;
        do {
            swapped = false;
            for (int i = 1; i < n; i++) {
                SceneObject obj1 = (SceneObject) renderables.elementAt(i - 1);
                SceneObject obj2 = (SceneObject) renderables.elementAt(i);

                // Calculate average depth for sorting. 
                // You might need a more sophisticated depth calculation depending on your needs.
                int depth1 = obj1.tz;
                int depth2 = obj2.tz;

                if (depth1 < depth2) { // Sort from closest to farthest

                    renderables.setElementAt(obj2, i - 1);
                    renderables.setElementAt(obj1, i);
                    swapped = true;
                }
            }
            n--;
        } while (swapped);
    }
}