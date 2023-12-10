import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        int width = 800;
        int height = 800;
        FractalImage canvas = FractalImage.create(width, height);
        Rect world = new Rect(-2, -2, 4, 4);

        // Примеры преобразований
        List<Transformation> variations = List.of(
                // Преобразование 1: Сжатие по X в 2 раза
                point -> new Point(point.x / 2, point.y),
                // Преобразование 2: Поворот на 90 градусов
                point -> new Point(-point.y, point.x),
                // Преобразование 3: Отражение по обеим осям
                point -> new Point(-point.x, -point.y)
                // Добавьте свои преобразования сюда
        );

        int samples = 10000;
        short iterPerSample = 50;
        long seed = System.currentTimeMillis();

        Renderer renderer = new CombinedRenderer();
        FractalImage result = renderer.render(canvas, world, variations, samples, iterPerSample, seed);

        // Сохраняем изображение в файл
        Path outputPath = Path.of("fractal_output.png");
        ImageUtils.save(result, outputPath, ImageFormat.PNG);
    }
}
