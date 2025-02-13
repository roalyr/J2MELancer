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
     * @param seed Seed for the random number generator.
     * @return a Model containing the random vertices and no edges.
     */
    public static Model create(int numVertices, long seed) {
        long[][] vertices = new long[numVertices][3];
        int count = 0;
        Random rnd = new Random(seed);
        while (count < numVertices) {
            float x = rnd.nextFloat() * 2 - 1;
            float y = rnd.nextFloat() * 2 - 1;
            float z = rnd.nextFloat() * 2 - 1;
            if ((x * x + y * y + z * z) <= 1.0f) {
                vertices[count][0] = FixedBaseMath.toFixed(x);
                vertices[count][1] = FixedBaseMath.toFixed(y);
                vertices[count][2] = FixedBaseMath.toFixed(z);
                count++;
            }
        }
        int[][] edges = new int[0][0];
        long boundingSphere = FixedBaseMath.FIXED1;
        return new Model(vertices, edges, boundingSphere);
    }
}
