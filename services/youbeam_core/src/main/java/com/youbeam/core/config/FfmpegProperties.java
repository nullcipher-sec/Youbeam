package com.youbeam.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Transcodificación con ffmpeg ({@code youbeam.ffmpeg.*}).
 *
 * @param ffmpegPath   ejecutable de ffmpeg
 * @param ffprobePath  ejecutable de ffprobe
 * @param timeout      tiempo máximo de cada proceso; al superarlo se mata, para que un archivo
 *                     malicioso o enorme no bloquee el servidor
 * @param videoBitrate tasa de bits del video VP8 (p. ej. {@code 2M})
 * @param frameRate    imágenes por segundo; debe coincidir con el ritmo al que el Streaming Server
 *                     envía los fotogramas (33 ms, unos 30 fps)
 */
@ConfigurationProperties(prefix = "youbeam.ffmpeg")
public record FfmpegProperties(String ffmpegPath, String ffprobePath, Duration timeout, String videoBitrate,
                               int frameRate) {
}
