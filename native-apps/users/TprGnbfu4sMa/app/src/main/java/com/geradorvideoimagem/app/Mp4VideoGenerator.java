package com.geradorvideoimagem.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4VideoGenerator {

    public interface VideoCallback {
        void onProgress(int percent);
        void onSuccess(File outputFile);
        void onError(String errorMessage);
    }

    public static void createMp4Video(final File outputFile,
                                      final String prompt,
                                      final String style,
                                      final int width,
                                      final int height,
                                      final int durationSec,
                                      final int fps,
                                      final VideoCallback callback) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                MediaCodec encoder = null;
                MediaMuxer muxer = null;
                Surface inputSurface = null;

                try {
                    int totalFrames = durationSec * fps;
                    int bitRate = 2500000; // 2.5 Mbps crisp video

                    MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                    format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                    format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                    format.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
                    format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

                    encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    inputSurface = encoder.createInputSurface();
                    encoder.start();

                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                    muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int trackIndex = -1;
                    boolean muxerStarted = false;
                    long frameDurationUs = 1000000L / fps;

                    Rect srcRect = new Rect(0, 0, width, height);
                    Rect dstRect = new Rect(0, 0, width, height);

                    for (int frame = 0; frame < totalFrames; frame++) {
                        float progress = (float) frame / (float) totalFrames;
                        Bitmap frameBitmap = ArtEngine.generateArtwork(prompt, style, width, height, progress);

                        Canvas canvas = inputSurface.lockCanvas(null);
                        if (canvas != null) {
                            canvas.drawBitmap(frameBitmap, srcRect, dstRect, null);
                            inputSurface.unlockCanvasAndPost(canvas);
                        }
                        frameBitmap.recycle();

                        // Drain output buffers
                        while (true) {
                            int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 2500);
                            if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                if (muxerStarted) {
                                    throw new RuntimeException("Format changed after muxer started");
                                }
                                MediaFormat newFormat = encoder.getOutputFormat();
                                trackIndex = muxer.addTrack(newFormat);
                                muxer.start();
                                muxerStarted = true;
                            } else if (outputIndex >= 0) {
                                ByteBuffer encodedData = encoder.getOutputBuffer(outputIndex);
                                if (encodedData != null) {
                                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                        bufferInfo.size = 0;
                                    }
                                    if (bufferInfo.size != 0) {
                                        if (!muxerStarted) {
                                            throw new RuntimeException("Muxer hasn't started yet");
                                        }
                                        encodedData.position(bufferInfo.offset);
                                        encodedData.limit(bufferInfo.offset + bufferInfo.size);
                                        bufferInfo.presentationTimeUs = frame * frameDurationUs;
                                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                                    }
                                    encoder.releaseOutputBuffer(outputIndex, false);
                                }
                            } else {
                                break;
                            }
                        }

                        if (callback != null) {
                            final int currentPercent = (int) (((frame + 1) * 100) / totalFrames);
                            callback.onProgress(currentPercent);
                        }
                    }

                    // Signal End Of Stream
                    encoder.signalEndOfInputStream();

                    boolean done = false;
                    while (!done) {
                        int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outputIndex >= 0) {
                            ByteBuffer encodedData = encoder.getOutputBuffer(outputIndex);
                            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                done = true;
                            }
                            if (bufferInfo.size > 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset);
                                encodedData.limit(bufferInfo.offset + bufferInfo.size);
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                            }
                            encoder.releaseOutputBuffer(outputIndex, false);
                        } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                            break;
                        }
                    }

                    if (callback != null) {
                        callback.onSuccess(outputFile);
                    }

                } catch (final Exception ex) {
                    ex.printStackTrace();
                    if (callback != null) {
                        callback.onError(ex.getMessage() != null ? ex.getMessage() : "Erro na compilação do MP4.");
                    }
                } finally {
                    try {
                        if (encoder != null) {
                            encoder.stop();
                            encoder.release();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if (inputSurface != null) {
                            inputSurface.release();
                        }
                    } catch (Exception ignored) {}

                    try {
                        if (muxer != null) {
                            muxer.stop();
                            muxer.release();
                        }
                    } catch (Exception ignored) {}
                }
            }
        }).start();
    }
}