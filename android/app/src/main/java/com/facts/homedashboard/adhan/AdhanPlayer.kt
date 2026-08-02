package com.facts.homedashboard.adhan

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager

/**
 * Plays the adhan. Looks for a bundled recording at res/raw/adhan (drop in your
 * preferred reciter's file named `adhan.mp3`). Until one is present it falls
 * back to the device's default alarm sound so the timing can be verified.
 */
object AdhanPlayer {

    @Volatile
    private var player: MediaPlayer? = null

    fun play(context: Context) {
        stop()
        val ctx = context.applicationContext
        val resId = ctx.resources.getIdentifier("adhan", "raw", ctx.packageName)

        val mp = MediaPlayer()
        // Attributes must be set before prepare so the adhan plays on the alarm
        // stream (audible even in Do-Not-Disturb / silent).
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        try {
            if (resId != 0) {
                ctx.resources.openRawResourceFd(resId).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            } else {
                mp.setDataSource(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            }
        } catch (e: Exception) {
            mp.release()
            return
        }
        mp.setOnPreparedListener { it.start() }
        mp.setOnCompletionListener {
            it.release()
            if (player === it) player = null
        }
        player = mp
        mp.prepareAsync()
    }

    fun stop() {
        player?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
        player = null
    }

    fun isBundledAdhanPresent(context: Context): Boolean =
        context.resources.getIdentifier("adhan", "raw", context.packageName) != 0
}
