package Models;

import FixedMath.FixedBaseMath;

public class Sphere {

    public static int[][] VERTICES;
    public static int[][] EDGES;

    public static final int minX;
    public static final int minY;
    public static final int minZ;
    public static final int maxX;
    public static final int maxY;
    public static final int maxZ;

    public static final int BOUNDING_SPHERE_RADIUS = FixedBaseMath.toQ24_8(1.0f);

    static {
        // Define parameters for the UV sphere
        int num_segments = 8; // Number of horizontal segments (longitude)
        int num_rings = 8; // Number of vertical rings (latitude)

        // Calculate the number of VERTICES and edges
        int numVERTICES = (num_segments + 1) * (num_rings + 1);
        int numEdges = num_segments * num_rings * 2;

        // Initialize VERTICES and edges arrays
        VERTICES = new int[numVERTICES][3];
        EDGES = new int[numEdges][2];

        // Generate VERTICES
        int vertexIndex = 0;
        for (int i = 0; i <= num_rings; i++) {
            float phi = (float) Math.PI * i / num_rings; // Vertical angle
            int sinPhi = FixedBaseMath.toQ24_8((float)Math.sin(phi));
            int cosPhi = FixedBaseMath.toQ24_8((float)Math.cos(phi));

            for (int j = 0; j <= num_segments; j++) {
                float theta = 2 * (float) Math.PI * j / num_segments; // Horizontal angle
                int sinTheta = FixedBaseMath.toQ24_8((float)Math.sin(theta));
                int cosTheta = FixedBaseMath.toQ24_8((float)Math.cos(theta));

                // Calculate vertex coordinates in Q24.8 format
                int x = FixedBaseMath.q24_8_mul(cosTheta, sinPhi);
                int y = cosPhi;
                int z = FixedBaseMath.q24_8_mul(sinTheta, sinPhi);

                VERTICES[vertexIndex++] = new int[]{x, y, z};
            }
        }

        // Generate edges
        int edgeIndex = 0;
        for (int i = 0; i < num_rings; i++) {
            for (int j = 0; j < num_segments; j++) {
                int p0 = i * (num_segments + 1) + j;
                int p1 = p0 + 1;
                int p2 = p0 + num_segments + 1;
                int p3 = p2 + 1;

                EDGES[edgeIndex++] = new int[]{p0, p1}; // Horizontal edge
                EDGES[edgeIndex++] = new int[]{p0, p2}; // Vertical edge
            }
        }
        
        // Calculate bounding box 
        int minX_temp = VERTICES[0][0], minY_temp = VERTICES[0][1], minZ_temp = VERTICES[0][2];
        int maxX_temp = VERTICES[0][0], maxY_temp = VERTICES[0][1], maxZ_temp = VERTICES[0][2];

        for (int i = 1; i < VERTICES.length; i++) {
            int x = VERTICES[i][0];
            int y = VERTICES[i][1];
            int z = VERTICES[i][2];

            if (x < minX_temp) minX_temp = x;
            if (x > maxX_temp) maxX_temp = x;
            if (y < minY_temp) minY_temp = y;
            if (y > maxY_temp) maxY_temp = y;
            if (z < minZ_temp) minZ_temp = z;
            if (z > maxZ_temp) maxZ_temp = z;
        }

        minX = minX_temp;
        minY = minY_temp;
        minZ = minZ_temp;
        maxX = maxX_temp;
        maxY = maxY_temp;
        maxZ = maxZ_temp;
    }
}