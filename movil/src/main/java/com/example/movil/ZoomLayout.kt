package com.example.movil

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.newFixedThreadPoolContext

/**
 * Layout class that has an ImageView, and detects gestures
 * that allow to zoom in or out that ImageView
 */
@SuppressLint("ClickableViewAccessibility")
class ZoomLayout(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs) {

    lateinit var imagePreview : ImageView
    val tag = "--- ZoomLayout ---"

    private val myGestureListener = MyGestureListener()
    private val myTouchListener = MyTouchListener(context, myGestureListener)

    /**
     * Method that sets the ImageView in the layout, and saves
     * it's screen coordinates in the Gesture Listener
     */
    fun setImageView(im : ImageView){
        imagePreview = im
        myGestureListener.imagePreview = im
        myGestureListener.originalY = imagePreview.y
        myGestureListener.originalX = imagePreview.x
        myTouchListener.imagePreview = im
    }

    init {
        this.setOnTouchListener(myTouchListener)
    }

    /**
     * Class that processes pinch gestures in order to zoom in or out an ImageView
     * Delegates double tap to its GestureListener
     */
    private class MyTouchListener(context: Context, myGestList: MyGestureListener) : OnTouchListener {
        val tag = "--- MyTouchListener ---"
        var firstFingerY1 = 0F
        var firstFingerX1 = 0F
        var secondFingerY1 = 0F
        var fingerDistanceY = 0F
        var isZooming = false
        var scale = 1F
        lateinit var imagePreview : ImageView
        val myGestureDetector = GestureDetector(context, myGestList)

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            myGestureDetector.onTouchEvent(event)

            when (event!!.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    //First finger pressed -> save the coordinates
                    //Log.d(tag, "Action down")
                    firstFingerY1 = event.y
                    firstFingerX1 = event.x
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    //Second finger pressed, get initial distance between fingers
                    //Log.d(tag, "Action pointer down")
                    secondFingerY1 = event.getY(1)
                    fingerDistanceY = kotlin.math.abs(firstFingerY1 - secondFingerY1)
                    isZooming = true
                    //Log.d(tag, "f1: $firstFingerY1, f2: $secondFingerY1, Distance $fingerDistanceY")
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isZooming) {
                        //Zoom the image
                        //calculating new distance between fingers
                        val firstFingerY2 = event.getY(0)
                        val secondFingerY2 = event.getY(1)
                        val newDistanceY = kotlin.math.abs(firstFingerY2 - secondFingerY2)
                        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                        scale = 1 + ((newDistanceY - fingerDistanceY) / screenHeight)
                        scale = 0.1f.coerceAtLeast(scale.coerceAtMost(5.0f))
                        //Log.d(tag, "Scale: $scale, d1=$fingerDistanceY, d2=$newDistanceY")
                        imagePreview.scaleX = scale
                        imagePreview.scaleY = scale
                    }else if(!isZooming && scale > 1F){
                        //if it's zoomed in we can move the image throug the screen
                        val newX = event.x
                        val newY = event.y
                        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
                        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

                        val variationX = (newX - firstFingerX1) / screenWidth * 100
                        val variationY = (newY - firstFingerY1) / screenHeight * 100

                        val newPosX = imagePreview.x + variationX
                        val newPosY = imagePreview.y + variationY
                        //Control image doesnt go out the screen
                        if(newPosX <= (screenWidth/2) && (newPosX >= (0-(screenWidth/2))))
                            imagePreview.x = newPosX
                        if(newPosY <= (screenHeight/2) && (newPosY >= (0-(screenWidth/2))))
                            imagePreview.y = newPosY
                    }
                    return true
                }
                MotionEvent.ACTION_POINTER_UP ->{
                    //Second finger lifted
                    isZooming = false
                    secondFingerY1 = 0F
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    //Swipe finished
                    //Log.d(tag, "Action up")
                    /*imagePreview.scaleX = 1f
                    imagePreview.scaleY = 1f
                    scale = 1F*/
                    firstFingerY1 = 0F
                    firstFingerX1 = 0F
                    secondFingerY1 = 0F
                    isZooming = false
                    return true
                }
            }
            return true
        }
    }

    /**
     * Class that processes when a double tap is made on the layout to reset
     * the ImageView's zoom
     */
    private class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        lateinit var imagePreview : ImageView
        val tag = "--- MyGestList ---"
        var originalX = 0F
        var originalY = 0F

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            val ret = super.onDoubleTap(e)
            imagePreview.scaleX=1F
            imagePreview.scaleY=1F
            imagePreview.x = originalX
            imagePreview.y = originalY
            return ret
        }
    }
}