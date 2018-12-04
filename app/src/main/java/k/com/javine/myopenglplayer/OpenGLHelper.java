package k.com.javine.myopenglplayer;

import android.opengl.GLES10Ext;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES31Ext;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @文件描述 :
 * @文件作者 : KuangYu
 * @创建时间 : 18-11-30
 */
public class OpenGLHelper implements IOpenGLHelper{
    private static final String TAG = "OpenGLHelper";

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int VERTICES_DATA_POS_OFFSET = 0;
    private static final int VERTICES_DATA_UV_OFFSET = 3;

    private float[] mProjMatrix = new float[16];
    private float[] mVMatrix = new float[16];
    private float[] mMMatrix = new float[16];
    private float[] mMvpMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int mProgram;
    private int mTextureID;
    private int muMVPMatrixHandle;
    private int muSTMatrixHandle;
    private int maPositionHandle;
    private int maTextureHandle;

    private float mRatio = 1.0f;

    private final float[] mVerticesData = {
       // X, Y, Z, U, V
       -1.0f, -1.0f, 0.f, 0.f, 0.f,
       1.0f, -1.0f, 0.f, 1.f, 0.f,
       -1.0f, 1.0f, 0.f, 0.f, 1.f,
       1.0f, 1.0f, 0.f, 1.0f, 1.0f
    };

    private FloatBuffer mVertices;

    private final String mVertexShader =
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uSTMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uMVPMatrix * aPosition;\n" +
            "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
            "}\n";

    private final String mFragmentShader =
            "#extension GL_OES_EGL_image_external : require \n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "  vec3 centralColor = texture2D(sTexture, vTextureCoord).rgb;\n" +
            "  gl_FragColor = vec4(0.299*centralColor.r + 0.587*centralColor.g + 0.114*centralColor.b);\n" +
            "}\n";

    public void resetData(){
        mVertices = ByteBuffer.allocateDirect(mVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertices.put(mVerticesData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mMMatrix, 0);
        Matrix.rotateM(mMMatrix, 0, 10, 0, 1, 0);
    }

    public void initGL(){
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glClearColor(0.3f, 0.2f, 0.1f, 1.0f);
        Matrix.setLookAtM(mVMatrix, 0, 0 , 0, 4f, 0f, 0f, 0f, 0f, 1f, 0f);
        setupProgram();
        setupTexture();
    }

    private boolean setupProgram() {
        mProgram = createProgram();
        if (mProgram == 0) {
            return false;
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Fail to get attribute location aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Fail to get attribute location aTextureCoord");
        }
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Fail to get uniform location uMVPMatrix");
        }
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Fail to get uniform location uSTMatrix");
        }
        return true;
    }

    private void setupTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureID = textures[0];
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameterf mTextureID");
    }

    public void updateViewport(int width, int height) {
        GLES20.glViewport(0,0,width, height);
        mRatio = (float) width/height;
        Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 3, 7);
    }

    /**
     * opengl绘制
     */
    public void drawFrame(){
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        mVertices.position(VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mVertices.position(VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                VERTICES_DATA_STRIDE_BYTES, mVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        Matrix.multiplyMM(mMvpMatrix, 0, mVMatrix, 0, mMMatrix, 0);
        Matrix.multiplyMM(mMvpMatrix, 0, mProjMatrix, 0, mMvpMatrix, 0);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
    }

    private int createProgram(){
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShader);
        if (vertexShader == 0) {
            return 0;
        }
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShader);
        if (fragmentShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, fragmentShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                Log.e(TAG, "Could not link program: ");
                Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    private int loadShader(int type, String shaderStr){
        int shader = GLES20.glCreateShader(type);
        if (shader != 0) {
            GLES20.glShaderSource(shader, shaderStr);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + type + ":");
                Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public void setSTMatrix(float[] tempMatrix) {
        mSTMatrix = tempMatrix;
    }

    public int getTextureID() {
        return mTextureID;
    }
}
