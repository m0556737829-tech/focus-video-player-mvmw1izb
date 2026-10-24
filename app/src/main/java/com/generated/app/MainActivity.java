package com.generated.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_PICK_VIDEO = 101;

    private VideoView mVideoView;
    private ProgressBar mLoadingBar;
    private LinearLayout mControlsOverlay;
    private SeekBar mSeekBar;
    private TextView mTextCurrentTime;
    private TextView mTextTotalTime;
    private Button mBtnPlayPause;
    private Button mBtnSpeed;
    private Button mBtnAbLoop;
    private Button mBtnFullscreen;
    private LinearLayout mInfoBanner;

    private LinearLayout mTopBar;
    private LinearLayout mPanelActions;
    private LinearLayout mPanelTabs;
    private View mScrollPlaylist;
    private LinearLayout mContainerPlaylist;
    private LinearLayout mPanelNotes;
    private LinearLayout mContainerNotes;
    private Button mTabPlaylist;
    private Button mTabNotes;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsFullscreen = false;
    private float mCurrentSpeed = 1.0f;
    private final float[] SPEED_OPTIONS = {0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    private int mSpeedIndex = 1;

    // A-B Repeat Loop feature
    private int mLoopStartMs = -1;
    private int mLoopEndMs = -1;

    private static class VideoItem {
        final String title;
        final String uriString;
        final String durationHint;

        VideoItem(String title, String uriString, String durationHint) {
            this.title = title;
            this.uriString = uriString;
            this.durationHint = durationHint;
        }
    }

    private static class StudyNote {
        final int timestampMs;
        final String timeFormatted;
        final String noteText;

        StudyNote(int timestampMs, String timeFormatted, String noteText) {
            this.timestampMs = timestampMs;
            this.timeFormatted = timeFormatted;
            this.noteText = noteText;
        }
    }

    private final List<VideoItem> mPlaylist = new ArrayList<>();
    private final List<StudyNote> mNotes = new ArrayList<>();
    private String mCurrentVideoUri = null;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (mVideoView != null && mVideoView.isPlaying()) {
                int current = mVideoView.getCurrentPosition();
                int total = mVideoView.getDuration();

                if (total > 0) {
                    int progress = (int) (((long) current * 1000) / total);
                    mSeekBar.setProgress(progress);
                    mTextCurrentTime.setText(formatTime(current));
                    mTextTotalTime.setText(formatTime(total));
                }

                // Check A-B loop
                if (mLoopStartMs >= 0 && mLoopEndMs > mLoopStartMs) {
                    if (current >= mLoopEndMs) {
                        mVideoView.seekTo(mLoopStartMs);
                    }
                }
            }
            mHandler.postDelayed(this, 300);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSampleVideos();
        renderPlaylist();
        renderNotes();

        // Handle intent if opened via "Open with"
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            playVideo(intent.getData().toString(), "סרטון חיצוני");
        } else if (!mPlaylist.isEmpty()) {
            playVideo(mPlaylist.get(0).uriString, mPlaylist.get(0).title);
        }

    }

    private void initViews() {
        mVideoView = findViewById(R.id.video_view);
        mLoadingBar = findViewById(R.id.video_loading);
        mControlsOverlay = findViewById(R.id.controls_overlay);
        mSeekBar = findViewById(R.id.seek_bar);
        mTextCurrentTime = findViewById(R.id.text_current_time);
        mTextTotalTime = findViewById(R.id.text_total_time);
        mBtnPlayPause = findViewById(R.id.btn_play_pause);
        mBtnSpeed = findViewById(R.id.btn_speed);
        mBtnAbLoop = findViewById(R.id.btn_ab_loop);
        mBtnFullscreen = findViewById(R.id.btn_fullscreen);
        mInfoBanner = findViewById(R.id.info_banner);

        mTopBar = findViewById(R.id.top_bar);
        mPanelActions = findViewById(R.id.panel_video_actions);
        mPanelTabs = findViewById(R.id.panel_tabs);
        mScrollPlaylist = findViewById(R.id.scroll_playlist);
        mContainerPlaylist = findViewById(R.id.container_playlist);
        mPanelNotes = findViewById(R.id.panel_notes);
        mContainerNotes = findViewById(R.id.container_notes);
        mTabPlaylist = findViewById(R.id.tab_button_playlist);
        mTabNotes = findViewById(R.id.tab_button_notes);

        findViewById(R.id.btn_rewind).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoView != null) {
                    int pos = Math.max(0, mVideoView.getCurrentPosition() - 10000);
                    mVideoView.seekTo(pos);
                }
            }
        });

        findViewById(R.id.btn_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVideoView != null) {
                    int pos = Math.min(mVideoView.getDuration(), mVideoView.getCurrentPosition() + 10000);
                    mVideoView.seekTo(pos);
                }
            }
        });

        mBtnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });

        mBtnSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleSpeed();
            }
        });

        mBtnAbLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleAbLoopToggle();
            }
        });

        mBtnFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFullscreen();
            }
        });

        findViewById(R.id.btn_open_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickVideoFile();
            }
        });

        findViewById(R.id.btn_open_url).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUrlInputDialog();
            }
        });

        findViewById(R.id.btn_add_note).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNoteDialog();
            }
        });

        mTabPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPlaylistTab();
            }
        });

        mTabNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotesTab();
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mVideoView != null && mVideoView.getDuration() > 0) {
                    int newPos = (int) (((long) progress * mVideoView.getDuration()) / 1000);
                    mVideoView.seekTo(newPos);
                    mTextCurrentTime.setText(formatTime(newPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mLoadingBar.setVisibility(View.GONE);
                int duration = mVideoView.getDuration();
                mTextTotalTime.setText(formatTime(duration));
                applyCurrentSpeed(mp);
                mVideoView.start();
                mBtnPlayPause.setText(getString(R.string.btn_pause));
                mHandler.post(mUpdateProgressTask);
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mBtnPlayPause.setText(getString(R.string.btn_play));
                if (mLoopStartMs >= 0) {
                    mVideoView.seekTo(mLoopStartMs);
                    mVideoView.start();
                }
            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mLoadingBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, R.string.video_error, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void setupSampleVideos() {
        // High quality standard educational demo streams
        mPlaylist.add(new VideoItem(
                getString(R.string.sample_video_title_1),
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "09:56"
        ));
        mPlaylist.add(new VideoItem(
                getString(R.string.sample_video_title_2),
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                "10:53"
        ));
        mPlaylist.add(new VideoItem(
                getString(R.string.sample_video_title_3),
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                "00:15"
        ));
    }

    private void playVideo(String uriString, String title) {
        if (uriString == null || uriString.trim().isEmpty()) return;
        mCurrentVideoUri = uriString;
        mLoopStartMs = -1;
        mLoopEndMs = -1;
        mBtnAbLoop.setText("חזרה");

        mLoadingBar.setVisibility(View.VISIBLE);
        try {
            Uri videoUri = Uri.parse(uriString);
            mVideoView.setVideoURI(videoUri);
        } catch (Exception e) {
            mLoadingBar.setVisibility(View.GONE);
            Toast.makeText(this, R.string.video_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePlayPause() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mBtnPlayPause.setText(getString(R.string.btn_play));
        } else {
            mVideoView.start();
            mBtnPlayPause.setText(getString(R.string.btn_pause));
            mHandler.post(mUpdateProgressTask);
        }
    }

    private void cycleSpeed() {
        mSpeedIndex = (mSpeedIndex + 1) % SPEED_OPTIONS.length;
        mCurrentSpeed = SPEED_OPTIONS[mSpeedIndex];
        mBtnSpeed.setText(String.format(Locale.US, "%.2fx", mCurrentSpeed));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // MediaPlayer playback parameters
                if (mVideoView != null) {
                    // Quick seek to refresh speed if supported
                    float speed = mCurrentSpeed;
                    // Some devices allow reflection or playback params
                }
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, "מהירות נגינה: " + mCurrentSpeed + "x", Toast.LENGTH_SHORT).show();
    }

    private void applyCurrentSpeed(MediaPlayer mp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mp != null) {
            try {
                android.media.PlaybackParams params = mp.getPlaybackParams();
                if (params == null) {
                    params = new android.media.PlaybackParams();
                }
                params.setSpeed(mCurrentSpeed);
                mp.setPlaybackParams(params);
            } catch (Exception ignored) {}
        }
    }

    private void handleAbLoopToggle() {
        int current = mVideoView.getCurrentPosition();
        if (mLoopStartMs < 0) {
            mLoopStartMs = current;
            mBtnAbLoop.setText("A: " + formatTime(mLoopStartMs));
            Toast.makeText(this, getString(R.string.loop_a_set), Toast.LENGTH_SHORT).show();
        } else if (mLoopEndMs < 0) {
            if (current > mLoopStartMs) {
                mLoopEndMs = current;
                mBtnAbLoop.setText("לולאה A-B");
                Toast.makeText(this, getString(R.string.loop_ab_active), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "נקודה B חייבת להיות אחרי נקודה A", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Reset loop
            mLoopStartMs = -1;
            mLoopEndMs = -1;
            mBtnAbLoop.setText("חזרה");
            Toast.makeText(this, getString(R.string.loop_ab_off), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFullscreen() {
        mIsFullscreen = !mIsFullscreen;
        FrameLayout videoContainer = findViewById(R.id.video_container);

        if (mIsFullscreen) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mTopBar.setVisibility(View.GONE);
            mInfoBanner.setVisibility(View.GONE);
            mPanelActions.setVisibility(View.GONE);
            mPanelTabs.setVisibility(View.GONE);
            mScrollPlaylist.setVisibility(View.GONE);
            mPanelNotes.setVisibility(View.GONE);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            videoContainer.setLayoutParams(lp);
            mBtnFullscreen.setText("צא ממסך מלא");

            // Hide status & navigation bar
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            mTopBar.setVisibility(View.VISIBLE);
            mInfoBanner.setVisibility(View.VISIBLE);
            mPanelActions.setVisibility(View.VISIBLE);
            mPanelTabs.setVisibility(View.VISIBLE);
            showPlaylistTab();

            int heightPx = (int) (220 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    heightPx
            );
            videoContainer.setLayoutParams(lp);
            mBtnFullscreen.setText(getString(R.string.btn_fullscreen));

            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void showPlaylistTab() {
        mScrollPlaylist.setVisibility(View.VISIBLE);
        mPanelNotes.setVisibility(View.GONE);
        mTabPlaylist.setBackgroundResource(R.drawable.bg_button_primary);
        mTabPlaylist.setTextColor(0xFFFFFFFF);
        mTabNotes.setBackgroundResource(R.drawable.bg_button_secondary);
        mTabNotes.setTextColor(0xFF1A237E);
    }

    private void showNotesTab() {
        mScrollPlaylist.setVisibility(View.GONE);
        mPanelNotes.setVisibility(View.VISIBLE);
        mTabNotes.setBackgroundResource(R.drawable.bg_button_primary);
        mTabNotes.setTextColor(0xFFFFFFFF);
        mTabPlaylist.setBackgroundResource(R.drawable.bg_button_secondary);
        mTabPlaylist.setTextColor(0xFF1A237E);
    }

    private void renderPlaylist() {
        mContainerPlaylist.removeAllViews();

        for (int i = 0; i < mPlaylist.size(); i++) {
            final VideoItem item = mPlaylist.get(i);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Icon / Number badge
            TextView numBadge = new TextView(this);
            numBadge.setText(String.valueOf(i + 1));
            numBadge.setTextColor(0xFF1A237E);
            numBadge.setTextSize(14);
            numBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            numBadge.setPadding(0, 0, 20, 0);
            card.addView(numBadge);

            // Details
            LinearLayout infoCol = new LinearLayout(this);
            infoCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            infoCol.setLayoutParams(infoParams);

            TextView titleView = new TextView(this);
            titleView.setText(item.title);
            titleView.setTextSize(14);
            titleView.setTextColor(0xFF212121);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);
            infoCol.addView(titleView);

            TextView durationView = new TextView(this);
            durationView.setText("משך משוער: " + item.durationHint);
            durationView.setTextSize(11);
            durationView.setTextColor(0xFF757575);
            infoCol.addView(durationView);

            card.addView(infoCol);

            // Play action button
            Button playBtn = new Button(this);
            playBtn.setText("נגן");
            playBtn.setTextSize(11);
            playBtn.setTextColor(0xFFFFFFFF);
            playBtn.setBackgroundResource(R.drawable.bg_button_primary);
            playBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) (70 * getResources().getDisplayMetrics().density),
                    (int) (34 * getResources().getDisplayMetrics().density)
            ));
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playVideo(item.uriString, item.title);
                }
            });
            card.addView(playBtn);

            mContainerPlaylist.addView(card);
        }
    }

    private void renderNotes() {
        mContainerNotes.removeAllViews();

        if (mNotes.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(R.string.no_notes_yet);
            emptyView.setTextColor(0xFF757575);
            emptyView.setTextSize(13);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(0, 32, 0, 32);
            mContainerNotes.addView(emptyView);
            return;
        }

        for (final StudyNote note : mNotes) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(20, 16, 20, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, 0, 12);
            card.setLayoutParams(lp);

            LinearLayout header = new LinearLayout(this);
            header.setOrientation(LinearLayout.HORIZONTAL);

            TextView timeView = new TextView(this);
            timeView.setText("דקה " + note.timeFormatted);
            timeView.setTextColor(0xFF00B0FF);
            timeView.setTypeface(null, android.graphics.Typeface.BOLD);
            timeView.setTextSize(12);
            timeView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
            header.addView(timeView);

            Button jumpBtn = new Button(this);
            jumpBtn.setText("קפוץ לזמן זה");
            jumpBtn.setTextSize(10);
            jumpBtn.setTextColor(0xFF1A237E);
            jumpBtn.setBackgroundResource(R.drawable.bg_button_secondary);
            jumpBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) (90 * getResources().getDisplayMetrics().density),
                    (int) (30 * getResources().getDisplayMetrics().density)
            ));
            jumpBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mVideoView != null) {
                        mVideoView.seekTo(note.timestampMs);
                        mVideoView.start();
                    }
                }
            });
            header.addView(jumpBtn);

            card.addView(header);

            TextView noteText = new TextView(this);
            noteText.setText(note.noteText);
            noteText.setTextColor(0xFF212121);
            noteText.setTextSize(13);
            noteText.setPadding(0, 8, 0, 0);
            card.addView(noteText);

            mContainerNotes.addView(card);
        }
    }

    private void pickVideoFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_VIDEO);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_GET_CONTENT);
            fallback.setType("video/*");
            startActivityForResult(Intent.createChooser(fallback, getString(R.string.btn_open_file)), REQUEST_PICK_VIDEO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                String title = "סרטון מקומי נבחר";
                mPlaylist.add(0, new VideoItem(title, videoUri.toString(), "--:--"));
                renderPlaylist();
                playVideo(videoUri.toString(), title);
            }
        }
    }

    private void showUrlInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_add_url, null);
        final EditText input = dialogView.findViewById(R.id.edit_video_url);

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = input.getText().toString().trim();
                if (!url.isEmpty()) {
                    String title = "שיעור רשת (" + url.substring(Math.max(0, url.length() - 15)) + ")";
                    mPlaylist.add(0, new VideoItem(title, url, "--:--"));
                    renderPlaylist();
                    playVideo(url, title);
                }
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();
    }

    private void showAddNoteDialog() {
        final int currentMs = mVideoView != null ? mVideoView.getCurrentPosition() : 0;
        final String formattedTime = formatTime(currentMs);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_note, null);
        final TextView timeHeader = dialogView.findViewById(R.id.dialog_note_time_header);
        final EditText noteInput = dialogView.findViewById(R.id.edit_note_text);

        timeHeader.setText("הערה חדשה בזמן " + formattedTime);

        builder.setView(dialogView);
        builder.setPositiveButton(R.string.dialog_save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = noteInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    mNotes.add(0, new StudyNote(currentMs, formattedTime, text));
                    renderNotes();
                    showNotesTab();
                    Toast.makeText(MainActivity.this, "הערת למידה נשמרה בהצלחה", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.dialog_cancel, null);
        builder.show();
    }

    private String formatTime(int ms) {
        int seconds = (ms / 1000) % 60;
        int minutes = (ms / (1000 * 60)) % 60;
        int hours = (ms / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateProgressTask);
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.pause();
            mBtnPlayPause.setText(getString(R.string.btn_play));
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsFullscreen) {
            toggleFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE && !mIsFullscreen) {
            toggleFullscreen();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT && mIsFullscreen) {
            toggleFullscreen();
        }
    }
}
