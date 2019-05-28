package com.example.filtercamera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.yarolegovich.discretescrollview.DSVOrientation;
import com.yarolegovich.discretescrollview.DiscreteScrollView;
import com.yarolegovich.discretescrollview.InfiniteScrollAdapter;
import com.yarolegovich.discretescrollview.transform.ScaleTransformer;
import com.zomato.photofilters.imageprocessors.Filter;
import com.zomato.photofilters.imageprocessors.subfilters.ColorOverlaySubFilter;
import com.zomato.photofilters.imageprocessors.subfilters.VignetteSubFilter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_AQUA;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_MONO;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE;
import static android.hardware.camera2.CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE;

public class MainActivity extends AppCompatActivity implements DiscreteScrollView.OnItemChangedListener,
        View.OnClickListener {
    static
    {
        System.loadLibrary("NativeImageProcessor");
    }
    private static final String TAG = "AndroidCameraApi";
    private ImageView captureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    private Size mPreviewSize;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    int val=1;
    Filter myFilter;
    int width=0;
    int height=0;
    Intent intent;
    Bitmap outputBitmap=null;
    Bitmap filterBitmap;
    RelativeLayout linearLayout;
    DiscreteScrollView scrollView;
    ArrayList<Integer> poseImageNames;
    ProgressBar progressBar;

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback(){

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d("Cameraopened", "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCurrentItemChanged(@Nullable RecyclerView.ViewHolder viewHolder, int i) {
        //int positionInDataSet = infiniteAdapter.getRealPosition(i);
        //onItemChanged(data.get(positionInDataSet));
    }

    private static class CompareSizeByArea implements Comparator<Size>{

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth()*lhs.getHeight()/
                    (long)rhs.getWidth()*rhs.getHeight());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
           /* final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideSystemUI();
                }
            }, 3000);*/
        }
    }


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

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
      //  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            Log.d("hidingnavigation","false");
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    hideSystemUI();
                                }
                            }, 3000);
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


        poseImageNames= new ArrayList<>();
        poseImageNames.add(R.drawable.image1);
        poseImageNames.add(R.drawable.image2);
        poseImageNames.add(R.drawable.image3);
        poseImageNames.add(R.drawable.image4);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int usableHeight = displayMetrics.heightPixels;
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        int realHeight = displayMetrics.heightPixels;
        int softKeyHeight=0;
        Log.d("widthandheight",realHeight+"  ,"+usableHeight);
        if (realHeight > usableHeight)
            softKeyHeight= realHeight - usableHeight;

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);
        linearLayout = findViewById(R.id.linearLayout);
        intent = new Intent(MainActivity.this,PhotoViewActivity.class);
        myFilter = new Filter();
       // myFilter.addSubFilter(new ColorOverlaySubFilter(100, .2f, .2f, .0f));
        myFilter.addSubFilter(new VignetteSubFilter(getApplicationContext(),100));
        captureButton = findViewById(R.id.CaptureButton);
        textureView = findViewById(R.id.textureView);
        assert textureView != null;
        scrollView = findViewById(R.id.picker);
        scrollView.setOrientation(DSVOrientation.HORIZONTAL);
        scrollView.addOnItemChangedListener(this);


       // RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(width,width);
        Log.d("widthandheight",height-width+softKeyHeight+"  ,"+softKeyHeight);
        RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(width,(height-width)+softKeyHeight);
        parms.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        linearLayout.setLayoutParams(parms);
        int s=(height-width)+softKeyHeight-80;
       /* int s=(height-width)+softKeyHeight-80;
        RelativeLayout.LayoutParams prms = new RelativeLayout.LayoutParams(s,s);
        poseCard.setLayoutParams(prms);*/

        int size =(int) this.getResources().getDimension(R.dimen.pose_Image_sizes);
        int length = (height-width)-size;
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
       // params.addRule();
        //scrollView.setLayoutParams(params);

        PoseImageAdapter poseImageAdapter = new PoseImageAdapter( s,poseImageNames, this);
        scrollView.setAdapter(poseImageAdapter);
        scrollView.setItemTransitionTimeMillis(185);
        scrollView.setItemTransformer(new ScaleTransformer.Builder()
                .setMinScale(0.8f)
                .build());

        scrollView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            }
        });

        //setAspectratioTextureView(imageDimension.getHeight(),imageDimension.getWidth());

        //openCamera();

        //RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(960,960);
        //textureView.setLayoutParams(parms);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.d("sizeoftextureview","w="+width+" ,h="+height);
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        textureView.setSurfaceTextureListener(textureListener);
        assert captureButton!=null;
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
                progressBar.setVisibility(View.VISIBLE);
                Intent i = new Intent(MainActivity.this,PhotoViewActivity.class);
               // startActivity(i);

               /* final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        filterBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        Intent i = new Intent(MainActivity.this,PhotoViewActivity.class);
                        i.putExtra("image",byteArray);
                        startActivity(i);
                    }
                }, 1000);
*/



               /* ByteArrayOutputStream stream = new ByteArrayOutputStream();
                filterBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                intent.putExtra("image",byteArray);
                startActivity(intent);*/



            }
        });



        /*textureView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.d("fileresapplied","is here");
                switch (val){

                    case 1:captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, 1);
                        Log.d("fileresapplied","is done");
                        val++;

                        createCameraPreview();
                        break;
                    case 2:captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, 2);
                        Log.d("fileresapplied","is done one");
                        val++;
                        createCameraPreview();
                        break;
                    case 3:captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE,CONTROL_EFFECT_MODE_AQUA);
                        Log.d("fileresapplied","is done two");
                        val++;
                        createCameraPreview();
                        break;
                }
            }
        });*/
    }

    private void setAspectratioTextureView(int ResolutionWidth, int ResolutionHeight) {
        if (ResolutionWidth>ResolutionHeight){
            int newWidth = width;
            int newHeight=((width*ResolutionWidth)/ResolutionHeight);
            UpdateTextureViewSize(newWidth,newHeight);
        }
        else {
            int newWidth = width;
            int newHeight=((width*ResolutionHeight)/ResolutionWidth);
            UpdateTextureViewSize(newWidth,newHeight);
        }
    }

    private void UpdateTextureViewSize(int newWidth, int newHeight) {
        Log.d("sizeoftextureviewssss","w="+newWidth+" ,h="+newHeight);
        textureView.setLayoutParams(new FrameLayout.LayoutParams(newWidth,newHeight));
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener(){

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d("sizeoftextureview","w="+width+" ,h="+height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void takePicture() {
        if(null == cameraDevice) {
            Log.d("OpenCamera", "cameraDevice is null");
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

          //  StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                //jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.getBitsPerPixel(width));
            }

            /*int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }*/

            int w = 640;
            int h = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                w = jpegSizes[0].getWidth();
                h = jpegSizes[0].getHeight();
            }

            Log.d("imagesizes","width:"+width+",height:"+height);
            ImageReader reader = ImageReader.newInstance(w, h, ImageFormat.JPEG, 1);
            Log.d("imagesizesss","width:"+reader.getWidth()+",height:"+reader.getHeight());
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            //textureView.setAspectRatio(width,height,true);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            //final File file = new File(Environment.getExternalStorageDirectory()+"/"+ UUID.randomUUID().toString() +".jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;

                    try {
                        image = reader.acquireLatestImage();



                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        //save(bytes);
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        Log.d("imagesizeandcrop",image.getHeight()+" ,"+image.getWidth()+","+bitmap.getHeight()+" ,"+bitmap.getWidth());
                        if (bitmap.getWidth() >= bitmap.getHeight()){
                            outputBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getHeight(), bitmap.getHeight());
                            //outputBitmap = Bitmap.createBitmap(bitmap, (bitmap.getWidth()/2)-(bitmap.getHeight()/2), 0, bitmap.getHeight(), bitmap.getHeight());
                        }else{
                            //outputBitmap = Bitmap.createBitmap(bitmap, 0, (bitmap.getHeight()/2)-(bitmap.getWidth()/2), bitmap.getWidth(), bitmap.getWidth());
                            outputBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getWidth());
                        }
                        Log.d("timetaken","thismuchstarted");
                        String path = saveToInternalStorage(outputBitmap);
                        Log.d("timetaken","thismuch");
                        intent.putExtra("imagePath",path);
                        startActivity(intent);
                        filterBitmap=outputBitmap;
                        /*ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        filterBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        Intent i = new Intent(MainActivity.this,PhotoViewActivity.class);
                        i.putExtra("image",byteArray);
                        startActivity(i);*/
                        //Bitmap outputBitmap = Bitmap.createBitmap(bitmap, 0, (image.getHeight()/2)-(image.getWidth()/2), image.getWidth(), image.getWidth());
                        // Bitmap outbitmap = myFilter.processFilter(bitmap);
                     /*   MediaScannerConnection.scanFile(getApplicationContext(), new String[]{file.toString()}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        // galleryPath=uri+"";
                                        //pathUri = uri;
                                    }
                                });*/
                      /*  File ffile = new File(Environment.getExternalStorageDirectory()+"/"+ UUID.randomUUID().toString() +".jpg");
                        OutputStream fOut = new FileOutputStream(ffile);
                      //  filterBitmap=outputBitmap;
                        outputBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
                        fOut.flush(); // Not really required
                        fOut.close();

                        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{ffile.toString()}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                            //closeCamera();
                                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                        outputBitmap.compress(Bitmap.CompressFormat.JPEG,30,stream);
                                        byte[] byteArray = stream.toByteArray();
                                        intent.putExtra("image",byteArray);
                                            Log.d("itsfinalyher", path+"  ,filenotfound, "+uri);
                                            startActivity(intent);
                                        // galleryPath=uri+"";
                                        //pathUri = uri;
                                    }
                                });*/
                        // countFaceNum.FaceCount(bitmap,getApplicationContext());
                    } /*catch (FileNotFoundException e) {
                        Log.d("Imageerror", "filenotfound, "+e.getMessage());
                        e.printStackTrace();
                    } catch (IOException e) {
                        Log.d("Imageerror", "IOexception, "+e.getMessage());
                        e.printStackTrace();
                    }*/
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.d("failedtotakepic","dsaf, "+e.getMessage());
            e.printStackTrace();
        }
    }

    private String saveToInternalStorage(Bitmap bitmapImage){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }


    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            //texture.setDefaultBufferSize(width, width);
           // texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //captureRequestBuilder.set(CaptureRequest.);
         //   captureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE,val);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
           // mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),width,width);
            Log.d("mPreviewSize",mPreviewSize+"   ,are there");

            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            setAspectratioTextureView(imageDimension.getHeight(),imageDimension.getWidth());

            //RelativeLayout.LayoutParams parms = new RelativeLayout.LayoutParams(mPreviewSize.getWidth(),mPreviewSize.getHeight());
          //  FrameLayout.LayoutParams parms = new FrameLayout.LayoutParams(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            //textureView.setLayoutParams(parms);
            Log.d("imageDimension",imageDimension+"");
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        progressBar.setVisibility(View.INVISIBLE);
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        closeCamera();
        super.onPause();
    }

    private static Size chooseOptimalSize(Size[] choices,int w,int h){
        List<Size> bigEnough = new ArrayList<Size>();
        for (Size option: choices){
            if (option.getHeight()==option.getWidth()*h/w && option.getWidth()>=w && option.getHeight()>h){
                bigEnough.add(option);
            }
        }
        Log.d("availablesizesss",bigEnough.size()+","+bigEnough);
        if (bigEnough.size()>0){
            Log.d("availablesizesd",bigEnough+"");
            return Collections.min(bigEnough,new CompareSizeByArea());
        }
        else
            return choices[0];
    }
}
