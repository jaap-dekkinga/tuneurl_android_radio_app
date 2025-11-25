package com.tuneurl.radio


import com.tuneurl.radio.core.Collection
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView

import androidx.media3.session.MediaController
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.tuneurl.radio.extensions.play
import com.tuneurl.radio.helpers.CollectionHelper
import com.tuneurl.radio.helpers.FileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerActivity : AppCompatActivity() {

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null
    private var collection: Collection = Collection()
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
        val uuid = intent.getStringExtra("uuid")?:""
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
            if (controller?.isPlaying == true ) {
                // stop playback
                controller?.pause()
                findViewById<ImageButton>(R.id.btnPlayPause).setImageResource(R.drawable.ic_player_play_symbol_42dp)
            }
            // CASE: the selected station is not playing (another station might be playing)
            else {

                findViewById<ImageButton>(R.id.btnPlayPause).setImageResource( R.drawable.ic_stop)
                controller?.play(this, CollectionHelper.getStation(collection, uuid))
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


        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            withContext(Dispatchers.Main){
                controller?.play(this@PlayerActivity, CollectionHelper.getStation(collection, uuid))

            }
        }
    }
    override fun onStart() {
        super.onStart()
        // initialize MediaController - connect to PlayerService
        initializeController()
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
//        controller.addListener(playerListener)
//        requestMetadataUpdate()
//        // handle start intent
//        handleStartIntent()
    }

}