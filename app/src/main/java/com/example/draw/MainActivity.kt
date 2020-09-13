package com.example.draw

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.brushsize_dialog.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var m_Text = ""
    private var imageButtonCurrentPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingview.SetBrushSize(5.toFloat())
        imageButtonCurrentPaint = color_palette[2] as ImageButton
        imageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.palette_selected)
        )

        brush.setOnClickListener{
            ShowBrushSizeDialog()
        }

        gallery.setOnClickListener{
            if(isReadStorageAllowed()){
                val pickPhotIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotIntent,GALLERY)
            }
            else{
                requestStoragePermission()
            }
        }

        undo.setOnClickListener{
            drawingview.onClickUndo()
        }

        save.setOnClickListener{
            if(isReadStorageAllowed()){
                setFilename()
            }
            else{
                requestStoragePermission()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode==Activity.RESULT_OK){
            if(requestCode == GALLERY){
                try {
                    if(data!!.data != null){
                        bg_image.visibility = View.VISIBLE
                        bg_image.setImageURI(data.data)
                    }
                    else{
                        Toast.makeText(this, "Error parsing image or it's corrupted",
                            Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun ShowBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brushsize_dialog)
        brushDialog.setTitle("Brush size: ")
        val smallButton = brushDialog.small_brush
        smallButton.setOnClickListener {
            drawingview.SetBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumButton = brushDialog.medium_brush
        mediumButton.setOnClickListener {
            drawingview.SetBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        val largeButton = brushDialog.large_brush
        largeButton.setOnClickListener {
            drawingview.SetBrushSize(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }


    fun PaintClicked(view: View){
        if(view != imageButtonCurrentPaint ){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingview.SetBrushColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.palette_selected)
            )
            imageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.palette_normal)
            )
            imageButtonCurrentPaint = view
        }
    }

    //Function to request permission
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this, "Need permission to add Bac", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE )
    }

    //Fumction to work with permission after it has been granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "Oops, you denied permission", Toast.LENGTH_SHORT).show()

        }
    }

    //Function to check if the user already allowed permission in settings
    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }


    //Put all the bacgorund and drawing togerther in one canvas
    private fun getBitmapFromView(view:View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap, var input:String): AsyncTask<Any,Void,String>(){
        private lateinit var progressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if(input.isEmpty()) input = "Draw"+ System.currentTimeMillis()/1000
            if(mBitmap!=null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + input + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result =  f.absolutePath
                }
                catch (e:java.lang.Exception){
                    result =""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if(!result!!.isEmpty()){
                Toast.makeText(this@MainActivity,"File saved successfully as $input",Toast.LENGTH_LONG).show()
            }
            else{
                Toast.makeText(this@MainActivity,"File save unsuccessful",Toast.LENGTH_SHORT).show()
            }

            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                path, uri ->  val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
                shareIntent.type = "image/png"

                startActivity(
                    Intent.createChooser(shareIntent, "Share")
                )
            }
        }

        private fun showProgressDialog(){
            progressDialog = Dialog(this@MainActivity)
            progressDialog.setContentView(R.layout.progressbar_dialogue)
            progressDialog.show()
        }

        private fun cancelProgressDialog(){
            progressDialog.dismiss()
        }
    }

    private fun setFilename(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Input filename")
        val input = EditText(this)         // Set up the input
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Save",
            DialogInterface.OnClickListener { dialog, which ->
                BitmapAsyncTask(getBitmapFromView(drawingview_container),input.text.toString()).execute() })
        builder.setNegativeButton("Cancel",
            DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
        builder.show()
    }

    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}