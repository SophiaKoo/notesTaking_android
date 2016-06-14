package com.lambton.finalnotetaking2;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditorActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1002;
    private static final int REQUEST_IMAGE_VIEW = 1003;
    private static final int REQUEST_ERROR_DIALOG = 9001;

    public static final String IMG_NAME = "ic_action_add.png";
    private static final float ZOOM_FACTOR = 15;
    private static final String LOG_TAG = "EditorActivity";
    public static final String MY_NOTES_APP = "MyNotesApp";
    public static final int IMG_WIDTH = 350;
    public static final int IMG_HEIGHT = 500;

    private String action;
    private EditText editor;
    private String noteFilter;
    private String oldText;
    private ProgressBar pbar;
    //private LinearLayout imgGallery;
    private ImageView imgView;

    private GoogleMap mMap;
    private GoogleApiClient mLocationClient;
    private LocationListener mListener;
    private Location currentLocation;
    private double savedLongitude;
    private double savedLatitude;
    private Uri uri;
    private Marker marker;

    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String mAudioFileName;

    private RecordButton mRecordButton;
    private PlayButton mPlayButton;
    private LinearLayout audio;
    private SeekBar mSeekBar;

    private Handler mHandler = new Handler();
    private String saveAudioName = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        editor = (EditText) findViewById(R.id.editText);
        pbar = (ProgressBar) findViewById(R.id.progessBar);
        //imgGallery = (LinearLayout) findViewById(R.id.imgGallery);
        imgView = (ImageView) findViewById(R.id.imgView);
        audio = (LinearLayout)findViewById(R.id.audio);

        Intent intent = this.getIntent();
        uri = intent.getParcelableExtra(NotesProvider.CONTENT_ITEM_TYPE);

        if (uri == null) {
            action = Intent.ACTION_INSERT;
            setTitle("New Note");
        } else {
            action = Intent.ACTION_EDIT;
            noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();
            Cursor cursor = getContentResolver().query(uri, DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);
            cursor.moveToFirst();
            oldText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
            editor.setText(oldText);
            editor.requestFocus();
            savedLatitude = cursor.getDouble(cursor.getColumnIndex(DBOpenHelper.LATITUDE));
            savedLongitude = cursor.getDouble(cursor.getColumnIndex(DBOpenHelper.LONGITUDE));

            String image = cursor.getString(cursor.getColumnIndex(DBOpenHelper.IMAGE));
            if(!image.equals("")){
                showImage(image);
            }
            String audio = cursor.getString(cursor.getColumnIndex(DBOpenHelper.AUDIO));
            if(!audio.equals("")){
                showAudio(audio);
            }
        }

        if (serviceOK()) {
            if (initMap()) {
                if (uri == null) {
                    mLocationClient = new GoogleApiClient.Builder(this)
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();

                    mLocationClient.connect();

                    //showCurrentLocation();
                } else {
                    //gotoLocation(savedLatitude, savedLongitude, ZOOM_FACTOR);
                    new AsyncLoadMap(savedLatitude, savedLongitude).execute();

                    //showMarker(new LatLng(savedLatitude, savedLongitude));

                }
//                if(uri == null){
//                    showCurrentLocation();
//                }
            }else {
                Toast.makeText(this, "Map Not Connected", Toast.LENGTH_LONG).show();
            }
        }

        EditorActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null) {
                    int mCurrentPosition = mPlayer.getCurrentPosition() / 1000;
                    mSeekBar.setProgress(mCurrentPosition);
                }
                mHandler.postDelayed(this, 1000);
            }
        });

    }

    private void showAudio(String audioFile) {
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + MY_NOTES_APP + "/";

        mAudioFileName = new StringBuffer(directory).append(audioFile).toString();

        mPlayButton = new PlayButton(this);
        audio.addView(mPlayButton);

    }

    private void showImages(String images) {
        String directory  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/"+MY_NOTES_APP +"/";
        String[] imgList = images.split(";");
        for(int i = 0; i < imgList.length; i++){
            String imgName = imgList[i];

            File img = new File(new StringBuffer(directory).append(imgName).toString());
            final Bitmap bitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
            imgView = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 10, 0);
            imgView.setLayoutParams(lp);
            imgView.setImageBitmap(bitmap);
            //imgGallery.addView(imgView);
        }
    }

    private void showImage(String image) {
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + MY_NOTES_APP + "/";

        File img = new File(new StringBuffer(directory).append(image).toString());
        Bitmap bitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
        Bitmap bMapScaled = Bitmap.createScaledBitmap(bitmap, IMG_WIDTH, IMG_HEIGHT, true);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,20,0);
        imgView.setLayoutParams(lp);
        imgView.setImageBitmap(bMapScaled);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //if(action.equals(Intent.ACTION_EDIT)){
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        //}
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finishEditing();
                break;
            case R.id.action_delete:
                deleteNote();
                break;
            case R.id.action_take_pic:
                takePic();
                break;
            case R.id.action_record_audio:
                recordAudio();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void recordAudio() {
        if(mRecordButton != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        audio.removeView(mPlayButton);
                        audio.removeView(mSeekBar);
                        mRecordButton = new RecordButton(EditorActivity.this);
                        audio.addView(mRecordButton);
                    }
                }
            };
            builder.setMessage(R.string.are_you_sure_audio)
                    .setPositiveButton(getString(android.R.string.yes), dialogClickListener)
                    .setNegativeButton(getString(android.R.string.no), dialogClickListener)
                    .show();
        }else {
            mRecordButton = new RecordButton(this);
            audio.addView(mRecordButton);
        }
    }

    private void takePic() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void deleteNote() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    getContentResolver().delete(NotesProvider.CONTENT_URI, noteFilter, null);
                    Toast.makeText(EditorActivity.this, R.string.string_note_deleted, Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
        };
        builder.setMessage(R.string.are_you_sure)
                .setPositiveButton(getString(android.R.string.yes), dialogClickListener)
                .setNegativeButton(getString(android.R.string.no), dialogClickListener)
                .show();
    }

    private void finishEditing() {
        String newText = editor.getText().toString().trim();

        switch (action) {
            case Intent.ACTION_INSERT:
                if (newText.length() == 0) {
                    setResult(RESULT_CANCELED);
                } else {
                    insertNote(newText);
                }
                break;
            case Intent.ACTION_EDIT:
                if (newText.length() == 0) {
                    deleteNote();
                } else if (oldText.equals(newText)) {
                    setResult(RESULT_CANCELED);
                } else {
                    updateNote(newText);
                }
                finish();
        }

    }

    private void updateNote(String newText) {
        String image = saveImage();
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, newText);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        values.put(DBOpenHelper.NOTE_MODIFIED, df.format(new Date()));
        values.put(DBOpenHelper.IMAGE, image);
        values.put(DBOpenHelper.AUDIO, saveAudioName);
        getContentResolver().update(NotesProvider.CONTENT_URI, values, noteFilter, null);
        Toast.makeText(EditorActivity.this, R.string.string_note_updated, Toast.LENGTH_LONG).show();
        setResult(RESULT_OK);
    }

    private void insertNote(String newText) {
        //String images = saveImages();
        String image = saveImage();
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, newText);
        values.put(DBOpenHelper.LATITUDE, currentLocation.getLatitude());
        values.put(DBOpenHelper.LONGITUDE, currentLocation.getLongitude());
        //values.put(DBOpenHelper.IMAGE, images);
        values.put(DBOpenHelper.IMAGE, image);
        values.put(DBOpenHelper.AUDIO, saveAudioName);
        getContentResolver().insert(NotesProvider.CONTENT_URI, values);
        setResult(RESULT_OK);
    }

    private String saveImages(){
        String rtnImgNames = "";
        File pictureStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), MY_NOTES_APP);
        if(!pictureStorageDir.exists()){
            if(!pictureStorageDir.mkdirs()){
                Log.d(LOG_TAG, "Failed to create directory");
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        for(int i = 0; i<imgGallery.getChildCount(); i++){
//            ImageView imgView = (ImageView) imgGallery.getChildAt(i);
//            //Bitmap bitmap = imgView.getDrawingCache();
//            Bitmap bitmap = ((BitmapDrawable)imgView.getDrawable()).getBitmap();
//            String imagName = "IMG_"+timeStamp + "_" + i+ ".png";
//
//            File pictureFile = new File(pictureStorageDir.getPath() + File.separator + imagName);
//
//            try{
//                FileOutputStream fos = new FileOutputStream(pictureFile);
//                //fos.write(bitmap);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
//                fos.flush();
//                fos.close();
//
//                if(rtnImgNames.equals("")){
//                    rtnImgNames = new StringBuilder(rtnImgNames).append(imagName).toString();
//                } else {
//                    rtnImgNames = new StringBuilder(rtnImgNames).append(";").append(imagName).toString();
//                }
//            } catch (FileNotFoundException e) {
//                Log.d(LOG_TAG, "File not found : " + e.getMessage());
//            } catch(IOException e){
//                Log.d(LOG_TAG, "Error accessing file : " + e.getMessage());
//            }
//        }
        return rtnImgNames;
    }

    private String saveImage(){
        String rtnImgNames = "";

        BitmapDrawable drawable = (BitmapDrawable) imgView.getDrawable();
        if(drawable != null){
            Bitmap bitmap = drawable.getBitmap();
            File pictureStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), MY_NOTES_APP);
            if(!pictureStorageDir.exists()){
                if(!pictureStorageDir.mkdirs()){
                    Log.d(LOG_TAG, "Failed to create directory");
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

            String imagName = "IMG_" + timeStamp + ".png";

            File pictureFile = new File(pictureStorageDir.getPath() + File.separator + imagName);

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                //fos.write(bitmap);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();

                rtnImgNames = imagName;

            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "File not found : " + e.getMessage());
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error accessing file : " + e.getMessage());
            }
        }

        return rtnImgNames;
    }

    @Override
    public void onBackPressed() {
        finishEditing();
    }

//    public void setImage(String imageResource) {
//        int position = Selection.getSelectionStart(editor.getText());
//
//        Spanned e = Html.fromHtml("<img src=\"" + imageResource + "\">",
//                imageGetter, null);
//
//        editor.getText().insert(position, e);
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("hyunju", editor.getSelectionStart() + " " + editor.getSelectionEnd());
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Bitmap bMapScaled = Bitmap.createScaledBitmap(imageBitmap, IMG_WIDTH, IMG_HEIGHT, true);
            //ImageView imgView = new ImageView(this);

//            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//            lp.setMargins(0, 0, 10, 0);
//            imgView.setLayoutParams(lp);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,20,0);
            imgView.setLayoutParams(lp);
            imgView.setImageBitmap(bMapScaled);
            //imgView.setImageBitmap(bMapScaled);
            //imgGallery.addView(imgView);


            //Bitmap bMapScaled = Bitmap.createScaledBitmap(imageBitmap, 350, 300, true);
//            Html.ImageGetter imageGetter = new Html.ImageGetter() {
//                @Override
//                public Drawable getDrawable(String source) {
//
//                    //Drawable d= getResources().getDrawable(R.id.action_delete);
//                    //int width = getWindowManager().getDefaultDisplay().getWidth() - 30;
//                    //Matrix matrix = new Matrix();
//                    //matrix.setRotate(90);
//                    //Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap , 0, 0, imageBitmap .getWidth(), imageBitmap .getHeight(), matrix, true);
//                    Bitmap bMapScaled = Bitmap.createScaledBitmap(imageBitmap, 750, 500, true);
//                    Drawable d = new BitmapDrawable(bMapScaled);
//                    int width = bMapScaled.getWidth();
//                    int height = bMapScaled.getHeight();
//                    d.setBounds(0, 0, width, height);
//                    return d;
//                }
//            };
//            Spanned cs = Html.fromHtml("<img src=" + IMG_NAME + "/>", imageGetter, null);
//            int start = editor.getSelectionStart();
//            int end = editor.getSelectionEnd();
//            editor.getText().replace(Math.min(start, end), Math.max(start, end), cs, 0, 1);

            //ImageView img = (ImageView) findViewById(R.id.imgView);
            //img.setImageBitmap(imageBitmap);
            //Drawable drawable = new BitmapDrawable(imageBitmap);
            //drawable.setBounds(0, 0, 500,500);
//            SpannableStringBuilder builder = new SpannableStringBuilder(editor.getText());
//            ImageSpan imgSpan = new ImageSpan(this, bMapScaled, ImageSpan.ALIGN_BASELINE);
//            builder.setSpan(imgSpan, editor.getSelectionStart(), editor.getSelectionEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//            editor.setText(builder);

        }
    }

    public boolean serviceOK() {
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        Log.d(LOG_TAG, "isAvailable = "+isAvailable);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(isAvailable, this, REQUEST_ERROR_DIALOG);
            dialog.show();
        } else {
            Toast.makeText(this, getString(R.string.cannot_connect_map), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private boolean initMap() {
        if (mMap == null) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mMap = mapFragment.getMap();
        }
        return (mMap != null);
    }

    private void gotoLocation(double lat, double lng, float zoomFactor) {
        LatLng latLng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, zoomFactor);
        mMap.animateCamera(update);
    }

    private void showCurrentLocation() {
        try{
            Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
            if(currentLocation == null) {
                Toast.makeText(this, getString(R.string.cannot_connect), Toast.LENGTH_LONG).show();
            } else {
                LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_FACTOR);

                mMap.animateCamera(update);
            }
        }catch (SecurityException e){
            Log.d(LOG_TAG, e.getLocalizedMessage());
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, getString(R.string.ready_to_map), Toast.LENGTH_LONG).show();

        if(uri == null) {
            mListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    currentLocation = location;
                    new AsyncLoadMap(location.getLatitude(), location.getLongitude()).execute();
                    //gotoLocation(location.getLatitude(), location.getLongitude(), ZOOM_FACTOR);
                    //showMarker(new LatLng(location.getLatitude(), location.getLongitude()));

                    try {
                        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
                    } catch (SecurityException e) {
                        Log.d(LOG_TAG, e.getLocalizedMessage());
                    }

                }
            };

            LocationRequest request = LocationRequest.create();
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            request.setInterval(5000);
            request.setFastestInterval(1000);
            request.setNumUpdates(1);
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, request, mListener);
            } catch (SecurityException e) {
                Log.d(LOG_TAG, e.getLocalizedMessage());
            }


        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.failed_to_map, Toast.LENGTH_LONG).show();
    }

    private class AsyncLoadMap extends AsyncTask<Integer, Integer, CameraUpdate> {
        private double mLatitude;
        private double mLongitude;

        public AsyncLoadMap(double latitude, double longitude) {
            mLatitude = latitude;
            mLongitude = longitude;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pbar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected CameraUpdate doInBackground(Integer... params) {
            publishProgress(50);
            try {
                Thread.sleep(2000);
                publishProgress(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            LatLng latLng = new LatLng(mLatitude, mLongitude);
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_FACTOR);

            //gotoLocation(mLatitude, mLongitude, ZOOM_FACTOR);

            return update;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pbar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(CameraUpdate update) {
            //super.onPostExecute(integer);
            mMap.animateCamera(update);
            showMarker(new LatLng(mLatitude, mLongitude));

            pbar.setVisibility(ProgressBar.INVISIBLE);
        }
    }

    private void showMarker(LatLng position) {
        if (marker != null) {
            marker.remove();
        }
        MarkerOptions options = new MarkerOptions()
                .position(position);
        marker = mMap.addMarker(options);
        //pbar.setVisibility(ProgressBar.INVISIBLE);
    }

    class RecordButton extends Button {

        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecord(mStartRecording);
                if(mStartRecording){
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context context) {
            super(context);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    private void onRecord(boolean start) {
        if(start){
            startRecording();
        }else {
            stopRecording();
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        audio.removeView(mRecordButton);
        mPlayButton = new PlayButton(this);
        audio.addView(mPlayButton);

    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        File storageDir = new File(Environment.getExternalStorageDirectory(), MY_NOTES_APP);
        if(!storageDir.exists()){
            if(!storageDir.mkdirs()){
                Log.d(LOG_TAG, "Failed to create directory");
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        saveAudioName = "AUDIO_" + timeStamp + ".3gp";

        mAudioFileName =  storageDir.getPath() + File.separator + saveAudioName;

        mRecorder.setOutputFile(mAudioFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Audio eecorder prepare() failed");
        }

        mRecorder.start();
    }

    class PlayButton extends Button {
        boolean mStartPlaying = true;

        OnClickListener clicker = new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if(mStartPlaying){
                    setText("Stop Playing");
                    if(mSeekBar == null) {
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        mSeekBar = new SeekBar(EditorActivity.this);
                        mSeekBar.setLayoutParams(params);
                        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if(mPlayer != null){
                                    mPlayer.seekTo(progress * 1000);
                                    mSeekBar.setProgress(progress * 1000);
                                }
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        });
                        audio.addView(mSeekBar);
                    }

                }else {
                    setText("Start Playing");
                }
                mStartPlaying = !mStartPlaying;
            }
        };

        public PlayButton(Context ctx) {
            super(ctx);
            setText("Start playing");
            setOnClickListener(clicker);
        }
    }

    private void onPlay(boolean start) {
        if(start) {
            startPlaying();
        }else {
            stopPlaying();
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mAudioFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Audio playing prepare() failed");
        }

    }


}



