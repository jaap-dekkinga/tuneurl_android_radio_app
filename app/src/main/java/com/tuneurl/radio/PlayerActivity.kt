package com.tuneurl.radio


import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.tuneurl.radio.core.Collection
import com.tuneurl.radio.extensions.play
import com.tuneurl.radio.helpers.CollectionHelper
import com.tuneurl.radio.helpers.FileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


class PlayerActivity : AppCompatActivity() {
    private val TAG: String = PlayerFragment::class.java.simpleName
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null
    private var collection: Collection = Collection()

    private var uuid :String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_player)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootPlayer)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        collection = FileHelper.readCollection(this)


        val name = intent.getStringExtra("name")?:""
          uuid = intent.getStringExtra("uuid")?:""
        val streamURL = intent.getStringExtra("streamURL")?:""
        val imageURL = intent.getStringExtra("imageURL")?:""
        val desc = intent.getStringExtra("desc")?:""
        val longDesc = intent.getStringExtra("longDesc")?:""
//        collection.
//        stations.add(
//            Station(
//                uuid = uuid,
//                name = name,
//                streamURL = streamURL,
//                imageURL = imageURL,
//                desc = desc,
//                longDesc = longDesc,
//                streamContent = "application/vnd.apple.mpegurl",
//                codec = "UNKNOWN"
//            )
//        )

        // streamContent = "application/vnd.apple.mpegurl",
//                    codec = "UNKNOWN"
//        CollectionHelper.saveCollection(this, collection)

        findViewById<ImageButton>(R.id.btnPlayPause).setOnClickListener {
            togglePlayButton(controller?.isPlaying==true)
            if (controller?.isPlaying == true ) {
                // stop playback
                controller?.pause()
//                findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(R.drawable.ic_player_play_symbol_42dp)
//                stopMusicAnimation()
            }
            // CASE: the selected station is not playing (another station might be playing)
            else {

                findViewById<ImageButton>(R.id.btnPlayPause).setImageResource( R.drawable.ic_stop)
                controller?.play(this, CollectionHelper.getStation(collection, uuid))
//                animateMusicIcon()
                // start playback
//                controller?.play(this, Station(
//                    uuid = UUID.randomUUID().toString(),
//                    name ,
//                    streamURL ,
//                    imageURL  ,
//                    desc,
//                    longDesc,
//
//                ))
            }
        }

        //http://as-hls-ww-live.akamaized.net/pool_74208725/live/ww/bbc_radio_two/bbc_radio_two.isml/bbc_radio_two-audio=320000.norewind.m3u8
// streamContent = "application/vnd.apple.mpegurl",
//                    codec = "UNKNOWN"
        val tvStationTitle = findViewById<TextView>(R.id.tvStationTitle)
        val tvNowPlayingTitle = findViewById<TextView>(R.id.tvNowPlayingTitle)
        val imgStation = findViewById<ImageView>(R.id.imgStation)

        tvStationTitle.text = name
        tvNowPlayingTitle.text = desc

        if (!imageURL.isNullOrEmpty()) {
            if (imageURL.startsWith("http")) {
                Glide.with(this).load(imageURL).into(imgStation)
            } else {
                val resId = resources.getIdentifier(imageURL, "drawable", packageName)
                if (resId != 0) imgStation.setImageResource(resId)
                //else imgStation.setImageResource(R.drawable.default_station)
            }
        }

        findViewById<ImageButton>(R.id.btnInfo).setOnClickListener {
            val intent = Intent(this, StationDetailsActivity::class.java)
            intent.putExtra("name", name)
            intent.putExtra("streamURL", streamURL)
            intent.putExtra("imageURL", imageURL)
            intent.putExtra("desc", desc)
            intent.putExtra("longDesc", longDesc)
            startActivity(intent)
        }

        /*if (controller?.isPlaying == true && stationUuid == playerState.stationUuid) {
            // stop playback
            controller?.pause()
        }
        // CASE: the selected station is not playing (another station might be playing)
        else {
            // start playback
            controller?.play(activity as Context, CollectionHelper.getStation(collection, stationUuid))
        }*/

        musicView = findViewById(R.id.btnPause)
//       CoroutineScope(Dispatchers.IO).launch {
//            delay(1000)
//            withContext(Dispatchers.Main){
//                if(!(controller?.isPlaying?:true)) controller?.play(this@PlayerActivity, CollectionHelper.getStation(collection, uuid))
//            }
//            animateMusicIcon()
//        }
        initVolume()

    }
    private fun initVolume(){
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager?

        // Set the music stream volume to 50% of its maximum
        val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?:0
        val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)?:0
        val seekBar = findViewById<SeekBar>(R.id.seekVolume)
        seekBar.max= maxVolume *100
        seekBar.progress = currentVolume  *100
        var oldProgress = currentVolume

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(
                p0: SeekBar?,
                p1: Int,
                p2: Boolean
            ) {
                val targetVolume =( (p0?.progress?:0)  /100)
                if (abs(oldProgress - targetVolume)>0){
                    oldProgress = targetVolume
                    audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

//                val targetVolume = (maxVolume.toFloat() * ( p0?.progress?:0)).toInt() // 50%

                val targetVolume =( (p0?.progress?:0)  /100)
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
            }

        })
    }
    override fun onStart() {
        super.onStart()
        // initialize MediaController - connect to PlayerService
        initializeController()
    }
    var index = 0
    var musicJob : Job?=null
    var musicView: ImageButton?=null
    private fun stopMusicAnimation(){
        musicJob?.cancel()
        musicJob=null
    }

    override fun onDestroy() {
        stopMusicAnimation()
        super.onDestroy()
    }
    private fun animateMusicIcon(){
        musicJob?.cancel()
        musicJob=null
        musicJob= CoroutineScope(Dispatchers.IO).launch {
            while (true){
                delay(100)
                withContext(Dispatchers.Main){
                    musicView?.setImageResource(
                        when(index%4){
                            0-> R.drawable.ic_now_playing_bars_0
                            1-> R.drawable.ic_now_playing_bars_1
                            2-> R.drawable.ic_now_playing_bars_2
                            else-> R.drawable.ic_now_playing_bars_3
                        }
                    )
                }
                index++
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializeController() {
        controllerFuture = MediaController.Builder(
            this,
            SessionToken(
                this,
                ComponentName(this, PlayerService::class.java)
            )
        ).buildAsync()
        controllerFuture.addListener({ setupController() }, MoreExecutors.directExecutor())
    }


    /* Releases MediaController */
    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }


    /* Sets up the MediaController  */
    private fun setupController() {
        val controller: MediaController = this.controller ?: return
        controller.addListener(playerListener)
//        requestMetadataUpdate()
//        // handle start intent
        handleStartIntent()
    }
    private fun handleStartIntent() {
        if ( intent.action != null) {
            when ( intent.action) {
                Keys.ACTION_SHOW_PLAYER -> handleShowPlayer()
                Intent.ACTION_VIEW -> handleViewIntent()
                Keys.ACTION_START -> handleStartPlayer()
            }
        }
        // clear intent action to prevent double calls
         intent.action = ""
    }
    /* Handles ACTION_SHOW_PLAYER request from notification */
    private fun handleShowPlayer() {
        Log.i(TAG, "Tap on notification registered.")
        // todo implement
    }
    /* Handles ACTION_VIEW request to add Station */
    private fun handleViewIntent() {

    }
    /* Handles this activity's start intent */
    private fun handleStartPlayer(){
        if (intent.hasExtra(Keys.EXTRA_START_LAST_PLAYED_STATION)){

        }else{
//            if(!(controller?.isPlaying?:true))
                controller?.play(this@PlayerActivity, CollectionHelper.getStation(collection, uuid))

            togglePlayButton(controller?.isPlaying==true)
        }
        animateMusicIcon()
    }

    private var playerListener: Player.Listener = object : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            // store new station
//            playerState.stationUuid = mediaItem?.mediaId ?: String()
//            // update station specific views
//            updatePlayerViews()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            // store state of playback
//            playerState.isPlaying = isPlaying
            // animate state transition of play button(s)
//            layout.animatePlaybackButtonStateTransition(activity as Context, isPlaying)
            togglePlayButton(isPlaying)
//            layout.updatePlayerViews(activity as Context, station, playerState.isPlaying)

            if (isPlaying) {
                // playback is active
//                layout.showPlayer(activity as Context)
//                layout.showBufferingIndicator(buffering = false)
            } else {
                // playback is paused or stopped
                // check if buffering (playback is not active but playWhenReady is true)
                if (controller?.playWhenReady == true) {
                    // playback is buffering, show the buffering indicator
//                    layout.showBufferingIndicator(buffering = true)
                } else {
                    // playback is not buffering, hide the buffering indicator
//                    layout.showBufferingIndicator(buffering = false)
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            if (playWhenReady && controller?.isPlaying == false) {
//                layout.showBufferingIndicator(buffering = true)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            togglePlayButton(false)
//            layout.showBufferingIndicator(false)
            Toast.makeText(this@PlayerActivity, R.string.toastmessage_connection_failed, Toast.LENGTH_LONG).show()
        }
    }
    fun togglePlayButton(isPlaying: Boolean) {
        if (!isPlaying) {

            findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(R.drawable.ic_player_play_symbol_42dp)
            stopMusicAnimation()
        } else {
            findViewById<ImageButton>(R.id.btnPlayPause).setImageResource( R.drawable.ic_stop)
            animateMusicIcon()
        }
    }
}