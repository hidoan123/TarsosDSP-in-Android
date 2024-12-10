package com.example.myapplication

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.PitchShifter
import com.google.android.material.slider.Slider
import java.io.File
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory

class MainActivity : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var tvSelectedFile: TextView
    private lateinit var sliderPitch: Slider
    private var selectedAudioUri: Uri? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentPitchFactor: Float = 1.0f
    val pickAudioFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedAudioUri = it
            tvSelectedFile.text = it.path
            btnPlayAudio.visibility = Button.VISIBLE
        } ?: run {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupViewId()
        setListener()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupViewId() {
        button = findViewById(R.id.btnChooseFileAuido)
        btnPlayAudio = findViewById(R.id.playAudio)
        tvSelectedFile = findViewById(R.id.tv_selected_file)
        sliderPitch = findViewById(R.id.slider_pitch)
    }
    private fun setListener(){
        button.setOnClickListener {
            pickAudioFile.launch("audio/*")
        }
        btnPlayAudio.setOnClickListener {

        }
        sliderPitch.addOnChangeListener { _, value, _ ->
            currentPitchFactor = value
            selectedAudioUri?.let { uri ->
                val tempFile = convertUriToFile(uri)
                tempFile.let { file ->
                    changeAudioPitch(file, currentPitchFactor)
                }
            }
        }

    }

        private fun changeAudioPitch(file: File, currentPitchFactor: Float) {

            val dispatcher = AudioDispatcherFactory.fromFile(file, 1024, 512)
            val pitchShifter = PitchShifter(currentPitchFactor.toDouble(), 44100.0, 1024, 512)
            dispatcher.addAudioProcessor(pitchShifter)
            dispatcher.addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    val audioTrack = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        44100,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
                        AudioTrack.MODE_STREAM
                    )
                    audioTrack.play()
                    audioTrack.write(audioEvent.byteBuffer, 0, audioEvent.byteBuffer.size)
                    return true
                }

                override fun processingFinished() {

                }

            })
            Thread {
                try {
                    dispatcher.run()
                    runOnUiThread {
                        Toast.makeText(this, "Audio pitch changed and playing!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error processing audio: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

    private fun convertUriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_audio.wav")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun playAudio(uri: Uri) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, uri)
        mediaPlayer?.start()
        Toast.makeText(this, "Playing audio...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}