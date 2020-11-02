package fl.wearable.autosport;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import fl.wearable.autosport.lib.FileItemAdapter;
import fl.wearable.autosport.lib.IDataListener;
import fl.wearable.autosport.lib.ISensorReadout;
import fl.wearable.autosport.lib.MeterView;
import fl.wearable.autosport.lib.SensorCollector;
import fl.wearable.autosport.sensors.HeartRateSensorData;
import fl.wearable.autosport.sync.AsyncSaver;
import fl.wearable.autosport.sync.DataLayerListenerService;
import fl.wearable.autosport.menu.CenterActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static android.content.ContentValues.TAG;

/**
 * Main activity of the wearable app.
 */
public class MainActivity extends FragmentActivity
        implements IDataListener<HeartRateSensorData>,
            DataClient.OnDataChangedListener,
            MessageClient.OnMessageReceivedListener,
            CapabilityClient.OnCapabilityChangedListener{
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final long REFRESH_INTERVAL_MS = 200;
    private static final String PREFERENCES_HEART_SENSOR = "heart_sensor";
    private static final String PREFERENCES_GEO_SENSOR = "geo_sensor";
    private static final String PREFERENCES_ACCELERATE_SENSOR = "accelerator_sensor";
    private static final String PREFERENCES_GYROSCOPE_SENSOR = "gyroscope_sensor";
    private static final String PREFERENCES_GEO_ALWAYS_ON = "geo_always_on";
    private static final String PREFERENCES_DISPLAY_ALWAYS_ON = "display_always_on";
    private Integer inferenceResult;
    private long fileName;

    // for transmissions
    private static final String CAPABILITY_1_NAME = "capability_1";
    private static final String CAPABILITY_2_NAME = "capability_2";
    private static final String COUNT_PATH = "/count";
    private static final String FILE_PATH = "/sync";
    private static final String DATA_KEY = "datafile";
    private static final String COUNT_KEY = "count";
    private java.util.concurrent.ScheduledExecutorService mGeneratorExecutor;
    private java.util.concurrent.ScheduledFuture<?> mDataItemGeneratorFuture;

    private final Collection<HeartRateSensorData> heartRateData = new ArrayList<>();
    private Intent sensorCollectorIntent;
    private Intent menuActivityIntent;
    private ViewPager mPager;
    private PagerAdapter pagerAdapter;
    private View mMainView;
    private ToggleButton mStartStopButton;
    private Button sendButton;
    private TextView mBigDisplayText;
    private MeterView mMeterView;
    private XYGraphView xyGraphView;
    private XYGraphView.XYData mHeartRateGraph;
    private View mSettingsView;
    private Switch mSwitchHeartRate, mSwitchGeoLocation, mSwitchGeoAlwaysOn, mSwitchAccelerator, mSwitchGyroscope, mSwitchDisplayOn;
    private View mFilesView;
    private View mCapacityView;
    private RecyclerView mFileList;
    private FileItemAdapter mFileItemAdapter;
    private ImageView mGeoAvailabilityImageView;
    private ISensorReadout mSensorReadout;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected " + name.getShortClassName());
            SensorCollector.LocalBinder binder = (SensorCollector.LocalBinder) service;
            mSensorReadout = binder.getService();
            mSensorReadout.registerDataListener(new HeartRateSensorData[0], MainActivity.this);
            MainActivity.this.mStartStopButton.setChecked(mSensorReadout.isSportActivityRunning());
            MainActivity.this.mStartStopButton.setEnabled(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected " + name.getShortClassName());
            mSensorReadout = null;
            MainActivity.this.mStartStopButton.setEnabled(false);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "Binding died to the service  " + name.getShortClassName());
            mSensorReadout = null;
            MainActivity.this.mStartStopButton.setEnabled(false);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.e(TAG, "Null binding from the service " + name.getShortClassName());
            MainActivity.this.mStartStopButton.setEnabled(false);
        }
    };
    private Handler mDataForwarderHandler;
    private final Runnable mDataForwarderTask = new Runnable() {
        @Override
        public void run() {
            if (MainActivity.this.mSensorReadout != null) {
                synchronized (heartRateData) {
                    for (HeartRateSensorData sensorData : heartRateData) {
                        mHeartRateGraph.put(sensorData.getHeartRate());
                    }

                    heartRateData.clear();
                }

                float positionAccuracy = mSensorReadout.getGeoAccuracy();
                int numSatellites = mSensorReadout.getBestSatellitesCount();

                int blendColor;
                if (Float.isNaN(positionAccuracy)) {
                    if (numSatellites == 0) {
                        blendColor = Color.DKGRAY;
                    } else if (numSatellites == 1) {
                        blendColor = Color.RED;
                    } else if (numSatellites == 2) {
                        blendColor = Color.MAGENTA;
                    } else {
                        blendColor = Color.YELLOW;
                    }
                } else {
                    blendColor = Color.WHITE;
                }

                mGeoAvailabilityImageView.setColorFilter(blendColor, PorterDuff.Mode.MULTIPLY);

                float minutes = (mSensorReadout.getSportActivityDurationNs() / 1000L / 1000L / 1000L) / 60f;
                int minutesInt = (int) minutes;
                int secondsInt = (int) ((minutes - minutesInt) * 60);
                mBigDisplayText.setText(String.format("%02d:%02d", minutesInt, secondsInt));

                mMeterView.setValue(mHeartRateGraph.getLastValue() / 200f);
            }
            MainActivity.this.mDataForwarderHandler.postDelayed(mDataForwarderTask, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate " + savedInstanceState);
        setContentView(R.layout.activity_screen_slide);

        sensorCollectorIntent = new Intent(this, SensorCollector.class);
        menuActivityIntent = new Intent(this, CenterActivity.class);

        mMainView = getLayoutInflater().inflate(R.layout.main, null);
        mStartStopButton = mMainView.findViewById(R.id.startStopButton);
        mBigDisplayText = mMainView.findViewById(R.id.bigDisplayText);
        //sendButton =  mMainView.findViewById(R.id.send);
        mMeterView = mMainView.findViewById(R.id.meterView);

        mSettingsView = getLayoutInflater().inflate(R.layout.settings_page, null);
        mSwitchHeartRate = mSettingsView.findViewById(R.id.switchHeartRate);
        mSwitchGeoLocation = mSettingsView.findViewById(R.id.switchGeoLocation);
        mSwitchGeoAlwaysOn = mSettingsView.findViewById(R.id.switchGeoAlwaysOn);
        mSwitchAccelerator = mSettingsView.findViewById(R.id.switchAccelerator);
        mSwitchGyroscope = mSettingsView.findViewById(R.id.switchGyroscope);
        mSwitchDisplayOn = mSettingsView.findViewById(R.id.switchDisplayOn);

        mCapacityView = getLayoutInflater().inflate(R.layout.capability,null);

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        mSwitchHeartRate.setChecked(preferences.getBoolean(PREFERENCES_HEART_SENSOR, true));
        mSwitchGeoLocation.setChecked(preferences.getBoolean(PREFERENCES_GEO_SENSOR, true));
        // todo: still buggy, not to be presented
        // mSwitchGeoAlwaysOn.setChecked(preferences.getBoolean(PREFERENCES_GEO_ALWAYS_ON, false));
        mSwitchAccelerator.setChecked(preferences.getBoolean(PREFERENCES_ACCELERATE_SENSOR, true));
        mSwitchGyroscope.setChecked(preferences.getBoolean(PREFERENCES_GYROSCOPE_SENSOR, true));

        // todo: rethink this feature
        // mSwitchDisplayOn.setChecked(preferences.getBoolean(PREFERENCES_DISPLAY_ALWAYS_ON, false));

        mSwitchHeartRate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCES_HEART_SENSOR, mSwitchHeartRate.isChecked());
                editor.apply();
            }
        });

        mSwitchGeoLocation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCES_GEO_SENSOR, mSwitchGeoLocation.isChecked());
                editor.apply();
            }
        });

        mSwitchAccelerator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCES_ACCELERATE_SENSOR, mSwitchAccelerator.isChecked());
                editor.apply();
            }
        });

        mSwitchGyroscope.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCES_GYROSCOPE_SENSOR, mSwitchGyroscope.isChecked());
                editor.apply();
            }
        });

        mFilesView = getLayoutInflater().inflate(R.layout.files_list, null);
        mFileList = mFilesView.findViewById(R.id.items);
        mFileItemAdapter = new FileItemAdapter();
        updateFileList();
        mFileList.setAdapter(mFileItemAdapter);
        mFileList.setLayoutManager(new LinearLayoutManager(this));

        mSwitchDisplayOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mSwitchDisplayOn.isChecked()) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCES_DISPLAY_ALWAYS_ON, mSwitchDisplayOn.isChecked());
                editor.apply();
            }
        });

        mSwitchGeoAlwaysOn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mSensorReadout != null) {
                    mSensorReadout.setGeoLocationAlwaysActive(mSwitchGeoAlwaysOn.isChecked());
                }

                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREFERENCES_GEO_ALWAYS_ON, mSwitchGeoAlwaysOn.isChecked());
                editor.apply();
            }
        });

        mGeoAvailabilityImageView = mMainView.findViewById(R.id.geoAvailabilityImageView);
        //mGeoAccuracyTextView = mMainView.findViewById(R.id.geoAccuracyTextView);
        //mSpeedTextView = mMainView.findViewById(R.id.speedTextView);
        // TODO: TBU
        xyGraphView = mMainView.findViewById(R.id.XYGraphView2);
        mHeartRateGraph = this.xyGraphView.addDataSet(Color.WHITE, 200f);
        mHeartRateGraph.addMarker(60, Color.GREEN);
        mHeartRateGraph.addMarker(150, Color.RED);

        mGeoAvailabilityImageView.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);

        mPager = findViewById(R.id.pager);
        pagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), mMainView, mCapacityView, mFilesView, mSettingsView);
        mPager.setAdapter(pagerAdapter);

        // for transmissions
        mGeneratorExecutor = new java.util.concurrent.ScheduledThreadPoolExecutor(1);

        mDataForwarderHandler = new Handler();

    }

    public void onSend(View view) {
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog))
                //.setTitle(R.string.send)
                .setMessage(R.string.send)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        File[] files = getFilesDir().listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".csv");
                            }
                        });

                        for (File file : files) {
                            sendData(toAsset(file));
                            file.delete();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult(requestCode = " + requestCode + ", resultCode = " + resultCode + ", ...)");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart, binding service");
        bindService(sensorCollectorIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mDataItemGeneratorFuture =
                mGeneratorExecutor.scheduleWithFixedDelay(
                        new DataItemGenerator(), 1, 5, java.util.concurrent.TimeUnit.SECONDS);
        Wearable.getDataClient(this).addListener(this);
        Wearable.getMessageClient(this).addListener(this);
        mDataForwarderHandler.post(mDataForwarderTask);
        Wearable.getCapabilityClient(this)
                .addListener(this, Uri.parse("wear://"), CapabilityClient.FILTER_REACHABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
        Wearable.getDataClient(this).removeListener(this);
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
        mDataForwarderHandler.removeCallbacks(mDataForwarderTask);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop, unbind service");
        unbindService(connection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (/*isFinishing() ||*/ mSensorReadout == null || !mSensorReadout.isSportActivityRunning()) {
            Log.d(TAG, "stopping service");
            stopService(sensorCollectorIntent);
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        if (mPager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }


    /**
     * On start stop sport.
     *
     * @param view the view
     */
    public void onStartStopSport(View view) {
        boolean isRunning = mSensorReadout != null && mSensorReadout.isSportActivityRunning();

        if (!mStartStopButton.isChecked() && isRunning) {
            onStopSport(view);
        } else if (mStartStopButton.isChecked() && !isRunning) {
            onStartSport(view);
        } else {
            // desync between the button and the sport activity ("it should never happen")
            Log.e(TAG, "Button/SportActivity desync.");
        }
    }

    /**
     * On delete all.
     *
     * @param view the view
     */
    public void onDeleteAll(View view) {
        // 'compat' due to a target API below 29
        new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog))
                //.setTitle(R.string.clear)
                .setMessage(R.string.delete_all_confirmation)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        File[] files = getFilesDir().listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".csv");
                            }
                        });

                        for (File file : files) {
                            file.delete();
                        }
                        files = getFilesDir().listFiles(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".trk");
                            }
                        });

                        for (File file : files) {
                            file.delete();
                        }

                        updateFileList();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void onStartSport(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            mStartStopButton.setChecked(false);
            return;
        }

        if (mSensorReadout == null) {
            Log.e(TAG, "No mSensorReadout in Start Sport handler.");
        } else {
            Log.i(TAG, "Starting Sport...");
            Log.d(TAG, "starting service");
            startService(sensorCollectorIntent);
            mSensorReadout.resetSportActivity();
            //mHeartRateGraph.clear();
            mSensorReadout.startSportActivity(mSwitchHeartRate.isChecked(), mSwitchAccelerator.isChecked(), mSwitchGyroscope.isChecked(), mSwitchGeoLocation.isChecked());
        }
    }

    private void onStopSport(View view) {
        if (mSensorReadout == null) {
            Log.e(TAG, "No mSensorReadout in Stop Sport handler.");
        } else {
            mStartStopButton.setEnabled(false);
            mSensorReadout.stopSportActivity();
            Log.d(TAG, "stopping service (user action)");
            stopService(sensorCollectorIntent);

            // todo
            new AsyncSaver(new Consumer<Pair>() {
                @Override
                public void accept(Pair pair) {
                    inferenceResult = (Integer) pair.first;
                    fileName = (long) pair.second;
                    updateFileList();
                    // todo: potential desync with service bind status
                    mStartStopButton.setEnabled(true);
                    if ( (long) pair.second == (long) 0) {
                        Toast.makeText(getApplicationContext(), "Save Failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            },
                    getFilesDir(), this).execute(mSensorReadout);
            //show inference result
            Intent showInference = new Intent(this, CenterActivity.class);
            showInference.putExtra("inferenceResult", inferenceResult);
            showInference.putExtra("filename", fileName);
            android.util.Log.d(TAG, "currentCSVName now 1 is " + fileName);

            startActivity(showInference);
        }
    }

    private void updateFileList() {

        File[] files = getFilesDir().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".trk");
            }
        });

        mFileItemAdapter.setFiles(files);
        Log.d(TAG, "files refreshed " + files.length);
    }

    @Override
    public void onDataReceived(HeartRateSensorData data) {
        synchronized (heartRateData) {
            heartRateData.add(data);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                Boolean key = DataMapItem.fromDataItem(event.getDataItem()).getDataMap().containsKey(DataLayerListenerService.MODEL_KEY);
                if (DataLayerListenerService.FILE_PATH.equals(path) && key) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset modelAsset =
                            dataMapItem.getDataMap().getAsset(DataLayerListenerService.MODEL_KEY);
                    // Loads image on background thread.
                    new LoadBitmapAsyncTask().execute(modelAsset);
                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    Log.d(TAG, "Data Changed for COUNT_PATH");
                } else if (event.getDataItem().getAssets().containsKey(DATA_KEY)){
                    Log.d(TAG, "It's the datafile");
                }
                else {
                    Log.d(TAG, "Unrecognized path: " + path);

                }

            } else {
                new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Dialog))
                        .setMessage(R.string.unknown_type);
            }
        }
    }
    public void onCapabilityDiscoveryButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.capability_2_btn:
                showNodes(CAPABILITY_2_NAME);
                break;
            case R.id.capabilities_1_and_2_btn:
                showNodes(CAPABILITY_1_NAME, CAPABILITY_2_NAME);
                break;
            default:
                Log.e(TAG, "Unknown click event registered");
        }
    }
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);
        Log.d(
                TAG, "onMessageReceived() A message from watch was received:"
                        + messageEvent.getRequestId()
                        + " "
                        + messageEvent.getPath()
        );
    }

    private static Asset toAsset(File file) {
        java.io.FileInputStream is = null;
        byte[] bFile = new byte[(int) file.length()];
        try {
            is = new java.io.FileInputStream(file);
            is.read(bFile);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Asset asset = Asset.createFromBytes(bFile);
        return asset;
    }

    private void sendData(Asset asset) {
        com.google.android.gms.wearable.PutDataMapRequest dataMap = com.google.android.gms.wearable.PutDataMapRequest.create(FILE_PATH);
        dataMap.getDataMap().putAsset(DATA_KEY, asset);
        dataMap.getDataMap().putLong("time", new java.util.Date().getTime());
        com.google.android.gms.wearable.PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<com.google.android.gms.wearable.DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);

        dataItemTask.addOnSuccessListener(
                new OnSuccessListener<com.google.android.gms.wearable.DataItem>() {
                    @Override
                    public void onSuccess(com.google.android.gms.wearable.DataItem dataItem) {
                        Log.d(TAG, "Sending image was successful: " + dataItem);
                    }
                });
    }

    private class DataItemGenerator implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            com.google.android.gms.wearable.PutDataMapRequest putDataMapRequest = com.google.android.gms.wearable.PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);

            com.google.android.gms.wearable.PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Log.d(TAG, "Generating DataItem: " + request);

            Task<com.google.android.gms.wearable.DataItem> dataItemTask =
                    Wearable.getDataClient(getApplicationContext()).putDataItem(request);

            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                com.google.android.gms.wearable.DataItem dataItem = Tasks.await(dataItemTask);

                Log.d(TAG, "DataItem saved: " + dataItem);

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }


    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
    }

    /** Find the connected nodes that provide at least one of the given capabilities. */
    private void showNodes(final String... capabilityNames) {

        Task<Map<String, CapabilityInfo>> capabilitiesTask =
                Wearable.getCapabilityClient(this)
                        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE);

        capabilitiesTask.addOnSuccessListener(
                new OnSuccessListener<Map<String, CapabilityInfo>>() {
                    @Override
                    public void onSuccess(Map<String, CapabilityInfo> capabilityInfoMap) {
                        Set<Node> nodes = new HashSet<>();

                        if (capabilityInfoMap.isEmpty()) {
                            showDiscoveredNodes(nodes);
                            return;
                        }
                        for (String capabilityName : capabilityNames) {
                            CapabilityInfo capabilityInfo = capabilityInfoMap.get(capabilityName);
                            if (capabilityInfo != null) {
                                nodes.addAll(capabilityInfo.getNodes());
                            }
                        }
                        showDiscoveredNodes(nodes);
                    }
                });
    }


    private void showDiscoveredNodes(Set<Node> nodes) {
        List<String> nodesList = new ArrayList<>();
        for (Node node : nodes) {
            nodesList.add(node.getDisplayName());
        }
        Log.d(
                TAG,
                "Connected Nodes: "
                        + (nodesList.isEmpty()
                        ? "No connected device was found for the given capabilities"
                        : TextUtils.join(",", nodesList)));
        String msg;
        if (!nodesList.isEmpty()) {
            msg = getString(R.string.connected_nodes, TextUtils.join(", ", nodesList));
        } else {
            msg = getString(R.string.no_device);
        }
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Asset... params) {

            if (params.length > 0) {

                Asset asset = params[0];

                Task<DataClient.GetFdForAssetResponse> getFdForAssetResponseTask =
                        Wearable.getDataClient(getApplicationContext()).getFdForAsset(asset);

                try {
                    // Block on a task and get the result synchronously. This is generally done
                    // when executing a task inside a separately managed background thread. Doing
                    // this on the main (UI) thread can cause your application to become
                    // unresponsive.
                    DataClient.GetFdForAssetResponse getFdForAssetResponse =
                            Tasks.await(getFdForAssetResponseTask);

                    InputStream assetInputStream = getFdForAssetResponse.getInputStream();

                    if (assetInputStream != null) {
                        File dir = new File(getFilesDir(),"sync");
                        if (!dir.exists()){
                            dir.mkdir();
                        }
                        File modelFile = new File(dir, "model.pt");
                        try (OutputStream fos = new FileOutputStream(modelFile)){
                            byte[] buffer = new byte[8 * 1024];
                            int read;
                            while ((read=assetInputStream.read(buffer)) != -1){
                                fos.write(buffer, 0, read);
                            }
                            fos.flush();
                            Log.d(TAG, "file saved");
                            return true;
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                assetInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    } else {
                        Log.w(TAG, "Requested an unknown Asset.");
                    }

                } catch (ExecutionException exception) {
                    Log.e(TAG, "Failed retrieving asset, Task failed: " + exception);

                } catch (InterruptedException exception) {
                    Log.e(TAG, "Failed retrieving asset, interrupt occurred: " + exception);
                }

            } else {
                Log.e(TAG, "Asset must be non-null");
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            if (b) {
                Log.d(TAG, "File received");

            }
        }
    }


    /**
     * The type Screen slide page fragment.
     */
    public static class ScreenSlidePageFragment extends Fragment {
        private static final String TAG = ScreenSlidePageFragment.class.getSimpleName();

        private final View view;

        /**
         * Instantiates a new Screen slide page fragment.
         *
         * @param view the view
         */
        public ScreenSlidePageFragment(View view) {
            this.view = view;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return view;
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private final ScreenSlidePageFragment[] fragments;

        /**
         * Instantiates a new Screen slide pager adapter.
         *
         * @param fm       the fm
         * @param allViews the all views
         */
        public ScreenSlidePagerAdapter(FragmentManager fm, View... allViews) {
            super(fm);
            this.fragments = new ScreenSlidePageFragment[allViews.length];
            for (int n = 0; n < allViews.length; n++) {
                this.fragments[n] = new ScreenSlidePageFragment(allViews[n]);
            }
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return this.fragments[position];
        }

        @Override
        public int getCount() {
            return this.fragments.length;
        }
    }
}
