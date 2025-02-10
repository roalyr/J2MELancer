package Renderer;


/**
 * A geometry container in Q24.8, e.g. a list of vertices + edges.
 * Similar to your single-cube approach, but can hold any shape.
 */
public class Model {

    public int[][] vertices; // each is [x,y,z] in Q24.8
    public int[][] edges;    // pairs of indices

    
    public int boundingSphereRadius;
    
    public Model(int[][] vertices, int[][] edges, int boundingSphereRadius) {
        this.vertices = vertices;
        this.edges = edges;
        this.boundingSphereRadius = boundingSphereRadius;
    }
}