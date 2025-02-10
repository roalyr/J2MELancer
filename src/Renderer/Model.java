package Renderer;

public class Model {

    public long[][] vertices; // each vertex is [x,y,z] in Q
    public int[][] edges;     // indices into vertices
    public long boundingSphereRadius;
    
    public Model(long[][] vertices, int[][] edges, long boundingSphereRadius) {
        this.vertices = vertices;
        this.edges = edges;
        this.boundingSphereRadius = boundingSphereRadius;
    }
}
