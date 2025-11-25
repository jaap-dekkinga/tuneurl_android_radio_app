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

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.tuneurl.radio.core.Station
import com.tuneurl.radio.helpers.AppThemeHelper
import com.tuneurl.radio.helpers.CollectionHelper
import com.tuneurl.radio.helpers.FileHelper
import com.tuneurl.radio.helpers.ImportHelper
import com.tuneurl.radio.helpers.PreferencesHelper
import java.io.InputStreamReader

import com.tuneurl.radio.core.Collection

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
        adapter = StationAdapter(stations)

        recyclerView.adapter = adapter

        FileHelper.createNomediaFile(getExternalFilesDir(null))


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
}
