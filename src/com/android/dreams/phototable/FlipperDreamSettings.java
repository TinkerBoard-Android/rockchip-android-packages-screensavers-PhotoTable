/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.dreams.phototable;

import android.Manifest;
import android.app.ListActivity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.LinkedList;

/**
 * Settings panel for photo flipping dream.
 */
public class FlipperDreamSettings extends ListActivity {
    @SuppressWarnings("unused")
    private static final String TAG = "FlipperDreamSettings";
    public static final String PREFS_NAME = FlipperDream.TAG;

    protected SharedPreferences mSettings;

    private PhotoSourcePlexor mPhotoSource;
    private SectionedAlbumDataAdapter mAdapter;
    private MenuItem mSelectAll;
    private AsyncTask<Void, Void, Void> mLoadingTask;

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        int checkPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (PERMISSION_REQUEST_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //init();//will do onresume, not need init again
            } else {
                String toast_text = getResources().getString(R.string.err_permission);
                Toast.makeText(this, toast_text, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        int checkPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        if (checkPermission == PackageManager.PERMISSION_GRANTED) {
            init();
        }
    }

    protected void init() {
        mPhotoSource = new PhotoSourcePlexor(this, mSettings);
        setContentView(R.layout.settingslist);
        if (mLoadingTask != null && mLoadingTask.getStatus() != Status.FINISHED) {
            mLoadingTask.cancel(true);
        }
        showApology(false);
        mLoadingTask = new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... unused) {
                mAdapter = new SectionedAlbumDataAdapter(FlipperDreamSettings.this,
                        mSettings,
                        R.layout.header,
                        R.layout.album,
                        new LinkedList<PhotoSource.AlbumData>(mPhotoSource.findAlbums()));
                return null;
            }

           @Override
           public void onPostExecute(Void unused) {
               mAdapter.registerDataSetObserver(new DataSetObserver () {
                       @Override
                       public void onChanged() {
                           updateActionItem();
                       }
                       @Override
                       public void onInvalidated() {
                           updateActionItem();
                       }
                   });
               setListAdapter(mAdapter);
               getListView().setItemsCanFocus(true);
               updateActionItem();
               showApology(mAdapter.getCount() == 0);
           }
        };
        mLoadingTask.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.photodream_settings_menu, menu);
        mSelectAll = menu.findItem(R.id.photodream_menu_all);
        updateActionItem();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.photodream_menu_all:
            if (mAdapter != null) {
                mAdapter.selectAll(!mAdapter.areAllSelected());
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void showApology(boolean apologize) {
        View empty = findViewById(R.id.spinner);
        View sorry = findViewById(R.id.sorry);
        if (empty != null && sorry != null) {
            empty.setVisibility(apologize ? View.GONE : View.VISIBLE);
            sorry.setVisibility(apologize ? View.VISIBLE : View.GONE);
        }
    }

    private void updateActionItem() {
        if (mAdapter != null && mSelectAll != null) {
            if (mAdapter.areAllSelected()) {
                mSelectAll.setTitle(R.string.photodream_select_none);
            } else {
                mSelectAll.setTitle(R.string.photodream_select_all);
            }
        }
    }
}
