package Models;

import Renderer.Model;
import FixedMath.FixedBaseMath;

/**
 * Generates a ring (circle) model in the XZ plane.
 * The ring is approximated by a polygon with the given number of segments.
 * All coordinates are in Q24.8 fixed‐point format.
 */
public class RingHorizontal {

    /**
     * Creates a horizontal ring model with the specified number of segments.
     * The ring is centered at the origin in the XZ plane with a radius of 1.0.
     * The model consists solely of edges connecting consecutive vertices,
     * with an edge closing the ring (from the last vertex back to the first).
     *
     * @param segments the number of segments (and vertices) for the ring (minimum 3)
     * @return a Model representing the horizontal ring
     */
    public static Model create(int segments) {
        if (segments < 3) {
            segments = 3;
        }
        
        float radius = 1.0f;
        long[][] vertices = new long[segments][3];
        int[][] edges = new int[segments][2];
        
        // Generate vertices on the circle in the XZ plane (Y = 0)
        for (int i = 0; i < segments; i++) {
            float angle = (float)(2.0 * Math.PI * i / segments);
            long xQ = FixedBaseMath.toFixed((float)Math.cos(angle) * radius);
            long yQ = FixedBaseMath.toFixed(0.0f); // Y remains 0 (horizontal plane)
            long zQ = FixedBaseMath.toFixed((float)Math.sin(angle) * radius);
            vertices[i] = new long[]{xQ, yQ, zQ};
        }
        
        // Generate edges connecting consecutive vertices, with the last connecting to the first
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            edges[i] = new int[]{i, next};
        }
        
        long boundingSphere = FixedBaseMath.toFixed(radius);
        return new Model(vertices, edges, boundingSphere);
    }
}
