package Renderer;

public class Material {
    public int colorNear;    // e.g., 0xFFFF0000
    public int colorFar;     // e.g., 0x110000FF
    public float colorExponent;
    public int nearMarginQ24_8; // e.g., near plane offset in Q24.8
    public int farMarginQ24_8;  // e.g., far plane offset in Q24.8
    public int renderType;   // e.g., 0 = vertices, 1 = edges (or flags)
    public int primitiveWidth;
    
    // How far from nearMargin/farMargin we ramp alpha from 0..1 or 1..0
    public int fadeDistanceQ24_8;  // e.g., 5 units in Q24.8

    public Material(int colorNear, int colorFar, float colorExponent,
                    int nearMarginQ, int farMarginQ, int fadeDistanceQ,
                    int renderType, int primitiveWidth) {
        this.colorNear = colorNear;
        this.colorFar = colorFar;
        this.colorExponent = colorExponent;
        this.nearMarginQ24_8 = nearMarginQ;
        this.farMarginQ24_8  = farMarginQ;
        this.renderType      = renderType;
        this.primitiveWidth  = primitiveWidth;
        this.fadeDistanceQ24_8 = fadeDistanceQ;
       
    }
}