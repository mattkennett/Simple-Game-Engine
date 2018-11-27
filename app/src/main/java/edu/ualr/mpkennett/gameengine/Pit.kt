package edu.ualr.mpkennett.gameengine

import android.graphics.*

class Pit(image: Bitmap?, x: Float, y: Float, w: Float, h: Float) : GameObject(image, x, y, w, h) {
    override fun draw(canvas: Canvas) {
        if(image != null)
        {
            val texture = BitmapShader(image, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            val brush = Paint()
            brush.shader = texture

            canvas.drawRect(x, y, (x + w), (y + h), brush)
        }
        else {
            val brush = Paint()
            brush.setARGB(127, 255, 0, 0)

            canvas.drawRect(x, y, (x + w), (y + h), brush)
        }
    }
}