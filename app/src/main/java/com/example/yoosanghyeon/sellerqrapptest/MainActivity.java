package com.example.yoosanghyeon.sellerqrapptest;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.BitmapCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Gson gson = new Gson();
        String jsonStr = gson.toJson(new Product("prodecutName", "3", "3,000"));
        Log.e(MainActivity.class.getName(), jsonStr);


        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.determinateBar);

        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

        final ImageView imageView = (ImageView) findViewById(R.id.qrimage);

        try {

            final Bitmap bitmap = barcodeEncoder.encodeBitmap(jsonStr, BarcodeFormat.QR_CODE, 900, 900);
            imageView.setImageBitmap(bitmap);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(MainActivity.this, "Click", Toast.LENGTH_LONG).show();
                    try {

                        File cachePath = new File(getCacheDir(), "images");
                        cachePath.mkdirs(); // don't forget to make the directory
                        FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        stream.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    File imagePath = new File(getCacheDir(), "images");
                    File newFile = new File(imagePath, "image.png");
                    Uri contentUri = FileProvider.getUriForFile(MainActivity.this, "com.example.yoosanghyeon.sellerqrapptest.fileprovider", newFile);

                    if (contentUri != null) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                        shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        startActivity(Intent.createChooser(shareIntent, "Choose an app"));
                    }
                }
            });

            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    new ImageDownLoader(Toast.makeText(MainActivity.this, "다운로드 완료", Toast.LENGTH_LONG), progressBar).execute(bitmap);
                    return true;
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }



    }




    private void shareBitmap(Bitmap bitmap, String fileName) {
        try {
            File file = new File(getCacheDir(), fileName + ".png");
            FileOutputStream fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            file.setReadable(true, true);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, "qrcode send"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class ImageDownLoader extends AsyncTask<Bitmap ,Integer, File>{
        Toast toast;
        ProgressBar progressBar;


        public ImageDownLoader(Toast toast, ProgressBar progressBar) {
            this.toast = toast;
            this.progressBar = progressBar;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
           progressBar.setProgress(values[0]);
        }

        @Override
        protected File doInBackground(Bitmap... bitmaps) {

            return getLocalBitmapUri(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(File file) {
            if (file != null){
                toast.show();
                Log.e(ImageDownLoader.class.getName() , "file nonull");
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }
            super.onPostExecute(file);
        }

        public File getLocalBitmapUri(Bitmap bitmap) {
            // Extract Bitmap from ImageView drawable
            Bitmap bmp = bitmap;
            // Store image to default external storage directory
            Uri bmpUri = null;
            File file = null;
            try {
                // Use methods on Context to access package-specific directories on external storage.
                // This way, you don't need to request external read/write permission.
                // See https://youtu.be/5xVh-7ywKpE?t=25m25s
                file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "share_image_" + System.currentTimeMillis() + ".png");
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                // **Warning:** This will fail for API >= 24, use a FileProvider as shown below instead.
                bmpUri = Uri.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return file;
        }

    }


}
