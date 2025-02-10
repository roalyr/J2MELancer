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
     * Creates a ModelQ24_8 representing a subdivided cube. All coordinates
     * are in Q24.8 fixed-point format, covering the region [-0.5,+0.5]^3.
     *
     * @param subdiv number of subdivisions per edge (>=1).
     * @return a ModelQ24_8 with vertices, edges, and boundingSphereRadius = 1.0 in Q24.8
     */
    public static Model create(int subdiv) {
        if (subdiv < 1) {
            subdiv = 1;
        }
        // Number of "points" along each axis: subdiv+1
        int n = subdiv + 1;

        // We'll store vertex positions in a dynamic list first
        Vector vertexList = new Vector();  // each element is int[]{xQ, yQ, zQ}

        // This 3D array maps (x,y,z) indices -> the index in vertexList (or -1 if not a surface)
        int[][][] indexMap = new int[n][n][n];
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    indexMap[x][y][z] = -1;
                }
            }
        }

        // Step 1: Generate all surface vertices
        int vertexCount = 0;
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    // Check if it's on the "surface" of the cube
                    if (x == 0 || x == n - 1 ||
                            y == 0 || y == n - 1 ||
                            z == 0 || z == n - 1) {

                        // Map x,y,z in [0..n-1] to [-0.5..+0.5]
                        float fx = ((float) x / (n - 1)) - 0.5f; // [0..1] -> [-0.5..0.5]

                        float fy = ((float) y / (n - 1)) - 0.5f;
                        float fz = ((float) z / (n - 1)) - 0.5f;

                        int xQ = FixedBaseMath.toFixed(fx);
                        int yQ = FixedBaseMath.toFixed(fy);
                        int zQ = FixedBaseMath.toFixed(fz);

                        // Store into vertex list
                        vertexList.addElement(new int[]{xQ, yQ, zQ});
                        indexMap[x][y][z] = vertexCount;
                        vertexCount++;
                    }
                }
            }
        }

        // Convert Vector -> int[][] for the final vertices array
        int[][] vertices = new int[vertexCount][3];
        for (int i = 0; i < vertexCount; i++) {
            vertices[i] = (int[]) vertexList.elementAt(i);
        }

        // Step 2: Build the edges. We'll connect each surface point to
        // adjacent points along +X, +Y, +Z if they exist on the surface.
        Vector edgeList = new Vector();
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    int idx = indexMap[x][y][z];
                    if (idx < 0) {
                        continue; // not a surface point

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

        // Convert edgeList -> int[][]
        int[][] edges = new int[edgeList.size()][2];
        for (int i = 0; i < edgeList.size(); i++) {
            edges[i] = (int[]) edgeList.elementAt(i);
        }

        // Step 3: bounding sphere radius for a [-0.5..+0.5] cube is ~0.866 * scale
        // but let's keep it as 1.0 in Q24.8 for simpler usage:
        int boundingSphere = FixedBaseMath.toFixed(1.0f);

        return new Model(vertices, edges, boundingSphere);
    }

    // Example usage:
    // ModelQ24_8 model = CubeSubdiv.create(3); // subdiv=3 => 4 points along each edge
}