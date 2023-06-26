package com.example.equalizer;

import static android.media.AudioManager.STREAM_MUSIC;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.AudioManager;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.example.equalizer.model.EqualizerModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class EqualizerFragment extends Fragment  {
    final String TAG = " conmeo";
    final String spotify = "com.spotify.music";
    final String soundcloud = "com.soundcloud.android";
    final String keeng = "com.vttm.keeng";
    final String nhaccuatui = "ht.nct";
    private static final String pimusic = "com.Project100Pi.themusicplayer";
    final String musicplayer = "musicplayer.musicapps.music.mp3player";
    final String zingmp3 = "com.zing.mp3";
    public String namePackage ="";

    AudioManager audioManager;
    private static final int PERMISSION_REQUEST_CODE = 100; // Mã yêu cầu read notification

    TextView tvtSong,tvtSinger;
    private Timer mMusicPlayerStartTimer;
    private MainActivity mainActivity;
    private Handler handler = new Handler(Looper.getMainLooper());
    public static final String ARG_AUDIO_SESSIOIN_ID = "audio_session_id";

    static int themeColor = Color.parseColor("#B24242");
    public Equalizer mEqualizer;
    SwitchCompat equalizerSwitch;
    public BassBoost bassBoost;
    LineChartView chart;
    public PresetReverb presetReverb;
    ImageView backBtn;

    int y = 0;
    ImageView spinnerDropDownIcon;
    TextView fragTitle;
    LinearLayout mLinearLayout, lnMusic;

    SeekBar[] seekBarFinal = new SeekBar[5];
    SeekBar seeBarVolume;
    ImageView imgVolume;
    SwitchCompat switchCompatVolume;
    AnalogController bassController, reverbController;

    Spinner presetSpinner;
    ImageView imgPrevious,imgPlay,imgNext;

    boolean play = true;
    Context ctx;

    public EqualizerFragment() {
        // Required empty public constructor
    }

    LineSet dataset;
    Paint paint;
    float[] points;
    short numberOfFrequencyBands;
    private int audioSesionId;
    static boolean showBackButton = true;

    public static EqualizerFragment newInstance(int audioSessionId) {
        Bundle args = new Bundle();
        args.putInt(ARG_AUDIO_SESSIOIN_ID, audioSessionId);
        EqualizerFragment fragment = new EqualizerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.isEditing = true;
        if (getArguments() != null && getArguments().containsKey(ARG_AUDIO_SESSIOIN_ID)) {
            audioSesionId = getArguments().getInt(ARG_AUDIO_SESSIOIN_ID);
        }
        if (Settings.equalizerModel == null) {
            Settings.equalizerModel = new EqualizerModel();
            Settings.equalizerModel.setReverbPreset(PresetReverb.PRESET_NONE);
            Settings.equalizerModel.setBassStrength((short) (1000 / 19));
        }
        //volume

        mEqualizer = new Equalizer(0, 0);


        bassBoost = new BassBoost(0, 0);
        bassBoost.setEnabled(Settings.isEqualizerEnabled);
        BassBoost.Settings bassBoostSettingTemp = bassBoost.getProperties();
        BassBoost.Settings bassBoostSetting = new BassBoost.Settings(bassBoostSettingTemp.toString());
        bassBoostSetting.strength = Settings.equalizerModel.getBassStrength();
        bassBoost.setProperties(bassBoostSetting);

        presetReverb = new PresetReverb(0, audioSesionId);
        presetReverb.setPreset(Settings.equalizerModel.getReverbPreset());
        presetReverb.setEnabled(Settings.isEqualizerEnabled);

        mEqualizer.setEnabled(Settings.isEqualizerEnabled);

        if (Settings.presetPos == 0) {
            for (short bandIdx = 0; bandIdx < mEqualizer.getNumberOfBands(); bandIdx++) {
                mEqualizer.setBandLevel(bandIdx, (short) Settings.seekbarpos[bandIdx]);
            }
        } else {
            mEqualizer.usePreset((short) Settings.presetPos);
        }


    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ctx = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Kiểm tra xem quyền đã được cấp hay chưa
        if (!isNotificationListenerEnabled()) {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            Toast.makeText(ctx, "Cần được cấp quyền truy cập thông báo", Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }

        // Inflate layout cho Fragment và trả về View
        View view = inflater.inflate(R.layout.fragment_equalizer, container, false);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imgPlay=view.findViewById(R.id.btn_Playpause);
        imgPrevious=view.findViewById(R.id.btn_previous);
        imgNext=view.findViewById(R.id.btn_Next);
         audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        tvtSinger = view.findViewById(R.id.singer);
        tvtSong = view.findViewById(R.id.name);
        mainActivity = (MainActivity) getActivity();
        int currentVolume = audioManager.getStreamVolume(STREAM_MUSIC);
        imgVolume = view.findViewById(R.id.imgVolume);
        seeBarVolume = view.findViewById(R.id.volume);
        switchCompatVolume = view.findViewById(R.id.switch_volume);
        switchCompatVolume.setChecked(true);
        lnMusic = view.findViewById(R.id.ln_music);
        switchCompatVolume.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    seeBarVolume.setEnabled(true);
                    imgVolume.setImageResource(R.drawable.baseline_volume_up_24);
                }else {

                    imgVolume.setImageResource(R.drawable.baseline_volume_off_24);
                    seeBarVolume.setEnabled(false);
                }
            }
        });
        int max = audioManager.getStreamMaxVolume(STREAM_MUSIC); // Giá trị tối đa
        int steps = 15; // Số lượng bước

// Tính toán giá trị tương ứng cho mỗi bước
        int stepValue = max / steps;

// Đặt giá trị tối thiểu và tối đa của SeekBar
        seeBarVolume.setMax(steps);
        seeBarVolume.setProgress(currentVolume/steps);
        seeBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value =  progress * stepValue;
                audioManager.setStreamVolume(STREAM_MUSIC,value,AudioManager.FLAG_SHOW_UI);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });




        backBtn = view.findViewById(R.id.equalizer_back_btn);
//        backBtn.setVisibility(showBackButton ? View.VISIBLE : View.GONE);


        fragTitle = view.findViewById(R.id.equalizer_fragment_title);
        equalizerSwitch = view.findViewById(R.id.equalizer_switch);
        equalizerSwitch.setChecked(Settings.isEqualizerEnabled);
        equalizerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "checked ?" + isChecked);
                mEqualizer.setEnabled(isChecked);
                bassBoost.setEnabled(isChecked);
                presetReverb.setEnabled(isChecked);
                Settings.isEqualizerEnabled = isChecked;
                Settings.equalizerModel.setEqualizerEnabled(isChecked);
            }
        });

        spinnerDropDownIcon = view.findViewById(R.id.spinner_dropdown_icon);
        spinnerDropDownIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presetSpinner.performClick();
            }
        });

        presetSpinner = view.findViewById(R.id.equalizer_preset_spinner);

//        equalizerBlocker = view.findViewById(R.id.equalizerBlocker);


        chart = view.findViewById(R.id.lineChart);
        paint = new Paint();
        dataset = new LineSet();

        bassController = view.findViewById(R.id.controllerBass);
        reverbController = view.findViewById(R.id.controller3D);

        bassController.setLabel("BASS");
        reverbController.setLabel("3D");

        bassController.circlePaint2.setColor(themeColor);
        bassController.linePaint.setColor(themeColor);
        bassController.invalidate();
        reverbController.circlePaint2.setColor(themeColor);
        bassController.linePaint.setColor(themeColor);
        reverbController.invalidate();

        if (!Settings.isEqualizerReloaded) {
            int x = 0;
            if (bassBoost != null) {
                try {
                    x = ((bassBoost.getRoundedStrength() * 19) / 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (presetReverb != null) {
                try {
                    y = (presetReverb.getPreset() * 19) / 6;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (x == 0) {
                bassController.setProgress(1);
            } else {
                bassController.setProgress(x);
            }

            if (y == 0) {
                reverbController.setProgress(1);
            } else {
                reverbController.setProgress(y);
            }
        } else {
            int x = ((Settings.bassStrength * 19) / 1000);
            y = (Settings.reverbPreset * 19) / 6;
            if (x == 0) {
                bassController.setProgress(1);
            } else {
                bassController.setProgress(x);
            }

            if (y == 0) {
                reverbController.setProgress(1);
            } else {
                reverbController.setProgress(y);
            }
        }

        bassController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.bassStrength = (short) (((float) 1000 / 19) * (progress));
                try {
                    bassBoost.setStrength(Settings.bassStrength);
                    Settings.equalizerModel.setBassStrength(Settings.bassStrength);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        reverbController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.reverbPreset = (short) ((progress * 6) / 19);
                Settings.equalizerModel.setReverbPreset(Settings.reverbPreset);
                try {
                    presetReverb.setPreset(Settings.reverbPreset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                y = progress;
            }
        });

        mLinearLayout = view.findViewById(R.id.equalizerContainer);

        TextView equalizerHeading = new TextView(getContext());
        equalizerHeading.setText(R.string.eq);
        equalizerHeading.setTextSize(20);
        equalizerHeading.setGravity(Gravity.CENTER_HORIZONTAL);

        numberOfFrequencyBands = 5;

        points = new float[numberOfFrequencyBands];

        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];
        final short upperEqualizerBandLevel = mEqualizer.getBandLevelRange()[1];

        for (short i = 0; i < numberOfFrequencyBands; i++) {
            final short equalizerBandIndex = i;
            final TextView frequencyHeaderTextView = new TextView(getContext());
            frequencyHeaderTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            frequencyHeaderTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frequencyHeaderTextView.setTextColor(Color.parseColor("#FFFFFF"));
            frequencyHeaderTextView.setText((mEqualizer.getCenterFreq(equalizerBandIndex) / 1000) + "Hz");//1000

            LinearLayout seekBarRowLayout = new LinearLayout(getContext());
            seekBarRowLayout.setOrientation(LinearLayout.VERTICAL);

            TextView lowerEqualizerBandLevelTextView = new TextView(getContext());
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            lowerEqualizerBandLevelTextView.setTextColor(Color.parseColor("#FFFFFF"));
            lowerEqualizerBandLevelTextView.setText((lowerEqualizerBandLevel / 100) + "dB");

            TextView upperEqualizerBandLevelTextView = new TextView(getContext());
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            upperEqualizerBandLevelTextView.setTextColor(Color.parseColor("#FFFFFF"));
            upperEqualizerBandLevelTextView.setText((upperEqualizerBandLevel / 100) + "dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.weight = 1;

            SeekBar seekBar = new SeekBar(getContext());
            TextView textView = new TextView(getContext());
            switch (i) {
                case 0:
                    seekBar = view.findViewById(R.id.seekBar1);
                    textView = view.findViewById(R.id.textView1);
                    break;
                case 1:
                    seekBar = view.findViewById(R.id.seekBar2);
                    textView = view.findViewById(R.id.textView2);
                    break;
                case 2:
                    seekBar = view.findViewById(R.id.seekBar3);
                    textView = view.findViewById(R.id.textView3);
                    break;
                case 3:
                    seekBar = view.findViewById(R.id.seekBar4);
                    textView = view.findViewById(R.id.textView4);
                    break;
                case 4:
                    seekBar = view.findViewById(R.id.seekBar5);
                    textView = view.findViewById(R.id.textView5);
                    break;
            }
            seekBarFinal[i] = seekBar;
            seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN));
            seekBar.getThumb().setColorFilter(new PorterDuffColorFilter(themeColor, PorterDuff.Mode.SRC_IN));
            seekBar.setId(i);
//            seekBar.setLayoutParams(layoutParams);
            seekBar.setMax(upperEqualizerBandLevel - lowerEqualizerBandLevel);

            textView.setText(frequencyHeaderTextView.getText());
            textView.setTextColor(Color.WHITE);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            if (Settings.isEqualizerReloaded) {
                points[i] = Settings.seekbarpos[i] - lowerEqualizerBandLevel;
                dataset.addPoint(frequencyHeaderTextView.getText().toString(), points[i]);
                seekBar.setProgress(Settings.seekbarpos[i] - lowerEqualizerBandLevel);
            } else {
                points[i] = mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel;
                dataset.addPoint(frequencyHeaderTextView.getText().toString(), points[i]);
                seekBar.setProgress(mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel);
                Settings.seekbarpos[i] = mEqualizer.getBandLevel(equalizerBandIndex);
                Settings.isEqualizerReloaded = true;
            }

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mEqualizer.setBandLevel(equalizerBandIndex, (short) (progress + lowerEqualizerBandLevel));
                    points[seekBar.getId()] = mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel;
                    Settings.seekbarpos[seekBar.getId()] = (progress + lowerEqualizerBandLevel);
                    Settings.equalizerModel.getSeekbarpos()[seekBar.getId()] = (progress + lowerEqualizerBandLevel);
                    dataset.updateValues(points);
                    chart.notifyDataUpdate();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    presetSpinner.setSelection(0);
                    Settings.presetPos = 0;
                    Settings.equalizerModel.setPresetPos(0);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        equalizeSound();

        paint.setColor(Color.parseColor("#555555"));
        paint.setStrokeWidth((float) (1.10 * Settings.ratio));

        dataset.setColor(themeColor);
        dataset.setSmooth(true);
        dataset.setThickness(5);

        chart.setXAxis(false);
        chart.setYAxis(false);

        chart.setYLabels(AxisController.LabelPosition.NONE);
        chart.setXLabels(AxisController.LabelPosition.NONE);
        chart.setGrid(ChartView.GridType.NONE, 7, 10, paint);

        chart.setAxisBorderValues(-300, 3300);

        chart.addData(dataset);
        chart.show();

        Button mEndButton = new Button(getContext());
        mEndButton.setBackgroundColor(themeColor);
        mEndButton.setTextColor(Color.WHITE);
        if(ContextCompat.checkSelfPermission(ctx, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE) == PackageManager.PERMISSION_GRANTED){
            Log.i(TAG, "App has permission!");
        } else
            Log.i(TAG, "App hasn't permission " + ContextCompat.checkSelfPermission(ctx, Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE));

        check();

        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
//        iF.addAction("com.htc.music.metachanged");
//        iF.addAction("fm.last.android.metachanged");
//        iF.addAction("com.sec.android.app.music.metachanged");
//        iF.addAction("com.nullsoft.winamp.metachanged");
//        iF.addAction("com.amazon.mp3.metachanged");
//        iF.addAction("com.miui.player.metachanged");
//        iF.addAction("com.real.IMP.metachanged");
//        iF.addAction("com.sonyericsson.music.metachanged");
        iF.addAction("com.audio.android.metachanged");
//        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
//        iF.addAction("com.andrew.apollo.metachanged");
        iF.addAction("com.android.music.metachanged");
//        iF.addAction("com.android.music.playstatechanged");
//        iF.addAction("com.android.music.playbackcomplete");
//        iF.addAction("com.android.music.queuechanged");
        iF.addAction(spotify+".metadatachanged");
        iF.addAction(nhaccuatui +".metadatachanged");
        iF.addAction(namePackage +".metadatachanged");
        iF.addAction(namePackage+".playstatechanged");
        iF.addAction(namePackage+".playbackcomplete");
        iF.addAction(namePackage+".queuechanged");
        iF.addAction(musicplayer +".metadatachanged");
        mainActivity.registerReceiver(mReceiver, iF);

    }
    private void check() {
        lnMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            playDialogMusicChoose();
            }
        });
        if (!audioManager.isMusicActive() ) { //&& !isSpotifyRunning()
//            startMusicPlayer();
            imgPlay.setImageResource(R.drawable.baseline_pause_circle_24);
        }else {
            imgPlay.setImageResource(R.drawable.baseline_play_circle_24);

        }
        imgNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSong();
            }
        });
        imgPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseMusic();
            }
        });
        imgPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previusSong();
            }
        });
    }

    private void playDialogMusicChoose() {
        View view =     getLayoutInflater().inflate(R.layout.ln_music,null);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ctx);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
        LinearLayout lnSpotify = bottomSheetDialog.findViewById(R.id.spotify);
        LinearLayout lnSounCloud = bottomSheetDialog.findViewById(R.id.soundcloud);
        LinearLayout lnNhaccuatui = bottomSheetDialog.findViewById(R.id.nhaccuatui);
        LinearLayout lnYtmusic = bottomSheetDialog.findViewById(R.id.ytmusic);
        LinearLayout lnKeeng = bottomSheetDialog.findViewById(R.id.keeng);
        LinearLayout lnPimusic = bottomSheetDialog.findViewById(R.id.pimusic);
        if(!isPackageInstalled(ctx, spotify)){
//            lnSpotify.setVisibility(View.VISIBLE);
//        }else {
            lnSpotify.setVisibility(View.GONE);
            Log.d("tag","sptify");
        }
        if(isPackageInstalled(ctx, soundcloud)){
            lnSounCloud.setVisibility(View.VISIBLE);
        }else {
            lnSounCloud.setVisibility(View.GONE);
        }
        if(isPackageInstalled(ctx, nhaccuatui)){
            lnNhaccuatui.setVisibility(View.VISIBLE);
        }else {
            lnNhaccuatui.setVisibility(View.GONE);
        }
        if(isPackageInstalled(ctx, keeng)){
            lnKeeng.setVisibility(View.VISIBLE);
        }else {
            lnKeeng.setVisibility(View.GONE);
        }
        if(isPackageInstalled(ctx, pimusic)){
            lnPimusic.setVisibility(View.VISIBLE);
        }else {
            lnPimusic.setVisibility(View.GONE);
        }
        lnSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                namePackage = spotify;
                bottomSheetDialog.dismiss();
            }
        });
        lnKeeng.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                namePackage= keeng;
                bottomSheetDialog.dismiss();

            }
        });
        lnSounCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                namePackage= soundcloud;
                bottomSheetDialog.dismiss();

            }
        });
       lnNhaccuatui.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               namePackage=nhaccuatui;
               bottomSheetDialog.dismiss();

           }
       });
       lnPimusic.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               namePackage= pimusic;
               bottomSheetDialog.dismiss();

           }
       });

    }

    public void nextSong() {
        Log.d("fat","next");
        int keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
        if (!isSpotifyRunning()) {
            Log.d("fat","before");
            //voo start music se nhảy sang spotify
//            startMusicPlayer();
        }

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        intent.setPackage(namePackage);
        synchronized (this) {
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            getContext().sendOrderedBroadcast(intent, null);

            intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            getContext().sendOrderedBroadcast(intent, null);
        }
    }
    public void previusSong() {
        Log.d("fat","next");
        int keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        if (!isSpotifyRunning()) {
            Log.d("fat","before");
            //voo start music se nhảy sang spotify
//            startMusicPlayer();
        }

        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
//        intent.setPackage(spotify);
//        intent.setPackage(nhaccuatui);
        intent.setPackage(namePackage);
//        intent.setPackage(musicplayer);

        synchronized (this) {
            intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            getContext().sendOrderedBroadcast(intent, null);

            intent.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            getContext().sendOrderedBroadcast(intent, null);
        }
    }


    public void playPauseMusic() {
        Log.d("fat","play");

        int keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
//        int keyCode = KeyEvent.ACTION_UP;

        if (!audioManager.isMusicActive() ) { //&& !isSpotifyRunning()
//            startMusicPlayer();
            imgPlay.setImageResource(R.drawable.baseline_pause_circle_24);
        }else {
            imgPlay.setImageResource(R.drawable.baseline_play_circle_24);

        }

        Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);

        i.setPackage(namePackage);

        synchronized (this) {
            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            getContext().sendOrderedBroadcast(i, null);

            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
            getContext().sendOrderedBroadcast(i, null);
        }
    }

    private void startMusicPlayer() {
        Log.d("fat","start");

//        Intent startPlayer = new Intent(Intent.ACTION_HEADSET_PLUG);
        Intent startPlayer = new Intent(Intent.ACTION_MAIN);
//        startPlayer.setPackage(spotify);
//        startPlayer.setPackage(nhaccuatui);
        startPlayer.setPackage(namePackage);
//        startPlayer.setPackage(musicplayer);

        startPlayer.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startPlayer);

        if (mMusicPlayerStartTimer != null) {
            mMusicPlayerStartTimer.cancel();
        }

        mMusicPlayerStartTimer = new Timer("MusicPlayerStartTimer", true);
        mMusicPlayerStartTimer.schedule(new MusicPlayerStartTimerTask(), DateUtils.SECOND_IN_MILLIS, DateUtils.SECOND_IN_MILLIS);
    }

    private boolean isSpotifyRunning() {

        Process ps = null;
        try {
//            String[] cmd = {
//                    "sh",
//                    "-c",
//                    "ps | grep com.spotify.music"
//            };
            String[] cmd = {
                    "sh",
                    "-c",
                    "ps | grep "+namePackage
            };

            ps = Runtime.getRuntime().exec(cmd);
            Log.d("DEBUG_TAG","runnning");
            ps.waitFor();

            return ps.exitValue() == 0;
        } catch (IOException e) {
            Log.e("DEBUG_TAG", "Could not execute ps", e);
        } catch (InterruptedException e) {
            Log.e("DEBUG_TAG", "Could not execute ps", e);
        } finally {
            if (ps != null) {
                ps.destroy();
            }
        }

        return false;
    }


    private class MusicPlayerStartTimerTask extends TimerTask {
        @Override
        public void run() {
            if (isSpotifyRunning()) {
                playPauseMusic();
                cancel();
            }
        }
    }
    private boolean isNotificationListenerEnabled() {
        String packageName = getActivity().getPackageName();
        String flat = android.provider.Settings.Secure.getString(getActivity().getContentResolver(), "enabled_notification_listeners");
        if (flat != null) {
            String[] names = flat.split(":");
            for (String name : names) {
                ComponentName componentName = ComponentName.unflattenFromString(name);
                if (componentName != null && TextUtils.equals(packageName, componentName.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Quyền đã được cấp, tiếp tục xử lý thông báo
                Toast.makeText(ctx, "Permission grantted", Toast.LENGTH_SHORT).show();
            } else {
                // Quyền bị từ chối, xử lý tương ứng (ví dụ: hiển thị thông báo cho người dùng)
                // ...

            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("package");
                namePackage = cmd;
                Log.v("tag ", action + " / " + cmd);
//            String artist = intent.getStringExtra("artist");
//            String album = intent.getStringExtra("album");
//            String track = intent.getStringExtra("track");
                String singer = intent.getStringExtra("nameSong");
                String song = intent.getStringExtra("titleSong");
                String bitmap = intent.getStringExtra("bitmap");
                if(singer!=null&& song!=null){
                    if(!singer.equals("")&& !song.equals("")){
                        tvtSong.setText(song);
                        tvtSinger.setText(singer);
                    }
                }


                Log.v("tag", song + ":" + singer +bitmap);


//            Toast.makeText(ctx, artist + ":" + album + ":" + track, Toast.LENGTH_SHORT).show();
//            Toast.makeText(ctx, track, Toast.LENGTH_SHORT).show();
        }
    };
    public boolean isPackageInstalled(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            Log.d("tag","get Package Name");
            return true; // Gói tồn tại trên thiết bị
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("tag","ko tồn tại");
            return false; // Gói không tồn tại trên thiết bị
        }
    }
    public void equalizeSound() {
        ArrayList<String> equalizerPresetNames = new ArrayList<>();
        ArrayAdapter<String> equalizerPresetSpinnerAdapter = new ArrayAdapter<>(ctx,
                R.layout.spinner_item,
                equalizerPresetNames);
        equalizerPresetSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        equalizerPresetNames.add("Custom");

        for (short i = 0; i < mEqualizer.getNumberOfPresets(); i++) {
            equalizerPresetNames.add(mEqualizer.getPresetName(i));
        }

        presetSpinner.setAdapter(equalizerPresetSpinnerAdapter);
        //presetSpinner.setDropDownWidth((Settings.screen_width * 3) / 4);
        if (Settings.isEqualizerReloaded && Settings.presetPos != 0) {
//            correctPosition = false;
            presetSpinner.setSelection(Settings.presetPos);
        }

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (position != 0) {
                        mEqualizer.usePreset((short) (position - 1));
                        Settings.presetPos = position;
                        short numberOfFreqBands = 5;

                        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];

                        for (short i = 0; i < numberOfFreqBands; i++) {
                            seekBarFinal[i].setProgress(mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel);
                            points[i] = mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel;
                            Settings.seekbarpos[i] = mEqualizer.getBandLevel(i);
                            Settings.equalizerModel.getSeekbarpos()[i] = mEqualizer.getBandLevel(i);
                        }
                        dataset.updateValues(points);
                        chart.notifyDataUpdate();
                    }
                } catch (Exception e) {
                    Toast.makeText(ctx, "Error while updating Equalizer", Toast.LENGTH_SHORT).show();
                }
                Settings.equalizerModel.setPresetPos(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mEqualizer != null) {
            mEqualizer.release();
        }

        if (bassBoost != null) {
            bassBoost.release();
        }

        if (presetReverb != null) {
            presetReverb.release();
        }

        Settings.isEditing = false;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static class Builder {
        private int id = -1;

        public Builder setAudioSessionId(int id) {
            this.id = id;
            return this;
        }

        public Builder setAccentColor(int color) {
            themeColor = color;
            return this;
        }

        public Builder setShowBackButton(boolean show) {
            showBackButton = show;
            return this;
        }

        public EqualizerFragment build() {
            return EqualizerFragment.newInstance(id);
        }
    }

    @Override
    public void onResume() {
        if (!audioManager.isMusicActive() ) { //&& !isSpotifyRunning()
//            startMusicPlayer();
            imgPlay.setImageResource(R.drawable.baseline_pause_circle_24);
        }else {
            imgPlay.setImageResource(R.drawable.baseline_play_circle_24);

        }
        getActivity().registerReceiver(mReceiver, new IntentFilter("keySong"));
        super.onResume();

    }

    @Override
    public void onPause() {
        if (!audioManager.isMusicActive() ) { //&& !isSpotifyRunning()
//            startMusicPlayer();
            imgPlay.setImageResource(R.drawable.baseline_pause_circle_24);
        }else {
            imgPlay.setImageResource(R.drawable.baseline_play_circle_24);

        }
        getActivity().unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        if (!audioManager.isMusicActive() ) { //&& !isSpotifyRunning()
//            startMusicPlayer();
            imgPlay.setImageResource(R.drawable.baseline_pause_circle_24);
        }else {
            imgPlay.setImageResource(R.drawable.baseline_play_circle_24);

        }
        super.onStop();
    }
}
