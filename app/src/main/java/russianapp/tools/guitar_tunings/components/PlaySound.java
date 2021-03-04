package russianapp.tools.guitar_tunings.components;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * @author Huanle Zhang, at University of California, Davis
 * www.huanlezhang.com
 * @version 0.2
 * @since 2019-05-28
 */

public class PlaySound {

    private static final int SOURCE_FREQ = 48000; // most widely supported
    private static Thread mThread;
    private static AudioTrack mAudioTrack;
    private static boolean mIsPlaying = false;
    // this is the interface to adjust the sound frequency
    public double mOutputFreq = 0;

    public void start() {

        assert mOutputFreq != 0;

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    int buffSize = AudioTrack.getMinBufferSize(SOURCE_FREQ,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            SOURCE_FREQ, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            buffSize, AudioTrack.MODE_STREAM);

                    short[] samples = new short[buffSize];
                    int amp = 32767;
                    final double twopi = 2 * Math.PI;
                    double ph = 0.0;
                    mAudioTrack.play();
                    mIsPlaying = true;
                    while (mIsPlaying) {
                        for (int i = 0; i < buffSize; i++) {
                            samples[i] = (short) (amp * Math.sin(ph));
                            ph += twopi * mOutputFreq / SOURCE_FREQ;
                        }
                        mAudioTrack.write(samples, 0, buffSize);
                    }
                    mAudioTrack.stop();
                    mAudioTrack.release();

                } catch (Exception e) {
                    mIsPlaying = false;
                    mAudioTrack = null;
                    mThread = null;
                }
            }
        });
        mThread.start();
    }

    public void stop() {
        mIsPlaying = false;
    }
}