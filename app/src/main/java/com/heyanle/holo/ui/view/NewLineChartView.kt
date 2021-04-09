package com.heyanle.holo.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class NewLineChartView : View {

    class TemData{
        var time = System.currentTimeMillis()
        var upModelTem = 0F
        var downModelTem = 0F
    }


    private val backgroundPaint = Paint()
    private val backgroundGrayPaint = Paint()
    private val backgroundStrokePaint = Paint()
    private val dottedLinePaint = Paint()
    private val blackPaint = Paint()
    private val redPaint = Paint()
    private val yTextPaint = TextPaint()
    private val xTextPaint = TextPaint()

    var isDrag = false

    init {
        backgroundPaint.color = Color.WHITE
        backgroundPaint.style = Paint.Style.FILL_AND_STROKE
        backgroundPaint.isAntiAlias = true

        backgroundGrayPaint.color = (0xfff7f7f8).toInt()
        backgroundGrayPaint.style = Paint.Style.FILL
        backgroundGrayPaint.isAntiAlias = true

        backgroundStrokePaint.color = (0xFFCCCCCC).toInt()
        backgroundStrokePaint.style = Paint.Style.STROKE
        backgroundStrokePaint.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1F, context.resources.displayMetrics)
        backgroundStrokePaint.isAntiAlias = true

        dottedLinePaint.color = (0xFFCCCCCC).toInt()
        dottedLinePaint.style = Paint.Style.STROKE
        dottedLinePaint.strokeWidth =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1F, context.resources.displayMetrics)
        dottedLinePaint.isAntiAlias = true
        dottedLinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0F)

        redPaint.color = (0xFFe6212b).toInt()
        redPaint.style = Paint.Style.STROKE
        redPaint.strokeWidth =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1F, context.resources.displayMetrics)
        redPaint.isAntiAlias = true

        blackPaint.color = Color.BLACK
        blackPaint.style = Paint.Style.STROKE
        blackPaint.strokeWidth =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                1F, context.resources.displayMetrics)
        blackPaint.isAntiAlias = true

        yTextPaint.color = (0xFF666666).toInt()
        yTextPaint.style = Paint.Style.FILL_AND_STROKE
        yTextPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                15F, context.resources.displayMetrics)
        yTextPaint.textAlign = Paint.Align.RIGHT
        yTextPaint.isAntiAlias = true

        xTextPaint.color = (0xFF666666).toInt()
        xTextPaint.style = Paint.Style.FILL_AND_STROKE
        xTextPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                12F, context.resources.displayMetrics)
        xTextPaint.textAlign = Paint.Align.CENTER
        xTextPaint.isAntiAlias = true
    }

    val list = arrayListOf<TemData>()

    private var minY = 0F
    private var maxY = 0F

    private var realStartX = 0L

    private var startX = 0L
    private var endX = 0L

    val gLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            40F, context.resources.displayMetrics)
    val gRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            16F, context.resources.displayMetrics)

    val gTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            16F, context.resources.displayMetrics)

    val gBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            37F, context.resources.displayMetrics)

    var maxT = 30*60*1000L

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 画白底
        canvas.drawRect(gLeft, gTop, (width - gRight).toFloat(), (height - gBottom).toFloat(), backgroundPaint)



        // 竖线
        val offset = (width-gLeft - gRight) /6F




        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = startX


        Log.i("LineCharView","realStartX -> ${realStartX}")
        Log.i("LineCharView","startX -> ${startX}")

        val del = (realStartX - startX)/((maxT).toFloat())*(width-gLeft - gRight)
        Log.i("LineCharView","del -> ${del}")


        val s = calendar.time.time

        var x = gLeft - del



        while (true){
            if(x > width-gRight){
                break
            }
            if( x < gLeft){
                x += offset
                calendar.add(Calendar.SECOND, (maxT/1000/6).toInt())
                continue
            }


            canvas.drawLine(x, gTop, x, (height - gBottom).toFloat(), backgroundStrokePaint)


            val fontMetrics: Paint.FontMetrics = xTextPaint.fontMetrics
            val top = fontMetrics.top //为基线到字体上边框的距离,即上图中的top

            val bottom = fontMetrics.bottom
            val textHeight =  - top + bottom

            val s0 = "${if(calendar.get(Calendar.HOUR_OF_DAY)<10 ) "0${calendar.get(Calendar.HOUR_OF_DAY)}"
            else "${calendar.get(Calendar.HOUR_OF_DAY)}"}:${if(calendar.get(Calendar.MINUTE)<10 ) "0${calendar.get(Calendar.MINUTE)}"
            else "${calendar.get(Calendar.MINUTE)}"}"

            val s1 = "${if(calendar.get(Calendar.MONTH)+1<10 ) "0${calendar.get(Calendar.MONTH)+1}"
            else "${calendar.get(Calendar.MONTH)+1}"}-${if(calendar.get(Calendar.DAY_OF_MONTH)<10 ) "0${calendar.get(Calendar.DAY_OF_MONTH)}"
            else "${calendar.get(Calendar.DAY_OF_MONTH)}"}"
            canvas.drawText(s0, x, height-gBottom-top, xTextPaint)
            canvas.drawText(s1, x, height-gBottom-top+textHeight, xTextPaint)

            calendar.add(Calendar.SECOND, (maxT/1000/6).toInt())
            x += offset
        }

        val yOffset = (height-gTop-gBottom)/5F
        var y = yOffset + gTop



        for(i in 0..3){
            canvas.drawLine(gLeft, y, (width - gRight).toFloat(), y, dottedLinePaint)
            y += yOffset
        }



        if(list.isEmpty()){
            return
        }

        var i = 0
        while(i<list.size && list[i].time < realStartX){
            i ++
        }

        i = max(0, i-1)
        var last = list[i]

        while(i < list.size){

            val startX = gLeft + ((last.time-realStartX)/(maxT).toFloat())*(width-gLeft-gRight)
            val startUpY = height-gBottom - (last.upModelTem-minY)/(maxY-minY)*(height-gTop-gBottom)
            val startDownY = height-gBottom - (last.downModelTem-minY)/(maxY-minY)*(height-gTop-gBottom)

            val endX = gLeft + ((list[i].time-realStartX)/(maxT).toFloat())*(width-gLeft-gRight)
            val endUpY = height-gBottom - (list[i].upModelTem-minY)/(maxY-minY)*(height-gTop-gBottom)
            val endDownY = height-gBottom - (list[i].downModelTem-minY)/(maxY-minY)*(height-gTop-gBottom)

            canvas.drawLine(startX, startUpY, endX, endUpY, blackPaint)
            canvas.drawLine(startX, startDownY, endX, endDownY, redPaint)
            if(endX > width-gRight){
                break
            }
            last = list[i]
            i ++
        }


        // 边框
        canvas.drawRect(
                gLeft + backgroundStrokePaint.strokeWidth / 2F,
                gTop + backgroundStrokePaint.strokeWidth / 2F,
                width - backgroundStrokePaint.strokeWidth / 2F - gRight,
                height - backgroundStrokePaint.strokeWidth / 2F - gBottom,
                backgroundStrokePaint
        )

        canvas.drawRect(0F, 0F, gLeft, height-gBottom, backgroundGrayPaint)
        canvas.drawRect(width-gRight, 0F,width+0F, height-gBottom, backgroundGrayPaint)
        var textY = gTop
        var text = maxY
        val offsetText = ((maxY -minY)/5).toInt()

        while(true){
            if(textY > height-gBottom+1){
                break
            }
            val fontMetrics: Paint.FontMetrics = yTextPaint.fontMetrics
            val top = fontMetrics.top //为基线到字体上边框的距离,即上图中的top
            val bottom = fontMetrics.bottom
            val base =  (textY- top/2 - bottom/2)
            canvas.drawText("${text.toInt()}", gLeft - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    12F, context.resources.displayMetrics), base, yTextPaint)
            text -= offsetText
            textY += yOffset
        }



    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        Log.i("NewLineChartView","x -> ${event.x}")
        Log.i("NewLineChartView","y -> ${event.y}")
        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                if(isDrag){
                    parent.parent.requestDisallowInterceptTouchEvent(true)
                    downX = event.x
                    downY = event.y
                }

            }
            MotionEvent.ACTION_MOVE -> {
                if(!isDrag){
                    parent.parent.requestDisallowInterceptTouchEvent(false)
                    return super.dispatchTouchEvent(event)
                }

            }
        }
        return super.dispatchTouchEvent(event)
    }
    var downX = 0F
    var downY = 0F
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(!isDrag){
            return super.onTouchEvent(event)
        }
        when(event.action){
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_MOVE -> {

                if(endX - startX <= 30*60*1000F){
                    return true
                }
                val dx = event.x - downX
                val dTime = dx/(width-gLeft-gRight)*(30*60*1000F)
                realStartX -= dTime.toLong()
                if(realStartX < startX){
                    realStartX = startX
                }
                if(realStartX + 30*60*1000 >= endX){
                    realStartX = endX - 30*60*1000
                }
                val p = SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.getDefault())

                Log.i("NewLineChart", "time -> ${p.format(Date().apply { 
                    time = realStartX
                })}")
                downX = event.x
                downY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {}

        }

        return super.onTouchEvent(event)
    }

    fun refresh(){


        if(list.isEmpty()){
            return
        }
        list.sortBy { it.time }
        minY = min(list[0].upModelTem , list[0].downModelTem)
        maxY = max(list[0].downModelTem, list[0].upModelTem)


        startX = list[0].time
        endX = list.last().time
        var isS = false
        realStartX = list[0].time


        maxT = ((((endX-startX)/1000L+(5*60))/((5*60)))*(5*60*1000L)).toLong()

        if(isDrag){
            maxT = max(maxT, 30*60*1000L)
        }

        Log.i("NewLineChartView","$maxT")
        if(realStartX + 30*60*1000L > endX - 30*1000) {
            realStartX = list[0].time
            isS = true
        }
        for(de in list){
            if(!isS)
                realStartX = min(realStartX, de.time)
            minY = min(min(minY, de.upModelTem ), de.downModelTem)
            maxY = max(max(maxY,de.downModelTem), de.upModelTem)

        }


        if(maxY.toInt()%10 != 0){
            maxY = (((maxY/10).toInt()+1)*10F)
        }
        if(minY.toInt()%10 != 0){
            minY = ((minY/10).toInt())*10F
        }
        if(maxY - minY < 30){
            maxY = minY+30
        }

        if(isS){
            realStartX = max(startX, list.last().time - 30*60*1000)
        }

        realStartX = startX

    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}