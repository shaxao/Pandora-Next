package com.lou.freegpt.handler;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioWebSocketHandler extends BinaryWebSocketHandler {
    private static final byte[] END_MARKER = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    private static final Path OUTPUT_PATH = Paths.get("output.wav");
    private FileOutputStream fileOutputStream;
    private long totalAudioLen = 0;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        fileOutputStream = new FileOutputStream(OUTPUT_PATH.toFile());
        writeWavHeaderPlaceholder();
        System.out.println("Connection established");
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
        ByteBuffer payload = message.getPayload();
        byte[] data = new byte[payload.remaining()];
        payload.get(data);

        if (isEndMarker(data)) {
            System.out.println("End marker received, closing file");
            updateWavHeader();
            fileOutputStream.close();
            session.close();
        } else {
            totalAudioLen += data.length;
            fileOutputStream.write(data);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
        System.out.println("Connection closed");
    }

    private boolean isEndMarker(byte[] data) {
        if (data.length != END_MARKER.length) {
            return false;
        }
        for (int i = 0; i < END_MARKER.length; i++) {
            if (data[i] != END_MARKER[i]) {
                return false;
            }
        }
        return true;
    }

    private void writeWavHeaderPlaceholder() throws IOException {
        byte[] header = new byte[44];
        fileOutputStream.write(header);
    }

    private void updateWavHeader() throws IOException {
        long totalDataLen = totalAudioLen + 36;
        int sampleRate = 16000;
        int channels = 1;
        int byteRate = sampleRate * channels * 2;
        int blockAlign = channels * 2;

        byte[] header = createWavHeader(totalAudioLen, totalDataLen, sampleRate, channels, byteRate, blockAlign);

        try (RandomAccessFile raf = new RandomAccessFile(OUTPUT_PATH.toFile(), "rw")) {
            raf.seek(0);
            raf.write(header);
        }
    }

    private byte[] createWavHeader(long totalAudioLen, long totalDataLen, int sampleRate, int channels, int byteRate, int blockAlign) {
        byte[] header = new byte[44];

        header[0] = 'R';  //
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) blockAlign;  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }
}
