package com.lou.freegpt.utils;

import net.sourceforge.lame.lowlevel.LameEncoder;
import net.sourceforge.lame.mp3.Lame;
import net.sourceforge.lame.mp3.MPEGMode;
import org.apache.commons.io.FileUtils;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 音频操作工具类
 */
public class AudioUtil {
    /**
     * 将PCM音频数据转换为MP3格式
     *
     * @param pcmData    PCM音频数据
     * @param sampleRate 采样率
     * @param bitRate    比特率
     * @param channels   声道数
     * @param quality    压缩质量(0~9, 0=最好(非常慢), 9=最差(最快))
     * @param outputFile 输出的MP3文件
     */
    public static void convertPcmToMp3(byte[] pcmData, int sampleRate, int bitRate, int channels, int quality, File outputFile) {
        try {
            // 创建音频格式
            AudioFormat sourceFormat = new AudioFormat(sampleRate, 16, channels, true, false);

            // 创建 Lame 编码器
            MPEGMode channelMode = channels == 1 ? MPEGMode.MONO : MPEGMode.STEREO;
            LameEncoder encoder = new LameEncoder(sourceFormat, bitRate, channelMode, quality, false);

            // 创建 MP3 缓冲区
            int mp3BufferSize = (int) (1.25 * pcmData.length) + 7200;
            byte[] mp3Buffer = new byte[mp3BufferSize];

            // 执行编码
            int bytesEncoded = encoder.encodeBuffer(pcmData, 0, pcmData.length, mp3Buffer);

            // 刷新编码器缓冲区
            int flushLength = encoder.encodeFinish(mp3Buffer);

            // 将编码后的数据写入文件
            FileUtils.writeByteArrayToFile(outputFile, mp3Buffer);

            // 关闭编码器
            encoder.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static byte[] createWavHeader(long totalAudioLen, long totalDataLen, int sampleRate, int channels, int byteRate) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        header.write("RIFF".getBytes());
        header.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) totalDataLen).array());
        header.write("WAVE".getBytes());
        header.write("fmt ".getBytes());
        header.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array());
        header.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 1).array());
        header.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) channels).array());
        header.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array());
        header.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(byteRate).array());
        header.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) (channels * 2)).array());
        header.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) 16).array());
        header.write("data".getBytes());
        header.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) totalAudioLen).array());

        return header.toByteArray();
    }

}
