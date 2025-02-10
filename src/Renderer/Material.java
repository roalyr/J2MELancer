package Renderer;

public class Material {

    public int colorNear;    // e.g., 0xFFFF0000
    public int colorFar;     // e.g., 0x110000FF
    
    public int nearMarginQ24_8; // e.g., near plane offset in Q24.8
    public int farMarginQ24_8;  // e.g., far plane offset in Q24.8
    public int fadeDistanceFarQ24_8;  // e.g., 5 units in Q24.8
    public int fadeDistanceNearQ24_8;  // e.g., 5 units in Q24.8

    public int renderType;   // e.g., 0 = vertices, 1 = edges (or flags)
    public int primitiveWidth;
    public int primitiveShape;
    public int ditherLevel;

    public Material(int colorNear, int colorFar,
            int nearMarginQ, int farMarginQ, int fadeDistanceQNear, int fadeDistanceQFar,
            int renderType, int primitiveWidth, int primitiveShape, int ditherLevel) {
        this.colorNear = colorNear;
        this.colorFar = colorFar;
        
        this.nearMarginQ24_8 = nearMarginQ;
        this.farMarginQ24_8 = farMarginQ;
        
        // Limit values to prevent issues with overflow and zero division.
        if (nearMarginQ24_8 <= 0) {
            nearMarginQ24_8 = 256;
        }
        if (farMarginQ24_8 <= 0) {
            farMarginQ24_8 = 256;
        }
        
        // TODO: add maximum values checks.
        
        
        this.fadeDistanceNearQ24_8 = fadeDistanceQNear;
        this.fadeDistanceFarQ24_8 = fadeDistanceQFar;
        
        this.renderType = renderType;     
        this.primitiveWidth = primitiveWidth;
        this.primitiveShape = primitiveShape;
        this.ditherLevel = ditherLevel;

    }
}