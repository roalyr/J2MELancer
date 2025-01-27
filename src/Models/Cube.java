package Models;

import Constants.Common;

public class Cube {
    private static int one_p = Common.ONE_POS;
    private static int one_n = Common.ONE_NEG;
    public static final int[][] VERTICES = {
        {one_n, one_n, one_n}, {one_n, one_n, one_p},
        {one_n, one_p, one_n}, {one_n, one_p, one_p},
        {one_p, one_n, one_n}, {one_p, one_n, one_p},
        {one_p, one_p, one_n}, {one_p, one_p, one_p}
    };
    public static final int[][] EDGES = {
        {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7},
        {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

}
