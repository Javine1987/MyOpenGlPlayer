package k.com.javine.myopenglplayer;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 18-11-30
 */
public class MyGlSurfaceView extends GLSurfaceView {
    MyRender myRender;

    public MyGlSurfaceView(Context context) {
        super(context);
        init();
    }

    public MyGlSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        myRender = new MyRender();
        setRenderer(myRender);
    }

    public SurfaceTexture getSurfaceTexture() {
        return myRender.getSurfaceTexture();
    }

    public void setFixedSize(int width, int height) {
        myRender.setFixedSize(width,height);
    }

}

class MyRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture mSurface;
    private IOpenGLHelper mOpenGLHelper = new NativeOpenGLHelper();
    private boolean updateSurface;
    private float[] tempMatrix = new float[16];

    public MyRender() {
        mOpenGLHelper.resetData();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mOpenGLHelper.initGL();

        mSurface = new SurfaceTexture(mOpenGLHelper.getTextureID());
        mSurface.setOnFrameAvailableListener(this);
        synchronized (this) {
            updateSurface = false;
        }
    }

    public void setFixedSize(int width, int height) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mOpenGLHelper.updateViewport(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (updateSurface) {
                mSurface.updateTexImage();
                mSurface.getTransformMatrix(tempMatrix);
                mOpenGLHelper.setSTMatrix(tempMatrix);
                updateSurface = false;
            }
        }
        mOpenGLHelper.drawFrame();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        updateSurface = true;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurface;
    }
}
