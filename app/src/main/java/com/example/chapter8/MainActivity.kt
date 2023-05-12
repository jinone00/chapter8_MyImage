package com.example.chapter8

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.chapter8.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val imageLoadLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uriList ->
        updateImages(uriList)
    }


    private lateinit var binding: ActivityMainBinding
    private lateinit var imageAdapter: ImageAdapter

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.apply {
            title = "사진 가져오기"
            setSupportActionBar(this)
        }




        binding.loadImageButton.setOnClickListener {
            checkPermission()
        }
        initRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_add -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermission()
                }
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun initRecyclerView() {
        imageAdapter = ImageAdapter(object : ImageAdapter.ItemClickListener {
            override fun onLoadMoreClick() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkPermission()
                }
            }
        })

        binding.imageRecyclerView.apply {
            adapter = imageAdapter
            layoutManager = GridLayoutManager(context, 2)
        }

        binding.navigateFrameActivityButton.setOnClickListener {
            navigateToFrameActivity()
        }

    }

    private fun navigateToFrameActivity(){
        val images = imageAdapter.currentList.filterIsInstance<ImageItems.Image>().map{it.uri.toString()}.toTypedArray()
        val intent = Intent(this, FrameActivity::class.java)
            .putExtra("images", images)
        startActivity(intent)

    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED -> {
                loadImage()
            }
            shouldShowRequestPermissionRationale(
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) -> {
                showPermissionInfoDialog()
            }
            else -> {
                requestReadExternalStorage()
            }
        }
    }

    private fun loadImage() {
        imageLoadLauncher.launch("image/*")
    }

    companion object {
        const val REQUEST_READ_EXTERNAL_STORAGE = 100
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE -> {
                val resultCode = grantResults.firstOrNull() ?: PackageManager.PERMISSION_DENIED
                if (resultCode == PackageManager.PERMISSION_GRANTED) {
                    loadImage()
                }
            }
        }
    }

    private fun showPermissionInfoDialog() {
        AlertDialog.Builder(this).apply {
            setMessage("이미지를 가져오기 위해서, 외부 저장소 읽기 권한이 필요합니다.")
            setNegativeButton("취소", null)
            setPositiveButton("동의") { _, _ ->
                requestReadExternalStorage()
            }
        }.show()

    }

    private fun requestReadExternalStorage() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
            REQUEST_READ_EXTERNAL_STORAGE
        )
    }

    private fun updateImages(uriList: List<Uri>) {
        val images = uriList.map { ImageItems.Image(it) }
        val updateImages = imageAdapter.currentList.toMutableList().apply {
            addAll(images)
        }
        imageAdapter.submitList(updateImages)
    }

}