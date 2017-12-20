package com.kongdy.slidemenulayoutSample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun myClick(v:View){
        if(sml_menu.isOpen){
            sml_menu.animToClose()
        } else {
            sml_menu.animToOpen()
        }
    }
}
