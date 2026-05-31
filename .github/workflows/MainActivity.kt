package com.atlas.thetagen

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.concurrent.thread
import kotlin.math.sin

class MainActivity : Activity() {
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    
    private var baseFrequency = 200.0 
    private var targetDifference = 5.0 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF121212.toInt())
            setPadding(40, 60, 40, 40)
        }

        val infoTextView = TextView(this).apply {
            text = "Mod: Beklemede\nSol: 0 Hz | Sağ: 0 Hz (Fark: 0 Hz)"
            setTextColor(0xFF888888.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        rootLayout.addView(infoTextView)

        fun createPresetButton(title: String, diff: Double, description: String) {
            val btn = Button(this).apply {
                text = "$title ($diff Hz) - $description"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1E1E1E.toInt())
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 8, 0, 8)
                layoutParams = params
            }
            btn.setOnClickListener {
                targetDifference = diff
                updateFrequencies(infoTextView)
                if (isPlaying) restartAudioEngine()
                Toast.makeText(this@MainActivity, "$title Aktif", Toast.LENGTH_SHORT).show()
            }
            rootLayout.addView(btn)
        }

        createPresetButton("DELTA", 2.5, "Derin Uyku & Hücresel Yenilenme")
        createPresetButton("THETA", 5.0, "Bilinçaltı & Derin Meditasyon")
        createPresetButton("ALPHA", 10.0, "Hafif Odak & Gevşeme")
        createPresetButton("BETA", 20.0, "Analitik Analiz & Karar Alma")
        createPresetButton("GAMMA", 40.0, "Yüksek İşlem & Maksimum Odak")

        val manualTitle = TextView(this).apply {
            text = "\nVEYA MANUEL FREKANS GİRİN (0.1 - 50 Hz):"
            setTextColor(0xFFBB86FC.toInt())
            textSize = 14f
            setPadding(0, 20, 0, 10)
        }
        rootLayout.addView(manualTitle)

        val inputFreq = EditText(this).apply {
            hint = "Örn: 7.83"
            setHintTextColor(0xFF555555.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            gravity = Gravity.CENTER
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 20)
            layoutParams = params
        }
        rootLayout.addView(inputFreq)

        val applyBtn = Button(this).apply {
            text = "Manuel Frekansı Uygula"
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFFBB86FC.toInt())
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = params
        }
        applyBtn.setOnClickListener {
            val value = inputFreq.text.toString().toDoubleOrNull()
            if (value != null && value > 0.0 && value <= 50.0) {
                targetDifference = value
                updateFrequencies(infoTextView)
                if (isPlaying) restartAudioEngine()
                Toast.makeText(this@MainActivity, "Frekans $value Hz Yapıldı", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Hata! 0.1 - 50 arası girin.", Toast.LENGTH_SHORT).show()
            }
        }
        rootLayout.addView(applyBtn)

        val mainControlBtn = Button(this).apply {
            text = "SESİ BAŞLAT"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF03DAC5.toInt())
            textSize = 18f
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 40, 0, 0)
            layoutParams = params
        }
        
        mainControlBtn.setOnClickListener {
            if (isPlaying) {
                isPlaying = false
                mainControlBtn.text = "SESİ BAŞLAT"
                mainControlBtn.setBackgroundColor(0xFF03DAC5.toInt())
                infoTextView.text = "Mod: Durduruldu\nSol: 0 Hz | Sağ: 0 Hz (Fark: 0 Hz)"
            } else {
                isPlaying = true
                mainControlBtn.text = "DURDUR"
                mainControlBtn.setBackgroundColor(0xFFCF6679.toInt())
                updateFrequencies(infoTextView)
                startAudioEngine()
            }
        }
        rootLayout.addView(mainControlBtn)

        setContentView(rootLayout)
        updateFrequencies(infoTextView)
    }

    private fun updateFrequencies(tv: TextView) {
        val left = baseFrequency
        val right = baseFrequency + targetDifference
        tv.text = "Aktif Frekans Ayarı\nSol: $left Hz | Sağ: $right Hz\n[ Hedef Beyin Dalgası: $targetDifference Hz ]"
    }

    private fun startAudioEngine() {
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
            audioTrack?.play()
            var sampleIndex = 0L

            while (isPlaying) {
                val freqLeft = baseFrequency
                val freqRight = baseFrequency + targetDifference

                for (i in 0 until numSamples) {
                    val t = sampleIndex / sampleRate.toDouble()
                    
                    val valLeft = (sin(2 * Math.PI * freqLeft * t) * Short.MAX_VALUE * 0.7).toInt().toShort()
                    val valRight = (sin(2 * Math.PI * freqRight * t) * Short.MAX_VALUE * 0.7).toInt().toShort()
                    
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

    private fun restartAudioEngine() {
        isPlaying = false
        Thread.sleep(40)
        isPlaying = true
        startAudioEngine()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
    }
}
