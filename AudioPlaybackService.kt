package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val surahId: Int = 1,
    val surahNameBangla: String = "",
    val reciterNameBangla: String = "",
    val ayahNumber: Int? = null,
    val isRepeatEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val isOfflineFile: Boolean = false,
    val errorMessage: String? = null
)

object AudioPlaybackController {
    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    fun updateState(transform: (AudioPlayerState) -> AudioPlayerState) {
        _state.value = transform(_state.value)
    }

    fun play(
        context: Context,
        sourceUrlOrPath: String,
        surahId: Int,
        surahNameBangla: String,
        reciterNameBangla: String,
        ayahNumber: Int? = null,
        speed: Float = _state.value.playbackSpeed,
        repeat: Boolean = _state.value.isRepeatEnabled,
        isOfflineFile: Boolean = false
    ) {
        val intent = Intent(context, AudioPlaybackService::class.java).apply {
            action = AudioPlaybackService.ACTION_PLAY
            putExtra(AudioPlaybackService.EXTRA_SOURCE, sourceUrlOrPath)
            putExtra(AudioPlaybackService.EXTRA_SURAH_ID, surahId)
            putExtra(AudioPlaybackService.EXTRA_SURAH_NAME, surahNameBangla)
            putExtra(AudioPlaybackService.EXTRA_RECITER_NAME, reciterNameBangla)
            if (ayahNumber != null) putExtra(AudioPlaybackService.EXTRA_AYAH_NUM, ayahNumber)
            putExtra(AudioPlaybackService.EXTRA_SPEED, speed)
            putExtra(AudioPlaybackService.EXTRA_REPEAT, repeat)
            putExtra(AudioPlaybackService.EXTRA_OFFLINE, isOfflineFile)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun togglePlayPause(context: Context) {
        context.startService(
            Intent(context, AudioPlaybackService::class.java).setAction(AudioPlaybackService.ACTION_TOGGLE)
        )
    }

    fun setSpeed(context: Context, speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
        context.startService(
            Intent(context, AudioPlaybackService::class.java)
                .setAction(AudioPlaybackService.ACTION_SPEED)
                .putExtra(AudioPlaybackService.EXTRA_SPEED, speed)
        )
    }

    fun toggleRepeat(context: Context) {
        val next = !_state.value.isRepeatEnabled
        _state.value = _state.value.copy(isRepeatEnabled = next)
        context.startService(
            Intent(context, AudioPlaybackService::class.java)
                .setAction(AudioPlaybackService.ACTION_REPEAT)
                .putExtra(AudioPlaybackService.EXTRA_REPEAT, next)
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, AudioPlaybackService::class.java).setAction(AudioPlaybackService.ACTION_STOP)
        )
    }
}

class AudioPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val src = intent.getStringExtra(EXTRA_SOURCE) ?: return START_NOT_STICKY
                val surahId = intent.getIntExtra(EXTRA_SURAH_ID, 1)
                val surahName = intent.getStringExtra(EXTRA_SURAH_NAME) ?: "কুরআন তিলাওয়াত"
                val reciterName = intent.getStringExtra(EXTRA_RECITER_NAME) ?: ""
                val ayahNum = if (intent.hasExtra(EXTRA_AYAH_NUM)) intent.getIntExtra(EXTRA_AYAH_NUM, 1) else null
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                val repeat = intent.getBooleanExtra(EXTRA_REPEAT, false)
                val isOffline = intent.getBooleanExtra(EXTRA_OFFLINE, false)

                AudioPlaybackController.updateState {
                    it.copy(
                        isLoading = true,
                        isPlaying = false,
                        surahId = surahId,
                        surahNameBangla = surahName,
                        reciterNameBangla = reciterName,
                        ayahNumber = ayahNum,
                        playbackSpeed = speed,
                        isRepeatEnabled = repeat,
                        isOfflineFile = isOffline,
                        errorMessage = null
                    )
                }
                startForeground(NOTIFICATION_ID, buildNotification(surahName, reciterName, true))
                prepareAndPlay(src, speed, repeat)
            }
            ACTION_TOGGLE -> {
                val player = mediaPlayer
                if (player != null) {
                    if (player.isPlaying) {
                        player.pause()
                        AudioPlaybackController.updateState { it.copy(isPlaying = false) }
                    } else {
                        player.start()
                        AudioPlaybackController.updateState { it.copy(isPlaying = true) }
                    }
                    val st = AudioPlaybackController.state.value
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIFICATION_ID, buildNotification(st.surahNameBangla, st.reciterNameBangla, st.isPlaying))
                }
            }
            ACTION_SPEED -> {
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                mediaPlayer?.let { mp ->
                    runCatching {
                        val wasPlaying = mp.isPlaying
                        mp.playbackParams = PlaybackParams().setSpeed(speed)
                        if (!wasPlaying) mp.pause()
                    }
                }
            }
            ACTION_REPEAT -> {
                val repeat = intent.getBooleanExtra(EXTRA_REPEAT, false)
                mediaPlayer?.isLooping = repeat
            }
            ACTION_STOP -> {
                releasePlayer()
                AudioPlaybackController.updateState { it.copy(isPlaying = false, isLoading = false) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun prepareAndPlay(source: String, speed: Float, repeat: Boolean) {
        releasePlayer()
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            isLooping = repeat
            setOnPreparedListener { mp ->
                runCatching {
                    if (speed != 1.0f) mp.playbackParams = PlaybackParams().setSpeed(speed)
                }
                mp.start()
                AudioPlaybackController.updateState { it.copy(isLoading = false, isPlaying = true, errorMessage = null) }
            }
            setOnCompletionListener {
                if (!isLooping) {
                    AudioPlaybackController.updateState { it.copy(isPlaying = false) }
                }
            }
            setOnErrorListener { _, _, _ ->
                AudioPlaybackController.updateState {
                    it.copy(
                        isLoading = false,
                        isPlaying = false,
                        errorMessage = "অডিও প্লে করতে ইন্টারনেট বা অফলাইন ডাউনলোড চেক করুন।"
                    )
                }
                true
            }
        }
        mediaPlayer = player
        runCatching {
            val file = File(source)
            if (file.exists()) {
                player.setDataSource(file.absolutePath)
            } else {
                player.setDataSource(source)
            }
            player.prepareAsync()
        }.onFailure { err ->
            AudioPlaybackController.updateState {
                it.copy(isLoading = false, isPlaying = false, errorMessage = err.localizedMessage)
            }
        }
    }

    private fun releasePlayer() {
        runCatching {
            mediaPlayer?.stop()
        }
        runCatching {
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    private fun buildNotification(title: String, subtitle: String, isPlaying: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AudioPlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AudioPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_muslim_bangla_emblem)
            .setContentTitle(title)
            .setContentText(subtitle.ifBlank { "কুরআন তিলাওয়াত চলছে" })
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .addAction(0, if (isPlaying) "বিরতি" else "চালু", toggleIntent)
            .addAction(0, "বন্ধ", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "কুরআন তিলাওয়াত অডিও",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "quran_audio_playback"
        const val NOTIFICATION_ID = 4101
        const val ACTION_PLAY = "com.example.audio.PLAY"
        const val ACTION_TOGGLE = "com.example.audio.TOGGLE"
        const val ACTION_SPEED = "com.example.audio.SPEED"
        const val ACTION_REPEAT = "com.example.audio.REPEAT"
        const val ACTION_STOP = "com.example.audio.STOP"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_SURAH_ID = "surah_id"
        const val EXTRA_SURAH_NAME = "surah_name"
        const val EXTRA_RECITER_NAME = "reciter_name"
        const val EXTRA_AYAH_NUM = "ayah_num"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_REPEAT = "repeat"
        const val EXTRA_OFFLINE = "offline"
    }
}
