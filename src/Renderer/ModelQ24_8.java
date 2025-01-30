package Renderer;


/**
 * A geometry container in Q24.8, e.g. a list of vertices + edges.
 * Similar to your single-cube approach, but can hold any shape.
 */
public class ModelQ24_8 {

    public int[][] vertices; // each is [x,y,z] in Q24.8
    public int[][] edges;    // pairs of indices

    public int minX;
    public int minY;
    public int minZ;

    public int maxX;
    public int maxY;
    public int maxZ;

    public int boundingSphereRadius;
    
    public ModelQ24_8(int[][] vertices, int[][] edges, int boundingSphereRadius) {
        this.vertices = vertices;
        this.edges = edges;
        this.boundingSphereRadius = boundingSphereRadius;

        // Initialize bounding box
        if (vertices.length > 0) {
            minX = maxX = vertices[0][0];
            minY = maxY = vertices[0][1];
            minZ = maxZ = vertices[0][2];
        } else {
            // Handle empty model case (up to you how you want to define this)
            minX = minY = minZ = maxX = maxY = maxZ = 0; 
        }

        // Calculate bounding box
        for (int i = 1; i < vertices.length; i++) {
            int x = vertices[i][0];
            int y = vertices[i][1];
            int z = vertices[i][2];

            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
    }
}