package com.heyanle.holo.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import com.bumptech.glide.Glide
import com.heyanle.holo.databinding.ActivityAdBinding
import kotlinx.coroutines.*

/**
 * Created by HeYanLe on 2021/2/6 0006 15:45.
 * https://github.com/heyanLE
 */

class ADActivity : BaseActivity(), CoroutineScope by MainScope(){

    companion object{
        const val DELAY_TIME = 3000L
    }

    var isClick = false

    private val binding: ActivityAdBinding by  lazy {
        ActivityAdBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        loadAd()
        
//        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        val clip: ClipData = ClipData.newPlainText("Holo 序列号", "dsfsef")
//        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }


    private fun loadAd(){

        launch {
            val url = async{
                ""
            }

            if (FirstActivity.adWebsite.isNotEmpty()){
                Glide.with(this@ADActivity).load(FirstActivity.adWebsite)
                        .into(binding.ivAd)
            }


            binding.ivAd.setOnClickListener {
                isClick = true
                val uri: Uri = Uri.parse(FirstActivity.adUrl)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }


            Handler(Looper.getMainLooper()).postDelayed({
                if(!isClick) {
                    val intent = Intent(this@ADActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }, DELAY_TIME)
        }

    }

    override fun onResume() {
        super.onResume()
        if(isClick){
            val intent = Intent(this@ADActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }



}