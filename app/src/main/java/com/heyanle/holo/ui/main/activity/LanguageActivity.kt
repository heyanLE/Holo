package com.heyanle.holo.ui.main.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.heyanle.holo.R
import com.heyanle.holo.databinding.ActivityLanguageBinding
import com.heyanle.holo.language.LanguageManager
import com.heyanle.holo.ui.activity.BaseActivity
import com.heyanle.holo.utils.ViewUtils

/**
 * Created by HeYanLe on 2021/2/8 0008 18:27.
 * https://github.com/heyanLE
 */

class LanguageActivity : BaseActivity(){

    companion object{
        const val KEY = "Language Key"
    }

    private val binding: ActivityLanguageBinding by  lazy {
        ActivityLanguageBinding.inflate(LayoutInflater.from(this))
    }

    private var select = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        select = LanguageManager.nowIndex
        refresh()

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_chevron_left_24)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        ViewUtils.setToolbarCenter(binding.toolbar)


        binding.layoutChinese.setOnClickListener {
            select = 0
            refresh()
            finish()
        }
        binding.layoutEnglish.setOnClickListener {
            select = 1
            refresh()
            finish()
        }
    }

    private fun refresh(){
        val intent = Intent()
        intent.putExtra(KEY, select)
        setResult(RESULT_OK, intent)

        if(select == 0){
            binding.ivChinese.visibility = View.VISIBLE
            binding.ivEnglish.visibility = View.INVISIBLE
        }else{
            binding.ivChinese.visibility = View.INVISIBLE
            binding.ivEnglish.visibility = View.VISIBLE
        }
    }

}