package com.example.knotes.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object FocusAlarmPlayer {
    private var audioTrack: AudioTrack? = null
    private var stopHandler: Handler? = null
    @Volatile
    private var isPlaying = false
    private const val TAG = "FocusAlarmPlayer"

    fun playCalmAlarm(context: Context, durationMs: Long = 10_000L) {
        stopAlarm()
        isPlaying = true

        Thread {
            try {
                val sampleRate = 44100
                val numSamplesPerChime = (sampleRate * 2.2).toInt() // 2.2 seconds per zen chime
                val samples = FloatArray(numSamplesPerChime)

                // Acoustic 528Hz Zen Meditation Chime Synthesis (Solfeggio Meditation Tone)
                val f1 = 528.0   // Primary Solfeggio Zen Frequency
                val f2 = 1056.0  // Octave Overtone
                val f3 = 1584.0  // Soft Ambient Harmonics

                for (i in 0 until numSamplesPerChime) {
                    val t = i.toDouble() / sampleRate

                    // 15ms Smooth Attack Envelope (No harsh click) + Exponential Decay
                    val attack = (i.toDouble() / (sampleRate * 0.015)).coerceAtMost(1.0)
                    val decay = exp(-t / 0.75) // Gentle exponential decay

                    val wave = (0.60 * sin(2.0 * PI * f1 * t) +
                                0.25 * sin(2.0 * PI * f2 * t) +
                                0.15 * sin(2.0 * PI * f3 * t)) * attack * decay

                    samples[i] = (wave * 0.28).toFloat() // Gentle soft master volume
                }

                // Convert float samples to 16-bit PCM Short Array
                val pcmData = ShortArray(numSamplesPerChime)
                for (i in samples.indices) {
                    pcmData[i] = (samples[i] * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufferSize, pcmData.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                val startTime = System.currentTimeMillis()
                Log.d(TAG, "Calm 528Hz Zen Meditation Chime started")

                // Repeat the serene meditation chime until durationMs or stopped
                while (isPlaying && (System.currentTimeMillis() - startTime < durationMs)) {
                    audioTrack?.write(pcmData, 0, pcmData.size)
                    Thread.sleep(400L) // Gentle pause between zen chimes
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error playing calm meditation chime", e)
            } finally {
                stopAlarm()
            }
        }.start()

        // Auto stop after durationMs (10 seconds)
        stopHandler = Handler(Looper.getMainLooper())
        stopHandler?.postDelayed({
            stopAlarm()
        }, durationMs)
    }

    fun stopAlarm() {
        try {
            isPlaying = false
            stopHandler?.removeCallbacksAndMessages(null)
            stopHandler = null
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
            audioTrack = null
            Log.d(TAG, "Calm meditation chime stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping calm meditation chime", e)
        }
    }
}
