package com.darina.PRM_2_S25580

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class StorageMode(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val path = Path()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 10f
        isAntiAlias = true
    }

    private val paths = ArrayList<Path>()
    private val paints = ArrayList<Paint>()
    private var backgroundBitmap: Bitmap? = null

    init {
        setBackgroundColor(Color.BLUE)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        backgroundBitmap?.let {
            val left = (width - it.width) / 2f
            val top = (height - it.height) / 2f
            canvas.drawBitmap(it, left, top, null)
        }
        for (i in paths.indices) {
            canvas.drawPath(paths[i], paints[i])
        }
        canvas.drawPath(path, paint)
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        val adjustedX = x - (width - (backgroundBitmap?.width ?: 0)) / 2f
        val adjustedY = y - (height - (backgroundBitmap?.height ?: 0)) / 2f

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(adjustedX, adjustedY)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(adjustedX, adjustedY)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                paths.add(Path(path))
                paints.add(Paint(paint))
                path.reset()
            }
        }
        return true
    }

    fun clear() {
        paths.clear()
        paints.clear()
        invalidate()
    }

    fun setBackgroundBitmap(bitmap: Bitmap) {
        if (width == 0 || height == 0) {
            post { setBackgroundBitmap(bitmap) }
            return
        }
        val bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height
        val viewAspectRatio = width.toFloat() / height

        val scale: Float = if (bitmapAspectRatio > viewAspectRatio) {
            height.toFloat() / bitmap.height
        } else {
            width.toFloat() / bitmap.width
        }

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap,
            (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)

        val left = (width - scaledBitmap.width) / 2f
        val top = (height - scaledBitmap.height) / 2f

        backgroundBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(backgroundBitmap!!)
        canvas.drawBitmap(scaledBitmap, left, top, null)

        invalidate()
    }

    fun exportDrawing(): Bitmap {
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        backgroundBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        paths.forEachIndexed { index, path ->
            canvas.drawPath(path, paints[index])
        }
        return resultBitmap
    }
}
