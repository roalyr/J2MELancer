package Renderer;

import FixedMath.FixedBaseMath;

public class Material {

    public int colorNear;    // e.g., 0xFFFF0000
    public int colorFar;     // e.g., 0x110000FF
    
    public long nearMarginQ; // in Q
    public long farMarginQ;  // in Q
    
    public long fadeDistanceNearQ;  // in Q
    public long fadeDistanceFarQ;  // in Q

    public int renderType;   // e.g., 0 = vertices, 1 = edges
    public int primitiveWidth;
    public int primitiveShape;
    public int ditherLevel;

    public Material(
            int colorNear, 
            int colorFar,
            
            long nearMarginQ, 
            long farMarginQ, 
            
            long fadeDistanceQNear, 
            long fadeDistanceQFar,
            
            int renderType, 
            int primitiveWidth, 
            int primitiveShape, 
            int ditherLevel) {
        
        this.colorNear = colorNear;
        this.colorFar = colorFar;
        this.nearMarginQ = nearMarginQ;
        this.farMarginQ = farMarginQ;
        if (this.nearMarginQ <= 0) {
            this.nearMarginQ = FixedBaseMath.toFixed(1.0f);
        }
        if (this.farMarginQ <= 0) {
            this.farMarginQ = FixedBaseMath.toFixed(1.0f);
        }
        this.fadeDistanceNearQ = fadeDistanceQNear;
        this.fadeDistanceFarQ = fadeDistanceQFar;
        this.renderType = renderType;     
        this.primitiveWidth = primitiveWidth;
        this.primitiveShape = primitiveShape;
        this.ditherLevel = ditherLevel;
    }
}
