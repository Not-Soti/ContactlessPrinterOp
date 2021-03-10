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

@SuppressLint("ClickableViewAccessibility")
class ZoomLayout(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs) {

    lateinit var imagePreview : ImageView
    val tag = "--- ZoomLayout ---"
    var firstFingerY1 = 0F
    var secondFingerY1 = 0F
    var fingerDistanceY = 0F
    var isZooming = false
    var scale = 1F
    private val myGestureListener = MyGestureListener()
    private val myTouchListener = MyTouchListener(context, myGestureListener)

    fun setImageView(im : ImageView){
        imagePreview = im
        myGestureListener.imagePreview = im
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
                    //First finger pressed
                    Log.d(tag, "Action down")
                    firstFingerY1 = event.y
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    //Second finger pressed, get initial distance between fingers
                    Log.d(tag, "Action pointer down")
                    secondFingerY1 = event.getY(1)
                    fingerDistanceY = kotlin.math.abs(firstFingerY1 - secondFingerY1)
                    isZooming = true
                    Log.d(tag, "f1: $firstFingerY1, f2: $secondFingerY1, Distance $fingerDistanceY")
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isZooming) {
                        //calculating new distance between fingers
                        val firstFingerY2 = event.getY(0)
                        val secondFingerY2 = event.getY(1)
                        val newDistanceY = kotlin.math.abs(firstFingerY2 - secondFingerY2)

                        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
                        scale = 1 + ((newDistanceY - fingerDistanceY) / screenHeight)
                        scale = 0.1f.coerceAtLeast(scale.coerceAtMost(5.0f))
                        Log.d(tag, "Scale: $scale, d1=$fingerDistanceY, d2=$newDistanceY")
                        imagePreview.scaleX = scale
                        imagePreview.scaleY = scale
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
                    Log.d(tag, "Action up")
                    /*imagePreview.scaleX = 1f
                    imagePreview.scaleY = 1f
                    scale = 1F*/
                    firstFingerY1 = 0F
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

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            Log.d(tag, "onDoubleTap")
            val ret = super.onDoubleTap(e)
            imagePreview.scaleX=1F
            imagePreview.scaleY=1F
            return ret
        }
    }
}