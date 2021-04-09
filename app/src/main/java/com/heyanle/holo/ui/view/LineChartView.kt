package com.heyanle.holo.ui.view

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by HeYanLe on 2021/2/9 0009 17:46.
 * https://github.com/heyanLE
 */

class LineChartView : View {

    class TemData{
        var time = System.currentTimeMillis()
        var upModelTem = 0F
        var downModelTem = 0F
    }


    private val backgroundPaint = Paint()
    private val backgroundStrokePaint = Paint()
    private val dottedLinePaint = Paint()
    private val blackPaint = Paint()
    private val redPaint = Paint()
    private val yTextPaint = TextPaint()
    private val xTextPaint = TextPaint()


    init {
        backgroundPaint.color = Color.WHITE
        backgroundPaint.style = Paint.Style.FILL_AND_STROKE
        backgroundPaint.isAntiAlias = true

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

        xTextPaint.color = (0xFF666666).toInt()
        xTextPaint.style = Paint.Style.FILL_AND_STROKE
        xTextPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                12F, context.resources.displayMetrics)
        xTextPaint.textAlign = Paint.Align.CENTER
    }

    private var xLength: Long = 1000* 60* 30

    var minY = 20
    var maxY = 50

    val redPath = Path()
    val blackPath = Path()
    val temList = arrayListOf<TemData>()

    var realMinX = 0L
    var realMaxX = 0L

    var mat = Matrix()



    val gLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            40F, context.resources.displayMetrics)
    val gRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            16F, context.resources.displayMetrics)

    val gTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            16F, context.resources.displayMetrics)

    val gBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            37F, context.resources.displayMetrics)

    val format = SimpleDateFormat("HH:mm|MM-dd", Locale.getDefault())


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)


        // 画白底
        canvas.drawRect(gLeft, gTop, (width - gRight).toFloat(), (height - gBottom).toFloat(), backgroundPaint)


        // 边框
        canvas.drawRect(
                gLeft + backgroundStrokePaint.strokeWidth / 2F,
                gTop + backgroundStrokePaint.strokeWidth / 2F,
                width - backgroundStrokePaint.strokeWidth / 2F - gRight,
                height - backgroundStrokePaint.strokeWidth / 2F - gBottom,
                backgroundStrokePaint
        )

        // 竖线
        val offset = (width-gLeft - gRight) /6F

        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = realMinX
        val nn = calendar.get(Calendar.MINUTE)%5
        if(nn != 0)
        calendar.add(Calendar.MINUTE, 5-nn)
        Log.i("LineCharView","${calendar.time.time}")


        val s = calendar.time.time
        val f = floatArrayOf((s - realMinX).toFloat(), 0F)
        mat.mapPoints(f)
        var x = f[0]

        while (true){
            if(x > width-gRight){
                break
            }
            if( x < gLeft){
                x += offset
                continue
            }
            canvas.drawLine(x, gTop, x, (height - gBottom).toFloat(), backgroundStrokePaint)


            val fontMetrics: Paint.FontMetrics = xTextPaint.fontMetrics
            val top = fontMetrics.top //为基线到字体上边框的距离,即上图中的top

            val bottom = fontMetrics.bottom
            val textHeight =  - top + bottom

            val s0 = "${if(calendar.get(Calendar.HOUR)<10 ) "0${calendar.get(Calendar.HOUR)}" 
            else "${calendar.get(Calendar.HOUR)}"}:${if(calendar.get(Calendar.MINUTE)<10 ) "0${calendar.get(Calendar.MINUTE)}"
            else "${calendar.get(Calendar.MINUTE)}"}"

            val s1 = "${if(calendar.get(Calendar.MONTH)+1<10 ) "0${calendar.get(Calendar.MONTH)+1}"
            else "${calendar.get(Calendar.MONTH)+1}"}-${if(calendar.get(Calendar.DAY_OF_MONTH)<10 ) "0${calendar.get(Calendar.DAY_OF_MONTH)}"
            else "${calendar.get(Calendar.DAY_OF_MONTH)}"}"
            canvas.drawText(s0, x, height-gBottom-top, xTextPaint)
            canvas.drawText(s1, x, height-gBottom-top+textHeight, xTextPaint)

            calendar.add(Calendar.MINUTE, 5)
            x += offset
        }





        val yOffset = (height-gTop-gBottom)/6F
        var y = yOffset + gTop



        for(i in 0..4){
            canvas.drawLine(gLeft, y, (width - gRight).toFloat(), y, dottedLinePaint)
            y += yOffset
        }


        val red = Path()
        red.addPath(redPath, mat)

        val black = Path()
        black.addPath(blackPath, mat)
        canvas.drawPath(red, redPaint)
        canvas.drawPath(black, blackPaint)


        var textY = gTop
        var text = maxY
        val offsetText = ((maxY -minY)/6).toInt()

        while(true){
            if(textY > height-gBottom+1){
                break
            }
            val fontMetrics: Paint.FontMetrics = yTextPaint.fontMetrics
            val top = fontMetrics.top //为基线到字体上边框的距离,即上图中的top
            val bottom = fontMetrics.bottom
            val base =  (textY- top/2 - bottom/2)
            canvas.drawText("$text", gLeft - TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    12F, context.resources.displayMetrics), base, yTextPaint)
            text -= offsetText
            textY += yOffset
        }


    }

    fun refresh(){


        if(temList.size == 0){
            return
        }
        temList.sortedByDescending {
            it.time
        }

        realMaxX = temList[0].time


        for(t in temList){
            if(t.time < realMaxX - xLength){
                realMinX = realMaxX - xLength
                break
            }
            realMinX = t.time
        }

        blackPath.moveTo((temList[0].time - realMinX).toFloat(), maxY - temList[0].downModelTem)
        redPath.moveTo((temList[0].time - realMinX).toFloat(), maxY - temList[0].upModelTem)
        for (i in 1 until temList.size){
            val x = (temList[i].time- realMinX).toFloat()
            if(x < 0){
                break
            }
            blackPath.lineTo(x, maxY - temList[i].downModelTem)
            Log.i("LineCharView", "x $x | y ${maxY - temList[i].downModelTem}")
            redPath.lineTo(x, maxY - temList[i].upModelTem)
        }

        mat.reset()

        val rectR = RectF(0F, 0F, xLength.toFloat(), maxY - minY.toFloat())
        val rectS = RectF(gLeft, gTop, (width - gRight).toFloat(), (height - gBottom).toFloat())
        mat.setRectToRect(rectR, rectS, Matrix.ScaleToFit.FILL)

    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
}