package Renderer;

import FixedMath.FixedBaseMath;

public class Material {

    public int colorNear;    // e.g., 0xFFFF0000
    public int colorFar;     // e.g., 0x110000FF
    
    public long nearMarginQ24_8; // in Q
    public long farMarginQ24_8;  // in Q
    public long fadeDistanceFarQ24_8;  // in Q
    public long fadeDistanceNearQ24_8;  // in Q

    public int renderType;   // e.g., 0 = vertices, 1 = edges
    public int primitiveWidth;
    public int primitiveShape;
    public int ditherLevel;

    public Material(int colorNear, int colorFar,
                    long nearMarginQ, long farMarginQ, long fadeDistanceQNear, long fadeDistanceQFar,
                    int renderType, int primitiveWidth, int primitiveShape, int ditherLevel) {
        this.colorNear = colorNear;
        this.colorFar = colorFar;
        this.nearMarginQ24_8 = nearMarginQ;
        this.farMarginQ24_8 = farMarginQ;
        if (nearMarginQ24_8 <= 0) {
            nearMarginQ24_8 = FixedBaseMath.toFixed(1.0f);
        }
        if (farMarginQ24_8 <= 0) {
            farMarginQ24_8 = FixedBaseMath.toFixed(1.0f);
        }
        this.fadeDistanceNearQ24_8 = fadeDistanceQNear;
        this.fadeDistanceFarQ24_8 = fadeDistanceQFar;
        this.renderType = renderType;     
        this.primitiveWidth = primitiveWidth;
        this.primitiveShape = primitiveShape;
        this.ditherLevel = ditherLevel;
    }
}
