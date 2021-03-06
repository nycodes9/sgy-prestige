package com.prestige;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ProfileActivity extends Activity {

	Button teacherBtn, studentBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_profile);
		
		teacherBtn = (Button) findViewById(R.id.teacherBtn);
		studentBtn = (Button) findViewById(R.id.studentbtn);
		
		teacherBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getBaseContext(), HomeActivity.class);
				i.putExtra("PROFILE_KEY", 0);
				startActivity(i);
			}
		});
		
		studentBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(getBaseContext(), HomeActivity.class);
				i.putExtra("PROFILE_KEY", 1);
				startActivity(i);
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.profile, menu);
		return true;
	}

}
