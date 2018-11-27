package edu.ualr.mpkennett.gameengine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

class GoalPost(image: Bitmap?, x: Float, y: Float, w: Float, h: Float) : GameObject(image, x, y, w, h) {
    override fun draw(canvas: Canvas) {
        if(image != null)
        {
            canvas.drawBitmap(image, x, y, null)
        }
        else {
            val brush = Paint()
            brush.setARGB(255, 255, 255, 255)

            canvas.drawRect(x, y, (x + w), (y + h), brush)
        }
    }
}