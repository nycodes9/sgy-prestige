package com.prestige;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.openclove.ovx.OVXCallListener;
import com.openclove.ovx.OVXException;
import com.openclove.ovx.OVXView;

import android.os.Bundle;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity implements ColorPickerDialog.OnColorChangedListener {

	private static final String TAG = HomeActivity.class.getSimpleName();
	private static final String FIREBASE_URL = "https://bunchbot.firebaseio.com/";

    private static final int COLOR_MENU_ID = Menu.FIRST;
    private static final int CLEAR_MENU_ID = Menu.FIRST + 1;
    private static final int VIDEO_MENU_ID = Menu.FIRST + 2;

    private DrawingView drawingView;
    private Firebase ref;
    private ValueEventListener connectedListener;

    FrameLayout firebaseCanvasFL;
    TextView whiteBoardStatusTV;
    
    OVXView ovxView ;
    Button ovxStartCallBtn;
    TextView ovxStatusTV;
    
    
    
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_home1);

        ref = new Firebase(FIREBASE_URL);
        firebaseCanvasFL = (FrameLayout) findViewById(R.id.firebaseCanvasFL);
        drawingView = new DrawingView(this, ref);
        firebaseCanvasFL.addView(drawingView);
        whiteBoardStatusTV = (TextView) findViewById(R.id.whiteBoardStatusTV); 
        
        
        ovxView = OVXView.getOVXContext(this);
        ovxStartCallBtn = (Button) findViewById(R.id.ovxStartBtn);
        ovxStatusTV = (TextView) findViewById(R.id.ovxStatusTV);
        try {
			ovxView.setApiKey("hew5c4pfx2fwks4wtap7v7v3");
			ovxView.setOvxUserId(Secure.getString(this.getContentResolver(), Secure.ANDROID_ID));
			ovxView.setOvxGroupId("ABCD");
			ovxView.setOvxMood("2");
			ovxView.setShowOVXMenuOnTap(true);
			ovxView.setCallListener(new OVXCallListener() {
				
				@Override
				public void ovxReceivedData(String arg0) {
					Log.d(TAG, "ovxReceivedData : " + arg0);
					ovxStartCallBtn.setText("Stop video chat");
					ovxStatusTV.setText("Video data received");
					ovxStatusTV.setBackgroundColor(Color.GREEN);
				}
				
				@Override
				public void callTerminated(String arg0) {
					Log.d(TAG, "callTerminated : " + arg0);
					ovxStartCallBtn.setText("Start video chat");
					ovxStatusTV.setText("Video call terminated");
					ovxStatusTV.setBackgroundColor(Color.RED);
				}
				
				@Override
				public void callStarted() {
					Log.d(TAG, "callStarted : ");
					ovxStartCallBtn.setText("Stop video chat");
					ovxStatusTV.setText("Video call started");
					ovxStatusTV.setBackgroundColor(Color.GREEN);
				}
				
				@Override
				public void callFailed() {
					Log.d(TAG, "callFailed : ");
					ovxStartCallBtn.setText("Start video chat");
					ovxStatusTV.setText("Video call failed");
					ovxStatusTV.setBackgroundColor(Color.RED);
				}
				
				@Override
				public void callEnded() {
					Log.d(TAG, "callEnded : ");
					ovxStartCallBtn.setText("Start video chat");
					ovxStatusTV.setText("Video call ended");
					ovxStatusTV.setBackgroundColor(Color.RED);
				}
			});
		} catch (OVXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        ovxStartCallBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					
					if (ovxView.isCallOn()) {
						ovxView.exitCall();
						ovxStartCallBtn.setText("Start video chat");
					} else {
						ovxView.call();
						ovxStartCallBtn.setText("Stop video chat");
					}
					
					
				} catch (OVXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
        
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set up a notification to let us know when we're connected or disconnected from the Firebase servers
        connectedListener = ref.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean)dataSnapshot.getValue();
                if (connected) {
//                    Toast.makeText(HomeActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                	whiteBoardStatusTV.setText("White board ONLINE");
                	whiteBoardStatusTV.setBackgroundColor(Color.GREEN);
                } else {
//                    Toast.makeText(HomeActivity.this, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
                	whiteBoardStatusTV.setText("White board OFFLINE");
                	whiteBoardStatusTV.setBackgroundColor(Color.RED);
                }
            }

            @Override
            public void onCancelled() {
                // No-op
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up our listener so we don't have it attached twice.
        ref.getRoot().child(".info/connected").removeEventListener(connectedListener);
        drawingView.cleanup();
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	ovxView.exitCall();
		//and free the resources used by OVX context
		ovxView.unregister();		
		android.os.Process.killProcess(android.os.Process.myPid());
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
        menu.add(1, CLEAR_MENU_ID, 0, "Clear").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(2, VIDEO_MENU_ID, 0, "Video Chat").setIcon(R.drawable.ic_video_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	switch (item.getItemId()) {
		case COLOR_MENU_ID:
			new ColorPickerDialog(this, this, 0xFFFF0000).show();
            return true;

		case CLEAR_MENU_ID: 
			drawingView.cleanCanvas();
            
		case VIDEO_MENU_ID: 
			try {
				
				if (ovxView.isCallOn()) {
					ovxView.exitCall();
					ovxStartCallBtn.setText("Start video chat");
				} else {
					ovxView.call();
					ovxStartCallBtn.setText("Stop video chat");
				}
				
				
			} catch (OVXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		default:
			return super.onOptionsItemSelected(item);
		}
    	
    }

    @Override
    public void colorChanged(int newColor) {
        drawingView.setColor(newColor);
    }

}
