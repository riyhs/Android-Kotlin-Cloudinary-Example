package com.sekon.mycloudinary

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.createSource
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {
    private var config: MutableMap<String, String> = HashMap()

    private fun configCloudinary() {
        config["cloud_name"] = "your_cloud_name"
        config["api_key"] = "123456789"
        config["api_secret"] = "your_api_secret"
        MediaManager.init(this@MainActivity, config)
    }

    companion object {
        private const val PERMISSION_CODE = 1
        private const val PICK_IMAGE = 1
    }

    var filePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configCloudinary()

        //when click mImageAdd request the permission to access the gallery
        bt_pick_image.setOnClickListener { //request permission to access external storage
            requestPermission()
        }
        bt_save.setOnClickListener {
            uploadToCloudinary(filePath)
        }
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            accessTheGallery()
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                accessTheGallery()
            } else {
                Toast.makeText(this@MainActivity, "permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun accessTheGallery() {
        val i = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        i.type = "image/*"
        startActivityForResult(i, PICK_IMAGE)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //get the image's file location
        filePath = getRealPathFromUri(data?.data, this@MainActivity)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            try {
                //set picked image to the mProfile
                if (Build.VERSION.SDK_INT < 28) {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data?.data)
                    imageView.setImageBitmap(bitmap)
                } else {
                    val source = data?.data?.let {
                        createSource(this.contentResolver, it)
                    }
                    val bitmap = source?.let { ImageDecoder.decodeBitmap(it) }
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("Recycle")
    private fun getRealPathFromUri(imageUri: Uri?, activity: Activity): String? {
        val cursor: Cursor? = activity.contentResolver.query(imageUri!!, null, null, null, null)
        return if (cursor == null) {
            imageUri.path
        } else {
            cursor.moveToFirst()
            val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            cursor.getString(idx)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun uploadToCloudinary(filePath: String?) {
        Log.d("A", "sign up uploadToCloudinary- ")
        MediaManager.get().upload(filePath).callback(object : UploadCallback {
            override fun onStart(requestId: String) {
                tv_status.text = "start"
            }

            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                tv_status.text = "Uploading... "
            }

            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                tv_status.text = "image URL: " + resultData["url"].toString()
            }

            override fun onError(requestId: String, error: ErrorInfo) {
                tv_status.text = "error " + error.description
            }

            override fun onReschedule(requestId: String, error: ErrorInfo) {
                tv_status.text = "Reshedule " + error.description
            }
        }).dispatch()
    }
}