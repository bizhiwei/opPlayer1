package com.lenovo.vrplayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;


public class GLVideoRender implements GLSurfaceView.Renderer,
		SurfaceTexture.OnFrameAvailableListener {
	private static String TAG = "GLVideoRender";

	private int mWidth = 0;
	private int mHeight = 0;
	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
	float r = 50.0f;
	public static final float UNIT_SIZE=1f;
	private final  int   a = 10;
	int vCount = 0;
	FloatBuffer mVertexBuffer;// 顶点坐标数据缓冲
	FloatBuffer mTextureBuffer;// 顶点纹理左眼缓冲
	FloatBuffer mTextureBufferR;// 顶点纹理右眼缓冲
	private final float[] mTriangleVerticesData_LeftView = {
			// X, Y, Z, U, V
			-0.4f*a , -0.45f*a , 2.0f, 0.5f, 0.0f,
			0.4f*a , -0.45f*a,  2.0f, 0f, 0.0f,
			-0.4f*a ,  0.45f*a,  2.0f, 0.5f, 1.0f,
			0.4f*a ,  0.45f*a,  2.0f, 0f, 1.0f,
	};
	private final float[] mTriangleVerticesData_RightView = {
			// X, Y, Z, U, V
			-1.0f , -1.0f , 2.0f, 1.0f, 0.0f,
			1.0f , -1.0f,  2.0f, 0.5f, 0.0f,
			-1.0f ,  1.0f,  2.0f, 1.0f, 1.0f,
			1.0f ,  1.0f,  2.0f, 0.5f, 1.0f,
   };

	private FloatBuffer mTriangleVertices;
	private FloatBuffer mTextureVertices;

	private final String mVertexShader = "uniform mat4 uMVPMatrix;\n"
			+ "uniform mat4 uSTMatrix;\n" + "attribute vec4 aPosition;\n"
			+ "attribute vec4 aTextureCoord;\n"
			+ "varying vec2 vTextureCoord;\n" + "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" + "}\n";

	private final String mFragmentShader = "#extension GL_OES_EGL_image_external : require\n"
			+ "precision mediump float;\n"
			+ "varying vec2 vTextureCoord;\n"
			+ "uniform samplerExternalOES sTexture;\n"
			+ "void main() {\n"
			+ "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" + "}\n";

	private float[] mMVPMatrix = new float[16];
	private float[] mSTMatrix = new float[16];
	private float[] mRotationMatrix = new float[16];
	private int mProgram;
	private int mTextureID;
	private int muMVPMatrixHandle;
	private int muSTMatrixHandle;
	private int maPositionHandle;
	private int maTextureHandle;

	private SurfaceTexture mSurface;
	private boolean updateSurface = false;

	private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

	private MediaPlayer mMediaPlayer;

	public GLVideoRender(Context context, MediaPlayer player) {
		
		mMediaPlayer = player;
		mTriangleVertices = ByteBuffer
				.allocateDirect(
						mTriangleVerticesData_RightView.length
								* FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTriangleVertices.put(mTriangleVerticesData_RightView).position(0);

		Matrix.setIdentityM(mSTMatrix, 0);
		Matrix.setIdentityM(mRotationMatrix, 0);
	}

	public void setMediaPlayer(MediaPlayer player) {
		mMediaPlayer = player;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		synchronized (this) {
			if (updateSurface) {
				mSurface.updateTexImage();
				mSurface.getTransformMatrix(mSTMatrix);
				updateSurface = false;
			}
		}

		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glUseProgram(mProgram);
		checkGlError("glUseProgram");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

		{
			GLES20.glDisable(GLES20.GL_CULL_FACE);
			//mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData_LeftView.length * FLOAT_SIZE_BYTES)	.order(ByteOrder.nativeOrder()).asFloatBuffer();
			//mTriangleVertices.put(mTriangleVerticesData_LeftView).position(0);

			//mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
			GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
					false, 3 * FLOAT_SIZE_BYTES, mVertexBuffer);
			checkGlError("glVertexAttribPointer maPosition");
			GLES20.glEnableVertexAttribArray(maPositionHandle);
			checkGlError("glEnableVertexAttribArray maPositionHandle");

			//mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
			GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT,
					false, 2*FLOAT_SIZE_BYTES, mTextureBuffer);
			checkGlError("glVertexAttribPointer maTextureHandle");
			GLES20.glEnableVertexAttribArray(maTextureHandle);
			checkGlError("glEnableVertexAttribArray maTextureHandle");

			Matrix.setIdentityM(mMVPMatrix, 0);
			float[] eyeMatrix = new float[16];
			Matrix.setLookAtM(eyeMatrix, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f);
			float[] projectMatrix = new float[16];
			float ratio = mHeight / 2.0f / mWidth;
			Matrix.frustumM(projectMatrix, 0, -1.0f, 1.0f, -ratio, ratio, 1.0f, 100.0f);
			Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, eyeMatrix, 0);
			Matrix.multiplyMM(mMVPMatrix, 0, projectMatrix, 0, mMVPMatrix, 0);


			GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix,
					0);
			GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
			GLES20.glViewport(0, mHeight/2, mWidth, mHeight/2);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
			checkGlError("glDrawArrays");
		}

		{
			GLES20.glDisable(GLES20.GL_CULL_FACE);
	/*		mTriangleVertices = ByteBuffer
					.allocateDirect(
							mTriangleVerticesData_RightView.length
									* FLOAT_SIZE_BYTES)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			mTriangleVertices.put(mTriangleVerticesData_RightView).position(0);

			mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);*/
			GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
					false, 3 * FLOAT_SIZE_BYTES, mVertexBuffer);
			checkGlError("glVertexAttribPointer maPosition");
			GLES20.glEnableVertexAttribArray(maPositionHandle);
			checkGlError("glEnableVertexAttribArray maPositionHandle");

//			mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
			/*GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT,
					false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
					mTriangleVertices);*/

			GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT,
					false, 2*FLOAT_SIZE_BYTES, mTextureBufferR);
			checkGlError("glVertexAttribPointer maTextureHandle");
			GLES20.glEnableVertexAttribArray(maTextureHandle);
			checkGlError("glEnableVertexAttribArray maTextureHandle");

			Matrix.setIdentityM(mMVPMatrix, 0);
			float[] eyeMatrix = new float[16];
			Matrix.setLookAtM(eyeMatrix, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f);
			float[] projectMatrix = new float[16];
			float ratio = mHeight / 2.0f / mWidth;
			Matrix.frustumM(projectMatrix, 0, -1.0f, 1.0f, -ratio, ratio, 1.0f, 100.0f);
			Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, eyeMatrix, 0);
			Matrix.multiplyMM(mMVPMatrix, 0, projectMatrix, 0, mMVPMatrix, 0);

			//GLES20.glViewport();
			GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix,0);
			GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

			GLES20.glViewport(0, 0, mWidth, mHeight / 2);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
			checkGlError("glDrawArrays");
		}

		GLES20.glFinish();

	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		mWidth = width;
		mHeight = height;
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		mProgram = createProgram(mVertexShader, mFragmentShader);
		if (mProgram == 0) {
			return;
		}
		maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
		checkGlError("glGetAttribLocation aPosition");
		if (maPositionHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aPosition");
		}
		maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
		checkGlError("glGetAttribLocation aTextureCoord");
		if (maTextureHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for aTextureCoord");
		}

		muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		checkGlError("glGetUniformLocation uMVPMatrix");
		if (muMVPMatrixHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uMVPMatrix");
		}

		muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
		checkGlError("glGetUniformLocation uSTMatrix");
		if (muSTMatrixHandle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for uSTMatrix");
		}

		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);

		mTextureID = textures[0];
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
		checkGlError("glBindTexture mTextureID");

		GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		/*
		 * Create the SurfaceTexture that will feed this textureID, and pass it
		 * to the MediaPlayer
		 */
		mSurface = new SurfaceTexture(mTextureID);
		mSurface.setOnFrameAvailableListener(this);

		Surface surface = new Surface(mSurface);
		mMediaPlayer.setSurface(surface);
		mMediaPlayer.setScreenOnWhilePlaying(true);
		surface.release();

		try {
			mMediaPlayer.prepare();
		} catch (IOException t) {
			Log.e(TAG, "media player prepare failed");
		}

		synchronized (this) {
			updateSurface = false;
		}

		mMediaPlayer.start();

		drawBall();
	}

	synchronized public void onFrameAvailable(SurfaceTexture surface) {
		updateSurface = true;
	}

	private int loadShader(int shaderType, String source) {
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

	private int createProgram(String vertexSource, String fragmentSource) {
		int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (vertexShader == 0) {
			return 0;
		}
		int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (pixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, vertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, pixelShader);
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

	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}

	public void updateRotationMatrix(float[] matrix)
	{
		mRotationMatrix = matrix;
	}
	ArrayList<Float> alVertix = new ArrayList<Float>();// 存放顶点坐标的ArrayList
	ArrayList<Float> alTextr = new ArrayList<Float>();// 存放纹理坐标左眼
	ArrayList<Float> alTextrR = new ArrayList<Float>();// 存放纹理坐标有眼
	private void drawBall(){
		final int angleSpan = 1;// 将球进行单位切分的角度
		for (int vAngle = -45; vAngle <45; vAngle = vAngle + angleSpan)// 垂直方向angleSpan度一份
		{
			for (int hAngle = 0; hAngle < 80; hAngle = hAngle + angleSpan)// 水平方向angleSpan度一份
			{// 纵向横向各到一个角度后计算对应的此点在球面上的坐标
				float x0 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle)) * Math.cos(Math
						.toRadians(hAngle)));
				float y0 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle)) * Math.sin(Math
						.toRadians(hAngle)));
				float z0 = (float) (r * UNIT_SIZE * Math.sin(Math
						.toRadians(vAngle)));

				float xt0 = (float) (hAngle/160.0f);
				float yt0 = (float)  1.0-((vAngle+45)/90.0f);


				float x1 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle)) * Math.cos(Math
						.toRadians(hAngle + angleSpan)));
				float y1 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle)) * Math.sin(Math
						.toRadians(hAngle + angleSpan)));
				float z1 = (float) (r * UNIT_SIZE * Math.sin(Math
						.toRadians(vAngle)));

				float xt1 = (float) ((hAngle+ angleSpan)/160.0f);
				float yt1 = (float)  1.0-((vAngle+45)/90.0f);

				float x2 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle + angleSpan)) * Math
						.cos(Math.toRadians(hAngle + angleSpan)));
				float y2 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle + angleSpan)) * Math
						.sin(Math.toRadians(hAngle + angleSpan)));
				float z2 = (float) (r * UNIT_SIZE * Math.sin(Math
						.toRadians(vAngle + angleSpan)));

				float xt2 = (float) ((hAngle+angleSpan)/160.0f);
				float yt2 = (float)  1.0-((vAngle+ angleSpan+45)/90.0f);

				float x3 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle + angleSpan)) * Math
						.cos(Math.toRadians(hAngle)));
				float y3 = (float) (r * UNIT_SIZE
						* Math.cos(Math.toRadians(vAngle + angleSpan)) * Math
						.sin(Math.toRadians(hAngle)));
				float z3 = (float) (r * UNIT_SIZE * Math.sin(Math
						.toRadians(vAngle + angleSpan)));

				float xt3 = (float)  ((hAngle)/160.0f);
				float yt3 = (float)  1.0-((vAngle+ angleSpan+45)/90.0f);
				// 将计算出来的XYZ坐标加入存放顶点坐标的ArrayList
				alVertix.add(x1);
				alVertix.add(y1);
				alVertix.add(z1);
				alVertix.add(x3);
				alVertix.add(y3);
				alVertix.add(z3);
				alVertix.add(x0);
				alVertix.add(y0);
				alVertix.add(z0);

				alTextr.add(xt1);
				alTextr.add(yt1);
				alTextr.add(xt3);
				alTextr.add(yt3);
				alTextr.add(xt0);
				alTextr.add(yt0);

				alTextrR.add(xt1+0.5f);
				alTextrR.add(yt1);
				alTextrR.add(xt3+0.5f);
				alTextrR.add(yt3);
				alTextrR.add(xt0+0.5f);
				alTextrR.add(yt0);


				alVertix.add(x1);
				alVertix.add(y1);
				alVertix.add(z1);
				alVertix.add(x2);
				alVertix.add(y2);
				alVertix.add(z2);
				alVertix.add(x3);
				alVertix.add(y3);
				alVertix.add(z3);

				alTextr.add(xt1);
				alTextr.add(yt1);
				alTextr.add(xt2);
				alTextr.add(yt2);
				alTextr.add(xt3);
				alTextr.add(yt3);

				alTextrR.add(xt1+0.5f);
				alTextrR.add(yt1);
				alTextrR.add(xt2+0.5f);
				alTextrR.add(yt2);
				alTextrR.add(xt3+0.5f);
				alTextrR.add(yt3);
			}
		}
		vCount = alVertix.size() / 3;// 顶点的数量为坐标值数量的1/3，因为一个顶点有3个坐标

		// 将alVertix中的坐标值转存到一个float数组中
		float vertices[] = new float[vCount * 3];
		for (int i = 0; i < alVertix.size(); i++) {
			vertices[i] = alVertix.get(i);
		}

		float textures[] = new float[alTextr.size()];
		for (int i = 0; i < alTextr.size(); i++) {
			textures[i] = alTextr.get(i);
		}

		float texturesR[] = new float[alTextrR.size()];
		for (int i = 0; i < alTextrR.size(); i++) {
			texturesR[i] = alTextrR.get(i);
		}

		// 创建顶点坐标数据缓冲
		// vertices.length*4是因为一个整数四个字节
		ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
		vbb.order(ByteOrder.nativeOrder());// 设置字节顺序
		mVertexBuffer = vbb.asFloatBuffer();// 转换为int型缓冲
		mVertexBuffer.put(vertices);// 向缓冲区中放入顶点坐标数据
		mVertexBuffer.position(0);// 设置缓冲区起始位置
		// 特别提示：由于不同平台字节顺序不同数据单元不是字节的一定要经过ByteBuffer
		// 转换，关键是要通过ByteOrder设置nativeOrder()，否则有可能会出问题
		ByteBuffer vbbt = ByteBuffer.allocateDirect(textures.length * 4);
		vbbt.order(ByteOrder.nativeOrder());// 设置字节顺序
		mTextureBuffer = vbbt.asFloatBuffer();// 转换为int型缓冲
		mTextureBuffer.put(textures);// 向缓冲区中放入顶点坐标数据
		mTextureBuffer.position(0);// 设置缓冲区起始位置

		//右眼纹理
		ByteBuffer vbbtR = ByteBuffer.allocateDirect(texturesR.length * 4);
		vbbtR.order(ByteOrder.nativeOrder());// 设置字节顺序
		mTextureBufferR = vbbtR.asFloatBuffer();// 转换为int型缓冲
		mTextureBufferR.put(texturesR);// 向缓冲区中放入顶点坐标数据
		mTextureBufferR.position(0);// 设置缓冲区起始位置
	}
} // End of class VideoRender.

