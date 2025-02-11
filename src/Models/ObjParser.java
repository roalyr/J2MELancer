package Models;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Vector;
import Renderer.Model;
import FixedMath.*;

public class ObjParser {

    public static Model create(String resourcePath) throws IOException {
        Vector vertices = new Vector(); // will store long[] of size 3
        Vector edges = new Vector();    // will store int[] of size 2
        long boundingSphereRadius = 0;

        // Open the resource file as an InputStream
        InputStream is = ObjParser.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Resource not found: " + resourcePath);
        }
        InputStreamReader isr = new InputStreamReader(is);

        String line;
        while ((line = readLine(isr)) != null) {
            int firstSpace = line.indexOf(' ');
            int secondSpace = line.indexOf(' ', firstSpace + 1);
            
            if (firstSpace > 0 && secondSpace > firstSpace) {
                String type = line.substring(0, firstSpace);
                
                if (type.equals("v")) {  // vertex line
                    int thirdSpace = line.indexOf(' ', secondSpace + 1);
                    if (thirdSpace == -1) {
                        // Malformed vertex line: skip it.
                        continue;
                    }
                    // Parse the three coordinates
                    long x = FixedBaseMath.toFixed(parseFloat(line.substring(firstSpace + 1, secondSpace)));
                    long y = FixedBaseMath.toFixed(parseFloat(line.substring(secondSpace + 1, thirdSpace)));
                    long z = FixedBaseMath.toFixed(parseFloat(line.substring(thirdSpace + 1)));
                    
                    // Add vertex as an array of three longs
                    vertices.addElement(new long[] { x, y, z });
                    
                    // Update bounding sphere radius
                    long distance = FixedBaseMath.fixedHypot3D(x, y, z);
                    if (distance > boundingSphereRadius) {
                        boundingSphereRadius = distance;
                    }
                } else if (type.equals("l") && line.substring(secondSpace + 1).indexOf(' ') == -1) {
                    // Only consider lines ("l") with exactly two vertex indices
                    int v1 = Integer.parseInt(line.substring(firstSpace + 1, secondSpace)) - 1; // adjust for 0-based index
                    int v2 = Integer.parseInt(line.substring(secondSpace + 1)) - 1;
                    
                    // Add edge as an array of two ints
                    edges.addElement(new int[] { v1, v2 });
                }
            }
        }
        
        // Close streams
        isr.close();
        is.close();
        
        // Convert Vectors to arrays:
        // For vertices, create a 2D array (each vertex is a long[3])
        long[][] verticesArray = new long[vertices.size()][];
        for (int i = 0; i < vertices.size(); i++) {
            verticesArray[i] = (long[]) vertices.elementAt(i);
        }
        
        // For edges, create a 2D array (each edge is an int[2])
        int[][] edgesArray = new int[edges.size()][];
        for (int i = 0; i < edges.size(); i++) {
            edgesArray[i] = (int[]) edges.elementAt(i);
        }
        
        return new Model(verticesArray, edgesArray, boundingSphereRadius);
    }

    /**
     * Reads a line of text from the InputStreamReader.
     * Returns null if the end of stream is reached.
     */
    private static String readLine(InputStreamReader isr) throws IOException {
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = isr.read()) != -1) {
            if (c == '\n') {
                break;
            }
            // Skip carriage returns
            if (c != '\r') {
                sb.append((char) c);
            }
        }
        // If we reached end-of-stream and no characters were read, return null
        if (c == -1 && sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }
    
    /**
     * Parses a float from a String.
     * In Java 1.3 you may need to use an alternative if Float.parseFloat is not available.
     */
    private static float parseFloat(String s) {
        return Float.valueOf(s).floatValue();
    }
}
