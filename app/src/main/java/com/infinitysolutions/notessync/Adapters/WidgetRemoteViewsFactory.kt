package com.infinitysolutions.notessync.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.infinitysolutions.notessync.Contracts.Contract.Companion.LIST_DEFAULT
import com.infinitysolutions.notessync.Contracts.Contract.Companion.NOTE_ID_EXTRA
import com.infinitysolutions.notessync.Model.Note
import com.infinitysolutions.notessync.Model.NotesRoomDatabase
import com.infinitysolutions.notessync.R
import com.infinitysolutions.notessync.Util.ChecklistConverter
import com.infinitysolutions.notessync.Util.ColorsUtil

class WidgetRemoteViewsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {
    private lateinit var notesList: List<Note>
    private val colorsUtil = ColorsUtil()

    override fun getViewAt(position: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_notes_item)
        val noteTitle = notesList[position].noteTitle
        if (noteTitle != null && noteTitle.isNotEmpty()){
            rv.setViewVisibility(R.id.title_text, VISIBLE)
            rv.setTextViewText(R.id.title_text, notesList[position].noteTitle)
        }else
            rv.setViewVisibility(R.id.title_text, GONE)

        var noteContent = notesList[position].noteContent
        if (noteContent != null) {
            if (notesList[position].noteType == LIST_DEFAULT && (noteContent.contains("[ ]") || noteContent.contains("[x]")))
                noteContent = ChecklistConverter.convertList(noteContent)
            rv.setTextViewText(R.id.content_preview_text, noteContent)
        }

        rv.setInt(R.id.list_item_container, "setBackgroundColor", Color.parseColor(colorsUtil.getColor(notesList[position].noteColor)))
        val fillInIntent = Intent()
        fillInIntent.putExtra(NOTE_ID_EXTRA, notesList[position].nId)
        rv.setOnClickFillInIntent(R.id.list_item_container, fillInIntent)
        return rv
    }

    override fun getCount(): Int {
        return notesList.size
    }

    override fun onCreate() {
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onDataSetChanged() {
        val notesDao = NotesRoomDatabase.getDatabase(context).notesDao()
        val identityToken = Binder.clearCallingIdentity()
        notesList = notesDao.getAllPresent()
        Binder.restoreCallingIdentity(identityToken)
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun onDestroy() {}
}