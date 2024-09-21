package com.pusan.alphatron.global.websocket.handler;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.pusan.alphatron.global.websocket.client.WSClient;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.imageio.ImageIO;

@Component
public class WebSocketHandler extends BinaryWebSocketHandler {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private WebSocketSession raspberrySession ;
    private WSClient flutterClient;

    private static final String H264_FILE_PATH = "received_video.h264";
    private static final String MP4_FILE_PATH = "output_video.mp4";

    private FileOutputStream h264OutputStream;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("해당 세션 ID로 연결했습니다: " + session.getId());



        // H.264 파일을 쓰기 모드로 열기
        h264OutputStream = new FileOutputStream(H264_FILE_PATH);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("해당 세션 ID는 종료되었스빈다.: " + session.getId());


        // H.264 파일 스트림 닫기
        if (h264OutputStream != null) {
            h264OutputStream.close();
            h264OutputStream = null;

            // H.264 파일을 MP4로 변환
            convertH264ToMp4(H264_FILE_PATH, MP4_FILE_PATH);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {

        byte[] h264Data = message.getPayload().array();

        // H.264 데이터 디코딩 및 프레임 추출 (FFmpeg 또는 JavaCV 라이브러리 필요)
        // 예제에서는 BufferedImage로 디코딩된 데이터를 가정

        BufferedImage image = decodeH264ToImage(h264Data);

        // JPEG로 인코딩
        ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpegOutputStream);

        // MJPEG 스트림을 위한 데이터 준비
        byte[] jpegData = jpegOutputStream.toByteArray();
        BinaryMessage jpegMessage = new BinaryMessage(jpegData);

        // 모든 연결된 클라이언트에게 JPEG 데이터 전송
        for (WebSocketSession wsSession : sessions.values()) {
            if (wsSession.isOpen()) {
                wsSession.sendMessage(jpegMessage);
            }
        }
    }

    private void convertH264ToMp4(String inputH264Path, String outputMp4Path) {
        try {
            String command = String.format("ffmpeg -i %s -c copy %s", inputH264Path, outputMp4Path);
            Process process = Runtime.getRuntime().exec(command);

            // 프로세스 출력 읽기
            new Thread(() -> {
                try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("H.264 to MP4 conversion successful.");
            } else {
                System.err.println("H.264 to MP4 conversion failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
