package com.lzr.videoview;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private CustomVideoView mVideoView;
    private ImageView mPlayOrPauseView, mVoiceView, mFullScreenView;
    private TextView mCurrentTiemView, mTotalTimeView;
    private SeekBar mVideoBar, mVoiceBar;
    private RelativeLayout mVideoViewLayout;
    //因为横竖屏会使控件拉伸，所以计算屏幕宽高
    private int mScreenWidth, mScreenHeight;
    //音频管理器控制音量大小
    private AudioManager mAudioManager;
    private boolean isFullScreen = false;
    //判断触摸事件是否合法
    private boolean isTuch = false;
    //触碰临界值
    private int criticalValue = 54;

    private static final int HANDLER_UPDATA_UI = 0;
    //填入本地视频路径
    private String path = "/storage/emulated/legacy/VID_20170713_211409.mp4";
    //亮度
    private float mLight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        initViews();
        setListeners();
    }

    private void initViews() {
        mVideoView = (CustomVideoView) findViewById(R.id.videoview);
        mPlayOrPauseView = (ImageView) findViewById(R.id.play_or_pause);
        mVoiceView = (ImageView) findViewById(R.id.voice);
        mFullScreenView = (ImageView) findViewById(R.id.fullscreen);
        mCurrentTiemView = (TextView) findViewById(R.id.current_play_time);
        mTotalTimeView = (TextView) findViewById(R.id.total_play_time);
        mVideoBar = (SeekBar) findViewById(R.id.video_seekbar);
        mVoiceBar = (SeekBar) findViewById(R.id.voice_seekbar);
        mVideoViewLayout = (RelativeLayout) findViewById(R.id.videoview_layout);
        Log.e("lzr", "path==" + path);
        //设置播放路径
        mVideoView.setVideoPath(path);
        //获取屏幕宽高
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        //获取系统最大音量
        int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //获取当前音量
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVoiceBar.setMax(streamMaxVolume);
        mVoiceBar.setProgress(streamVolume);

    }

    private void setListeners() {
        mPlayOrPauseView.setOnClickListener(this);
        mFullScreenView.setOnClickListener(this);
        //播放进度条监听
        mVideoBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //拖动时
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //拖动的时间显示当前的时间也跟着变化
                updataTextViewWithTimeFormat(mCurrentTiemView, progress);
            }

            //刚开始拖动时
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //停止UI自动刷新
                UiHandler.removeMessages(HANDLER_UPDATA_UI);
                mVideoView.pause();
            }

            //停止拖动时候调用
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //因为开始的时候已经设置了总时间为seekbar的最大值
                int progress = seekBar.getProgress();
                //所以会跟videoview的播放时间同步
                mVideoView.seekTo(progress);
                mVideoView.start();
                UiHandler.sendEmptyMessage(HANDLER_UPDATA_UI);
            }
        });
        mVoiceBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //设置音量 第一个参数类型，第二个进度值，第三个标记
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //设置触摸事件。改变屏幕光暗和音量大小
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            //记录最后一次的x,y坐标
            float lastX = 0, lastY = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //获取X，Y坐标
                float x = event.getX();
                float y = event.getY();
                switch (event.getAction()) {
                    //当手指按下（只调用一次）
                    case MotionEvent.ACTION_DOWN:
                        lastX = x;
                        lastY = y;
                        break;
                    //当手指移动（调用多次）
                    case MotionEvent.ACTION_MOVE:
                        //移动偏移量
                        float detlaX = x - lastX;
                        float detlaY = y - lastY;
                        //计算出两者绝对值
                        float absDetlaX = Math.abs(detlaX);
                        float absDetlaY = Math.abs(detlaY);
                        Log.e("lzr", "absDetlaX==" + absDetlaX + ",lastX==" + lastX + "" +
                                ",absDetlaY==" + absDetlaY + ",lastY==" + lastY);

                        //滑动距离都大于x,y的偏移量的时候，就是斜着滑动
                        if (absDetlaX > criticalValue && absDetlaY > criticalValue) {
                            //这种情况处理策略是将屏幕1分为2，落在左边就认为是光暗，右边就是音量
                            if (absDetlaX < absDetlaY) {
                                isTuch = true;
                            } else {
                                isTuch = false;
                            }
                        } else if (absDetlaX < criticalValue && absDetlaY > criticalValue) {
                            isTuch = true;
                        } else if (absDetlaX > criticalValue && absDetlaY < criticalValue) {
                            isTuch = false;
                        }
                        Log.e("lzr", "手势是否合法==" + isTuch);
                        if (isTuch) {
                            //在当前手势合法的前提下，区分音量还是光暗
                            if (x < mScreenWidth / 2) {
                                //调节亮度
                                if (detlaY > 0) {
                                    //降低亮度
                                } else {
                                    //增加亮度
                                }
                                changLight(detlaY);
                            } else {
                                //调节音量
                                if (detlaY > 0) {
                                    //减少音量
                                } else {
                                    //增加音量
                                }
                                //减少声音是大于0的数，但方法里面声音是当前声音+计算百分比声音，所以加了个负
                                changVoice(-detlaY);
                            }
                        }
//                        lastX = x;
//                        lastY = y;
                        break;
                    //当手指松开（只调用一次）
                    case MotionEvent.ACTION_UP:

                        break;
                }
                return true;
            }
        });
    }

    /**
     * 调节音量
     * @param detlaY
     */
    private void changVoice(float detlaY) {
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVoice = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //计算移动的百分比然后对应max添加到当前音量去，因为太小所以乘以3倍
        int index = (int) (detlaY / mScreenHeight * max * 3);
        //最少要大于0
        int voice = Math.max(currentVoice + index, 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, voice, 0);
        //同时音量进度条跟着变化
        mVoiceBar.setProgress(voice);
    }

    /**
     * 调节亮度
     * @param detlaY
     */
    private void changLight(float detlaY){
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        mLight = lp.screenBrightness;
        float index = detlaY/mScreenHeight/3;
        mLight+= index;
        if (mLight >1.0f){
            mLight = 1.0f;
        }
        if (mLight<0.01f){
            mLight = 0.01f;
        }
        lp.screenBrightness = mLight;
        getWindow().setAttributes(lp);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_or_pause:
                videoPlayEvent();
                break;
            case R.id.fullscreen:
                //如果是全屏就切换半屏 反之
                if (isFullScreen) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    mFullScreenView.setBackgroundResource(R.drawable.selector_fullscreen);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    mFullScreenView.setBackgroundResource(R.drawable.selector_restore);
                }
                break;

        }
    }

    /**
     * 显示播放时间
     *
     * @param view        显示的textview
     * @param millisecond 毫秒值
     */
    private void updataTextViewWithTimeFormat(TextView view, int millisecond) {
        int second = millisecond / 1000;
        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String str = null;
        //判断时长是否大于1小时以上，大于的话显示04:20:12格式，小于的话显示20:12格式
        if (hh != 0) {
            //%02d 如果是单位数会自动补0
            str = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            str = String.format("%02d:%02d", mm, ss);
        }
        view.setText(str);
    }

    /**
     * 定时刷新播放时间的handler
     */
    private Handler UiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_UPDATA_UI) {
                //当前播放时间
                int currentTime = mVideoView.getCurrentPosition();
                //视频总时间
                int totalTime = mVideoView.getDuration();
                //更新显示时间
                updataTextViewWithTimeFormat(mCurrentTiemView, currentTime);
                updataTextViewWithTimeFormat(mTotalTimeView, totalTime);
                //播放时间和进度条同步
                mVideoBar.setMax(totalTime);
                mVideoBar.setProgress(currentTime);
                //自己不停更新ui
                UiHandler.sendEmptyMessageDelayed(HANDLER_UPDATA_UI, 500);
            }
        }
    };

    /**
     * 监听屏幕横竖变化
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //当屏幕为横屏的时候
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setViewScale(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            isFullScreen = true;
            //解决屏幕切换后仍然会有黑色多余框的问题，1.自定义videoview重写onMeasure.重新测量其宽高
            //2.清除原来的状态，设置新的屏幕状态，Onmeasuer方法就更加准确
            //移除半屏状态
            getWindow().clearFlags((WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN));
            //设置全屏状态
            getWindow().addFlags((WindowManager.LayoutParams.FLAG_FULLSCREEN));
        } else {
            //当屏幕为竖屏的时候
            setViewScale(ViewGroup.LayoutParams.MATCH_PARENT, DisplayUtils.dip2px(this, 240));
            isFullScreen = false;
            getWindow().clearFlags((WindowManager.LayoutParams.FLAG_FULLSCREEN));
            getWindow().addFlags((WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN));
        }

    }

    /**
     * 控制视频的播放暂停
     */
    private void videoPlayEvent() {
        if (mVideoView.isPlaying()) {
            mPlayOrPauseView.setBackgroundResource(R.drawable.selector_play);
            mVideoView.pause();
            //暂停的时候停止UI和进度条更新
            UiHandler.removeMessages(HANDLER_UPDATA_UI);
        } else {
            mPlayOrPauseView.setBackgroundResource(R.drawable.selector_pause);
            mVideoView.start();
            UiHandler.sendEmptyMessage(HANDLER_UPDATA_UI);
        }
    }

    /**
     * 根据传入的宽高重新设置videoview 和layout的宽高
     *
     * @param width  宽
     * @param height 高
     */
    private void setViewScale(int width, int height) {
        //拉伸videoview
        ViewGroup.LayoutParams vvp = mVideoView.getLayoutParams();
        vvp.width = width;
        vvp.height = height;
        mVideoView.setLayoutParams(vvp);
        //拉伸外层相对布局
        ViewGroup.LayoutParams rllp = mVideoViewLayout.getLayoutParams();
        rllp.width = width;
        rllp.height = height;
        mVideoViewLayout.setLayoutParams(rllp);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //暂停的时候停止UI和进度条更新
        UiHandler.removeMessages(HANDLER_UPDATA_UI);
    }
}
