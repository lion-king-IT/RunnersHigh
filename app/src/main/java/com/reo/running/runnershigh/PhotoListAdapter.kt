package com.reo.running.runnershigh

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class PhotoListAdapter(val list: List<RunResult>) : RecyclerView.Adapter<PhotoListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_photo_item,parent,false)
        return PhotoListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PhotoListViewHolder, position: Int) {
        holder.run {
            runPhoto.run {
                setImageBitmap(list[position].bitmap)
                if (list[position].colorId.isNotEmpty()) {
                    setBackgroundColor(Color.parseColor(list[position].colorId))
                }
            }
            runDate.setText(list[position].runData)
            runTime.setText(list[position].time)
            runDistance.text = "${list[position].distance}km"
            runCalorie.text = "${list[position].calorie}kcal"
            runMemo.text = "${list[position].memo}"
            runFeedBack.run {
                when (list[position].revaluationMark) {
                    FeedBack.PERFECT.revaluationMark -> {
                        setImageResource(R.drawable.ic_perfect)
                        setColorFilter(ContextCompat.getColor(context,R.color.perfect))
                        visibility = View.VISIBLE
                    }
                    FeedBack.GOOD.revaluationMark -> {
                        setImageResource(R.drawable.ic_good)
                        setColorFilter(ContextCompat.getColor(context,R.color.good))
                        visibility = View.VISIBLE
                    }
                    FeedBack.SOSO.revaluationMark -> {
                        setImageResource(R.drawable.ic_soso)
                        setColorFilter(ContextCompat.getColor(context,R.color.soso))
                        visibility = View.VISIBLE
                    }
                    FeedBack.BAD.revaluationMark -> {
                        setImageResource(R.drawable.ic_bad)
                        setColorFilter(ContextCompat.getColor(context,R.color.bad))
                        visibility = View.VISIBLE
                    }
                    FeedBack.TOOBAD.revaluationMark -> {
                        setImageResource(R.drawable.ic_sick)
                        setColorFilter(ContextCompat.getColor(context,R.color.tooBad))
                        visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    override fun getItemCount(): Int = list.size
}

enum class FeedBack(val revaluationMark:String) {
    PERFECT("perfect"),
    GOOD("good"),
    SOSO("soso"),
    BAD("bad"),
    TOOBAD("tooBad")
}

