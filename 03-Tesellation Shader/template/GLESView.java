package com.astromedicomp.threelightssphere;

import android.content.Context; 
import android.opengl.GLSurfaceView; 
import javax.microedition.khronos.opengles.GL10; 
import javax.microedition.khronos.egl.EGLConfig; 
import android.opengl.GLES31; 
import android.view.MotionEvent; 
import android.view.GestureDetector; 
import android.view.GestureDetector.OnGestureListener; 
import android.view.GestureDetector.OnDoubleTapListener; 
import android.graphics.BitmapFactory; 
import android.graphics.Bitmap; 
import android.opengl.GLUtils; 

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.opengl.Matrix; // for Matrix math
import java.lang.Math; // for sqrt()
import java.util.Random;

public class GLESView extends GLSurfaceView implements GLSurfaceView.Renderer, OnGestureListener, OnDoubleTapListener
{
    private final Context context;
    
    private GestureDetector gestureDetector;
    
    private int vertexShaderObject;
    private int fragmentShaderObject;
    private int shaderProgramObject;
    
    private int[] vao = new int[1];
    private int[] vbo_position = new int[1];
    private int[] vbo_color = new int[1];
    private int[] vbo_velocity = new int[1];
    private int[] vbo_start_time = new int[1];

    private int  gMVPUniform;
   
    private int doubleTapUniform;

    private float perspectiveProjectionMatrix[]=new float[16]; 

    private int doubleTap; 

    private int RAND_MAX=100000;
    private float particleTime=0.1f;
    private float angle=0.1f;
    private int color1;
    private int color2;
    private int color3;
    private int location;

    static int arrayWidth;
    static int arrayHeight;

    private float verts[] = new float [120000];//100*100*3*4
    private float colors[] = new float [120000];//100*100*3*4
    private float velocities[] = new float[120000];//100*100*3*4
    private float startTimes[] = new float[30000];//100*100*3=

    float vptr[]=new float[120000];
    float cptr[]=new float[120000];
    float velptr[]=new float[120000];
    float stptr[]=new float[30000];

    float zzz=0.0f;
    String Error="hello";


    public GLESView(Context drawingContext)
    {
        super(drawingContext);
        
        context=drawingContext;

        setEGLContextClientVersion(3);

        setRenderer(this);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        gestureDetector = new GestureDetector(context, this, null, false);
        gestureDetector.setOnDoubleTapListener(this);
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        String glesVersion = gl.glGetString(GL10.GL_VERSION);
        System.out.println("SSK: OpenGL-ES Version = "+glesVersion);
        String glslVersion=gl.glGetString(GLES31.GL_SHADING_LANGUAGE_VERSION);
        System.out.println("SSK: GLSL Version = "+glslVersion);


        initialize(gl);
    }
 
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height)
    {
        resize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused)
    {
        display();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        int eventaction = e.getAction();
        if(!gestureDetector.onTouchEvent(e))
            super.onTouchEvent(e);
        return(true);
    }
    
    @Override
    public boolean onDoubleTap(MotionEvent e)
    {
//        doubleTap++;
  //      if(doubleTap > 3)
    //        doubleTap=0;
        return(true);
    }
    
    @Override
    public boolean onDoubleTapEvent(MotionEvent e)
    {
        return(true);
    }
    
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e)
    {
        return(true);
    }
    
    @Override
    public boolean onDown(MotionEvent e)
    {
        return(true);
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        return(true);
    }
    
    @Override
    public void onLongPress(MotionEvent e)
    {
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        uninitialize();
        System.exit(0);
        return(true);
    }
    
    @Override
    public void onShowPress(MotionEvent e)
    {
    }
    
    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        return(true);
    }

    private void initialize(GL10 gl)
    {
        createPoints(100,100);
        vertexShaderObject=GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        
        final String vertexShaderSourceCode =String.format
        (
         "#version 310 es"+
         "\n"+
         "uniform float Time;"+
         "uniform vec4 Background;"+
         "uniform mat4 MVPMatrix;"+
         "in vec4 MCVertex;"+
         "in vec4 MColor;"+
         "in vec3 Velocity;"+
         "out vec3 ChangeVelocity;"+
         "in float StartTime;"+
         "out vec4 Color;"+
         "void main(void)"+
         "{"+
            "vec4 vert;"+
            "float t=Time-StartTime;"+
            "if(t >= 0.0)"+
            "{"+
                "vert=MCVertex + vec4(Velocity*t,0.0);"+
                "vert.z=9.8*t*t;"+
                "Color=MColor;"+
                "ChangeVelocity=Velocity;"+
            "}"+
            "else"+
            "{"+
                "vert=MCVertex;"+
                "Color=Background;"+
                "ChangeVelocity=Velocity;"+
            "}"+
            "gl_Position=MVPMatrix*vert;"+
            "gl_PointSize=2.0;"+
        "}"
        );

        GLES31.glShaderSource(vertexShaderObject,vertexShaderSourceCode);
        
        GLES31.glCompileShader(vertexShaderObject);
        int[] iShaderCompiledStatus = new int[1];
        int[] iInfoLogLength = new int[1];
        String szInfoLog=null;
        GLES31.glGetShaderiv(vertexShaderObject, GLES31.GL_COMPILE_STATUS, iShaderCompiledStatus, 0);
        if (iShaderCompiledStatus[0] == GLES31.GL_FALSE)
        {
            GLES31.glGetShaderiv(vertexShaderObject, GLES31.GL_INFO_LOG_LENGTH, iInfoLogLength, 0);
            if (iInfoLogLength[0] > 0)
            {
                szInfoLog = GLES31.glGetShaderInfoLog(vertexShaderObject);
                System.out.println("SSK: Vertex Shader Compilation Log = "+szInfoLog);
                uninitialize();
                System.exit(0);
           }
        }

        fragmentShaderObject=GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        
        final String fragmentShaderSourceCode =String.format
        (
         "#version 310 es"+
         "\n"+
         "precision highp float;"+
         "in vec4 Color;"+
         "in vec3 ChangeVelocity;"+
         "out vec4 out_Color;"+
         "void main(void)"+
         "{"+
            "out_Color=Color;"+
         "}"
        );


        GLES31.glShaderSource(fragmentShaderObject,fragmentShaderSourceCode);
        
        GLES31.glCompileShader(fragmentShaderObject);
        iShaderCompiledStatus[0] = 0; // re-initialize
        iInfoLogLength[0] = 0; // re-initialize
        szInfoLog=null; // re-initialize
        GLES31.glGetShaderiv(fragmentShaderObject, GLES31.GL_COMPILE_STATUS, iShaderCompiledStatus, 0);
        if (iShaderCompiledStatus[0] == GLES31.GL_FALSE)
        {
            GLES31.glGetShaderiv(fragmentShaderObject, GLES31.GL_INFO_LOG_LENGTH, iInfoLogLength, 0);
            if (iInfoLogLength[0] > 0)
            {
                szInfoLog = GLES31.glGetShaderInfoLog(fragmentShaderObject);
                System.out.println("SSK: Fragment Shader Compilation Log = "+szInfoLog);
                uninitialize();
                System.exit(0);
            }
        }

    
        shaderProgramObject=GLES31.glCreateProgram();
        
        GLES31.glAttachShader(shaderProgramObject,vertexShaderObject);
        
        GLES31.glAttachShader(shaderProgramObject,fragmentShaderObject);
        
        GLES31.glBindAttribLocation(shaderProgramObject,GLESMacros.SSK_ATTRIBUTE_VERTEX,"MCVertex");
        GLES31.glBindAttribLocation(shaderProgramObject,GLESMacros.SSK_ATTRIBUTE_COLOR,"MColor");
        GLES31.glBindAttribLocation(shaderProgramObject,GLESMacros.SSK_ATTRIBUTE_VELOCITY,"Velocity");
        GLES31.glBindAttribLocation(shaderProgramObject,GLESMacros.SSK_ATTRIBUTE_START_TIME,"StartTime");

        GLES31.glLinkProgram(shaderProgramObject);
        int[] iShaderProgramLinkStatus = new int[1];
        iInfoLogLength[0] = 0; // re-initialize
        szInfoLog=null; // re-initialize
        GLES31.glGetProgramiv(shaderProgramObject, GLES31.GL_LINK_STATUS, iShaderProgramLinkStatus, 0);
        if (iShaderProgramLinkStatus[0] == GLES31.GL_FALSE)
        {
            GLES31.glGetProgramiv(shaderProgramObject, GLES31.GL_INFO_LOG_LENGTH, iInfoLogLength, 0);
            if (iInfoLogLength[0] > 0)
            {
                szInfoLog = GLES31.glGetProgramInfoLog(shaderProgramObject);
                System.out.println("SSK: Shader Program Link Log = "+szInfoLog);
                uninitialize();
                System.exit(0);
            }
        }

        gMVPUniform = GLES31.glGetUniformLocation(shaderProgramObject, "MVPMatrix");
        location = GLES31.glGetUniformLocation(shaderProgramObject, "Time");


//        GLES31.glPointSize(2.0f);        
        GLES31.glGenVertexArrays(1,vao,0);
        GLES31.glBindVertexArray(vao[0]);
        
        GLES31.glGenBuffers(1,vbo_position,0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,vbo_position[0]);
  
        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(verts.length * 4);      
//        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(verts.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer mybuff=byteBuffer.asFloatBuffer();
        mybuff.put(verts);
        mybuff.position(0);


        for(int ii=0;ii<verts.length;ii++)
        {
            vptr[ii]=verts[ii];
        }
        
        
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,
                            120000,
                            mybuff,
                            GLES31.GL_STATIC_DRAW);
        
        GLES31.glVertexAttribPointer(GLESMacros.SSK_ATTRIBUTE_VERTEX,
                                     3,
                                     GLES31.GL_FLOAT,
                                     false,0,0);
        
        GLES31.glEnableVertexAttribArray(GLESMacros.SSK_ATTRIBUTE_VERTEX);
        
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,0);




        GLES31.glGenBuffers(1,vbo_color,0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,vbo_color[0]);
        
        byteBuffer=ByteBuffer.allocateDirect(colors.length*4);
        //byteBuffer=ByteBuffer.allocateDirect(colors.length*4);
        byteBuffer.order(ByteOrder.nativeOrder());
        mybuff=byteBuffer.asFloatBuffer();
        mybuff.put(colors);
        mybuff.position(0);
        for(int ii=0;ii<colors.length;ii++)
        {
            cptr[ii]=colors[ii];
        }

        
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,
                            120000,
                            mybuff,
                            GLES31.GL_STATIC_DRAW);
        
        GLES31.glVertexAttribPointer(GLESMacros.SSK_ATTRIBUTE_COLOR,
                                     3,
                                     GLES31.GL_FLOAT,
                                     false,0,0);
        
        GLES31.glEnableVertexAttribArray(GLESMacros.SSK_ATTRIBUTE_COLOR);
        
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,0);


        GLES31.glGenBuffers(1,vbo_velocity,0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,vbo_velocity[0]);
        
        //byteBuffer=ByteBuffer.allocateDirect(velocities.length*4);
        byteBuffer=ByteBuffer.allocateDirect(velocities.length*4);
        byteBuffer.order(ByteOrder.nativeOrder());
        mybuff=byteBuffer.asFloatBuffer();
        mybuff.put(velocities);
        mybuff.position(0);

        for(int ii=0;ii<velocities.length;ii++)
        {
            velptr[ii]=velocities[ii];
        }

        
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,
                            120000,
                            mybuff,
                            GLES31.GL_STATIC_DRAW);
        
        GLES31.glVertexAttribPointer(GLESMacros.SSK_ATTRIBUTE_VELOCITY,
                                     3,
                                     GLES31.GL_FLOAT,
                                     false,0,0);
        
        GLES31.glEnableVertexAttribArray(GLESMacros.SSK_ATTRIBUTE_VELOCITY);
        
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,0);


        GLES31.glGenBuffers(1,vbo_start_time,0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,vbo_start_time[0]);
        
        byteBuffer=ByteBuffer.allocateDirect(startTimes.length*4);      
//        byteBuffer=ByteBuffer.allocateDirect(startTimes.length*4);
        byteBuffer.order(ByteOrder.nativeOrder());
        mybuff=byteBuffer.asFloatBuffer();
        mybuff.put(startTimes);
        mybuff.position(0);
        

        for(int ii=0;ii<startTimes.length;ii++)
        {
            stptr[ii]=startTimes[ii];
        }

        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,
                            30000,
                            mybuff,
                            GLES31.GL_STATIC_DRAW);
        
        GLES31.glVertexAttribPointer(GLESMacros.SSK_ATTRIBUTE_START_TIME,
                                     1,
                                     GLES31.GL_FLOAT,
                                     false,0,0);
        
        GLES31.glEnableVertexAttribArray(GLESMacros.SSK_ATTRIBUTE_START_TIME);
        
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,0);

        GLES31.glBindVertexArray(0);


        GLES31.glEnable(GLES31.GL_DEPTH_TEST);
        GLES31.glDepthFunc(GLES31.GL_LEQUAL);
        GLES31.glEnable(GLES31.GL_CULL_FACE);
        
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); 
        
        doubleTap=0;
        
        Matrix.setIdentityM(perspectiveProjectionMatrix,0);
    }
 

    private void resize(int width, int height)
    {
        GLES31.glViewport(0, 0, width, height);
        
        Matrix.perspectiveM(perspectiveProjectionMatrix,0,45.0f,(float)width/(float)height,0.1f,100.0f); // typecasting is IMP
    }
    
    public void display()
    {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT);
        
        GLES31.glUseProgram(shaderProgramObject);

        float modelViewMatrix[]=new float[16];
        float modelViewProjectionMatrix[]=new float[16];
        float tempMatrix[]=new float[16];

        Matrix.setIdentityM(modelViewMatrix,0);
        Matrix.setIdentityM(modelViewProjectionMatrix,0);
        Matrix.setIdentityM(tempMatrix,0);

        Matrix.translateM(modelViewMatrix,0,0.0f,0.0f,-30.0f);
/*        Matrix.multiplyMM(tempMatrix,0,perspectiveProjectionMatrix,0,modelViewMatrix,0);
        Matrix.multiplyMM(modelViewProjectionMatrix,0,modelViewProjectionMatrix,0,tempMatrix,0);

*/
        Matrix.multiplyMM(modelViewProjectionMatrix,0,perspectiveProjectionMatrix,0,modelViewMatrix,0);

        GLES31.glUniformMatrix4fv(gMVPUniform,1,false,modelViewProjectionMatrix,0);
        GLES31.glUniform1f(location,particleTime);

        GLES31.glBindVertexArray(vao[0]);
            GLES31.glDrawArrays(GLES31.GL_POINTS, 0, arrayWidth*arrayHeight);
        GLES31.glBindVertexArray(0);

        GLES31.glUseProgram(0);
        
        particleTime=particleTime +0.001f;   
        requestRender();
    }



    void uninitialize()
    {

        if(vao[0] != 0)
        {
            GLES31.glDeleteVertexArrays(1, vao, 0);
            vao[0]=0;
        }
        
        if(vbo_color[0] != 0)
        {
            GLES31.glDeleteBuffers(1, vbo_color, 0);
            vbo_color[0]=0;
        }
        
        if(vbo_velocity[0] != 0)
        {
            GLES31.glDeleteBuffers(1, vbo_velocity, 0);
            vbo_velocity[0]=0;
        }
        
        if(vbo_start_time[0] != 0)
        {
            GLES31.glDeleteBuffers(1, vbo_start_time, 0);
            vbo_start_time[0]=0;
        }

        if(shaderProgramObject != 0)
        {
            if(vertexShaderObject != 0)
            {
                GLES31.glDetachShader(shaderProgramObject, vertexShaderObject);
                GLES31.glDeleteShader(vertexShaderObject);
                vertexShaderObject = 0;
            }
            
            if(fragmentShaderObject != 0)
            {
                GLES31.glDetachShader(shaderProgramObject, fragmentShaderObject);
                GLES31.glDeleteShader(fragmentShaderObject);
                fragmentShaderObject = 0;
            }
        }

        if(shaderProgramObject != 0)
        {
            GLES31.glDeleteProgram(shaderProgramObject);
            shaderProgramObject = 0;
        }
    }

    void createPoints(int w, int h)
    {
    //100, 100

    
        float i, j;
        float r=0.1f;

/*        verts = new float [120000];//100*100*3*4
        colors = new float [120000];//100*100*3*4
        velocities = new float[120000];//100*100*3*4
        startTimes = new float[30000];//100*100*3=
 */


        float move=0.1f;
        int k=0,l=0,m=0,n=0;
        for (i = 0.5f / w - 0.5f; i < 0.5f; i = i + 1.0f/w)
        {
            for (j = 0.5f / h - 0.5f; j < 0.5f; j = j + 1.0f/h)
            {
/*                vptr[k]   = (i+ ((float) Math.cos(angle)));
                vptr[k+1] = (i+ ((float) Math.sin(angle)));
                vptr[k+2] = 0;
                k=k+3;

                if(i<=0.20f)
                {
                    cptr[l] =  1.0f;
                    cptr[l+1] =  0.0f;
                    cptr[l+2] =  0.0f;
                    l=l+3;
                }
                else if(i>=0.500f)
                {
                    cptr[l]=  0.0f;
                    cptr[l+1] =  1.0f;
                    cptr[l+2] =  0.0f;
                    l=l+3;
                }
                else
                {
                    cptr[l] =  1.0f;
                    cptr[l+1] =  1.0f;
                    cptr[l+2] =  1.0f;
                    l=l+3;
                }

                velptr[m]       = (((float)Math.cos(angle)) * 10.0f);
                velptr[m+1]     = (((float)Math.sin(angle)) * 10.0f);
                velptr[m+2]     = 0.0f;
                m=m+3;

                stptr[n]  = ((float) (rand() / RAND_MAX))*10.0f;
    //          stptr[n]    =stptr[n]+1;
                n=n+3;
*/

                verts[k]   = (i+ ((float) Math.cos(angle)));
                verts[k+1] = (i+ ((float) Math.sin(angle)));
                verts[k+2] = 0;
                k=k+2;

                if(i<=0.20f)
                {
                    colors[l] =  1.0f;
                    colors[l+1] =  0.0f;
                    colors[l+2] =  0.0f;
                    l=l+3;
                }
                else if(i>=0.500f)
                {
                    colors[l]=  0.0f;
                    colors[l+1] =  1.0f;
                    colors[l+2] =  0.0f;
                    l=l+3;
                }
                else
                {
                    colors[l] =  1.0f;
                    colors[l+1] =  1.0f;
                    colors[l+2] =  1.0f;
                    l=l+3;
                }

                velocities[m]       = (((float)Math.cos(angle)) * 10.0f);
                velocities[m+1]     = (((float)Math.sin(angle)) * 10.0f);
                velocities[m+2]     = 0.0f;
                m=m+3;

                startTimes[n]  = ((float) (rand() / RAND_MAX))*10.0f;
//              stptr[n]    =stptr[n]+1;
                n=n+3;

                angle=angle+180.0f;
                //r=r+0.1f;     
            }
        }
        arrayWidth = w;
        arrayHeight = h;
    }

    float rand()
    {
        Random random=new Random();
        return (random.nextFloat());
    }
}
