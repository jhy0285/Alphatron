package com.pusan.alphatron.global.websocket.handler;

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

//            try {
//                flutterClient = new WSClient("ws://E_SERVER_URI");
//                flutterClient.connectBlocking();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }


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
        System.out.println("이건진짜되야지 ㅇㅈ?");
            byte[] payload = message.getPayload().array();
        System.out.println("이건출력되야지");

        // H.264 데이터를 파일에 쓰기 (필요 시)
        if (h264OutputStream != null) {
            h264OutputStream.write(payload);
        }
        System.out.println("보낼준비완료");
        // 모든 연결된 클라이언트에게 바이너리 메시지 전송
        for (WebSocketSession wsSession : sessions.values()) {
            if (wsSession.isOpen()) {
                try {
                    System.out.println("이거출력되면 보내기직전");
                    wsSession.sendMessage(new BinaryMessage(payload));
                    System.out.println("이거출력되면 보낸거까지성공");
                    System.out.println(new BinaryMessage(payload));
                } catch (IOException e) {
                    System.err.println("Failed to send message to session " + wsSession.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }



        System.out.println("민재한테받아서 대영이한테 줌 ㅋ.");
//        }
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
