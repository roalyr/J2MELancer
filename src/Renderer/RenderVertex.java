package Renderer;

public class RenderVertex {

    /**
     * Draw a single 'vertex' shape at (x, y) with given color and depth test.
     */
    public static void drawVertex(
            int shape,
            int width,
            int height,
            int[] frameBuffer,
            long[] depthBuffer,
            int x,
            int y,
            long z,
            int color
    ) {
        RenderEffects.drawMarkerDepthTest(shape, width, height, frameBuffer, depthBuffer, x, y, color, z);
    }
}
