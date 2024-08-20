package com.pusan.alphatron.global.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.FileOutputStream;
import java.io.IOException;

@Component
public class WebSocketHandler extends BinaryWebSocketHandler {

    private static final String H264_FILE_PATH = "received_video.h264";
    private static final String MP4_FILE_PATH = "output_video.mp4";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 클라이언트와의 WebSocket 연결이 성공적으로 맺어진 후 호출됩니다.
        System.out.println("Connection established with session ID: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 클라이언트와의 WebSocket 연결이 닫힌 후 호출됩니다.
        System.out.println("Connection closed with session ID: " + session.getId() + " with status: " + status);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        System.out.println("Received binary message with size: " + message.getPayload().remaining() + " bytes");

        // 라즈베리파이로부터 받은 H.264 바이너리 데이터
        byte[] payload = message.getPayload().array();

        // 받은 데이터를 H.264 파일로 저장
        try (FileOutputStream fos = new FileOutputStream(H264_FILE_PATH, true)) {
            fos.write(payload);
        }

        // H.264 파일을 MP4로 변환
        convertH264ToMp4(H264_FILE_PATH, MP4_FILE_PATH);

        System.out.println("Received and converted binary frame to MP4");
    }

    private void convertH264ToMp4(String inputH264Path, String outputMp4Path) {
        try {
            // FFmpeg 명령어 실행
            String command = String.format("ffmpeg -i %s -c copy %s", inputH264Path, outputMp4Path);
            Process process = Runtime.getRuntime().exec(command);

            // 프로세스의 결과를 읽어서 출력 (디버깅용)
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

            // 프로세스가 완료될 때까지 대기
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
