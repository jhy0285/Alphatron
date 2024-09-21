

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class H264Decoder {

    public BufferedImage decodeH264ToImage(byte[] h264Data) {
        FFmpegFrameGrabber grabber = null;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        try {
            // Byte array를 ByteArrayInputStream으로 변환
            ByteArrayInputStream inputStream = new ByteArrayInputStream(h264Data);

            // FFmpegFrameGrabber로 inputStream 설정
            grabber = new FFmpegFrameGrabber(inputStream);
            grabber.start();

            // 프레임을 가져와서 디코딩
            Frame frame = grabber.grabImage();

            // 프레임이 유효한 경우 BufferedImage로 변환
            if (frame != null) {
                BufferedImage bufferedImage = converter.convert(frame);
                return bufferedImage;
            } else {
                System.out.println("Failed to grab frame.");
                return null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (FFmpegFrameGrabber.Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
