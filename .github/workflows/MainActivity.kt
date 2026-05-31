package com.atlas.thetagen

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFF121212.toInt())
        }
        
        val button = Button(this).apply {
            text = "Theta Frekansını Başlat (5 Hz Fark)"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(50, 50, 50, 50)
        }
        
        layout.addView(button)
        setContentView(layout)

        button.setOnClickListener {
            if (isPlaying) {
                isPlaying = false
                button.text = "Theta Frekansını Başlat (5 Hz Fark)"
            } else {
                isPlaying = true
                button.text = "Frekansı Durdur"
                startThetaGen()
            }
        }
    }

    private fun startThetaGen() {
        thread {
            val sampleRate = 44100
            val numSamples = sampleRate
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            val buffer = ShortArray(numSamples * 2)
            val freqLeft = 250.0
            val freqRight = 255.0

            audioTrack?.play()
            var sampleIndex = 0L

            while (isPlaying) {
                for (i in 0 until numSamples) {
                    val t = sampleIndex / sampleRate.toDouble()
                    
                    val valLeft = (sin(2 * Math.PI * freqLeft * t) * Short.MAX_VALUE).toInt().toShort()
                    val valRight = (sin(2 * Math.PI * freqRight * t) * Short.MAX_VALUE).toInt().toShort()
                    
                    buffer[i * 2] = valLeft
                    buffer[i * 2 + 1] = valRight
                    sampleIndex++
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
            
            audioTrack?.stop()
            audioTrack?.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
    }
}
