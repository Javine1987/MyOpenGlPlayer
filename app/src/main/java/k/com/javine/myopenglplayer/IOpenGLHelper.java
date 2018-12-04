package k.com.javine.myopenglplayer;

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 18-12-4
 */
public interface IOpenGLHelper {
    void resetData();
    void initGL();
    void drawFrame();
    void updateViewport(int width, int height);
    int getTextureID();
    void setSTMatrix(float[] temp);
}
