package com.eszdman.photoncamera.processing.opengl.postpipeline;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;

import com.eszdman.photoncamera.R;
import com.eszdman.photoncamera.app.PhotonCamera;
import com.eszdman.photoncamera.processing.opengl.GLFormat;
import com.eszdman.photoncamera.processing.opengl.GLTexture;
import com.eszdman.photoncamera.processing.opengl.nodes.Node;
import com.eszdman.photoncamera.processing.opengl.postpipeline.dngprocessor.Histogram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.glReadPixels;

public class Equalization extends Node {
    public Equalization(int rid, String name) {
        super(rid, name);
    }
    private static final float MIN_GAMMA = 0.55f;
    @Override
    public void Compile() {}
    private Histogram Analyze(){
        /*GLTexture r0 = glUtils.interpolate(previousNode.WorkingTexture,new Point(previousNode.WorkingTexture.mSize.x/8,previousNode.WorkingTexture.mSize.x/8));
        GLTexture r1 = glUtils.interpolate(r0,new Point(40,40));
        glUtils.ops(r1,"#import xyztoxyy","XYZtoxyY(in1.rgb),1.0","");*/
        int resize = 16;
        GLTexture r1 = new GLTexture(previousNode.WorkingTexture.mSize.x/resize,
                previousNode.WorkingTexture.mSize.y/resize,previousNode.WorkingTexture.mFormat);
        glProg.useProgram(R.raw.analyze);
        glProg.setTexture("InputBuffer",previousNode.WorkingTexture);
        glProg.setVar("samplingFactor",resize);
        glProg.drawBlocks(r1);
        float [] brArr = new float[r1.mSize.x*r1.mSize.y * 4];
        FloatBuffer fb = ByteBuffer.allocateDirect(brArr.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.mark();
        glReadPixels(0, 0, r1.mSize.x, r1.mSize.y, GL_RGBA, GL_FLOAT, fb.reset());
        fb.get(brArr);
        //Log.d(Name,"brArr:"+ Arrays.toString(brArr));
        r1.close();
        return new Histogram(brArr, r1.mSize.x*r1.mSize.y);
    }
    private float EqualizePower = 0.6f;
    @Override
    public void Run() {
        WorkingTexture = basePipeline.getMain();

        Histogram histParser = Analyze();
        Bitmap lutbm = BitmapFactory.decodeResource(PhotonCamera.getResourcesStatic(), R.drawable.lut2);
        GLTexture histogram = new GLTexture(histParser.hist.length,1,new GLFormat(GLFormat.DataType.FLOAT_16),
                FloatBuffer.wrap(histParser.hist), GL_LINEAR, GL_CLAMP_TO_EDGE);
        Log.d(Name,"hist:"+Arrays.toString(histParser.hist));

        float eq = histParser.gamma;
        Log.d(Name,"Gamma:"+eq);
        float minGamma = Math.min(1f, MIN_GAMMA + 3f * (float) Math.hypot(histParser.sigma[0], histParser.sigma[0]));
        eq = Math.max(minGamma, eq < 1.f ? 0.55f + 0.45f * eq : eq);
        eq = (float) Math.pow(eq, 0.6);
        GLTexture postCurve = new GLTexture(new Point(6,1),new GLFormat(GLFormat.DataType.FLOAT_16),
                FloatBuffer.wrap(new float[]{0f,0.12f,0.4f,0.60f,0.78f,1.f}),GL_LINEAR,GL_CLAMP_TO_EDGE);
        Log.d(Name,"Equalizek:"+eq);
        glProg.useProgram(R.raw.equalize);
        glProg.setVar("Equalize",eq);
        glProg.setTexture("Histogram",histogram);
        float bilatHistFactor = Math.max(0.4f, 1f - histParser.gamma * EqualizePower
                - 4f * (float) Math.hypot(histParser.sigma[0], histParser.sigma[1]));
        Log.d(Name,"HistFactor:"+bilatHistFactor*EqualizePower);
        glProg.setVar("HistFactor",bilatHistFactor*EqualizePower);
        glProg.setVar("histOffset", 0.5f, 1.f - 1.f / histParser.hist.length);
        glProg.setTexture("PostCurve",postCurve);
        glProg.setTexture("InputBuffer",previousNode.WorkingTexture);
        glProg.drawBlocks(WorkingTexture);
        postCurve.close();
        histogram.close();
        lutbm.recycle();
        glProg.closed = true;
    }
}
