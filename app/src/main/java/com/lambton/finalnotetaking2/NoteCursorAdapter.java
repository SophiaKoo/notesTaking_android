package com.lambton.finalnotetaking2;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;

/**
 * Created by hyunjukoo on 3/17/16.
 */
public class NoteCursorAdapter extends CursorAdapter {

    //public static final int IMG_WIDTH = 250;
    //public static final int IMG_HEIGHT = 350;
    private ImageView imageView;
    public NoteCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.note_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String noteText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
        String modifedDate = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_MODIFIED));
        String image = cursor.getString(cursor.getColumnIndex(DBOpenHelper.IMAGE));
        String audio = cursor.getString(cursor.getColumnIndex(DBOpenHelper.AUDIO));

        TextView tv = (TextView) view.findViewById(R.id.tvNote);
        TextView tvSub = (TextView) view.findViewById(R.id.tvSub);
        imageView = (ImageView) view.findViewById(R.id.imageDocIcon);

        tv.setText(noteText);
        tvSub.setText(modifedDate);
        if(!audio.equals("")) {
            Drawable micImg = view.getResources().getDrawable(R.drawable.ic_action_mic);
            tvSub.setCompoundDrawablesWithIntrinsicBounds(null, null, micImg, null);
        }
        imageView.setImageBitmap(getImage(image));
    }

    private Bitmap getImage(String image) {
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + EditorActivity.MY_NOTES_APP + "/";

        File img = new File(new StringBuffer(directory).append(image).toString());
        Bitmap bitmap = BitmapFactory.decodeFile(img.getAbsolutePath());
        //    Bitmap bMapScaled = Bitmap.createScaledBitmap(bitmap, IMG_WIDTH, IMG_HEIGHT, true);
            //RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            //lp.setMargins(0,0,20,0);
            //imgView.setLayoutParams(lp);

        //imageView.setImageBitmap(bMapScaled);

        return bitmap;
    }


}
