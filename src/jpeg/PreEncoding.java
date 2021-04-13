package jpeg;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.Vector;

/**
 * 图像预处理
 */
public class PreEncoding {
    private static final DCT dct = new DCT();

    /**
     * 余弦函数变换、量化后分割成块
     */
    public static int[][][] encode(double[][] matrix, int type) {
        //首先将图像进行分块
        double[][][] win_tmp = new double[matrix.length / 8 * matrix[1].length / 8][8][8];
        int[][][] win = new int[matrix.length / 8 * matrix[1].length / 8][8][8];
        for (int i = 0; i < matrix.length; i += 8) {
            for (int j = 0; j < matrix[1].length; j += 8) {
                int index = i * matrix[1].length / 64 + j / 8;
                for (int k = 0; k < 8; k++)
                    for (int l = 0; l < 8; l++) {
                        if (j + l < matrix[1].length && index < win_tmp.length) {
                            win_tmp[index][k][l] = matrix[i + k][j + l];
                        }
                    }
            }
        }
        write2File("./debug/pic_8x8_after_DCT_encoding.txt", win_tmp);
        write2File("./debug/pic_256x256_encoding.txt", matrix);
        for (int i = 0; i < win_tmp.length; i++) {
            // 对每个8*8矩阵进行DCT变换
            win_tmp[i] = dct.dct_8(win_tmp[i]);
            // 量化
            if (type == 0) {
                win[i] = Quatization.quatizeY(win_tmp[i]);

            } else {
                win[i] = Quatization.quatizeCbCr(win_tmp[i]);
            }
        }
        return win;
    }


    /**
     * 反量化、iDCT 后组合成整块
     */
    public static double[][] decode(int[][][] matrix, int type, int width, int height) {
        double[][][] win_tmp = new double[matrix.length][matrix[0].length][matrix[0][0].length];
        System.out.println(matrix.length + "pic");
        System.out.println(matrix[1].length + "pi1c");
        for (int i = 0; i < win_tmp.length; i++) {
            if (type == 0)
                win_tmp[i] = Quatization.iQuatizeY(matrix[i]);
            else
                win_tmp[i] = Quatization.iQuatizeUV(matrix[i]);
            win_tmp[i] = dct.idct_8(win_tmp[i]);
        }
        System.out.println(win_tmp.length + "first");
        System.out.println(win_tmp[0].length + "Second");

        int m;
        int n;
        if (type == 0) {
            m = width;
            n = height;
        } else {
            m = width / 2;
            n = height / 2;
        }
        double[][] pic = new double[m][n];
        for (int i = 0; i < pic.length; i += 8) {
            for (int j = 0; j < pic[1].length; j += 8) {
                int index = i * pic[1].length / 64 + j / 8;
                for (int k = 0; k < 8; k++)
                    for (int l = 0; l < 8; l++) {
                        if (j + l < pic[1].length && index < win_tmp.length) {
                            pic[i + k][j + l] = win_tmp[index][k][l];
                        }
                    }
            }
        }
        write2File("./debug/pic_8x8_after_IDCT_decoding.txt", win_tmp);
        write2File("./debug/pic_256x256_decoding.txt", pic);
        System.out.println(pic.length + "pic");
        return pic;
    }

    /**
     * Zig-zag编码 将2d窗格转化为1d数组 同时实现DC的差分编码
     */
    public static int[][] trs21d(int[][][] wins) {
        int[][] data1d = new int[wins.length][8 * 8];
        for (int i = 0; i < wins.length; i++) {
            // DC差分编码
            if (i == 0)
                data1d[0][0] = wins[0][0][0];
            else
                data1d[i][0] = wins[i][0][0] - wins[i - 1][0][0];
            int index = 1, j = 0, k = 1;
            int deltaJ = 1, deltaK = -1;
            // zig-zag编码
            while (!(j > 7 || k > 7)) {
                data1d[i][index++] = wins[i][j][k];
                if (j == 0) {
                    deltaJ = 1;
                    deltaK = -1;
                    if (k % 2 == 0) {
                        k++;
                        continue;
                    }
                }
                if (k == 0) {
                    deltaJ = -1;
                    deltaK = 1;
                    if (j % 2 != 0) {
                        j++;
                        continue;
                    }
                }
                k += deltaK;
                j += deltaJ;
            }
        }
        return data1d;
    }

    /**
     * 反Zig-zag编码 将1d数组转化为2d窗格 同时实现DC的差分解码
     */
    public static int[][][] trs22d(Vector<int[]> data1d) {
        int[][][] wins = new int[data1d.size()][8][8];
        for (int i = 0; i < wins.length; i++) {
            if (i == 0)
                wins[0][0][0] = data1d.elementAt(0)[0];
            else
                wins[i][0][0] = wins[i - 1][0][0] + data1d.elementAt(i)[0];
            int index = 1, j = 0, k = 1;
            int deltaJ = 1, deltaK = -1;
            while (!(j > 7 || k > 7)) {
                wins[i][j][k] = data1d.elementAt(i)[index++];
                if (j == 0) {
                    deltaJ = 1;
                    deltaK = -1;
                    if (k % 2 == 0) {
                        k++;
                        continue;
                    }
                }
                if (k == 0) {
                    deltaJ = -1;
                    deltaK = 1;
                    if (j % 2 != 0) {
                        j++;
                        continue;
                    }
                }
                k += deltaK;
                j += deltaJ;
            }
        }
        return wins;
    }

    public static void write2File(String fileName, double[][][] matrix) {
        FileOutputStream outSTr;
        BufferedOutputStream Buff;
        DecimalFormat df = new DecimalFormat("#.000");
        try {
            outSTr = new FileOutputStream("./" + fileName);
            Buff = new BufferedOutputStream(outSTr);
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[1].length; j++) {
                    for (int k = 0; k < matrix[1][1].length; k++) {
                        String s = df.format(matrix[i][j][k]) + " ";
                        Buff.write(s.getBytes());
                    }
                    Buff.write("\n".getBytes());
                }
                Buff.write("--------------------\n".getBytes());
            }
            Buff.flush();
            Buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write2File(String fileName, int[][][] matrix) {
        FileOutputStream outSTr;
        BufferedOutputStream Buff;
        try {
            outSTr = new FileOutputStream("./" + fileName);
            Buff = new BufferedOutputStream(outSTr);
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[1].length; j++) {
                    for (int k = 0; k < matrix[1][1].length; k++) {
                        String s = matrix[i][j][k] + "\t";
                        Buff.write(s.getBytes());
                    }
                    Buff.write("\n".getBytes());
                }
                Buff.write("-----------------------------\n".getBytes());
            }
            Buff.flush();
            Buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write2File(String fileName, double[][] matrix) {
        FileOutputStream outSTr;
        BufferedOutputStream Buff;
        DecimalFormat df = new DecimalFormat("#.0");
        try {
            outSTr = new FileOutputStream("./" + fileName);
            Buff = new BufferedOutputStream(outSTr);
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[1].length; j++) {
                    String s = df.format(matrix[i][j]) + " ";
                    Buff.write(s.getBytes());

                }
                Buff.write("\n".getBytes());
            }
            Buff.flush();
            Buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write2File(String fileName, int[][] matrix) {
        FileOutputStream outSTr;
        BufferedOutputStream Buff;
        try {
            outSTr = new FileOutputStream("./" + fileName);
            Buff = new BufferedOutputStream(outSTr);
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[1].length; j++) {
                    String s = matrix[i][j] + "\t";
                    Buff.write(s.getBytes());
                }
                Buff.write("\n".getBytes());
            }
            Buff.flush();
            Buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void write2File(String fileName, Vector<int[]> matrix) {
        FileOutputStream outSTr;
        BufferedOutputStream Buff;
        try {
            outSTr = new FileOutputStream("./" + fileName);
            Buff = new BufferedOutputStream(outSTr);
            for (int i = 0; i < matrix.size(); i++) {
                for (int j = 0; j < matrix.elementAt(i).length; j++) {
                    String s = matrix.elementAt(i)[j] + "\t";
                    Buff.write(s.getBytes());
                }
                Buff.write("\n".getBytes());
            }
            Buff.flush();
            Buff.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * DCT工具类
     */
    public static class DCT {
        /**
         * 矩阵和转置矩阵
         */
        RealMatrix matrixT, matrixT_trspt;

        public DCT() {
            double[][] T = new double[8][8];
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) {
                    if (i == 0)
                        T[i][j] = 1 / Math.sqrt(8);
                    else {
                        T[i][j] = Math.cos(Math.PI * i * (2 * j + 1) / 16) / 2;
                    }
                }
            matrixT = new Array2DRowRealMatrix(T);
            matrixT_trspt = matrixT.transpose();//转置
        }

        /**
         * 进行DCT变换
         *
         * @param matrix 矩阵
         * @return DCT变换后的矩阵
         */
        public double[][] dct_8(double[][] matrix) {
            RealMatrix m = new Array2DRowRealMatrix(matrix);
            m = (matrixT.multiply(m)).multiply(matrixT_trspt);
            return m.getData();
        }

        /**
         * 反DCT变换
         *
         * @param matrix 矩阵
         * @return DCT反变换后的矩阵
         */
        public double[][] idct_8(double[][] matrix) {
            RealMatrix m = new Array2DRowRealMatrix(matrix);
            m = (matrixT_trspt.multiply(m)).multiply(matrixT);
            return m.getData();
        }
    }

    public static class Quatization {
        /**
         * 亮度表
         */
        private static final int[][] qY = {
                {16, 11, 10, 16, 24, 40, 51, 61}, {12, 12, 14, 19, 26, 58, 60, 55},
                {14, 13, 16, 24, 40, 57, 69, 56}, {14, 17, 22, 29, 51, 87, 80, 62},
                {18, 22, 37, 56, 68, 109, 103, 77}, {24, 35, 55, 64, 81, 104, 113, 92},
                {49, 64, 78, 87, 103, 121, 120, 101}, {72, 92, 95, 98, 112, 100, 103, 99}
        };
        /**
         * 色度表
         */
        private static final int[][] qC = {
                {17, 18, 24, 47, 99, 99, 99, 99}, {18, 21, 26, 66, 99, 99, 99, 99},
                {24, 26, 56, 99, 99, 99, 99, 99}, {47, 66, 99, 99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99, 99, 99, 99}, {99, 99, 99, 99, 99, 99, 99, 99},
                {99, 99, 99, 99, 99, 99, 99, 99}, {99, 99, 99, 99, 99, 99, 99, 99},
        };

        public static int[][] quatizeY(double[][] pic) {
            int[][] p = new int[8][8];
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    p[i][j] = (int) Math.round(pic[i][j] / qY[i][j]);
            return p;
        }

        public static int[][] quatizeCbCr(double[][] pic) {
            int[][] p = new int[8][8];
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    p[i][j] = (int) Math.round(pic[i][j] / qC[i][j]);
            return p;
        }

        /**
         * 亮度逆变换
         */
        public static double[][] iQuatizeY(int[][] pic) {
            double[][] p = new double[8][8];
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    p[i][j] = pic[i][j] * qY[i][j];
            return p;
        }

        /**
         * 色度逆变换
         */
        public static double[][] iQuatizeUV(int[][] pic) {
            double[][] p = new double[8][8];
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    p[i][j] = pic[i][j] * qC[i][j];
            return p;
        }
    }
}



