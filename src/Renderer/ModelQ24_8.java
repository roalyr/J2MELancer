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
    
    public ModelQ24_8(int[][] vertices, int[][] edges) {
        this.vertices = vertices;
        this.edges    = edges;
    }
}
