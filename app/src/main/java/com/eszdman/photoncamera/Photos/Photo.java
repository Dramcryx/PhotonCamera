package com.eszdman.photoncamera.Photos;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;

import com.eszdman.photoncamera.Camera2Api;
import com.eszdman.photoncamera.ui.MainActivity;
import com.eszdman.photoncamera.api.Interface;

import java.io.File;
import java.util.Comparator;

public class Photo {
    public static Photo instance;
    private Handler galleryhandl;
    public Photo(){
        instance = this;
        galleryhandl = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Uri uri = (Uri) msg.obj;
                Camera2Api.context.img.setImageURI(uri);
            }
        };
    }
    Intent imgintent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
    public void ShowPhoto(File imageFile) {

        String mediaId = "";
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME
        };
        Cursor cursor = MainActivity.act.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);

        while (cursor != null && cursor.moveToNext()) {
            String name = cursor.getString((cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)));
            if (name.equals(imageFile.getName())) {
                mediaId = cursor.getString((cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)));
                break;
            }
        }

        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        if (!mediaId.equals("")) {
            mediaUri = mediaUri.buildUpon()
                    .authority("media")
                    .appendPath(mediaId)
                    .build();
        }
        Log.d("TagInfo", "Uri:  " + mediaUri);
        Intent intent = new Intent(Intent.ACTION_VIEW, mediaUri);
        MainActivity.act.startActivity(intent);
    }
    public void ShowPhoto(){
        if(Interface.i.settings.lastPicture != null){
            ShowPhoto(new File(Interface.i.settings.lastPicture));
        }
    }
    static class CompareFilesByDate implements Comparator<File> {
        @Override
        public int compare(File lhs, File rhs) {
            return Long.signum(rhs.lastModified() - lhs.lastModified());
        }
    }
    public void SaveImg(File in){

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            boolean wasnull = Interface.i.settings.lastPicture == null;
            Interface.i.settings.lastPicture = in.getAbsolutePath();
            if(wasnull) Interface.i.settings.save();
            Uri contentUri = Uri.fromFile(in);
            try {
            //CircleImageView button = MainActivity.act.findViewById(R.id.ImageOut);
                //Camera2Api.context.img.setImageURI(contentUri);
            Message urim = new Message();
            urim.obj = contentUri;
            galleryhandl.sendMessage(urim);
            } catch (Exception e){
                e.printStackTrace();
            }
            mediaScanIntent.setData(contentUri);
            MainActivity.act.sendBroadcast(mediaScanIntent);
            File outputDir = MainActivity.act.getCacheDir();
            File outputFile = null;

    }
}