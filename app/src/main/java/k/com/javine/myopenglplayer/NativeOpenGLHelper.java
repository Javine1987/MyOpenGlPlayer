package k.com.javine.myopenglplayer;

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 18-12-3
 */
public class NativeOpenGLHelper implements IOpenGLHelper{

    static {
        System.loadLibrary("gl2jni");
    }

    @Override
    public void resetData() {

    }

    @Override
    public void initGL() {
        ninitMyOpenGL();
    }

    @Override
    public void drawFrame() {
        ndrawFrame();
    }


    @Override
    public void updateViewport(int width, int height) {
        nsetViewport(width, height);
    }

    @Override
    public int getTextureID() {
        return ngetTextureID();
    }

    @Override
    public void setSTMatrix(float[] temp) {
        nsetSTMatrix(temp);
    }


    public static native void ninitMyOpenGL();
    public static native void nsetViewport(int width, int height);
    public static native void ndrawFrame();
    public static native void nsetSTMatrix(float[] tempMatrix);
    public static native int ngetTextureID();

}
