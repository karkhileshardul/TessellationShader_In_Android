package com.astromedicomp.threelightssphere;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.content.pm.ActivityInfo;

public class MainActivity extends Activity {
	private GLESView glesView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        						WindowManager.LayoutParams.FLAG_FULLSCREEN);
        MainActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        glesView=new GLESView(this);
        setContentView(glesView);
    }
  @Override
  protected void onPause(){
  	super.onPause();
  }
  @Override
  protected void onResume(){
  	super.onResume();
  }
}
