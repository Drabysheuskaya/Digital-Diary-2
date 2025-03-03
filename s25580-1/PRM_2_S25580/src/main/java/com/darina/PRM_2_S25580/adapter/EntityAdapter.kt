package com.darina.PRM_2_S25580.adapter

import EntityDetailActivity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.darina.PRM_2_S25580.model.Entity
import java.text.SimpleDateFormat
import java.util.Locale
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.darina.PRM_2_S25580.R

class EntityAdapter(private val context: Context, private var entityList: List<Entity>) :
    RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.resourse_entity, parent, false)
        return EntityViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        holder.bind(entityList[position])
    }

    override fun getItemCount(): Int = entityList.size

    inner class EntityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textEntity: TextView = itemView.findViewById(R.id.textEntity)
        private val textLocation: TextView = itemView.findViewById(R.id.textLocation)
        private val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)
        private val imageViewPhoto: ImageView = itemView.findViewById(R.id.imageEntryPhoto)

        fun bind(entity: Entity) {
            textEntity.text = entity.text
            textLocation.text = entity.location
            textTimestamp.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(entity.timestamp)

            entity.photoData?.let {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                imageViewPhoto.setImageBitmap(bitmap)
            }

            itemView.setOnClickListener {
                val intent = Intent(context, EntityDetailActivity::class.java).apply {
                    putExtra("ENTITY_ID", entity.id)
                }
                context.startActivity(intent)
            }
        }
    }

    fun updateData(newEntityList: List<Entity>) {
        entityList = newEntityList
        notifyDataSetChanged()
    }
}
