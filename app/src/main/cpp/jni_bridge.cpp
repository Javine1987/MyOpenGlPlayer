//
// Created by kuangyu on 18-12-3.
//

#include "jni.h"
#include "glm/glm.hpp"
#include "glm/ext/matrix_transform.hpp"
#include "glm/gtc/matrix_transform.hpp"
#include "glm/ext.hpp"
#include <android/log.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

using namespace glm;

#define LOG_TAG "Javine-native"
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static void printGLString(const char *name, GLenum s) {
    const char* v= (const char*) glGetString(s);
    LOGI("GL %s = %s\n", name, v);
}

static void checkGlError(const char* op) {
    for (GLint error= glGetError(); error; error = glGetError()) {
        LOGE("after %s glError (0x%x)\n", op, error);
    }
}

auto gVertexShader =
        "uniform mat4 uMVPMatrix;\n"
        "uniform mat4 uSTMatrix;\n"
        "attribute vec4 aPosition;\n"
        "attribute vec4 aTextureCoord;\n"
        "varying vec2 vTextureCoord;\n"
        "void main() {\n"
        "  gl_Position = uMVPMatrix * aPosition;\n"
        "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n"
        "}\n";
auto gFragmentShader =
        "#extension GL_OES_EGL_image_external : require \n"
        "precision mediump float;\n"
        "varying vec2 vTextureCoord;\n"
        "uniform samplerExternalOES sTexture;\n"
        "void main() {\n"
        "  vec3 centralColor = texture2D(sTexture, vTextureCoord).rgb;\n"
        "  gl_FragColor = vec4(0.299*centralColor.r + 0.587*centralColor.g + 0.114*centralColor.b);\n"
        "}\n";

const GLint cVertexStride = 5 * (sizeof(GL_FLOAT));

GLuint mProgram;
GLint gaPositionHandle;
GLint gaTextureHandle;
GLint guMVPMatrixHandle;
GLint guSTMatrixHandle;
GLint mTextureID;
GLuint mVBHandle;

glm::mat4 mVMatrix;
glm::mat4 mMMatrix;
glm::mat4 mMVPMatrix;
glm::mat4 mProjMatrix;
glm::mat4 mSTMatrix;

const GLfloat gVerticesData[] = {
        // x, y, z, u, v
        -1.0f, -1.0f, 0.f, 0.f, 0.f,
        1.0f, -1.0f, 0.f, 1.f, 0.f,
        -1.0f, 1.0f, 0.f, 0.f, 1.f,
        1.0f, 1.0f, 0.f, 1.0f, 1.0f
};

GLuint loadShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    if (shader) {
        glShaderSource(shader, 1, &source, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*)malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d: \n %s\n", type, buf);
                    free(buf);
                }
            }
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

GLuint createProgram(){
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, gVertexShader);
    if (vertexShader == 0) {
        return 0;
    }
    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, gFragmentShader);
    if (fragmentShader == 0) {
        return 0;
    }

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader");
        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            GLint bufLen = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLen);
            if (bufLen) {
                char* buf = (char*)malloc(bufLen);
                if (buf) {
                    glGetProgramInfoLog(program, bufLen, NULL, buf);
                    LOGE("Could not link program:\n%s\n", buf);
                    free(buf);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

extern "C" {

    JNIEXPORT void JNICALL
    Java_k_com_javine_myopenglplayer_NativeOpenGLHelper_ninitMyOpenGL(JNIEnv *env, jclass type) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glClearColor(0.f, 0.f, 0.f, 1.f);
        vec3 eye = {0.f, 0.f, 4.0f};
        vec3 center = {0.f, 0.f, 0.f};
        vec3 up = {0.f, 1.f, 0.f};
        mVMatrix = lookAt(eye, center, up);
        mSTMatrix = mat4(1.0f);
        mMMatrix = mat4(1.0f);
        vec3 aixY = {0.f, 1.f, 0.f};
        mMMatrix = glm::rotate(mMMatrix, 0.314f, aixY);
        // create program
        mProgram = createProgram();
        if (!mProgram) {
            return;
        }
        gaPositionHandle = glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        gaTextureHandle = glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        guMVPMatrixHandle = glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        guSTMatrixHandle = glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        // create texture
        GLuint texture = 0;
        glGenTextures(1,&texture);
        mTextureID = texture;
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);
        checkGlError("glBindTexture");

        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameterf mTextureID");

        //绑定顶点数据
        glGenBuffers(1, &mVBHandle);
        glBindBuffer(GL_ARRAY_BUFFER, mVBHandle);
        checkGlError("glBindBuffer");
        glBufferData(GL_ARRAY_BUFFER, sizeof(gVerticesData), gVerticesData, GL_STATIC_DRAW);
        checkGlError("glBufferData");
    }

    JNIEXPORT void JNICALL
    Java_k_com_javine_myopenglplayer_NativeOpenGLHelper_nsetViewport(JNIEnv *env, jclass type,
                                                                    jint width, jint height) {
        glViewport(0, 0, width, height);
        float ratio = (float)width/height;
        mProjMatrix = frustum(-ratio, ratio, -1.f, 1.f, 3.f, 7.f);
    }

    JNIEXPORT void JNICALL
    Java_k_com_javine_myopenglplayer_NativeOpenGLHelper_ndrawFrame(JNIEnv *env, jclass type) {
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
        glUseProgram(mProgram);
        checkGlError("glUseProgram");
        glBindBuffer(GL_ARRAY_BUFFER, mVBHandle);
        glVertexAttribPointer(gaPositionHandle, 3, GL_FLOAT, GL_FALSE,
                              cVertexStride, (const void*)0);
        glEnableVertexAttribArray(gaPositionHandle);
        checkGlError("glEnableVertexAttribArray aPosition");
        glVertexAttribPointer(gaTextureHandle, 2, GL_FLOAT, GL_FALSE,
                              cVertexStride, (const void*)(3*sizeof(GL_FLOAT)));
        glEnableVertexAttribArray(gaTextureHandle);
        checkGlError("glEnableVertexAttribArray textureCoord");

//        mMVPMatrix = mProjMatrix * mVMatrix * mMMatrix;
        mMVPMatrix = mProjMatrix * mVMatrix * mMMatrix;

        glUniformMatrix4fv(guMVPMatrixHandle, 1, GL_FALSE, value_ptr(mMVPMatrix));
        glUniformMatrix4fv(guSTMatrixHandle, 1, GL_FALSE, value_ptr(mSTMatrix));

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
    }

    JNIEXPORT void JNICALL
    Java_k_com_javine_myopenglplayer_NativeOpenGLHelper_nsetSTMatrix(JNIEnv *env, jclass type,
                                                                    jfloatArray tempMatrix_) {
        jfloat *tempMatrix = env->GetFloatArrayElements(tempMatrix_, NULL);

        mSTMatrix = make_mat4(tempMatrix);

        env->ReleaseFloatArrayElements(tempMatrix_, tempMatrix, 0);
    }

    JNIEXPORT jint JNICALL
    Java_k_com_javine_myopenglplayer_NativeOpenGLHelper_ngetTextureID(JNIEnv *env, jclass type) {
        return mTextureID;
    }
}