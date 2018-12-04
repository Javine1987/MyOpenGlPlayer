package k.com.javine.myopenglplayer;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Button mStartBtn;
    private MyGlSurfaceView mGlSurfaceView;
    private MediaPlayer mMediaPlayer;
    private AssetManager assetManager;
    private boolean mMediaPlayerPrepared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mGlSurfaceView = findViewById(R.id.glsurfaceview);
        mStartBtn = findViewById(R.id.btn_start);
        assetManager = getApplication().getAssets();
        initMediaPlayer();

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SurfaceTexture st = mGlSurfaceView.getSurfaceTexture();
                Surface s = new Surface(st);
                mMediaPlayer.setSurface(s);
                s.release();

                if (!mMediaPlayerPrepared) {

                    try{
                        AssetFileDescriptor clipFd = assetManager.openFd("clips/NativeMedia.ts");
                        mMediaPlayer.setDataSource(clipFd.getFileDescriptor(),
                                clipFd.getStartOffset(),
                                clipFd.getLength());
                        clipFd.close();
                    } catch (IOException e) {
                        Log.e("Javine", "IOException: " +e.toString());
                    }
                    mMediaPlayer.prepareAsync();
                }
            }
        });
    }

    private void initMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                int width = mp.getVideoWidth();
                int height = mp.getVideoHeight();
                if (width != 0 && height != 0) {
                    mGlSurfaceView.setFixedSize(width, height);
                }
                mMediaPlayerPrepared = true;
                mp.start();
            }
        });

        mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                if (width != 0 && height != 0) {
                    mGlSurfaceView.setFixedSize(width, height);
                }
            }
        });
    }
}
