import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.List;
import java.util.Random;

class CombinedRenderer implements Renderer {
    private static final int symmetry = 1; // Ваше значение

    @Override
    public FractalImage render(FractalImage canvas, Rect world, List<Transformation> variations, int samples, short iterPerSample, long seed) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<FractalImage>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; ++i) {
            final int threadIndex = i;
            FractalImage finalCanvas = canvas;
            Callable<FractalImage> task = () -> performRendering(finalCanvas.copy(), world, variations, samples / numThreads, iterPerSample, seed + threadIndex);
            futures.add(executorService.submit(task));
        }

        for (Future<FractalImage> future : futures) {
            try {
                canvas = combineImages(canvas, future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorService.shutdown();

        return canvas;
    }

    private FractalImage performRendering(FractalImage originalCanvas, Rect world, List<Transformation> variations, int samples, short iterPerSample, long seed) {
        Random random = new Random(seed);
        FractalImage canvas = originalCanvas.copy();

        for (int num = 0; num < samples; ++num) {
            Point pw = random(world);

            for (short step = 0; step < iterPerSample; ++step) {
                Transformation variation = random(variations, random);

                pw = variation.apply(pw);

                double theta2 = 0.0;
                for (int s = 0; s < symmetry; theta2 += Math.PI * 2 / symmetry, ++s) {
                    Point pwr = rotate(pw, theta2);
                    if (!world.contains(pwr)) continue;

                    Pixel pixel = map_range(world, pwr, canvas);
                    if (pixel == null) continue;

                    synchronized (pixel) {
                        int r = (pixel.r * pixel.hitCount + pixel.r) / (pixel.hitCount + 1);
                        int g = (pixel.g * pixel.hitCount + pixel.g) / (pixel.hitCount + 1);
                        int b = (pixel.b * pixel.hitCount + pixel.b) / (pixel.hitCount + 1);
                        pixel = new Pixel(r, g, b, pixel.hitCount + 1);
                        canvas.data[getIndex(pwr, canvas.width)] = pixel;
                    }
                }
            }
        }

        return canvas;
    }

    private FractalImage combineImages(FractalImage canvas1, FractalImage canvas2) {
        for (int i = 0; i < canvas1.data.length; i++) {
            Pixel pixel1 = canvas1.data[i];
            Pixel pixel2 = canvas2.data[i];

            int r = (pixel1.r * pixel1.hitCount + pixel2.r * pixel2.hitCount) / (pixel1.hitCount + pixel2.hitCount);
            int g = (pixel1.g * pixel1.hitCount + pixel2.g * pixel2.hitCount) / (pixel1.hitCount + pixel2.hitCount);
            int b = (pixel1.b * pixel1.hitCount + pixel2.b * pixel2.hitCount) / (pixel1.hitCount + pixel2.hitCount);

            canvas1.data[i] = new Pixel(r, g, b, pixel1.hitCount + pixel2.hitCount);
        }

        return canvas1;
    }

    private Point random(Rect world) {
        double x = world.x + Math.random() * world.width;
        double y = world.y + Math.random() * world.height;
        return new Point(x, y);
    }

    private Transformation random(List<Transformation> variations, Random random) {
        int index = random.nextInt(variations.size());
        return variations.get(index);
    }

    private Point rotate(Point point, double angle) {
        double x = point.x * Math.cos(angle) - point.y * Math.sin(angle);
        double y = point.x * Math.sin(angle) + point.y * Math.cos(angle);
        return new Point(x, y);
    }

    private Pixel map_range(Rect world, Point point, FractalImage canvas) {
        if (!world.contains(point)) {
            return null;
        }

        int x = (int) Math.floor((point.x - world.x) / world.width * canvas.width);
        int y = (int) Math.floor((point.y - world.y) / world.height * canvas.height);

        return canvas.pixel(x, y);
    }

    private int getIndex(Point point, int width) {
        int x = (int) point.x;
        int y = (int) point.y;
        return y * width + x;
    }
}
