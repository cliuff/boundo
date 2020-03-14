package com.madness.collision.unit.no_media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.madness.collision.R
import com.madness.collision.settings.SettingsFunc
import com.madness.collision.unit.Unit
import com.madness.collision.unit.no_media.data.BasicInfo
import com.madness.collision.unit.no_media.data.Dir
import com.madness.collision.util.PermissionUtil
import com.madness.collision.util.X
import com.madness.collision.util.availableWidth
import kotlinx.android.synthetic.main.unit_no_media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.madness.collision.unit.no_media.R as MyR

class MyUnit : Unit() {

    companion object{
        private const val REQUEST_READ_EXTERNAL = 30
    }

    private val folders: MutableList<Dir> = mutableListOf()
    private val foldersMap: MutableMap<String, List<BasicInfo>> = emptyMap<String, List<BasicInfo>>().toMutableMap()
    private var itemWidth = 0
    private var itemHeight = 0
    private var spanCount = 0

    private lateinit var recyclerView: RecyclerView
    private lateinit var manager: RecyclerView.LayoutManager

    override fun createOptions(context: Context, toolbar: Toolbar, iconColor: Int): Boolean {
        toolbar.setTitle(R.string.tools_nm)
        toolbar.setOnClickListener {
            manager.scrollToPosition(0)
        }
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context ?: return null
        SettingsFunc.updateLanguage(context)
        return inflater.inflate(MyR.layout.unit_no_media, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val context = context ?: return
        val activity = activity ?: return

        democratize()

        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (PermissionUtil.check(context, permissions).isNotEmpty()) {
            if (X.aboveOn(X.M)) {
                requestPermissions(permissions, REQUEST_READ_EXTERNAL)
                return
            } else {
                X.toast(context, R.string.toast_permission_storage_denied, Toast.LENGTH_SHORT)
                return
            }
        }
        init(context, activity)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_READ_EXTERNAL -> {
                if (grantResults.isEmpty()) return
                val context = context ?: return
                when (grantResults[0]) {
                    PackageManager.PERMISSION_GRANTED -> {
                        val activity = activity ?: return
                        init(context, activity)
                    }
                    else -> {
                        nmProgressBar.visibility = View.GONE
                        X.toast(context, R.string.toast_permission_storage_denied, Toast.LENGTH_SHORT)
                    }
                }
            }
        }
    }

    private fun init(context: Context, activity: FragmentActivity){
        GlobalScope.launch(Dispatchers.Main) {
            recyclerView = nmRv
            recyclerView.adapter = withContext(Dispatchers.Default) {
                //todo calculate span count
                spanCount = if (X.aboveOn(X.N) && activity.isInMultiWindowMode) 2 else {
                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val rotation = windowManager.defaultDisplay.rotation
                    val isLand = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
                    if (isLand) 4 else 2
                }
                manager = GridLayoutManager(context, spanCount)
                itemWidth = availableWidth / spanCount
                itemHeight = X.size(context, 240f, X.DP).toInt()

                folders.clear()
                foldersMap.clear()
                getMedia(context, false)
                getMedia(context, true)
                Adapter(context, activity.supportFragmentManager, folders, foldersMap, itemWidth, itemHeight).also {
                    it.spanCount = spanCount
                    it.topCover = mainViewModel.contentWidthTop.value ?: 0
                    it.bottomCover = mainViewModel.contentWidthBottom.value ?: 0
                }
            }
            recyclerView.layoutManager = manager
            nmProgressBar.visibility = View.GONE
        }
    }

    private fun getMedia(context: Context, isVideo: Boolean){
        val cursor: Cursor?
        if (isVideo){
            val projection = arrayOf(
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA,
                    MediaStore.Video.Media._ID
            )
            val contentUri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            cursor = MediaStore.Video.query(context.contentResolver, contentUri, projection)
        }else{
            val projection = arrayOf(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media._ID
            )
            val contentUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            cursor = MediaStore.Images.Media.query(context.contentResolver, contentUri, projection)
        }
        if (cursor == null || !cursor.moveToFirst()) return
        val listInfoFull: MutableList<BasicInfo> = mutableListOf()
        do {
            val itemName: String = cursor.getString(0) ?: ""
            val itemPath: String = cursor.getString(1) ?: ""
            val itemId: Long = cursor.getLong(2)
            val itemDir = itemPath.replace(File.separator + itemName, "")
            listInfoFull.add(BasicInfo(itemDir, itemName, itemPath, itemId, isVideo))
        }while (cursor.moveToNext())
        cursor.close()

        val map: Map<String, List<BasicInfo>> = listInfoFull.groupBy { it.dir }
        map.forEach {
            if (X.aboveOn(X.N)) foldersMap.merge(it.key, it.value){ l1, l2 -> l1 + l2 }
            else foldersMap.putAll(map)
            // todo potential data loss
            folders.add(Dir(context, it.key, it.value, true))
        }
    }
}