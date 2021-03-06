package robin.stopmotion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.*;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.view.ViewGroup.LayoutParams;
/// import android.R;

public class StopmotionCamera extends Activity implements SurfaceHolder.Callback {

    private static String PREFS_NAME = "StopmotionCameraPreferences";

    private static int ITEMID_PREVIEW = 12;
    private static int ITEMID_PICTURE = 23;
    private static int GROUPID_PREVIEW = 0;
    private static int GROUPID_PICTURE = 1;
    private static int GROUPID_OTHER = 2;

    private String dateFormat = "yyyy-MM-dd-HH";
    private String defaultDateFormat = "yyyy-MM-dd-HH";

    private static String LOGTAG = "StopmotionCameraLog-StopmotionCamera";
    private static String BUTTON_TOGGLE_STRETCH = "Toggle";
    //  private static String CHANGE_OPACITY_INC = "Opac+";
    //  private static String CHANGE_OPACITY_DEC = "Opac-";

    private static String ONION_LEAF_INC = "Skin+";
    private static String ONION_LEAF_DEC = "Skin-";

    private static String CHANGE_DATE_FORMAT = "Settings";
    private static String SHOW_RUSHES = "Preview";

    private int numSkins = 3;

    private boolean takingPicture = false;

    Process process;

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;

    boolean justfocussed = false;

    Bitmap lastPicture = null;
    String[] lastPictureFile = new String[3];
    Canvas canvas;

    File currentDirectory;

    Camera.Size previewSize = null;
    Camera.Size pictureSize = null;

    int previewSizeWhich = -1;
    int pictureSizeWhich = -1;

    Onionskin onionskin;

    boolean stretch = false;

    LayoutInflater controlInflater = null;
    LinearLayout viewControl;

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        process = launchLogcat();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.main_camera_activity);

        getWindow().setFormat(PixelFormat.UNKNOWN);


        Object ob = findViewById(R.id.camerapreview);
        ;
        Log.d(LOGTAG, "fucking fuck bags" + ob);
        surfaceView = (SurfaceView) ob;

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        /// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater = LayoutInflater.from(getBaseContext());
        viewControl = (LinearLayout) (controlInflater.inflate(R.layout.control, null));

        LayoutParams layoutParamsControl
                = new LayoutParams(LayoutParams.MATCH_PARENT,/// FILL_PARENT,
                LayoutParams.MATCH_PARENT);/// FILL_PARENT);

        this.addContentView(viewControl, layoutParamsControl);

        initOnionskin(viewControl, 3);

        Log.d(LOGTAG, "created");
    }

    Button.OnClickListener buttonClickListener =
            new Button.OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    /// TODO Auto-generated method stub
                    Log.d(LOGTAG, "on click listener");

                    if (takingPicture) return;

                    takingPicture = true;

                    if (justfocussed) {
                        justfocussed = false;
                    } else {
                        try {
                            Log.d(LOGTAG, "going to take picture");
                            camera.takePicture(myShutterCallback, myPictureCallback_RAW, myPictureCallback_JPG);
                        } catch (Exception ex) {
                            Log.d(LOGTAG, "failed to take picture!");
                        }

                    }
                    takingPicture = false;
                }
            };

    Camera.ShutterCallback myShutterCallback = new Camera.ShutterCallback() {

        @Override
        public void onShutter() {
            /// TODO Auto-generated method stub
        }
    };
    Camera.PictureCallback myPictureCallback_RAW = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {
            /// TODO Auto-generated method stub
        }
    };
    Camera.PictureCallback myPictureCallback_JPG = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] arg0, Camera arg1) {


            lastPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);

            String x = new SimpleDateFormat(dateFormat).format(new Date());
            currentDirectory = getAlbumStorageDir("Stopmotion-" + x);

            Uri uriTarget = android.net.Uri.fromFile(new File(currentDirectory, String.valueOf((new Date()).getTime()) + ".jpg"));

            OutputStream imageFileOS;
            try {
                imageFileOS = getContentResolver().openOutputStream(uriTarget);
                imageFileOS.write(arg0);
                imageFileOS.flush();
                imageFileOS.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            onionskin.setBmp(lastPicture);

            for (int ll = lastPictureFile.length - 1; ll > 1; ll--) {
                lastPictureFile[ll] = lastPictureFile[ll - 1];
            }

            lastPictureFile[0] = uriTarget.getPath();
            onionskin.updateBackgound();
            camera.startPreview();
            previewing = true;

            Log.d(LOGTAG, "picture " + uriTarget.toString());

        }
    };

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        Log.d(LOGTAG, "onRestoreInstanceState");
        lastPictureFile = bundle.getStringArray("lastPictureFile");

        if (lastPictureFile.length == 0) lastPictureFile = new String[]{"", "", ""};

        //lastPictureFile = bundle.getString("lastPictureFile", "");
        for (String _lastPicture : lastPictureFile) {
            if (_lastPicture != null && !_lastPicture.equals("") && (new File(_lastPicture).exists())) {
                Log.d(LOGTAG, "picture file from settings " + _lastPicture);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                lastPicture = BitmapFactory.decodeFile(_lastPicture, bmOptions);
                onionskin.setBmp(lastPicture);
            }
        }

        stretch = bundle.getBoolean("stretch", false);
        onionskin.setOpacity(bundle.getInt("opacity", 128));
        previewSizeWhich = bundle.getInt("previewSizeWhich", 100);
        pictureSizeWhich = bundle.getInt("pictureSizeWhich", 100);
        dateFormat = bundle.getString("dateFormat", defaultDateFormat);

        numSkins = bundle.getInt("numSkins", 3);

        idPreviewSize("bollocks", previewSizeWhich);
        idPictureSize("bollocks", pictureSizeWhich);

        onionskin.setSkins(numSkins);
        onionskin.setOpacity();
        onionskin.updateBackgound();
        onionskin.invalidate();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.d(LOGTAG, "onSaveInstanceState");

        bundle.putString("dateFormat", dateFormat);
        bundle.putStringArray("lastBmp", lastPictureFile);
        //bundle.putString("lastBmp", lastPictureFile);
        bundle.putInt("opacity", onionskin.getOpacity());
        bundle.putBoolean("stretch", stretch);
        bundle.putInt("previewSizeWhich", previewSizeWhich);
        bundle.putInt("pictureSizeWhich", pictureSizeWhich);
        bundle.putInt("numSkins", numSkins);
        onionskin.invalidate();

    }

    private Process launchLogcat() {

        try {
            File filename = new File(Environment.getExternalStorageDirectory() + "/stopmotion-logfile.log");
            filename.createNewFile();
            String cmd = "logcat -d -f " + filename.getAbsolutePath();
            return Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return null;
        }
    }

    private void initOnionskin(LinearLayout viewControl, int skins) {

        viewControl.removeView(onionskin);

        onionskin = new Onionskin(this, skins);

        onionskin.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        viewControl.addView(onionskin);

        onionskin.setOnClickListener(buttonClickListener);

        onionskin.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        justfocussed = true;
                        Toast.makeText(StopmotionCamera.this, "focus", Toast.LENGTH_LONG).show();
                    }
                });
                return false;
            }
        });

        onionskin.setOpacity();
        onionskin.updateBackgound();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
            previewing = false;
        }

        save();

        onionskin.updateBackgound();
        onionskin.invalidate();
        Log.d(LOGTAG, "paused");

    }

    private void save() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        SharedPreferences.Editor editor = settings.edit();

        Set<String> set = new HashSet(Arrays.asList(lastPictureFile));

        editor.putStringSet("lastBmp", set);

        //      editor.putString("lastBmp", lastPictureFile);
        editor.putBoolean("stretch", stretch);
        editor.putInt("opacity", onionskin.getOpacity());
        editor.putInt("previewSizeWhich", previewSizeWhich);
        editor.putInt("pictureSizeWhich", pictureSizeWhich);
        editor.putString("dateFormat", dateFormat);
        editor.putInt("numSkins", numSkins);
        // Commit the edits!
        editor.commit();
        Log.d(LOGTAG, "committed");

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");

        load();
        onionskin.updateBackgound();
        onionskin.invalidate();
    }

    private void load() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        stretch = settings.getBoolean("stretch", false);
        onionskin.setOpacity(settings.getInt("opacity", 128));
        previewSizeWhich = settings.getInt("previewSizeWhich", 100);
        pictureSizeWhich = settings.getInt("pictureSizeWhich", 100);
        numSkins = settings.getInt("numSkins", 3);

        onionskin.setSkins(numSkins);

        dateFormat = settings.getString("dateFormat", defaultDateFormat);

        idPreviewSize("bollocks", previewSizeWhich);
        idPictureSize("bollocks", pictureSizeWhich);

        Log.d(LOGTAG, "about to get load of pictures");
        Object[] o = settings.getStringSet("lastPictureFile", new HashSet()).toArray();

        lastPictureFile = new String[o.length];

        for (int oo = 0; oo < o.length; oo++) {
            lastPictureFile[oo] = (String) o[oo];
            Log.d(LOGTAG, ">>>" + lastPictureFile[oo]);
        }

        if (lastPictureFile.length == 0) lastPictureFile = new String[]{"", "", ""};

        //lastPictureFile=settings.getStringArray("lastPictureFile");
        //lastPictureFile = bundle.getString("lastPictureFile", "");
        for (String _lastPicture : lastPictureFile) {
            if (_lastPicture != null && !_lastPicture.equals("") && (new File(_lastPicture).exists())) {
                Log.d(LOGTAG, "picture file from settings " + _lastPicture);
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                lastPicture = BitmapFactory.decodeFile(_lastPicture, bmOptions);
                onionskin.setBmp(lastPicture);
            }
        }









/*

        lastPictureFile = settings.getString("lastBmp", "");
        if (!lastPictureFile.equals("") && (new File(lastPictureFile).exists())) {
            Log.d(LOGTAG, "picture file from settings " + lastPictureFile);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            lastPicture = BitmapFactory.decodeFile(lastPictureFile, bmOptions);

            onionskin.setBmp(lastPicture);
        }
*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (menu.findItem(ITEMID_PREVIEW) == null || menu.findItem(ITEMID_PICTURE) == null)
            return createMenu(menu);
        else return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /// public boolean onGroupItemClick(MenuItem item) {

        if (camera != null) {

            if (previewing) camera.stopPreview();
        }

        boolean success = false;

        /// Handle item selection
        if (item.getGroupId() == GROUPID_PREVIEW) {
            /// preview
            success = idPreviewSize(item.getTitle().toString(), -1);

        } else if (item.getGroupId() == GROUPID_PICTURE) {

            /// pict

            success = idPictureSize(item.getTitle().toString(), -1);

        } else if (item.getGroupId() == GROUPID_OTHER) {
            if (item.getTitle().equals(BUTTON_TOGGLE_STRETCH)) {

                setStretch(!stretch);

                //    } else if (item.getTitle().equals(CHANGE_OPACITY_DEC)) {
                //        onionskin.decreaseOpacity();

                //     } else if (item.getTitle().equals(CHANGE_OPACITY_INC)) {
                //       onionskin.increaseOpacity();

            } else if (item.getTitle().equals(ONION_LEAF_INC)) {
                numSkins++;
                onionskin.setSkins(numSkins);

            } else if (item.getTitle().equals(ONION_LEAF_DEC)) {
                if (numSkins > 1) {
                    numSkins--;
                    onionskin.setSkins(numSkins);
                }

            } else if (item.getTitle().equals(SHOW_RUSHES)) {

                onionskin.setActivated(false);

                String x = new SimpleDateFormat(dateFormat).format(new Date());
                currentDirectory = getAlbumStorageDir("Stopmotion-" + x);

                (new Dialog(this) {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {

                        super.onCreate(savedInstanceState);

                        requestWindowFeature(Window.FEATURE_NO_TITLE);
                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);

                        setContentView(R.layout.stopmotion_rushes_panel);

                    }

                    protected void onStart() {

                        final SquashedPreview squashedPreview = (SquashedPreview) findViewById(R.id.view);

                        SeekBar seekBar = (SeekBar) findViewById(R.id.previewSeekBar);
                        squashedPreview.setSeekbar(seekBar);

                        squashedPreview.setDirectory(currentDirectory);

                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                squashedPreview.setImageNumber(progress);
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        });

                    }
                }).show();

                onionskin.setActivated(true);

            } else if (item.getTitle().equals(CHANGE_DATE_FORMAT)) {
                // showEditDialog();

                onionskin.setActivated(false);

                (new Dialog(this) {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.stopmotion_settings_panel);

                        getWindow().setLayout(600, 400);

                        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
                        seekBar.setMax(255);
                        seekBar.setProgress(onionskin.getOpacity());

                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                Log.d(LOGTAG, "progress " + progress);
                                onionskin.setOpacity(progress);
                                onionskin.updateBackgound();
                                onionskin.invalidate();
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }
                        });

                        final EditText editText = (EditText) findViewById(R.id.editDate);
                        editText.setClickable(true);
                        editText.setEnabled(true);
                        editText.setText(dateFormat);

                        final Button button = (Button) findViewById(R.id.button);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dateFormat = editText.getText().toString();

                            }
                        });

                        final Button defbutton = (Button) findViewById(R.id.defaultDateButton);
                        defbutton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                editText.setText(defaultDateFormat);
                            }
                        });

                    }
                }).show();
                onionskin.setActivated(true);
            }

            save();
        }

        if (camera != null) {
            if (previewing) camera.startPreview();
        }
        onionskin.invalidate();
        return success;
    }

    public void setStretch(boolean stretch) {

        this.stretch = stretch;
        if (previewSize != null) setSize(previewSize.width, previewSize.height);
        Log.d(LOGTAG, "setStretch to " + this.stretch);
        onionskin.invalidate();

    }

    private boolean idPreviewSize(String thing, int idnum) {

        Log.d(LOGTAG, "idPreviewSize");

        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        boolean success = false;
        int enumc = 0;

        for (Camera.Size size : previewSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (thing.startsWith(text) || enumc == idnum) {
                Camera.Parameters params = camera.getParameters();
                params.setPreviewSize(size.width, size.height);
                camera.setParameters(params);
                success = true;
                previewSize = size;
                setSize(size.width, size.height);
                previewSizeWhich = enumc;
                save();
            }
            enumc++;
        }
        return success;
    }

    private boolean idPictureSize(String startswith, int idnum) {

        Log.d(LOGTAG, "idPictureSize");
        boolean success = false;
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

        int enumc = 0;

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height);
            if (startswith.startsWith(text) || enumc == idnum) {
                Camera.Parameters params = camera.getParameters();
                params.setPictureSize(size.width, size.height);
                pictureSize = size;
                camera.setParameters(params);
                success = true;
                pictureSizeWhich = enumc;
                save();
            }
            enumc++;
        }
        return success;

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null) {
            try {
                //   if (previewSize == null) previewSize = camera.getParameters().getPreviewSize();
                //  if (pictureSize == null) pictureSize = camera.getParameters().getPictureSize();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                /// TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    public boolean createMenu(Menu menu) {

        menu.clear();

        List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
        List<Camera.Size> pictureSizes = camera.getParameters().getSupportedPictureSizes();

        int order = 0;

        menu.add(2, Menu.NONE, order++, BUTTON_TOGGLE_STRETCH);
        //    menu.add(2, Menu.NONE, order++, CHANGE_OPACITY_DEC);
        //   menu.add(2, Menu.NONE, order++, CHANGE_OPACITY_INC);
        menu.add(2, Menu.NONE, order++, ONION_LEAF_DEC);
        menu.add(2, Menu.NONE, order++, ONION_LEAF_INC);
        menu.add(2, Menu.NONE, order++, CHANGE_DATE_FORMAT);
        menu.add(2, Menu.NONE, order++, SHOW_RUSHES);

        SubMenu sm1 = menu.addSubMenu(GROUPID_PREVIEW, ITEMID_PREVIEW, order++, "Preview Size");

        for (Camera.Size size : previewSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height) + " | " + String.format("%.3f", (float) size.width / size.height);
            MenuItem mi = sm1.add(GROUPID_PREVIEW, Menu.NONE, order++, text);

        }

        SubMenu sm2 = menu.addSubMenu(GROUPID_PREVIEW, ITEMID_PICTURE, order++, "Picture Size");

        for (Camera.Size size : pictureSizes) {
            String text = String.valueOf(size.width) + "x" + String.valueOf(size.height) + " | " + String.format("%.3f", (float) size.width / size.height);
            ;
            MenuItem mi = sm2.add(GROUPID_PICTURE, Menu.NONE, order++, text);
        }

        Log.d(LOGTAG, "created Menu for first time");

        return true;
    }

    public void setSize(int width, int height) {

        float asp = (float) width / height;

        int measuredHeight = surfaceView.getMeasuredHeight();
        int measuredWidth = surfaceView.getMeasuredWidth();

        float dev_asp = (float) measuredWidth / measuredHeight;

        if (stretch || width > measuredWidth || height > measuredHeight) {

            if (asp > dev_asp) {
                /// wider, set width to device, change height
                width = measuredWidth;
                height = (int) ((float) measuredHeight / asp);

            } else if (asp < dev_asp) {
                /// narrower, set height to device, change width
                height = measuredHeight;
                width = (int) (asp * measuredHeight);
            } else {
                height = measuredHeight;
                width = measuredWidth;
            }

        } else {
            if (width > measuredWidth) {
                width = measuredWidth;
                height = (int) ((float) measuredHeight / asp);
            } else if (height > measuredHeight) {
                width = (int) (asp * measuredHeight);
                height = measuredHeight;

            }
        }

        int l = (measuredWidth - width) / 2;
        int t = (measuredHeight - height) / 2;

        surfaceView.layout(l, t, l + width, t + height);
        surfaceView.invalidate();

        onionskin.layout(l, t, l + width, t + height);
        onionskin.updateBackgound();
        onionskin.invalidate();

        Log.d(LOGTAG, "setSize " + width + " " + height);

    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.d(LOGTAG, "couldn't create " + albumName);
        }

        Log.d(LOGTAG, "getAlbumStorageDir " + file.toString());

        return file;
    }

    @Override
    public void onStart() {
        super.onStart();
        camera = Camera.open();
        Log.d(LOGTAG, "START");
        new CountDownTimer(2000, 200) {
            @Override
            public void onFinish() {
                Log.d(LOGTAG, "set stretch with timebombtick");
                setStretch(stretch);
            }

            @Override
            public void onTick(long millisUntilFinished) {
                Log.d(LOGTAG, "tick");
            }
        }.start();
    }
}

