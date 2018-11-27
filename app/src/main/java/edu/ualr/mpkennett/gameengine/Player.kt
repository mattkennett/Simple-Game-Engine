package edu.ualr.mpkennett.gameengine

import android.graphics.*
import android.util.Log
import kotlin.math.abs

class Player(image: Bitmap?, x: Float, y: Float, w: Float, h: Float) : GameObject(image, x, y, w, h) {
    var isDead: Boolean = false
    var hasWon: Boolean = false
    var maxVelocityX: Float = 50f
    var maxVelocityY: Float = 50f
    var deltaX: Float = 0f
    var deltaY: Float = 0f

    var movingLeft: Boolean = false
    var movingRight: Boolean = true
    var movingUp: Boolean = false
    var movingDown: Boolean = true

    init {
        monitorCollisions = true

        velocityY = 10f
    }

    override fun draw(canvas: Canvas) {
        if(image != null)
        {
            // Rotate the image 180 degrees if we're moving left
            if(movingLeft) {
                canvas.drawBitmap(image!!.flip(), x, y, null)
            }
            else {
                canvas.drawBitmap(image, x, y, null)
            }
        }
        else {
            val brush = Paint()
            brush.setARGB(255, 63, 63, 255)

            canvas.drawRect(x, y, (x + w), (y + h), brush)
        }
    }

    override fun update() {
        lastX = x
        lastY = y

        val lastVelocityX = velocityX
        val lastVelocityY = velocityY

        velocityX += deltaX
        velocityY += deltaY

        if (lastVelocityX < 0f && velocityX >= 0f) {
            deltaX = 0f
            velocityX = 0f
        }else if (lastVelocityX > 0f && velocityX <= 0f) {
            deltaX = 0f
            velocityX = 0f
        }

        // Add quasi-gravity
        if (velocityY < maxVelocityY) {
            velocityY += 10f
        }

        if (velocityY > maxVelocityY) {
            velocityY = maxVelocityY
        }

        if (velocityY < maxVelocityY * -1f) {
            velocityY = maxVelocityY * -1f
        }

        if (velocityX > maxVelocityX) {
            velocityX = maxVelocityX
        }

        if (velocityX < maxVelocityX * -1f) {
            velocityX = maxVelocityX * -1f
        }

        x += velocityX
        y += velocityY


    }

    override fun processCollision(collidedWith: GameObject) {
        var collisionResolved = false

        if (lastX != x) {
            movingLeft = lastX > x
            movingRight = lastX < x
        }

        if (lastY != y) {
            movingUp = lastY > y
            movingDown = lastY < y
        }


        var fixY = false
        var fixX = false

        if (movingDown && lastY + h <= collidedWith.y && y + h > collidedWith.y) {
            // We've moved down into the object
            fixY = true
        }
        if (movingUp && lastY >= collidedWith.y + collidedWith.h && y < collidedWith.y + collidedWith.h)
        {
            // We've moved up into the object
            fixY = true
        }
        if (movingRight && lastX + w <= collidedWith.x && x + w > collidedWith.x) {
            // We've moved right into the object
            fixX = true
        }
        if (movingLeft && lastX >= collidedWith.x + collidedWith.w && x < collidedWith.x + collidedWith.w)
        {
            // We've moved left into the object
            fixX = true
        }

        if (collidedWith is Pit) {
            if (movingDown) {
                isDead = true
            }

            collisionResolved = true
        }
        else if(collidedWith is GoalPost) {
            hasWon = true

            collisionResolved = true
        }
        else if (collidedWith is SolidSurface) {

            if (movingRight && fixX) {
                x = collidedWith.x - w - 1f
                velocityX = 0f
                collisionResolved = true
            }
            if (movingLeft && fixX) {
                x = collidedWith.x + collidedWith.w + 1f
                velocityX = 0f
                collisionResolved = true
            }
            if ((movingDown) && fixY) {
                y = collidedWith.y - h - 1f
                collisionResolved = true
            }
            if (movingUp && fixY) {
                y = collidedWith.y + collidedWith.h + 1f
                collisionResolved = true
            }
        }
        else if(collidedWith is Spring) {
            velocityY = -60f
            collisionResolved = true
        }

        if (!collisionResolved) {
            Log.d("GAME_UTIL", "Unresolved Collision")
        }
    }

    fun moveLeft() {
        if (abs(velocityX) < maxVelocityX) {
            velocityX -= 10f

            if (abs(velocityX) > maxVelocityX) {
                velocityX = maxVelocityX * -1f
            }
        }
    }

    fun moveRight() {
        if (velocityX < maxVelocityX) {
            velocityX += 10f

            if(velocityX > maxVelocityX) {
                velocityX = maxVelocityX
            }
        }
    }

    fun moveButtonUp() {
        if (velocityX > 0) {
            deltaX = -10f
        }
        if (velocityX < 0) {
            deltaX = 10f
        }
    }
}