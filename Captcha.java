package june.jungle.indexing.controller.Sessions;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.Base64;
import java.util.Random;

@RestController
public class Captcha {

//    public static void main(String[] args) {
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//
//        Mat img = generateCaptcha(220, 80, 4);
//        Imgcodecs.imwrite("captcha_clear.jpg", img);
//
//        System.out.println("生成成功：captcha_clear.jpg");
//    }

    @GetMapping("/captcha")
    public String getCaptchaBase64() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        Mat img = generateCaptcha(220, 80, 4);

        // --- ★★ 关键步骤：Mat → JPG buffer → Base64 ★★
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, buffer);

        String base64 = Base64.getEncoder().encodeToString(buffer.toArray());

        return "data:image/jpeg;base64," + base64;
    }

    public static Mat generateCaptcha(int width, int height, int length) {
        Random r = new Random();

        Mat img = new Mat(height, width, CvType.CV_8UC3);

        // 浅色背景
        Scalar bg = new Scalar(
                20 + r.nextInt(200),
                20 + r.nextInt(200),
                20 + r.nextInt(200)
        );
        img.setTo(bg);

        // 字符集
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();

        // 生成随机验证码
        for (int i = 0; i < length; i++)
            sb.append(chars.charAt(r.nextInt(chars.length())));
        String text = sb.toString();

        // 绘制字符
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            int font = Imgproc.FONT_HERSHEY_SIMPLEX;
            double fontScale = 1.8;
            Scalar color = new Scalar(r.nextInt(154), r.nextInt(154), r.nextInt(154));

            int x = 25 + 50 * i + r.nextInt(5);
            int y = 55 + r.nextInt(5);

            // 轻度旋转
            Mat layer = new Mat(img.size(), img.type(), new Scalar(0,0,0));
            Imgproc.putText(layer, Character.toString(c), new Point(x, y), font, fontScale, color, 3);

            double angle = r.nextInt(30) - 15;
            Mat rot = Imgproc.getRotationMatrix2D(new Point(x, y), angle, 1.0);

            Imgproc.warpAffine(layer, layer, rot, img.size(), Imgproc.INTER_LINEAR, Core.BORDER_TRANSPARENT);
            Core.addWeighted(img, 1.0, layer, 1.0, 0, img);
        }

        // 干扰曲线（轻度 & 不遮挡文字）
        drawSmoothLine(img);
        drawSmoothLine(img);

        // 少量噪点（不破坏清晰度）
        for (int i = 0; i < 100; i++) {
            int px = r.nextInt(width);
            int py = r.nextInt(height);
            Scalar c = new Scalar(r.nextInt(150), r.nextInt(150), r.nextInt(150));
            Imgproc.circle(img, new Point(px, py), 1, c, -1);
        }

        return img;
    }

    // =========================
    // 平滑干扰曲线
    // =========================
    private static void drawSmoothLine(Mat img) {
        Random r = new Random();
        int w = img.cols();
        int h = img.rows();

        Point last = new Point(0, r.nextInt(h));

        for (int x = 1; x < w; x++) {
            double offset = Math.sin(x / 18.0) * 10;
            Point next = new Point(x, last.y + offset);

            Scalar col = new Scalar(120 + r.nextInt(60), 120 + r.nextInt(60), 120 + r.nextInt(60));
            Imgproc.line(img, last, next, col, 2);

            last = next;
        }
    }
}