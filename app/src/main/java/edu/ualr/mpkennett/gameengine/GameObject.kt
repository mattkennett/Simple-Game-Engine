package edu.ualr.mpkennett.gameengine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

open class GameObject(var image: Bitmap?, var x: Float, var y: Float, var w: Float, var h: Float) {
    // To calculate the direction that a collision comes from, we need to know the previous values
    // of X and Y for a game object
    var lastX: Float = 0f
    var lastY: Float = 0f

    var velocityX: Float = 0f
    var velocityY: Float = 0f

    // The following variable determines whether or not a GameObject will be monitored for collisions
    var monitorCollisions: Boolean = false

    open fun draw(canvas: Canvas) {
        // Child classes will determine how they're drawn
    }

    open fun update() {
        // Child classes will determine how they're updated
    }

    open fun processCollision(collidedWith: GameObject) {
        // Child classes will determine how to process their collisions
    }

    fun collidesWith(target: GameObject) : Boolean {
        // This is the Axis-Aligned Bounding Box algorithm for detecting a collision between two
        // instances of GameObject.  You can read more about the algorithm on MDN Web Docs:
        // https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection
        return ( x < target.x + target.w &&
                x + w > target.x &&
                y < target.y + target.h &&
                y + h > target.y)

    }

    // This function will flip a bitmap 180 degrees along the Y axis
    fun Bitmap.flip(): Bitmap {
        val matrix = Matrix().apply {
            setScale(-1f, 1f)
            postTranslate(width.toFloat(), 0f)
        }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}