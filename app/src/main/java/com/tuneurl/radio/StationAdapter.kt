package com.tuneurl.radio

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuneurl.radio.core.Station

/**
 * Created by ramasheesh.y@macrew.net$ on 12-11-2025$.
 */

class StationAdapter(private val stations: List<Station>) :
    RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    inner class StationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name = view.findViewById<TextView>(R.id.tvStationName)
        val desc = view.findViewById<TextView>(R.id.tvStationDesc)
        val image = view.findViewById<ImageView>(R.id.imgStationLogo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_station_card, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val item = stations[position]
        holder.name.text = item.name
        holder.desc.text = item.desc

        // load image (local drawable or remote URL)
       /* val context = holder.image.context
        if (item.imageURL.startsWith("http"))
            Glide.with(context).load(item.imageURL).into(holder.image)
        else {
            val resId = context.resources.getIdentifier(
                item.imageURL, "drawable", context.packageName
            )
            if (resId != 0) holder.image.setImageResource(resId)
            else holder.image.setImageResource(R.drawable.default_station)
        }*/
        val context = holder.image.context
        val resId = context.resources.getIdentifier(
            item.imageURL.trim(), "drawable",  context.packageName
        )

        if (resId != 0) {
            holder.image.setImageResource(resId)
        } else {
           // holder.image.setImageResource(R.drawable.default_station)
        }


        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT) // even rows
        } else {
            holder.itemView.setBackgroundColor(
                Color.parseColor("#33000000") // black with 20% opacity (#33 = 20% alpha)
            )
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("name", item.name)
            intent.putExtra("uuid", item.uuid)
            intent.putExtra("streamURL", item.getStreamUri())
            intent.putExtra("imageURL", item.imageURL)
            intent.putExtra("desc", item.desc)
            intent.putExtra("longDesc", item.longDesc)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = stations.size
}
