package com.pusan.alphatron.global.websocket.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;


@RequiredArgsConstructor
@Component
public class WebSocketHandler extends BinaryWebSocketHandler {

    private final ConcurrentMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
//    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);  // 스케줄러 추가



    // 저장할 디렉토리 경로
    private static final String IMAGE_SAVE_DIR = "images/";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("afterConnectionEstablished: 시작");
        sessions.put(session.getId(), session);
        System.out.println("해당 세션 ID로 연결했습니다: " + session.getId());
        System.out.println("Binary message buffer size: " + session.getBinaryMessageSizeLimit());
        System.out.println("Text message buffer size: " + session.getTextMessageSizeLimit());


    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("해당 세션 ID는 종료되었습니다: " + session.getId());
        System.out.println("afterConnectionClosed: 종료");
    }
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            System.out.println("Binary message received from session: " + session.getId());
            ByteBuffer payload = message.getPayload();
            byte[] data = payload.array();
            broadcastToAllSessions(data);
        } catch (Exception e) {
            System.err.println("Error in handleBinaryMessage: " + e.getMessage());
            e.printStackTrace();
        }
    }





    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private void broadcastToAllSessions(byte[] data) {
        BinaryMessage message = new BinaryMessage(data);
        System.out.println("Broadcasting data to all sessions...");

        for (WebSocketSession wsSession : sessions.values()) {
            if (wsSession.isOpen()) {
                executorService.submit(() -> {
                    try {
                        wsSession.sendMessage(message);
                        System.out.println("Data sent to session ID: " + wsSession.getId());
                    } catch (IOException e) {
                        System.err.println("Failed to send data to session " + wsSession.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        }

        System.out.println("Broadcast complete.");
    }




}














//    // 모든 세션에 메시지 브로드캐스트 메서드
//    private void broadcastToAllSessions(byte[] jpgData) {
//
//        BinaryMessage jpgMessage=new BinaryMessage(jpgData);
//        // 모든 세션에 메시지 전송
//        System.out.println("Broadcasting JPEG to all sessions...");
//        scheduler.scheduleAtFixedRate(() -> {
//            for (WebSocketSession wsSession : sessions.values()) {
//                if (wsSession.isOpen()) {
//                    try {
//                        wsSession.sendMessage(jpgMessage);
//                        System.out.println("JPEG sent to session ID: " + wsSession.getId());
//                    } catch (IOException e) {
//                        System.err.println("Failed to send JPEG to session " + wsSession.getId() + ": " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }, 0, 500, TimeUnit.MILLISECONDS);  // 500ms 간격으로 실행
//
//        System.out.println("Broadcast complete.");
//    }




//    // JPEG 파일 저장 메서드
//    private String saveJpgFile(byte[] jpgData) {
//        // 저장 디렉토리가 없을 경우 생성
//        try {
//            if (!Files.exists(Paths.get(IMAGE_SAVE_DIR))) {
//                Files.createDirectories(Paths.get(IMAGE_SAVE_DIR));
//            }
//        } catch (IOException e) {
//            System.err.println("Failed to create directory: " + IMAGE_SAVE_DIR);
//            e.printStackTrace();
//            return null;
//        }
//
//        // 파일 이름을 현재 타임스탬프를 기준으로 생성
//        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
//        String fileName = "image_" + timestamp + ".jpg";
//        String filePath = IMAGE_SAVE_DIR + fileName;
//
//        // 파일 저장
//        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
//            fileOutputStream.write(jpgData);
//            System.out.println("JPEG file saved: " + filePath);
//            return filePath;
//        } catch (IOException e) {
//            System.err.println("Failed to save JPEG file: " + filePath);
//            e.printStackTrace();
//            return null;
//        }
//    }