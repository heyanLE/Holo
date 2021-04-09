package com.heyanle.holo.utils

import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.heyanle.holo.HoloApplication
import com.heyanle.holo.R
import com.heyanle.holo.ui.view.LineChartView
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by HeYanLe on 2021/2/6 0006 22:32.
 * https://github.com/heyanLE
 */
object ViewUtils {


    fun setToolbarCenter(toolbar: Toolbar){
        val title = "title"
        val originalTitle = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            toolbar.title
        } else {
            return
        }
        toolbar.title = title
        for (i in 0 until toolbar.childCount) {
            val view = toolbar.getChildAt(i)
            if (view is TextView) {
                if (title.contentEquals(view.text)) {
                    view.gravity = Gravity.CENTER
                    val params = androidx.appcompat.widget.Toolbar.LayoutParams(
                        androidx.appcompat.widget.Toolbar.LayoutParams.WRAP_CONTENT,
                        androidx.appcompat.widget.Toolbar.LayoutParams.MATCH_PARENT
                    )
                    params.gravity = Gravity.CENTER
                    view.layoutParams = params
                }
            }
            toolbar.title = originalTitle
        }
    }

    fun initChart(lineChart: LineChart){
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)
        lineChart.isDragEnabled = true
        lineChart.setTouchEnabled(true)
        lineChart.isScaleXEnabled = false
        lineChart.isScaleYEnabled = false
        val xAxis = lineChart.xAxis
        val leftYAxis = lineChart.axisLeft
        val rightYAxis = lineChart.axisRight

        rightYAxis.isEnabled = false

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisMinimum = 0F
        xAxis.granularity = 1f
        xAxis.textSize = 16F
        xAxis.setDrawGridLines(true)
        xAxis.setAvoidFirstLastClipping(false)
        lineChart.extraBottomOffset = 2*16F
        lineChart.extraRightOffset = 20F

        leftYAxis.textSize = 16F


        val legend = lineChart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textSize = 16F

        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL

        legend.setDrawInside(false)

        lineChart.setXAxisRenderer(
            CustomXAxisRenderer(
                lineChart.viewPortHandler, lineChart.xAxis,
                lineChart.getTransformer(YAxis.AxisDependency.LEFT)
            )
        )




    }

    fun initLineDataSet(
        lineDataSet: LineDataSet,
        color: Int
    ){

        lineDataSet.color = color
        lineDataSet.setCircleColor(color)
        lineDataSet.lineWidth = 1F
        lineDataSet.circleRadius = 3f

        lineDataSet.setDrawCircleHole(false)
        lineDataSet.valueTextSize = 0F

        lineDataSet.setDrawFilled(true)
        lineDataSet.formLineWidth = 1F
        lineDataSet.formSize = 15F


        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

    }

    fun showLineChart(lineChart: LineChart, data: ArrayList<LineChartView.TemData>) {

        val red = (0xFFe6212b).toInt()
        val black = Color.BLACK

        data.sortedBy {
            it.time
        }



        val redEntries = arrayListOf<Entry>()


        val blackEntries = arrayListOf<Entry>()
        if(data.isNotEmpty()){
            val min = data[0].time
            for (d in data) {
                var entry = Entry((d.time - min).toFloat(), d.upModelTem)
                redEntries.add(entry)
                entry = Entry((d.time - min).toFloat(), d.downModelTem)
                blackEntries.add(entry)
            }


            lineChart.xAxis.granularity = 5*60*1000F


            if(data.last().time >= 30*60*1000+min){
                lineChart.xAxis.axisMaximum = (data.last().time - min).toFloat()
                lineChart.xAxis.axisMinimum = (data.last().time - min - 30*60*1000).toFloat()
            }else{
                lineChart.xAxis.axisMaximum = (30*60*1000).toFloat()
                lineChart.xAxis.axisMinimum = (0).toFloat()
            }


            lineChart.xAxis.valueFormatter = object: ValueFormatter(){



                override fun getAxisLabel(value: Float, axis: AxisBase?): String {



                    val format = SimpleDateFormat(
                        "MM-dd\nHH:mm", Locale.getDefault()
                    )
                    return format.format(Date().apply {
                        time = (value + min).toLong()
                    })
                }
            }



        }



        val lineDatSet = LineDataSet(
            redEntries,
            HoloApplication.INSTANCE.getString(R.string.upModel_te_no)
        )
        initLineDataSet(lineDatSet, red)

        val blackLineSet = LineDataSet(
            blackEntries,
            HoloApplication.INSTANCE.getString(R.string.downModel_tem_no)
        )
        initLineDataSet(blackLineSet, black)

        val lineData = LineData()
        lineData.addDataSet(lineDatSet)
        lineData.addDataSet(blackLineSet)
        lineChart.data = lineData






    }

}