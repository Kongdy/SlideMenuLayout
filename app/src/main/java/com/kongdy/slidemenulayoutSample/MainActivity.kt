package com.kongdy.slidemenulayoutSample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun myClick(v:View){
        when (v.id) {
            R.id.fab_op -> {
                if(sml_menu.isOpen){
                    sml_menu.animToClose()
                } else {
                    sml_menu.animToOpen()
                }
            }
            R.id.actv_content_text -> {
                Toast.makeText(this,"click content text suc",Toast.LENGTH_SHORT).show()
            }
            R.id.actv_test -> {
                Toast.makeText(this,"click menu text suc",Toast.LENGTH_SHORT).show()
            }
        }
    }
}
