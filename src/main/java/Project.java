import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.*;

class Pixel {
    public final int r;
    public final int g;
    public final int b;
    public final int hitCount;

    public Pixel(int r, int g, int b, int hitCount) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.hitCount = hitCount;
    }
}

class Point {
    public final double x;
    public final double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

class Rect {
    public final double x;
    public final double y;
    public final double width;
    public final double height;

    public Rect(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(Point p) {
        return p.x >= x && p.x <= x + width && p.y >= y && p.y <= y + height;
    }
}

@FunctionalInterface
interface Transformation {
    Point apply(Point point);
}

class FractalImage {
    public final Pixel[] data;
    public final int width;
    public final int height;

    public FractalImage(Pixel[] data, int width, int height) {
        this.data = data;
        this.width = width;
        this.height = height;
    }

    public static FractalImage create(int width, int height) {
        Pixel[] data = new Pixel[width * height];
        for (int i = 0; i < data.length; i++) {
            data[i] = new Pixel(0, 0, 0, 0);
        }
        return new FractalImage(data, width, height);
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Pixel pixel(int x, int y) {
        return data[y * width + x];
    }

    public FractalImage copy() {
        Pixel[] newData = new Pixel[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        return new FractalImage(newData, width, height);
    }
}

@FunctionalInterface
interface Renderer {
    FractalImage render(FractalImage canvas, Rect world, List<Transformation> variations, int samples, short iterPerSample, long seed);
}


class ImageUtils {
    private ImageUtils() {}

    public static void save(FractalImage image, Path filename, ImageFormat format) {
        int[] pixels = new int[image.data.length];
        for (int i = 0; i < image.data.length; i++) {
            Pixel pixel = image.data[i];
            int r = pixel.r / pixel.hitCount;
            int g = pixel.g / pixel.hitCount;
            int b = pixel.b / pixel.hitCount;
            pixels[i] = (255 << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage bufferedImage = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB);
        bufferedImage.setRGB(0, 0, image.width, image.height, pixels, 0, image.width);

        try {
            ImageIO.write(bufferedImage, format.name().toLowerCase(Locale.ROOT), new File(filename.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

enum ImageFormat {
    PNG,
    JPG,
    GIF
}

