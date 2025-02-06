package Models;

import FixedMath.FixedBaseMath;
import Renderer.Model;

/**
 * Generates a UV sphere in Q24.8 fixed-point format, wireframe style.
 */
public class Sphere {

    /**
     * Creates a UV-sphere model with the given number of horizontal
     * segments (numSegments) and vertical rings (numRings).
     * The resulting ModelQ24_8 has its boundingSphereRadius set to 1.0 in Q24.8
     * (since it is effectively a "unit sphere" from -1..+1).
     *
     * @param numSegments Number of horizontal segments (longitude)
     * @param numRings    Number of vertical rings (latitude)
     * @return a ModelQ24_8 representing the sphere
     */
    public static Model create(int numSegments, int numRings) {
        int[][] verts = generateVertices(numSegments, numRings);
        int[][] edges = generateEdges(numSegments, numRings);

        // For a normalized sphere in [-1..+1], radius = 1.0
        // (If you consider the sphere in [-0.5..+0.5], adjust as needed.)
        int boundingSphere = FixedBaseMath.toQ24_8(1.0f);

        return new Model(verts, edges, boundingSphere);
    }

    /**
     * Generates sphere vertices in Q24.8 by sweeping phi (vertical)
     * and theta (horizontal).
     */
    private static int[][] generateVertices(int numSegments, int numRings) {
        int numVertices = (numSegments + 1) * (numRings + 1);
        int[][] verts = new int[numVertices][3];
        int vertexIndex = 0;

        for (int i = 0; i <= numRings; i++) {
            float phi = (float) Math.PI * i / numRings;  // 0..PI
            int sinPhi = FixedBaseMath.toQ24_8((float)Math.sin(phi));
            int cosPhi = FixedBaseMath.toQ24_8((float)Math.cos(phi));

            for (int j = 0; j <= numSegments; j++) {
                float theta = 2.0f * (float)Math.PI * j / numSegments; // 0..2PI
                int sinTheta = FixedBaseMath.toQ24_8((float)Math.sin(theta));
                int cosTheta = FixedBaseMath.toQ24_8((float)Math.cos(theta));

                // x = sinPhi * cosTheta
                // y = cosPhi
                // z = sinPhi * sinTheta
                int x = FixedBaseMath.q24_8_mul(cosTheta, sinPhi);
                int y = cosPhi;
                int z = FixedBaseMath.q24_8_mul(sinTheta, sinPhi);

                verts[vertexIndex++] = new int[]{ x, y, z };
            }
        }
        return verts;
    }

    /**
     * Generates wireframe edges between adjacent vertices
     * in the horizontal and vertical directions.
     */
    private static int[][] generateEdges(int numSegments, int numRings) {
        // Each "quad" gets 2 edges: horizontal + vertical
        // So total edges = numSegments * numRings * 2
        int numEdges = numSegments * numRings * 2;
        int[][] edges = new int[numEdges][2];
        int edgeIndex = 0;

        for (int i = 0; i < numRings; i++) {
            for (int j = 0; j < numSegments; j++) {
                int p0 = i * (numSegments + 1) + j;
                int p1 = p0 + 1;
                int p2 = p0 + numSegments + 1;

                // horizontal edge
                edges[edgeIndex++] = new int[]{ p0, p1 };
                // vertical edge
                edges[edgeIndex++] = new int[]{ p0, p2 };
            }
        }
        return edges;
    }
}