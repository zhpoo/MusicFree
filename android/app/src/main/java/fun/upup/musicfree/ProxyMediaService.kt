package `fun`.upup.musicfree

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import com.doublesymmetry.trackplayer.model.Track
import com.doublesymmetry.trackplayer.service.MusicService

/**
 * 代理播放服务，让系统识别为播放器应用。
 */
class ProxyMediaService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private var musicService: MusicService? = null
    private var pendingIntent: Intent? = null

    companion object {
        const val PLAY_FILE_ACTION = "fun.upup.musicfree.ACTION_PLAY_FILE"
        const val PLAY_FILE_URI = "FILE_URI"
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            musicService = (binder as MusicService.MusicBinder).service
            initializeMediaSession()

            handleIntent(pendingIntent?.also { pendingIntent = null })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        bindService(Intent(this, MusicService::class.java), connection, BIND_AUTO_CREATE)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        handleIntent(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleIntent(intent: Intent?) {
        if (musicService == null || intent == null) {
            pendingIntent = intent
            return
        }
        if (intent.action == PLAY_FILE_ACTION) {
            val uri = intent.getStringExtra(PLAY_FILE_URI)?.toUri() ?: return
            val track = Track(applicationContext, Bundle(), 0)
            track.uri = uri
            musicService?.load(track)
            musicService?.play()
        }
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "ProxyMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    musicService?.play()
                }

                override fun onPause() {
                    musicService?.pause()
                }

                override fun onStop() {
                    musicService?.stop()
                }

                override fun onSkipToNext() {
                    musicService?.skipToNext()
                }

                override fun onSkipToPrevious() {
                    musicService?.skipToPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    musicService?.seekTo(pos / 1000F)
                }
            })
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot("MUSIC_FREE", null)
    }

    override fun onLoadChildren(
        parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        unbindService(connection)
    }
}