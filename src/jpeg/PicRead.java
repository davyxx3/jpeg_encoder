package jpeg;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class PicRead {
    private double[][] Y, Cb, Cr;
    private Color[][] pic;

    /**
     * 输入目录下的pic文件名，读取图像文件
     */
    public PicRead(String picName) {

        File f = new File(picName);
        System.out.println(picName);
        if (!f.exists()) {
            //若不存在该文件，输出错误提示
            System.out.println("-------------[WARNING] Wrong path!-------------");
            return;
        }
        BufferedImage bi = null;
        try {
            bi = ImageIO.read(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //获取文件宽度与高度
        int width = bi.getWidth();
        System.out.println("-------------[INFORMATION] the width is : " + width + "-------------");
        int height = bi.getHeight();
        System.out.println("-------------[INFORMATION] the height is : " + height + "-------------");

        //根据图像的长宽设置pic二维数组的范围，并图像每个位置对应的信息填入数组
        pic = new Color[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pic[i][j] = new Color(bi.getRGB(i, j));
            }
        }
    }

    public static void writeImage(Color[][] pic, String name, String type) {
        System.out.println("-------------[INFORMATION] processing is beginning! -------------");
        try {
            BufferedImage bi = new BufferedImage(pic.length, pic[0].length, BufferedImage.TYPE_INT_RGB);
            if (pic.length % 2 != 0 || pic[0].length % 2 != 0) {
                for (int i = 0; i < pic.length - 1; i++) {
                    for (int j = 0; j < pic[0].length - 1; j++) {
                        bi.setRGB(i, j, pic[i][j].getRGB());
                    }
                }
            } else {
                for (int i = 0; i < pic.length; i++) {
                    for (int j = 0; j < pic[0].length; j++) {
                        bi.setRGB(i, j, pic[i][j].getRGB());
                    }
                }
            }
            Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName(type);// 定义图像格式
            ImageWriter writer = it.next();
            ImageOutputStream ios;
            ios = ImageIO.createImageOutputStream(new File("./" + name + "." + type));
            writer.setOutput(ios);
            writer.write(bi);
            bi.flush();
            ios.flush();
            System.out.println("-------------[INFORMATION] Write Success! -------------");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 使用YCbCr为411的采样格式
     */
    public void RGB2YCbCr() {
        //从pic数组中采样，转换图像格式,将亮度、色度通道进行处理
        Y = new double[pic.length][pic[1].length];
        Cb = new double[pic.length / 2][pic[1].length / 2];
        Cr = new double[pic.length / 2][pic[1].length / 2];

        //将色度通道进行预处理，防止出现空值
        for (int i = 0; i < pic.length / 2; i++) {
            for (int j = 0; j < pic[1].length / 2; j++) {
                Cb[i][j] = Cr[i][j] = 0;
            }
        }
        //将矩阵中的图像进行rgb2YCbCr转换
        //若图像长宽不为2的倍数，使其取整
        if (pic.length % 2 != 0 || pic[0].length % 2 != 0) {
            for (int i = 0; i < pic.length - 1; i++) {
                for (int j = 0; j < pic[1].length - 1; j++) {
                    int r = pic[i][j].getRed();
                    int g = pic[i][j].getGreen();
                    int b = pic[i][j].getBlue();
                    Y[i][j] = 0.299 * r + 0.587 * g + 0.114 * b;
                    Cb[i / 2][j / 2] = -0.169 * r - 0.331 * g + 0.5 * b + 128;
                    Cr[i / 2][j / 2] = 0.5 * r - 0.419 * g - 0.081 * b + 128;
                }

            }
        } else {
            for (int i = 0; i < pic.length; i++) {
                for (int j = 0; j < pic[1].length; j++) {
                    int r = pic[i][j].getRed();
                    int g = pic[i][j].getGreen();
                    int b = pic[i][j].getBlue();
                    Y[i][j] = 0.299 * r + 0.587 * g + 0.114 * b;
                    Cb[i / 2][j / 2] = -0.169 * r - 0.331 * g + 0.5 * b + 128;
                    Cr[i / 2][j / 2] = 0.5 * r - 0.419 * g - 0.081 * b + 128;
                }

            }
        }

        //将三个通道的信息分别写入
        PreEncoding.write2File("./debug/Y_256x256_encoding.txt", Y);
        PreEncoding.write2File("./debug/U_128x128_encoding.txt", Cb);
        PreEncoding.write2File("./debug/V_128x128_encoding.txt", Cr);
    }

    /**
     * 将YCbCr转换回RGB
     */
    public Color[][] YCbCr2RGB(double[][] Y, double[][] Cb, double[][] Cr) {
        Color[][] pic = new Color[Y.length][Y[1].length];
        if (pic.length % 2 != 0 || pic[0].length % 2 != 0) {
            for (int i = 0; i < pic.length - 1; i++) {
                for (int j = 0; j < pic[0].length - 1; j++) {
                    double y = Y[i][j];
                    double cb = Cb[i / 2][j / 2];
                    double cr = Cr[i / 2][j / 2];

                    int r, g, b;
                    r = (int) (y + 1.13983 * (cr - 128));
                    g = (int) (y - 0.39465 * (cb - 128) - 0.58060 * (cr - 128));
                    b = (int) (y + 2.03211 * (cb - 128));
                    r = r > 255 ? 255 : Math.max(r, 0);
                    g = g > 255 ? 255 : Math.max(g, 0);
                    b = b > 255 ? 255 : Math.max(b, 0);

                    pic[i][j] = new Color(r, g, b);
                }
            }
        } else {

            for (int i = 0; i < pic.length; i++) {
                for (int j = 0; j < pic[0].length; j++) {
                    double y = Y[i][j];
                    double cb = Cb[i / 2][j / 2];
                    double cr = Cr[i / 2][j / 2];

                    int r, g, b;
                    r = (int) (y + 1.13983 * (cr - 128));
                    g = (int) (y - 0.39465 * (cb - 128) - 0.58060 * (cr - 128));
                    b = (int) (y + 2.03211 * (cb - 128));
                    r = r > 255 ? 255 : Math.max(r, 0);
                    g = g > 255 ? 255 : Math.max(g, 0);
                    b = b > 255 ? 255 : Math.max(b, 0);

                    pic[i][j] = new Color(r, g, b);
                }
            }
        }
        return pic;
    }

    double[][] getY() {
        return Y.clone();
    }

    double[][] getCb() {
        return Cb.clone();
    }

    double[][] getCr() {
        return Cr.clone();
    }
}
