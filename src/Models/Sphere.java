package Models;

import Renderer.Model;
import FixedMath.FixedBaseMath;

/**
 * Generates a UV sphere in Q24.8 fixed-point format, wireframe style.
 */
public class Sphere {

    /**
     * Creates a UV-sphere model with the given number of horizontal
     * segments (numSegments) and vertical rings (numRings).
     * The resulting Model has its boundingSphereRadius set to 1.0 in Q24.8.
     *
     * @param numSegments Number of horizontal segments (longitude)
     * @param numRings    Number of vertical rings (latitude)
     * @return a Model representing the sphere
     */
    public static Model create(int numSegments, int numRings) {
        long[][] vertices = generateVertices(numSegments, numRings);
        int[][] edges = generateEdges(numSegments, numRings);
        long boundingSphere = FixedBaseMath.toFixed(1.0f);
        return new Model(vertices, edges, boundingSphere);
    }

    /**
     * Generates sphere vertices in Q24.8 by sweeping phi (vertical)
     * and theta (horizontal).
     */
    private static long[][] generateVertices(int numSegments, int numRings) {
        int numVertices = (numSegments + 1) * (numRings + 1);
        long[][] verts = new long[numVertices][3];
        int vertexIndex = 0;

        for (int i = 0; i <= numRings; i++) {
            float phi = (float) Math.PI * i / numRings;  // 0..PI
            long sinPhi = FixedBaseMath.toFixed((float) Math.sin(phi));
            long cosPhi = FixedBaseMath.toFixed((float) Math.cos(phi));

            for (int j = 0; j <= numSegments; j++) {
                float theta = 2.0f * (float) Math.PI * j / numSegments; // 0..2PI
                long sinTheta = FixedBaseMath.toFixed((float) Math.sin(theta));
                long cosTheta = FixedBaseMath.toFixed((float) Math.cos(theta));

                long x = FixedBaseMath.fixedMul(cosTheta, sinPhi);
                long y = cosPhi;
                long z = FixedBaseMath.fixedMul(sinTheta, sinPhi);

                verts[vertexIndex++] = new long[]{x, y, z};
            }
        }
        return verts;
    }

    /**
     * Generates wireframe edges between adjacent vertices
     * in the horizontal and vertical directions.
     */
    private static int[][] generateEdges(int numSegments, int numRings) {
        int numEdges = numSegments * numRings * 2;
        int[][] edges = new int[numEdges][2];
        int edgeIndex = 0;

        for (int i = 0; i < numRings; i++) {
            for (int j = 0; j < numSegments; j++) {
                int p0 = i * (numSegments + 1) + j;
                int p1 = p0 + 1;
                int p2 = p0 + numSegments + 1;

                edges[edgeIndex++] = new int[]{p0, p1};
                edges[edgeIndex++] = new int[]{p0, p2};
            }
        }
        return edges;
    }
}
