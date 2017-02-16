package com.chatchat.textures;

/**
 * Created by whitelegg_n on 09/03/2016.
 */



import java.nio.FloatBuffer;



import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import java.nio.ByteOrder;


public class GLRect {

    FloatBuffer vertexBuffer;
    ShortBuffer indexBuffer;

    short[] indices;
    float[] colour;

    public GLRect(float[] vertices, float[] colour)
    {
        ByteBuffer buf = ByteBuffer.allocateDirect(12*4);
        buf.order(ByteOrder.nativeOrder());
        vertexBuffer = buf.asFloatBuffer();
        ByteBuffer ibuf = ByteBuffer.allocateDirect(6*2);
        ibuf.order(ByteOrder.nativeOrder());
        indexBuffer = ibuf.asShortBuffer();

        indices = new short[]  {0,1,2,2,3,0};


        vertexBuffer.put(vertices);
        indexBuffer.put(indices);
        vertexBuffer.position(0);
        indexBuffer.position(0);

        this.colour=colour;
    }

    public void draw(GPUInterface gpu)
    {
        if(colour!=null)
            gpu.setUniform4fv("uColour", colour);
        gpu.drawIndexedBufferedData(vertexBuffer, indexBuffer, 12, "aVertex");


    }

}