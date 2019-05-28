package com.example.filtercamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.material.snackbar.Snackbar;
import com.zomato.photofilters.SampleFilters;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PhotoViewActivity extends AppCompatActivity implements ThumbnailCallback, SeekBar.OnSeekBarChangeListener {

    static {
        System.loadLibrary("NativeImageProcessor");
    }

    private Activity activity;
    private RecyclerView thumbListView;
    private ImageView placeHolderImageView;
    ImageView backButtonImageView;
    Bitmap thumbImage;
    Bitmap filterBitmap;
    Button saveButton;
    Button loadButton;
    Button filterButton;
    Button editButton;
    int height;
    int width;
    ProgressBar progressBar;
    RelativeLayout relativeLayout;
    boolean filterApplied = false;
    String galleryPath;
    SeekBar seekBarBrightness;
    SeekBar seekBarContrast;
    SeekBar seekBarSaturation;
    Filter f ;
    Filter myFilter;
    LinearLayout buttonLayout;
    LinearLayout seekBarLayout;
    Boolean enterFirstTime = true;
    Boolean bitmapValuesChanged = false;
    String filterName="none";
    Bitmap filteredBitmap;
    Bitmap finalBitmap;
    int brightnessFinal = 0;
    float saturationFinal = 1.0f;
    float contrastFinal = 1.0f;
    List<String> appliedFilterName;


    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    public void resetValues(){
         brightnessFinal = 0;
         saturationFinal = 1.0f;
         contrastFinal = 1.0f;

        seekBarBrightness.setProgress(100);
        seekBarContrast.setProgress(0);
        seekBarSaturation.setProgress(10);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_photo_view);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {

                            if (enterFirstTime){
                                hideSystemUI();
                                enterFirstTime=false;
                            }
                            else {
                                Log.d("hidingnavigation","false");
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideSystemUI();
                                    }
                                }, 3000);
                            }

                            // TODO: The system bars are visible. Make any desired
                            // adjustments to your UI, such as showing the action bar or
                            // other navigational controls.
                        } else {
                            Log.d("hidingnavigation","true");

                            // TODO: The system bars are NOT visible. Make any desired
                            // adjustments to your UI, such as hiding the action bar or
                            // other navigational controls.
                        }
                    }
                });

        /*DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;*/

        f= new Filter();

        seekBarBrightness= findViewById(R.id.seekbar_brightness);
        seekBarBrightness.setMax(200);
        seekBarBrightness.setProgress(100);
        seekBarBrightness.setOnSeekBarChangeListener(this);

        seekBarContrast= findViewById(R.id.seekbar_contrast);
        seekBarContrast.setMax(20);
        seekBarContrast.setProgress(0);
        seekBarContrast.setOnSeekBarChangeListener(this);

        seekBarSaturation= findViewById(R.id.seekbar_saturation);
        seekBarSaturation.setMax(30);
        seekBarSaturation.setProgress(10);
        seekBarSaturation.setOnSeekBarChangeListener(this);



        myFilter = new Filter();
        appliedFilterName = new ArrayList<>();
        activity=this;
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);
        filterButton = findViewById(R.id.filterButton);
        editButton = findViewById(R.id.editButton);
        progressBar=findViewById(R.id.progressBar2);
        relativeLayout = findViewById(R.id.relativelayout);
        buttonLayout = findViewById(R.id.buttonLayout);
        seekBarLayout = findViewById(R.id.seekBarLayout);
        backButtonImageView = findViewById(R.id.backButton);
        int h = height-width;
        Log.d("buttonheight",saveButton.getHeight()+","+height+","+width+","+loadButton.getHeight()+","+h);
        RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,h);
        parms.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.setLayoutParams(parms);

        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        buttonLayout.setLayoutParams(buttonParams);



        String imagePath = getIntent().getStringExtra("imagePath");
        loadImageFromStorage(imagePath);
        Log.d("timetaken","ishere");
        thumbListView = findViewById(R.id.thumbnails);
        placeHolderImageView = findViewById(R.id.place_holder_imageview);
        //placeHolderImageView.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getApplicationContext().getResources(), R.drawable.photo), 640, 640, false));

        placeHolderImageView.setImageBitmap(thumbImage);
        initHorizontalList();

        backButtonImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thumbListView.setVisibility(View.VISIBLE);
                seekBarLayout.setVisibility(View.INVISIBLE);
                filterButton.setTextColor(getResources().getColor(R.color.ButtonEnabled));
                editButton.setTextColor(getResources().getColor(R.color.ButtonDisabled));
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thumbImage!=null){
                    thumbListView.setVisibility(View.INVISIBLE);
                    seekBarLayout.setVisibility(View.VISIBLE);
                    filterButton.setTextColor(getResources().getColor(R.color.ButtonDisabled));
                    editButton.setTextColor(getResources().getColor(R.color.ButtonEnabled));
                }
                else {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            thumbListView.setVisibility(View.INVISIBLE);
                            seekBarLayout.setVisibility(View.VISIBLE);
                            filterButton.setTextColor(getResources().getColor(R.color.ButtonDisabled));
                            editButton.setTextColor(getResources().getColor(R.color.ButtonEnabled));
                        }
                    }, 1000);
                }

            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // progressBar.setVisibility(View.VISIBLE);

                saveButton.setVisibility(View.VISIBLE);
                loadButton.setVisibility(View.INVISIBLE);

             /*   final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                        saveButton.setVisibility(View.VISIBLE);
                        loadButton.setVisibility(View.INVISIBLE);
                    }
                }, 1000);*/
            }
        });


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File ffile = new File(Environment.getExternalStorageDirectory()+"/"+ UUID.randomUUID().toString() +".jpg");
                OutputStream fOut = null;
                try {
                    fOut = new FileOutputStream(ffile);


                /*if (filterApplied)
                    filterBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                else
                    thumbImage.compress(Bitmap.CompressFormat.JPEG, 100, fOut);*/
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush(); // Not really required
                fOut.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                MediaScannerConnection.scanFile(getApplicationContext(), new String[]{ffile.toString()}, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.d("filesavedhere","in "+uri);
                                galleryPath=uri+"";
                                Snackbar.make(findViewById(android.R.id.content), "Open Gallery", Snackbar.LENGTH_LONG)
                                        .setAction("View", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                //   Toast.makeText(mContext, "snackbar", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(Intent.ACTION_PICK,
                                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                                intent.setDataAndType(Uri.parse(galleryPath),"image/*");
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);
                                            }
                                        }).show();
                                //closeCamera();
                                // galleryPath=uri+"";
                                //pathUri = uri;
                            }
                        });
            }
        });
    }

    private void loadImageFromStorage(String path)
    {

        try {
            File f=new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            finalBitmap=b;
            thumbImage = b;
            filterBitmap=b;
            filteredBitmap=b;
            //ImageView img=(ImageView)findViewById(R.id.imgPicker);
            //img.setImageBitmap(b);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    private void initHorizontalList() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
//        layoutManager.scrollToPosition(0);


        thumbListView.setLayoutManager(layoutManager);
        //thumbListView.setHasFixedSize(true);
        bindDataToAdapter();
    }

    private void bindDataToAdapter() {
        final Context context = this.getApplication();
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
               // Bitmap thumbImage = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.photo), 640, 640, false);
                ThumbnailItem t1 = new ThumbnailItem();
                ThumbnailItem t2 = new ThumbnailItem();
                ThumbnailItem t3 = new ThumbnailItem();
                ThumbnailItem t4 = new ThumbnailItem();
                ThumbnailItem t5 = new ThumbnailItem();
                ThumbnailItem t6 = new ThumbnailItem();
                ThumbnailItem t7 = new ThumbnailItem();
                ThumbnailItem t8 = new ThumbnailItem();
                ThumbnailItem t9 = new ThumbnailItem();
                ThumbnailItem t10 = new ThumbnailItem();

                t1.image = thumbImage;
                t2.image = thumbImage;
                t3.image = thumbImage;
                t4.image = thumbImage;
                t5.image = thumbImage;
                t6.image = thumbImage;
                t7.image = thumbImage;
                t8.image = thumbImage;
                t9.image = thumbImage;
                t10.image = thumbImage;
                ThumbnailsManager.clearThumbs();
                ThumbnailsManager.addThumb(t1); // Original Image
                appliedFilterName.add("Normal");



                t2.filter = SampleFilters.getStarLitFilter();
                ThumbnailsManager.addThumb(t2);
                appliedFilterName.add("Starlit");

                t3.filter = SampleFilters.getBlueMessFilter();
                ThumbnailsManager.addThumb(t3);
                appliedFilterName.add("Bluemess");

                t4.filter = SampleFilters.getAweStruckVibeFilter();
                ThumbnailsManager.addThumb(t4);
                appliedFilterName.add("Struck");

                t5.filter = SampleFilters.getLimeStutterFilter();
                ThumbnailsManager.addThumb(t5);
                appliedFilterName.add("Lime");

                t6.filter = SampleFilters.getNightWhisperFilter();
                ThumbnailsManager.addThumb(t6);
                appliedFilterName.add("Whisper");

                t7.filter = SampleFilter.Clarendon();
                ThumbnailsManager.addThumb(t7);
                appliedFilterName.add("Clarendon");

                t8.filter = SampleFilter.Sunset();
                ThumbnailsManager.addThumb(t8);
                appliedFilterName.add("Sunset");

                t9.filter = SampleFilter.Hefe();
                ThumbnailsManager.addThumb(t9);
                appliedFilterName.add("Hefe");

                t10.filter = SampleFilter.xpro();
                ThumbnailsManager.addThumb(t10);
                appliedFilterName.add("Sierra");



                List<ThumbnailItem> thumbs = ThumbnailsManager.processThumbs(context);

                ThumbnailsAdapter adapter = new ThumbnailsAdapter(appliedFilterName ,thumbs, (ThumbnailCallback) activity);
                thumbListView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        };
        handler.post(r);
    }


    @Override
    public void onThumbnailClick(Filter filter) {
        //placeHolderImageView.setImageBitmap(filter.processFilter(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getApplicationContext().getResources(), R.drawable.photo), 640, 640, false)));
        //placeHolderImageView.setImageBitmap(filter.processFilter(Bitmap.createScaledBitmap(thumbImage,640,640,false)));

        resetValues();
            filterBitmap= filter.processFilter(Bitmap.createScaledBitmap(thumbImage,width,width,false));

        placeHolderImageView.setImageBitmap(filterBitmap);
        seekBarBrightness.setProgress(100);
        filteredBitmap=filterBitmap;
        finalBitmap=filterBitmap;
        filterApplied=true;
        if (filterBitmap!=null){
            saveButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        Filter fFilter ;//= new Filter();
        int h = width;
        int w = thumbImage.getWidth();
        float one = 1.0f;
        //Bitmap outputBitmap = thumbImage;
       // Log.d("imagebitmap",bitmapValuesChanged+"is ,"+outputBitmap);
        if (seekBar.getId() == R.id.seekbar_brightness) {
            Log.d("seekbarval",""+progress);
            // brightness values are b/w -100 to +100

            brightnessFinal=progress-100;






            /* fFilter = new Filter();
            fFilter.addSubFilter(new BrightnessSubFilter(0));
            filteredBitmap=fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));
            brightnessFinal=progress-100;
            fFilter.addSubFilter(new BrightnessSubFilter(progress-100));
            fFilter.addSubFilter(new ContrastSubFilter(contrastFinal));
            fFilter.addSubFilter(new SaturationSubFilter(saturationFinal));
            Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));

          // Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(inputBitmap,width,h,false));
           placeHolderImageView.setImageBitmap(outputBitmap);
           finalBitmap=outputBitmap;
*/
            filterName = "brightness";


        }

        if (seekBar.getId() == R.id.seekbar_contrast){
            progress += 10;
            float floatValC = .10f * progress;


            contrastFinal=floatValC;



            /*fFilter = new Filter();
            fFilter.addSubFilter(new ContrastSubFilter(one));
            filteredBitmap=fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));
            contrastFinal=floatValC;
            fFilter.addSubFilter(new BrightnessSubFilter(brightnessFinal));
            fFilter.addSubFilter(new ContrastSubFilter(floatValC));
            fFilter.addSubFilter(new SaturationSubFilter(saturationFinal));
            Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));

            //Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(inputBitmap,width,width,false));
            placeHolderImageView.setImageBitmap(outputBitmap);
            finalBitmap=outputBitmap;*/

            filterName = "contrast";
        }

        if (seekBar.getId() == R.id.seekbar_saturation){
            float floatVal = .10f * progress;
            Log.d("floatval",""+floatVal);

            saturationFinal=floatVal;


           /*
            fFilter = new Filter();
            fFilter.addSubFilter(new SaturationSubFilter(1f));
            filteredBitmap=fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));
            saturationFinal=floatVal;
            fFilter.addSubFilter(new BrightnessSubFilter(brightnessFinal));
            fFilter.addSubFilter(new ContrastSubFilter(contrastFinal));
            fFilter.addSubFilter(new SaturationSubFilter(floatVal));
            Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));

           // Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(inputBitmap,width,width,false));
            placeHolderImageView.setImageBitmap(outputBitmap);
            finalBitmap=outputBitmap;*/

            filterName = "saturation";
        }
        bitmapValuesChanged=true;
        filterApplied=true;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        addFilter(brightnessFinal,contrastFinal,saturationFinal);
    }

    public void addFilter(int brightness,float contrast,float saturation){
        Filter fFilter = new Filter();
        int h = width;

        fFilter.addSubFilter(new BrightnessSubFilter(0));
        //filteredBitmap=fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));
        fFilter.addSubFilter(new ContrastSubFilter(1.0f));
        //filteredBitmap=fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));
        fFilter.addSubFilter(new SaturationSubFilter(1.0f));
        filteredBitmap=fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));

        fFilter.addSubFilter(new BrightnessSubFilter(brightness));
        fFilter.addSubFilter(new ContrastSubFilter(contrast));
        fFilter.addSubFilter(new SaturationSubFilter(saturation));
        Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(filteredBitmap,width,h,false));

        // Bitmap outputBitmap = fFilter.processFilter(Bitmap.createScaledBitmap(inputBitmap,width,width,false));
        placeHolderImageView.setImageBitmap(outputBitmap);
        finalBitmap=outputBitmap;
    }

public void setBrightness(Bitmap inputImage, int value){
    Filter myFilter = new Filter();
    myFilter.addSubFilter(new BrightnessSubFilter(value));
    myFilter.addSubFilter(new ContrastSubFilter(1.1f));
    filterBitmap = myFilter.processFilter(filterBitmap);
}



}
