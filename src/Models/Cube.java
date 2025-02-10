package Models;

import FixedMath.FixedBaseMath;
import java.util.Vector;
import Renderer.Model;

/**
 * Generates a subdivided cube from -0.5..+0.5 in each dimension.
 * Subdivision parameter "subdiv" means how many segments along each edge:
 *   subdiv=1 => standard cube with 8 corners.
 *   subdiv=2 => adds 1 intermediate vertex per edge, etc.
 */
public class Cube {

    /**
     * Creates a Model representing a subdivided cube. All coordinates
     * are in Q24.8 fixed-point format, covering the region [-0.5,+0.5]^3.
     *
     * @param subdiv number of subdivisions per edge (>=1).
     * @return a Model with vertices, edges, and boundingSphereRadius = 1.0 in Q24.8
     */
    public static Model create(int subdiv) {
        if (subdiv < 1) {
            subdiv = 1;
        }
        int n = subdiv + 1;

        Vector vertexList = new Vector();  // each element is long[]{xQ, yQ, zQ}
        int[][][] indexMap = new int[n][n][n];
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    indexMap[x][y][z] = -1;
                }
            }
        }

        int vertexCount = 0;
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    // Check if it's on the "surface" of the cube
                    if (x == 0 || x == n - 1 ||
                        y == 0 || y == n - 1 ||
                        z == 0 || z == n - 1) {

                        // Map x,y,z in [0..n-1] to [-0.5..+0.5]
                        float fx = ((float) x / (n - 1)) - 0.5f;
                        float fy = ((float) y / (n - 1)) - 0.5f;
                        float fz = ((float) z / (n - 1)) - 0.5f;

                        long xQ = FixedBaseMath.toFixed(fx);
                        long yQ = FixedBaseMath.toFixed(fy);
                        long zQ = FixedBaseMath.toFixed(fz);

                        vertexList.addElement(new long[]{xQ, yQ, zQ});
                        indexMap[x][y][z] = vertexCount;
                        vertexCount++;
                    }
                }
            }
        }

        long[][] vertices = new long[vertexCount][3];
        for (int i = 0; i < vertexCount; i++) {
            vertices[i] = (long[]) vertexList.elementAt(i);
        }

        Vector edgeList = new Vector();
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    int idx = indexMap[x][y][z];
                    if (idx < 0) {
                        continue;
                    }
                    // neighbor in +X direction
                    if (x + 1 < n && indexMap[x + 1][y][z] >= 0) {
                        edgeList.addElement(new int[]{idx, indexMap[x + 1][y][z]});
                    }
                    // neighbor in +Y direction
                    if (y + 1 < n && indexMap[x][y + 1][z] >= 0) {
                        edgeList.addElement(new int[]{idx, indexMap[x][y + 1][z]});
                    }
                    // neighbor in +Z direction
                    if (z + 1 < n && indexMap[x][y][z + 1] >= 0) {
                        edgeList.addElement(new int[]{idx, indexMap[x][y][z + 1]});
                    }
                }
            }
        }

        int[][] edges = new int[edgeList.size()][2];
        for (int i = 0; i < edgeList.size(); i++) {
            edges[i] = (int[]) edgeList.elementAt(i);
        }

        long boundingSphere = FixedBaseMath.toFixed(1.0f);
        return new Model(vertices, edges, boundingSphere);
    }
}
