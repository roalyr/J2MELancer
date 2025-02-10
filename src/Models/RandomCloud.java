package Models;

import Renderer.Model;
import FixedMath.FixedBaseMath;
import java.util.Random;

public class RandomCloud {


    /**
     * Creates a Model consisting of a random cloud of vertices
     * uniformly distributed within a unit sphere (radius 1.0).
     * No edges are defined.
     *
     * @param numVertices Number of random vertices to generate.
     * @return a Model containing the random vertices and no edges.
     */
    public static Model create(int numVertices, long seed) {
        int[][] vertices = new int[numVertices][3];
        int count = 0;
        Random rnd = new Random(seed);
        while (count < numVertices) {
            // Generate random float values between -1.0 and 1.0
            float x = (float) (rnd.nextFloat() * 2 - 1);
            float y = (float) (rnd.nextFloat() * 2 - 1);
            float z = (float) (rnd.nextFloat() * 2 - 1);
            // Accept only if inside the unit sphere (x^2 + y^2 + z^2 <= 1.0)
            if ((x * x + y * y + z * z) <= 1.0f) {
                vertices[count][0] = FixedBaseMath.toFixed(x);
                vertices[count][1] = FixedBaseMath.toFixed(y);
                vertices[count][2] = FixedBaseMath.toFixed(z);
                count++;
            }
        }
        // No edges for a random cloud
        int[][] edges = new int[0][0];
        // The bounding sphere for points in a unit sphere is 1.0 (in Q24.8)
        int boundingSphere = FixedBaseMath.toFixed(1.0f);

        return new Model(vertices, edges, boundingSphere);
    }
}
