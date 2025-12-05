package com.tuneurl.radio

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide

class StationDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_station_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val ivStationLogo = findViewById<ImageView>(R.id.ivStationLogo)
        val tvStationName = findViewById<TextView>(R.id.tvStationName)
        val tvStationSubtitle = findViewById<TextView>(R.id.tvStationSubtitle)
        val tvStationLongDesc = findViewById<TextView>(R.id.tvStationLongDesc)
        val btnOkay = findViewById<TextView>(R.id.btnOkay)

        val name = intent.getStringExtra("name")
        val imageURL = intent.getStringExtra("imageURL")
        val desc = intent.getStringExtra("desc")
        val longDesc = intent.getStringExtra("longDesc")

        tvStationName.text = name
        tvStationSubtitle.text = desc
        tvStationLongDesc.text = longDesc

        if (!imageURL.isNullOrEmpty()) {
            if (imageURL.startsWith("http")) {
                Glide.with(this).load(imageURL).into(ivStationLogo)
            } else {
                val resId = resources.getIdentifier(imageURL, "drawable", packageName)
                if (resId != 0) ivStationLogo.setImageResource(resId)
            }
        }

        ivBack.setOnClickListener { finish() }
        btnOkay.setOnClickListener { finish() }
    }
}