/*
 * MainActivity.kt
 * Implements the MainActivity class
 * MainActivity is the default activity that can host the player fragment and the settings fragment
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package com.tuneurl.radio

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dekidea.tuneurl.util.Constants
import com.dekidea.tuneurl.util.TuneURLManager
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.tuneurl.radio.core.Collection
import com.tuneurl.radio.core.Station
import com.tuneurl.radio.helpers.AppThemeHelper
import com.tuneurl.radio.helpers.CollectionHelper
import com.tuneurl.radio.helpers.FileHelper
import com.tuneurl.radio.helpers.ImportHelper
import com.tuneurl.radio.helpers.PreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

/*
 * MainActivity class
 */
class MainActivity : AppCompatActivity() {

    /* Main class variables */
    private lateinit var appBarConfiguration: AppBarConfiguration


    private lateinit var adapter: StationAdapter
    private var collection: Collection = Collection()
    /* Overrides onCreate from AppCompatActivity */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeResources()
        // house-keeping: determine if edit stations is enabled by default todo: remove in 2023
        if (PreferencesHelper.isHouseKeepingNecessary()) {
            // house-keeping 1: remove hard coded default image
            ImportHelper.removeDefaultStationImageUris(this)
            // house-keeping 2: if existing user detected, enable Edit Stations by default
            if (PreferencesHelper.loadCollectionSize() != -1) {
                // existing user detected - enable Edit Stations by default
                PreferencesHelper.saveEditStationsEnabled(true)
            }
            PreferencesHelper.saveHouseKeepingNecessaryState()
        }

        // set up views
        setContentView(R.layout.activity_main)

        // create .nomedia file - if not yet existing
        FileHelper.createNomediaFile(getExternalFilesDir(null))
        collection = FileHelper.readCollection(this)

        // register listener for changes in shared preferences
        PreferencesHelper.registerPreferenceChangeListener(sharedPreferenceChangeListener)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerStations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        var stations: ArrayList<Station> = ArrayList()
        if (collection.stations.isEmpty()){
            stations = ArrayList(loadStationsFromAssets())
            stations.forEach {
                it.streamUris.add(it.streamURL)
            }
            collection.stations.addAll(stations)
        }else{
            stations = ArrayList(collection.stations)
        }

        CollectionHelper.saveCollection(this, collection, canupdate = true)
        adapter = StationAdapter(stations){station->
            findViewById<TextView>(R.id.tvNowPlayingTitle).text=station.desc
            findViewById<TextView>(R.id.tvNowPlayingSubtitle).text=station.name
            animateMusicIcon()
        }

        recyclerView.adapter = adapter

        FileHelper.createNomediaFile(getExternalFilesDir(null))
        findViewById<ImageView>(R.id.playingNow).setOnClickListener {
            showCurrentPlaying()
        }
        musicView = findViewById(R.id.imgNowPlayingBars)
        musicView?.setOnClickListener {
            showCurrentPlaying()
        }
    }
    private fun showCurrentPlaying(){
        adapter.selectedItem?.let {item->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.putExtra("name", item.name)
            intent.putExtra("uuid", item.uuid)
            intent.putExtra("streamURL", item.getStreamUri())
            intent.putExtra("imageURL", item.imageURL)
            intent.putExtra("desc", item.desc)
            intent.putExtra("longDesc", item.longDesc)
            startActivity(intent)
        }
    }
    var index = 0
    var musicJob : Job?=null
    var musicView: ImageView?=null
    private fun stopMusicAnimation(){
        musicJob?.cancel()
        musicJob=null
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


    /* Overrides onSupportNavigateUp from AppCompatActivity */
//    override fun onSupportNavigateUp(): Boolean {
//        // Taken from: https://developer.android.com/guide/navigation/navigation-ui#action_bar
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_host_container) as NavHostFragment
//        val navController = navHostFragment.navController
//        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
//    }


    /* Overrides onDestroy from AppCompatActivity */
    override fun onDestroy() {
        stopMusicAnimation()
        super.onDestroy()
        // unregister listener for changes in shared preferences
        PreferencesHelper.unregisterPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                Keys.PREF_THEME_SELECTION -> {
                    AppThemeHelper.setTheme(PreferencesHelper.loadThemeSelection())
                }
            }
        }
    /*
     * End of declaration
     */
    private fun loadStationsFromAssets(): List<Station> {
        val inputStream = assets.open("stations.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, List<Station>>>() {}.type
        val data: Map<String, List<Station>> = Gson().fromJson(reader, type)
        return data["station"] ?: emptyList()
    }

    //Splash code

    private
     val SETTING_TRIGGER_FILE_PATH: String = "com.dekidea.tuneurl.SETTING_TRIGGER_FILE_PATH"
    private
     val DEFAULT_TUNEURL_API_BASE_URL: String =
        "http://ec2-54-213-252-225.us-west-2.compute.amazonaws.com"
    private
     val DEFAULT_SEARCH_FINGERPRINT_URL: String =
        "https://pnz3vadc52.execute-api.us-east-2.amazonaws.com/dev/search-fingerprint"
    private
     val DEFAULT_POLL_API_URL: String =
        "http://pollapiwebservice.us-east-2.elasticbeanstalk.com/api/pollapi"
    private
     val DEFAULT_INTERESTS_API_URL: String =
        "https://65neejq3c9.execute-api.us-east-2.amazonaws.com/interests"
    //private  final String DEFAULT_GET_CYOA_API_URL = "https://65neejq3c9.execute-api.us-east-2.amazonaws.com/get-your-cyoa";
    private
     val DEFAULT_GET_CYOA_API_URL: String =
        "https://pnz3vadc52.execute-api.us-east-2.amazonaws.com/dev/get-cyoa-mp3"

    private fun initializeResources() {
        var reference_file_path =
            TuneURLManager.fetchStringSetting(this, SETTING_TRIGGER_FILE_PATH, null)

        if (reference_file_path == null || reference_file_path.isEmpty()) {
            TuneURLManager.updateStringSetting(
                this,
                Constants.SETTING_TUNEURL_API_BASE_URL,
                DEFAULT_TUNEURL_API_BASE_URL
            )
            TuneURLManager.updateStringSetting(
                this,
                Constants.SETTING_SEARCH_FINGERPRINT_URL,
                DEFAULT_SEARCH_FINGERPRINT_URL
            )
            TuneURLManager.updateStringSetting(
                this,
                Constants.SETTING_POLL_API_URL,
                DEFAULT_POLL_API_URL
            )
            TuneURLManager.updateStringSetting(
                this,
                Constants.SETTING_INTERESTS_API_URL,
                DEFAULT_INTERESTS_API_URL
            )
            TuneURLManager.updateStringSetting(
                this,
                Constants.SETTING_GET_CYOA_API_URL,
                DEFAULT_GET_CYOA_API_URL
            )

            reference_file_path =
                installReferenceWavFile(this, R.raw.trigger_audio, "trigger_audio.raw")

            TuneURLManager.updateStringSetting(this, SETTING_TRIGGER_FILE_PATH, reference_file_path)
        }
    }


    private fun installReferenceWavFile(
        context: Context,
        raw_resource: Int,
        file_name: String?
    ): String? {
        var output_file_path: String? = null

        var input_stream: InputStream? = null

        try {
            input_stream =
                context.getApplicationContext().getResources().openRawResource(raw_resource)

            val file_path = getExternalFilesDir(null)!!.getPath() + "/" + file_name

            val success = writeFile(input_stream, file_path)

            if (success) {
                output_file_path = file_path
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                input_stream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return output_file_path
    }


    private fun writeFile(input_stream: InputStream, output_file_path: String): Boolean {
        var success = false

        var output_stream: OutputStream? = null

        try {
            val out_file = File(output_file_path)
            if (!out_file.exists()) {
                output_stream = FileOutputStream(output_file_path)

                val buffer = ByteArray(1024)
                var length: Int

                while ((input_stream.read(buffer).also { length = it }) != -1) {
                    output_stream.write(buffer, 0, length)
                }

                output_stream.flush()

                success = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                if (output_stream != null) {
                    output_stream.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return success
    }
}
