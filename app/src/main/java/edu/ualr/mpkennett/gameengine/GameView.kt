package edu.ualr.mpkennett.gameengine

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.lang.Thread.sleep
import java.util.*


class GameView(context: Context, attributes: AttributeSet) : SurfaceView(context, attributes), SurfaceHolder.Callback {
    private val thread: GameThread
    private lateinit var gameObjects: MutableList<GameObject>
    private lateinit var objectCollisions: MutableList<Pair<GameObject, GameObject>>
    private lateinit var player: Player

    // These variables are updated in onTouchEvent() so that the game can capture and process inputs
    // from a user
    private var touchDown: Boolean = false
    private var touchUp: Boolean = false
    private var touchedX: Float = 0f
    private var touchedY: Float = 0f

    // Since Android devices have a variety of screen sizes, we need access to the width and height
    // of the current device
    private val screenWidth: Int = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight: Int = Resources.getSystem().displayMetrics.heightPixels

    // The GameView will calculate the edges of the play area based on the screen width and height
    // of the device and the size of the game's textures.  These floating point values are initialized
    // to 0f because primitive types cannot be lateinit in Kotlin.  The values will be calculated
    // in surfaceCreated()
    private var leftEdge: Float = 0f
    private var rightEdge: Float = 0f
    private var topEdge: Float = 0f
    private var bottomEdge: Float = 0f
    private var UIyTop: Float = 0f
    private var UIbuttonWidth: Float = 0f
    private var UIbuttonHeight: Float = 0f

    private val playerInitX = 150f
    private val playerInitY = 50f
    private var playerInitW = 40f
    private var playerInitH = 60f

    // This value represents the width and height of a single tile on the play surface.  Its value
    // will be calculated when textures are loaded in surfaceCreated()
    private var tileSize: Int = 0

    var gameStatus: String = "start_screen"

    init {
        // add callback
        holder.addCallback(this)

        // instantiate the game thread
        thread = GameThread(holder, this)
    }


    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        gameObjects = mutableListOf()
        objectCollisions = mutableListOf()

        // All game textures are loaded before the game loop begins
        val brickTexture: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.brick)
        val spikeTexture: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.spike)
        val playerTexture: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.player)
        val springTexture: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.spring)
        val goalTexture: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.goal)

        // Check to ensure that all textures have loaded successfully
        val texturesLoaded: Boolean = brickTexture != null
                && spikeTexture != null
                && playerTexture != null
                && springTexture != null
                && goalTexture != null

        if (!texturesLoaded) {
            Log.d("GAME_ERROR", "Error Loading Textures")
        }

        if (playerTexture != null) {
            playerInitW = playerTexture.width.toFloat()
            playerInitH = playerTexture.height.toFloat()
        }

        // The playerTexture is as wide as one tile of the gaming surface
        tileSize = playerTexture!!.width

        // The following code calculates the edges of the gaming surface.  If the width or height
        // of a device is not evenly divisible by tileSize, some padding will be added around the
        // edges of the gaming surface
        val tileWidth: Int = screenWidth / tileSize
        val tileHeight: Int = screenHeight / tileSize

        val deadXSpace: Int = screenWidth - tileWidth * tileSize
        val deadYSpace: Int = screenHeight - tileHeight * tileSize

        if (deadXSpace == 0) {
            rightEdge = screenWidth.toFloat()
        }
        else {
            val unusedXPadding: Float = deadXSpace / 2f
            leftEdge = unusedXPadding
            rightEdge = screenWidth - unusedXPadding
        }

        if (deadYSpace == 0) {
            bottomEdge = screenHeight.toFloat()
        }
        else {
            val unusedYPadding: Float = deadYSpace / 2f
            topEdge = unusedYPadding
            bottomEdge = screenHeight - unusedYPadding
        }

        // Some space is saved at the bottom of the screen for a user interface
        UIbuttonHeight = 3f * tileSize
        UIbuttonWidth = 4f * tileSize

        UIyTop = bottomEdge - UIbuttonHeight

        // The following code initializes all game objects and adds them to the gameObjects list
        player = Player(
                playerTexture,
                playerInitX,
                playerInitY,
                playerTexture!!.width.toFloat(),
                playerTexture!!.height.toFloat()
        )
        val pit = Pit(
                spikeTexture,
                leftEdge,
                UIyTop - spikeTexture!!.height.toFloat(),
                rightEdge - leftEdge,
                spikeTexture!!.height.toFloat()
        )
        val ceiling = SolidSurface(
                brickTexture,
                leftEdge,
                topEdge,
                rightEdge - leftEdge,
                tileSize.toFloat()
        )
        val wallLeft = SolidSurface(
                brickTexture,
                leftEdge,
                topEdge,
                tileSize.toFloat(),
                UIyTop - topEdge
        )
        val wallRight = SolidSurface(
                brickTexture,
                rightEdge - tileSize.toFloat(),
                topEdge,
                tileSize.toFloat(),
                UIyTop - topEdge
        )
        val floorOne = SolidSurface(
                brickTexture,
                leftEdge,
                topEdge + 5 * tileSize,
                18 * tileSize.toFloat(),
                tileSize.toFloat()
        )
        val floorTwo = SolidSurface(
                brickTexture,
                leftEdge + 23 * tileSize,
                topEdge + 5 * tileSize,
                rightEdge - leftEdge - 23 * tileSize,
                tileSize.toFloat()
        )
        val floorThree = SolidSurface(
                brickTexture,
                leftEdge,
                UIyTop - tileSize * 2,
                leftEdge + 6 * tileSize,
                tileSize.toFloat()
        )
        val floorFour = SolidSurface(
                brickTexture,
                leftEdge + 9 * tileSize,
                UIyTop - tileSize * 2,
                leftEdge + 18 * tileSize,
                tileSize.toFloat()
        )
        val springOne = Spring(
                springTexture,
                leftEdge + 9 * tileSize,
                UIyTop - tileSize * 2,
                tileSize.toFloat(),
                tileSize.toFloat()
        )
        val goalPost = GoalPost(
                goalTexture,
                leftEdge + tileSize,
                UIyTop - tileSize * 3,
                tileSize.toFloat(),
                tileSize.toFloat()
        )

        gameObjects.add(player)
        gameObjects.add(pit)
        gameObjects.add(ceiling)
        gameObjects.add(wallLeft)
        gameObjects.add(wallRight)
        gameObjects.add(floorOne)
        gameObjects.add(floorTwo)
        gameObjects.add(floorThree)
        gameObjects.add(floorFour)
        gameObjects.add(springOne)
        gameObjects.add(goalPost)

        // The following code starts the game's thread
        thread.running = true
        thread.start()
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, i: Int, i1: Int, i2: Int) {
        // This function must be implemented because GameView implements SurfaceView
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        // The following code kills the game's thread when the Surface is destroyed
        var retry = true
        while (retry) {
            try {
                thread.join()
                thread.running = false
            } catch (e: Exception) {
                e.printStackTrace()
            }

            retry = false
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // This function is called on every loop of the game.  The status of the game determines
        // what will be drawn on the canvas
        when (gameStatus) {
            "game_running" -> {
                drawGameScreen(canvas)
                drawTouchControls(canvas)
            }
            "start_screen" -> drawStartScreen(canvas)
            "game_over" -> drawGameOver(canvas)
            "game_won" -> drawGameWon(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // The following code captures a touch event and updates the variables that GameView uses
        // to process user input
        touchedX = event.x
        touchedY = event.y

        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                touchDown = true
                touchUp = false
            }
            MotionEvent.ACTION_MOVE -> {
                touchDown = true
                touchUp = false
            }
            MotionEvent.ACTION_UP -> {
                touchDown = false
                touchUp = true
            }
            MotionEvent.ACTION_CANCEL -> {
                touchDown = false
                touchUp = false
            }
            MotionEvent.ACTION_OUTSIDE -> {
                touchDown = false
                touchUp = false
            }
        }

        return true
    }

    fun startScreen() {
        // The player's values are initialized to their original values here so that the game can
        // restart gracefully after a game over or win condition
        player.x = playerInitX
        player.y = playerInitY
        player.velocityX = 0f
        player.velocityY = 0f
        player.deltaX = 0f
        player.deltaY = 0f
        player.isDead = false
        player.hasWon = false

        // The gameStatus will be changed to game_running if the user touches the start button
        if(touchDown) {
            val touchedStartButton: Boolean = touchedX > screenWidth.toFloat() / 2f - 150f &&
                    touchedX < screenWidth.toFloat() / 2f + 100f &&
                    touchedY > screenHeight.toFloat() / 2f &&
                    touchedY < screenHeight.toFloat() / 2f + 150f

            if(touchedStartButton) {
                gameStatus = "game_running"
            }
        }
    }

    fun gameOver() {
        // The gameStatus will be changed to start_screen if the user touches the restart button
        if(touchDown) {
            val touchedStartButton: Boolean = touchedX > screenWidth.toFloat() / 2f - 150f &&
                    touchedX < screenWidth.toFloat() / 2f + 100f &&
                    touchedY > screenHeight.toFloat() / 2f &&
                    touchedY < screenHeight.toFloat() / 2f + 150f

            if(touchedStartButton) {
                gameStatus = "start_screen"
            }
        }
    }

    fun gameWon() {
        // The gameStatus will be changed to start_screen if the user touches the restart button
        if(touchDown) {
            val touchedStartButton: Boolean = touchedX > screenWidth.toFloat() / 2f - 150f &&
                    touchedX < screenWidth.toFloat() / 2f + 100f &&
                    touchedY > screenHeight.toFloat() / 2f &&
                    touchedY < screenHeight.toFloat() / 2f + 150f

            if(touchedStartButton) {
                gameStatus = "start_screen"
            }
        }
    }

    fun gameLoop() {
        when {
            // The Player objects keeps up with whether or not it is dead or has reached the goal.
            // On each game loop, we check to see if the player is dead or has won the game and
            // update the game status if necessary
            player.isDead -> gameStatus = "game_over"
            player.hasWon -> gameStatus = "game_won"
            else -> {
                // This is the main game loop.  On each iteration of the loop, we will process input
                // from our user, update all game objects, detect any collisions, and finally resolve
                // any collisions that we detect
                processInput()
                updateGameObjects()
                detectCollisions()
                processCollisions()
            }
        }
    }

    private fun processInput() {
        // The following code checks to see if the left or right movement buttons were touched down
        // or up.  If so, the player is updated to reflect the touch
        val leftButtonTouched = (touchedY >= UIyTop &&
                touchedX <= UIbuttonWidth)
        val rightButtonTouched = (touchedY >= UIyTop &&
                touchedX > UIbuttonWidth &&
                touchedX <= (UIbuttonWidth * 2f))

        if (touchDown) {
            // Left button touchDown
            if (leftButtonTouched) {
                player.moveLeft()
            }
            // Right button touchDown
            else if (rightButtonTouched) {
                player.moveRight()
            }
        }
        else if (touchUp) {
            if (leftButtonTouched || rightButtonTouched) {
                player.moveButtonUp()
            }
        }
    }

    private fun updateGameObjects() {
        for(gameObject: GameObject in gameObjects) {
            gameObject.update()
        }
    }

    private fun detectCollisions() {
        // This collision detection is quite naive.  It iterates through each game object to see if
        // the object should be monitored for collisions.  If so, that game object is checked against
        // all of the other game objects to see if a collision has occurred.  This simple approach
        // works for relatively small numbers of game objects, but would be too slow for large numbers
        // of game objects.
        for(monitoredObject: GameObject in gameObjects) {
            if (monitoredObject.monitorCollisions) {
                for(gameObject: GameObject in gameObjects) {
                    if (gameObject != monitoredObject) {
                        if (monitoredObject.collidesWith(gameObject)) {
                            objectCollisions.add(Pair(monitoredObject, gameObject))
                        }
                    }
                }
            }
        }
    }

    private fun processCollisions() {
        for(collision: Pair<GameObject, GameObject> in objectCollisions) {
            collision.first.processCollision(collision.second)
        }
        // Once the collisions have been processed, the list of collisions should be emptied before
        // the next loop of the game runs
        objectCollisions = mutableListOf()
    }

    private fun drawTouchControls(canvas: Canvas) {
        val brush = Paint()
        brush.setARGB(255, 255, 255, 255)

        brush.style = Paint.Style.STROKE

        canvas.drawRect(
                leftEdge,
                UIyTop,
                UIbuttonWidth,
                UIyTop + UIbuttonHeight - 1f,
                brush
        )
        canvas.drawRect(
                leftEdge + UIbuttonWidth,
                UIyTop,
                leftEdge + (UIbuttonWidth * 2),
                UIyTop + UIbuttonHeight - 1f,
                brush
        )

        brush.textSize = 90f
        brush.typeface = Typeface.SANS_SERIF
        brush.style = Paint.Style.FILL_AND_STROKE

        canvas.drawText(
                "<-",
                leftEdge + (UIbuttonWidth / 3f),
                UIyTop + (UIbuttonHeight / 2f),
                brush
        )
        canvas.drawText(
                "->",
                leftEdge + UIbuttonWidth + (UIbuttonWidth / 3f),
                UIyTop + (UIbuttonHeight / 2f),
                brush
        )
    }

    private fun drawSplashScreenBackground(canvas: Canvas) {
        val brush = Paint()

        val backgroundShader: Shader = RadialGradient(
                (screenWidth / 2).toFloat(),
                (screenHeight / 2).toFloat(),
                (screenWidth / 2).toFloat(),
                Color.parseColor("#BBBDC1"),
                Color.parseColor("#3A4351"),
                Shader.TileMode.CLAMP
        )
        val foregroundShader: Shader = LinearGradient(
                0f,
                100f,
                0f,
                screenHeight - 100f,
                Color.parseColor("#6D747F"),
                Color.parseColor("#3A4351"),
                Shader.TileMode.CLAMP
        )

        brush.style = Paint.Style.FILL
        brush.shader = backgroundShader

        canvas.drawRect(
                0f,
                0f,
                screenWidth.toFloat(),
                screenHeight.toFloat(),
                brush
        )

        brush.shader = foregroundShader

        canvas.drawRect(
                100f,
                100f,
                screenWidth.toFloat() - 100f,
                screenHeight.toFloat() - 100f,
                brush
        )

        brush.style = Paint.Style.STROKE
        brush.strokeWidth = 4f
        brush.shader = null
        brush.setARGB(255, 0, 0, 0)

        canvas.drawRect(
                100f,
                100f,
                screenWidth.toFloat() - 100f,
                screenHeight.toFloat() - 100f,
                brush
        )
    }

    private fun drawStartScreen(canvas: Canvas) {
        drawSplashScreenBackground(canvas)

        val brush = Paint()

        brush.textSize = 120f
        brush.typeface = Typeface.SANS_SERIF

        brush.style = Paint.Style.FILL
        brush.setARGB(255, 193, 192, 198)

        canvas.drawText(
                "Welcome to Tank Jump!",
                250f,
                400f,
                brush
        )

        brush.style = Paint.Style.STROKE
        brush.strokeWidth = 3f
        brush.setARGB(255, 0, 0, 0)

        canvas.drawText(
                "Welcome to Tank Jump!",
                250f,
                400f,
                brush
        )

        brush.style = Paint.Style.FILL
        brush.setARGB(255, 193, 192, 198)

        canvas.drawRect(
                (screenWidth.toFloat() / 2f) - 150f,
                screenHeight.toFloat() / 2f,
                (screenWidth.toFloat() / 2f) + 100f,
                (screenHeight.toFloat() / 2f) + 150f,
                brush
        )

        brush.setARGB(255, 0, 0, 0)
        brush.textSize = 90f

        canvas.drawText(
                "Start",
                (screenWidth.toFloat() / 2f) - 130f,
                screenHeight.toFloat() / 2f + 100f,
                brush
        )
    }

    private fun drawGameScreen(canvas: Canvas) {
        // Draw Background
        val brush = Paint()
        val shader: Shader = LinearGradient(
                0f,
                0f,
                0f,
                UIyTop,
                Color.parseColor("#A2B6B4"),
                Color.parseColor("#6D747F"),
                Shader.TileMode.CLAMP
        )

        brush.shader = shader
        brush.style = Paint.Style.FILL_AND_STROKE
        canvas.drawRect(
                0f,
                0f,
                screenWidth.toFloat(),
                screenHeight.toFloat(),
                brush
        )

        brush.setARGB(255, 0, 0, 0)
        brush.shader = null
        canvas.drawRect(
                0f,
                UIyTop,
                screenWidth.toFloat(),
                screenHeight.toFloat(),
                brush
        )

        for (gameObject in gameObjects) {
            gameObject.draw(canvas)
        }
    }

    private fun drawGameOver(canvas: Canvas) {
        drawSplashScreenBackground(canvas)

        val brush = Paint()

        brush.textSize = 65f
        brush.typeface = Typeface.SANS_SERIF

        brush.style = Paint.Style.FILL
        brush.setARGB(255, 255, 255, 255)

        canvas.drawText(
                "Game Over :(",
                250f,
                400f,
                brush
        )

        brush.style = Paint.Style.STROKE
        brush.setARGB(255, 0, 0, 0)

        canvas.drawText("Game Over :(", 250f, 400f, brush)

        brush.style = Paint.Style.FILL
        brush.setARGB(255, 255, 255, 255)

        canvas.drawRect(
                (screenWidth.toFloat() / 2f) - 150f,
                screenHeight.toFloat() / 2f,
                (screenWidth.toFloat() / 2f) + 100f,
                (screenHeight.toFloat() / 2f) + 150f,
                brush
        )

        brush.setARGB(255, 0, 0, 0)

        canvas.drawText(
                "Restart",
                (screenWidth.toFloat() / 2f) - 130f,
                screenHeight.toFloat() / 2f + 100f,
                brush
        )
    }

    private fun drawGameWon(canvas: Canvas) {
        drawSplashScreenBackground(canvas)

        val brush = Paint()

        brush.textSize = 65f
        brush.typeface = Typeface.SANS_SERIF

        brush.style = Paint.Style.FILL
        brush.setARGB(255, 255, 255, 255)

        canvas.drawText(
                "You Win!",
                250f,
                400f,
                brush
        )

        brush.style = Paint.Style.STROKE
        brush.setARGB(255, 0, 0, 0)

        canvas.drawText(
                "You Win!",
                250f,
                400f,
                brush
        )

        brush.style = Paint.Style.FILL
        brush.setARGB(255, 255, 255, 255)

        canvas.drawRect(
                (screenWidth.toFloat() / 2f) - 150f,
                screenHeight.toFloat() / 2f,
                (screenWidth.toFloat() / 2f) + 100f,
                (screenHeight.toFloat() / 2f) + 150f,
                brush
        )

        brush.setARGB(255, 0, 0, 0)

        canvas.drawText(
                "Restart",
                (screenWidth.toFloat() / 2f) - 130f,
                screenHeight.toFloat() / 2f + 100f,
                brush
        )
    }

    private fun ClosedRange<Int>.random() =
            Random().nextInt((endInclusive + 1) - start) +  start


}