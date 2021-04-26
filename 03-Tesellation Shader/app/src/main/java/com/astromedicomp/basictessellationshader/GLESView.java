package com.astromedicomp.basictessellationshader;

import android.content.Context; 
import android.opengl.GLSurfaceView; 
import javax.microedition.khronos.opengles.GL10; 
import javax.microedition.khronos.egl.EGLConfig; 
import android.opengl.GLES31; 
import android.opengl.GLES32; 
import android.view.MotionEvent; 
import android.view.GestureDetector; 
import android.view.GestureDetector.OnGestureListener; 
import android.view.GestureDetector.OnDoubleTapListener; 
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import android.opengl.Matrix; 

public class GLESView extends GLSurfaceView implements GLSurfaceView.Renderer, OnGestureListener, OnDoubleTapListener
{
    private final Context context;
    
    private GestureDetector gestureDetector;
    
    private int vertexShaderObject;
    private int fragmentShaderObject;
    private int shaderProgramObject;
    private float perspectiveProjectionMatrix[]=new float[16];     

    private int tessellationControlShaderObject;
    private int tessellationEvaluationShaderObject;
    private int vao[]=new int[1];
    private int vbo[]=new int[1];
    private int numberOfSegmentsUniform;
    private int numberOfStripsUniform;
    private int numberOfLineSegments;
    private int lineColorUniform;
 
    private int MVPUniform;


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
  /*      numberOfLineSegments++;
        if(numberOfLineSegments >= 50)
        {
            numberOfLineSegments=50;
        }
*/

        numberOfLineSegments--;
        if(numberOfLineSegments<=0)
        {
            numberOfLineSegments=1;
        }
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
        numberOfLineSegments++;
        if(numberOfLineSegments >= 50)
        {
            numberOfLineSegments=50;
        }


        
        /*
        numberOfLineSegments--;
        if(numberOfLineSegments<=0)
        {
            numberOfLineSegments=1;
        }
*/
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
        vertexShaderObject=GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER);
        
        final String vertexShaderSourceCode =String.format
        (
         "#version 310 es"+
         "\n"+
         "in vec2 vPosition;"+
         "void main(void)"+
         "{"+
           "gl_Position=vec4(vPosition,0.0,1.0);"+
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

        System.out.println("SSK: Vertex Shader Successfullly compiled");
        tessellationControlShaderObject=GLES31.glCreateShader(GLES32.GL_TESS_CONTROL_SHADER);

        final String tessellationControlShaderSourceCode = String.format
        (
            "#version 310 es"+
            "\n"+
            "layout(vertices=4)out;"+
            "uniform int numberOfSegments;"+
            "uniform int numberOfStrips;"+
            "void main(void)"+
            "{"+
            "gl_out[gl_InvocationID].gl_Position= gl_in[gl_InvocationID].gl_Position;"+
            "gl_TessLevelOuter[0] =float(numberOfStrips);"+
            "gl_TessLevelOuter[1] =float(numberOfSegments);"+ 
            "}"
        );

        GLES31.glShaderSource(tessellationControlShaderObject,tessellationControlShaderSourceCode);

        GLES31.glCompileShader(tessellationControlShaderObject);
        iShaderCompiledStatus[0]=0;
        iInfoLogLength[0]=0;
        szInfoLog=null;
        GLES31.glGetShaderiv(tessellationControlShaderObject,GLES31.GL_COMPILE_STATUS,iShaderCompiledStatus,0);
        if(iShaderCompiledStatus[0]==GLES31.GL_FALSE)
        {
            GLES31.glGetShaderiv(tessellationControlShaderObject,GLES31.GL_INFO_LOG_LENGTH,iInfoLogLength,0);
            if(iInfoLogLength[0] > 0)
            {
                szInfoLog= GLES31.glGetShaderInfoLog(tessellationControlShaderObject);
                System.out.println("SSK: Tessellation Control Shader Compilation Log = "+szInfoLog);
                uninitialize();
                System.exit(0);
            }
        }

        System.out.println("SSK: Tesellation Control Shader Successfullly compiled");
        tessellationEvaluationShaderObject=GLES31.glCreateShader(GLES32.GL_TESS_EVALUATION_SHADER);

        final String tessellationEvaluationShaderSourceCode = String.format
        (
            "#version 310 es"+
            "\n"+
            "layout(isolines)in;"+
            "uniform mat4 u_mvp_matrix;"+
            "void main(void)"+
            "{"+
            "float u=gl_TessCoord.x;"+
            "vec3 p0 = gl_in[0].gl_Position.xyz;"+
            "vec3 p1 = gl_in[1].gl_Position.xyz;"+
            "vec3 p2 = gl_in[2].gl_Position.xyz;"+
            "vec3 p3 = gl_in[3].gl_Position.xyz;"+
            "float u1 = (1.0 - u);"+
            "float u2 = u * u;"+
            "float b3 = u2 * u;"+
            "float b2 = 3.0 * u2 *u1;"+
            "float b1 = 3.0 * u * u1 * u1;"+
            "float b0 = u1 * u1 * u1;"+
            "vec3 p= p0 * b0 + p1 * b1 + p2 * b2 + p3 * b3;"+
            "gl_Position = u_mvp_matrix * vec4(p,1.0);"+
            "}"
        );

        GLES31.glShaderSource(tessellationEvaluationShaderObject,tessellationEvaluationShaderSourceCode);

        GLES31.glCompileShader(tessellationEvaluationShaderObject);
        iShaderCompiledStatus[0]=0;
        iInfoLogLength[0]=0;
        szInfoLog=null;
        GLES31.glGetShaderiv(tessellationEvaluationShaderObject,GLES31.GL_COMPILE_STATUS,iShaderCompiledStatus,0);
        if(iShaderCompiledStatus[0]==GLES31.GL_FALSE)
        {
            GLES31.glGetShaderiv(tessellationEvaluationShaderObject,GLES31.GL_INFO_LOG_LENGTH,iInfoLogLength,0);
            if(iInfoLogLength[0] > 0)
            {
                szInfoLog= GLES31.glGetShaderInfoLog(tessellationEvaluationShaderObject);
                System.out.println("SSK: Tessellation Evaluation Shader Compilation Log = "+szInfoLog);
                uninitialize();
                System.exit(0);
            }
        }

        System.out.println("SSK: Tesellation Evaaluation Shader Successfullly compiled");
        fragmentShaderObject=GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER);
        
        final String fragmentShaderSourceCode =String.format
        (
         "#version 310 es"+
         "\n"+
         "precision highp float;"+
         "uniform vec4 lineColor;"+
         "out vec4 FragColor;"+
         "void main(void)"+
         "{"+
         "FragColor = lineColor;"+
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

        System.out.println("SSK: Fragment Shader Successfullly compiled");
        shaderProgramObject=GLES31.glCreateProgram();
        
        GLES31.glAttachShader(shaderProgramObject,vertexShaderObject);
        
        GLES31.glAttachShader(shaderProgramObject,tessellationControlShaderObject);

        GLES31.glAttachShader(shaderProgramObject,tessellationEvaluationShaderObject);

        GLES31.glAttachShader(shaderProgramObject,fragmentShaderObject);
        
        GLES31.glBindAttribLocation(shaderProgramObject,GLESMacros.SSK_ATTRIBUTE_VERTEX,"vPosition");

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

        MVPUniform = GLES31.glGetUniformLocation(shaderProgramObject, "u_mvp_matrix");
        
        numberOfSegmentsUniform = GLES31.glGetUniformLocation(shaderProgramObject,"numberOfSegments");

        numberOfStripsUniform = GLES31.glGetUniformLocation(shaderProgramObject,"numberOfStrips");

        lineColorUniform = GLES31.glGetUniformLocation(shaderProgramObject,"lineColor");

        float vertices[] = new float[8];
        //vertices[] = { -1.0f, -1.0f, -0.5f, 1.0f, 0.5f, -1.0f, 1.0f, 1.0f };

        vertices[0]=-1.0f;
        vertices[1]=-1.0f;

        vertices[2]=-0.5f;
        vertices[3]=1.0f;

        vertices[4]=0.5f;
        vertices[5]=-1.0f;

        vertices[6]=1.0f;
        vertices[7]=1.0f;

        
        System.out.println("SSK: get Uniform Successfull ");
        
        GLES31.glGenVertexArrays(1,vao,0);
        GLES31.glBindVertexArray(vao[0]);
        
        GLES31.glGenBuffers(1,vbo,0);
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,vbo[0]);
        
        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer verticesBuffer=byteBuffer.asFloatBuffer();
        verticesBuffer.put(vertices);
        verticesBuffer.position(0);
        
        GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER,
                            vertices.length * 4,
                            verticesBuffer,
                            GLES31.GL_STATIC_DRAW);
        
        GLES31.glVertexAttribPointer(GLESMacros.SSK_ATTRIBUTE_VERTEX,
                                     2,
                                     GLES31.GL_FLOAT,
                                     false,0,0);
        
        GLES31.glEnableVertexAttribArray(GLESMacros.SSK_ATTRIBUTE_VERTEX);
        
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER,0);
        
        GLES31.glBindVertexArray(0);

        System.out.println("SSK: Vao and VBo  Successfull ");

  //    GLES31.glClearDepth(1.0f);
  //    GLES31.glHint(GLES31.GL_PERSPECTIVE_CORRECTION_HINT,GLES31.GL_NICEST);
        GLES31.glEnable(GLES31.GL_DEPTH_TEST);
        GLES31.glDepthFunc(GLES31.GL_LEQUAL);
        GLES31.glEnable(GLES31.GL_CULL_FACE);
        GLES31.glLineWidth(3.0f);
        
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); 
        
        numberOfLineSegments=1;
        Matrix.setIdentityM(perspectiveProjectionMatrix,0);
    }
 

    private void resize(int width, int height)
    {
        GLES31.glViewport(0, 0, width, height);
        
        Matrix.perspectiveM(perspectiveProjectionMatrix,0,45.0f,(float)width/(float)height,0.1f,100.0f); // typecasting is IMP
    }
    
    public void display()
    {
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT | GLES31.GL_DEPTH_BUFFER_BIT
                        | GLES31.GL_STENCIL_BUFFER_BIT);
        
        GLES31.glUseProgram(shaderProgramObject);
        float modelViewMatrix[]=new float[16];
        float modelViewProjectionMatrix[]=new float[16];
        
        float lineCo[]=new float[4];
        lineCo[0]=1.0f;
        lineCo[1]=1.0f;
        lineCo[2]=0.0f;
        lineCo[3]=1.0f;

        // set modelMatrix and viewMatrix matrices to identity matrix
        Matrix.setIdentityM(modelViewMatrix,0);
        Matrix.setIdentityM(modelViewProjectionMatrix,0);

        // apply z axis translation to go deep into the screen by -1.5,
        // so that pyramid with same fullscreen co-ordinates, but due to above translation will look small
        Matrix.translateM(modelViewMatrix,0,0.5f,0.5f,-3.0f);
        
		Matrix.multiplyMM(modelViewProjectionMatrix,0,perspectiveProjectionMatrix,0,modelViewMatrix,0);

        GLES31.glUniformMatrix4fv(MVPUniform,1,false,modelViewProjectionMatrix,0);
        
        GLES31.glUniform1i(numberOfSegmentsUniform,numberOfLineSegments);
    
        GLES31.glUniform1i(numberOfStripsUniform,1);

        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(lineCo.length * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer verticesBuffer=byteBuffer.asFloatBuffer();
        verticesBuffer.put(lineCo);
        verticesBuffer.position(0);
    
    
        GLES31.glUniform4fv(lineColorUniform,1,verticesBuffer);
        
        // bind vao
        GLES31.glBindVertexArray(vao[0]);
        
        GLES31.glDrawArrays(GLES32.GL_PATCHES, 0, 4);
        
        GLES31.glBindVertexArray(0);

        GLES31.glUseProgram(0);
        
        requestRender();
    }


    void uninitialize()
    {

        if(vao[0] != 0)
        {
            GLES31.glDeleteVertexArrays(1, vao, 0);
            vao[0]=0;
        }
        
        if(vbo[0] != 0)
        {
            GLES31.glDeleteBuffers(1, vbo, 0);
            vbo[0]=0;
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

            if(tessellationControlShaderObject !=0)
            {
                GLES31.glDetachShader(shaderProgramObject,tessellationControlShaderObject);
                GLES31.glDeleteShader(tessellationControlShaderObject);
                tessellationControlShaderObject=0;
            }

            if(tessellationEvaluationShaderObject !=0)
            {
                GLES31.glDetachShader(shaderProgramObject,tessellationEvaluationShaderObject);
                GLES31.glDeleteShader(tessellationEvaluationShaderObject);
                tessellationEvaluationShaderObject=0;
            }
        }

        if(shaderProgramObject != 0)
        {
            GLES31.glDeleteProgram(shaderProgramObject);
            shaderProgramObject = 0;
        }
    }
}