package com.infinitysolutions.notessync.Fragments


import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.infinitysolutions.notessync.Adapters.GalleryAdapter
import com.infinitysolutions.notessync.Contracts.Contract.Companion.FILE_PROVIDER_AUTHORITY

import com.infinitysolutions.notessync.R
import com.infinitysolutions.notessync.ViewModel.DatabaseViewModel
import com.infinitysolutions.notessync.ViewModel.MainViewModel
import kotlinx.android.synthetic.main.fragment_image_gallery.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception

class ImageGalleryFragment : Fragment() {
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_image_gallery, container, false)
        val mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        val databaseViewModel = ViewModelProviders.of(activity!!).get(DatabaseViewModel::class.java)

        viewPager = rootView.view_pager
        val galleryAdapter = GalleryAdapter(
            context!!,
            mainViewModel.getImagesList(),
            databaseViewModel
        )
        viewPager.adapter = galleryAdapter

        val toolbar = rootView.toolbar
        toolbar.setNavigationOnClickListener {
            findNavController(this).navigateUp()
        }
        toolbar.inflateMenu(R.menu.gallery_view_menu)
        toolbar.setOnMenuItemClickListener {item->
            when(item.itemId){
                R.id.delete_image_menu_item->{
                    if(galleryAdapter.itemCount > 1) {
                        AlertDialog.Builder(context)
                            .setTitle("Delete")
                            .setMessage("Are you sure you want to delete this image?")
                            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                                galleryAdapter.deleteImage(viewPager.currentItem)
                            }
                            .setNegativeButton("No", null)
                            .setCancelable(true)
                            .show()
                    }else{
                        AlertDialog.Builder(context)
                            .setTitle("Delete")
                            .setMessage("Are you sure you want to delete this image?")
                            .setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                                galleryAdapter.deleteImage(viewPager.currentItem)
                                findNavController(this).navigateUp()
                            }
                            .setNegativeButton("No", null)
                            .setCancelable(true)
                            .show()
                    }
                }
                R.id.share_image_menu_item->{
                    val imageData = galleryAdapter.getItemAtPosition(viewPager.currentItem)
                    Glide.with(context!!).asBitmap().load(imageData.imagePath).into(object: CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            sendBitmap(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
                }
            }
            true
        }

        val position: Int? = arguments?.getInt("currentPosition")
        if (position != null) {
            viewPager.setCurrentItem(position, false)
        }

        return rootView
    }

    private fun sendBitmap(bitmap: Bitmap){
        GlobalScope.launch(Dispatchers.IO){
            val folder = File(activity!!.cacheDir, "images")
            try{
                folder.mkdirs()
                val file = File(folder, "1.png")
                if(file.exists())
                    file.delete()
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                val uri = FileProvider.getUriForFile(context!!, FILE_PROVIDER_AUTHORITY, file)
                withContext(Dispatchers.Main){
                    if(uri != null){
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.type = "image/png"
                        startActivity(Intent.createChooser(intent, "Share"))
                    }
                }
            }catch(e: IOException){
                e.printStackTrace()
            }
        }
    }
}