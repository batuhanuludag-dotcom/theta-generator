package com.atlas.thetagen

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null
    
    // Varsayılan taşıyıcı frekans (Carrier) ve hedef fark frekansı
    private var baseFrequency = 200.0 
    private var targetDifference = 5.0 // Varsayılan Theta (5 Hz)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ana Arayüz Düzeni
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF121212.toInt())
            setPadding(40, 60, 40, 40)
        }

        // Durum Göstergesi Bilgisi
        val infoTextView = TextView(this).apply {
            text = "Mod: Beklemede\nSol: 0 Hz | Sağ: 0 Hz (Fark: 0 Hz)"
            setTextColor(0xFF888888.toInt())
            textSize = 16f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        rootLayout.addView(infoTextView)

        // Taslak Butonları Fonksiyonu
        fun createPresetButton(title: String, diff: Double, description: String) {
            val btn = Button(this).apply {
                text = "$title ($diff Hz) - $description"
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF1E1E1E.toInt())
                setPadding(20, 30, 20, 30)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 10, 0, 10)
                layoutParams = params
            }
            btn.setOnClickListener {
                targetDifference = diff
                updateFrequencies(infoTextView)
                if (isPlaying) restartAudio()
                Toast.makeText(this@MainActivity, "$title Seçildi", Toast.LENGTH_SHORT).show()
            }
            rootLayout.addView(btn)
        }

        // Beyin Dalgası Taslakları (Presets)
        createPresetButton("DELTA", 2.5, "Derin Uyku & Yenilenme")
        createPresetButton("THETA", 5.0, "Bilinçaltı & Derin Meditasyon")
        createPresetButton("ALPHA", 10.0, "Hafif Odak & Rahatlama")
        createPresetButton("BETA", 20.0, "Aktif Düşünme & Analitik Analiz")
        createPresetButton("GAMMA", 40.0, "Yüksek İşlem & Maksimum Odak")

        // Manuel Giriş Alanı Başlığı
        val manualTitle = TextView(this).apply {
            text = "\nVEYA MANUEL HEDEF FREKANS GİRİN (Hz):"
            setTextColor(0xFFBB86FC.toInt())
            textSize = 14f
            setPadding(0, 20, 0, 10)
        }
        rootLayout.addView(manualTitle)

        // Manuel Giriş Düzeni
        val manualLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        
        val inputFreq = EditText(this).apply {
            hint = "Örn: 7.5"
            setHintTextColor(0xFF555555.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            val params = LinearLayout.LayoutParams(250, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutParams = params
        }
        manualLayout.addView(inputFreq)

        val applyBtn = Button(this).apply {
            text = "Uygula"
            setTextColor(0xFF000000.toInt())
            setBackgroundColor(0xFFBB86FC.toInt())
        }
        applyBtn.setOnClickListener {
            val value = inputFreq.text.toString().toDoubleOrNull()
            if (value != null && value > 0 && value <= 50) {
                targetDifference = value
                updateFrequencies(infoTextView)
                if (isPlaying) restartAudio()
                Toast.makeText(this@MainActivity, "Manuel Frekans: $value Hz", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Geçersiz değer! (0.1 - 50 Hz arası girin)", Toast.LENGTH_SHORT).show()
            }
        }
        manualLayout.addView(applyBtn)
        rootLayout.addView(manualLayout)

        // Ana Başlat / Durdur Butonu
        val mainControlBtn = Button(this).apply {
            text = "SESİ BAŞLAT"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF03DAC5.toInt())
            textSize = 18f
            setPadding(40, 40, 40, 40)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 50, 0, 0)
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
        tv.text = "Aktif Frekans Ayarı\nSol Kulak: $left Hz | Sağ Kulak: $right Hz\n[ Beyin Hedefi: $targetDifference Hz ]"
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
                    
                    val valLeft = (sin(2 * Math.PI * freqLeft * t) * Short.MAX_VALUE * 0.8).toInt().toShort()
                    val valRight = (sin(2 * Math.PI * freqRight * t) * Short.MAX_VALUE * 0.8).toInt().toShort()
                    
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

    private fun restartAudio() {
        isPlaying = false
        Thread.sleep(50)
        isPlaying = true
        startAudioEngine()
    }

    override fun onDestroy() {
        super.onDestroy()
        isPlaying = false
    }
}
