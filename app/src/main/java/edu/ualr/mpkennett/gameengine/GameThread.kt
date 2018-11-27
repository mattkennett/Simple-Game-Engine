package edu.ualr.mpkennett.gameengine

import android.graphics.Canvas
import android.util.Log
import android.view.SurfaceHolder

class GameThread(private val surfaceHolder: SurfaceHolder, private val gameView: GameView) : Thread() {
    var running: Boolean = false

    private val targetFPS = 24 // frames per second, the rate at which you would like to refresh the Canvas

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        val targetTime = (1000 / targetFPS).toLong()

        while (running) {
            startTime = System.nanoTime()
            canvas = null

            try {
                // The canvas must be locked before we are allowed to draw on it
                canvas = this.surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    when(gameView.gameStatus) {
                        "game_running" -> gameView.gameLoop()
                        "game_over" -> gameView.gameOver()
                        "game_won" -> gameView.gameWon()
                        "start_screen" -> gameView.startScreen()
                    }

                    this.gameView.draw(canvas!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        // We need to unlock the canvas when we're done using it
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // The following code calculates how long it has taken to get to this point in the
            // loop and then attempts to sleep if we've executed faster than our target frames
            // per second time
            timeMillis = (System.nanoTime() - startTime) / 1000000
            waitTime = targetTime - timeMillis

            if(waitTime > 0) {
                try {
                    sleep(waitTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            else {
                Log.d("GAME_UTIL", "Target FPS missed")
            }
        }
    }

    companion object {
        private var canvas: Canvas? = null
    }

}