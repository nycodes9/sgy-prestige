package com.prestige;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.openclove.ovx.OVXCallListener;
import com.openclove.ovx.OVXException;
import com.openclove.ovx.OVXView;
import com.prestige.metrics.Badge;
import com.prestige.metrics.CompetencyProgress;

import android.os.Bundle;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity implements ColorPickerDialog.OnColorChangedListener {

	private static final String TAG = HomeActivity.class.getSimpleName();
	private static final String FIREBASE_URL_WHITEBOARD = "https://bunchbot.firebaseio.com/whiteboard";
	private static final String FIREBASE_URL_COMPETENCY = "https://bunchbot.firebaseio.com/competency";
	private static final String FIREBASE_URL_BADGES = "https://bunchbot.firebaseio.com/badges";

	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int CLEAR_MENU_ID = Menu.FIRST + 1;
	private static final int VIDEO_MENU_ID = Menu.FIRST + 2;

	private DrawingView drawingView;
	private Firebase firebaseRefWB;
	private Firebase firebaseRefCompetency;
	private Firebase firebaseRefBadges;
	private ValueEventListener connectedListener;

	LinearLayout firebaseCanvasFL;
	TextView whiteBoardStatusTV ;

	OVXView ovxView;
	Button ovxStartCallBtn;
	TextView ovxStatusTV;

	ExpandableListView competencyMetricLV;

	Map<Integer, String> groupData = new HashMap<Integer, String>();
	Map<String, String> groupDescMap = new HashMap<String, String>();

	Map<Integer, Map<Integer, String>> childData = new HashMap<Integer, Map<Integer, String>>();
	Map<String, String> childLevelMap = new HashMap<String, String>();

	boolean isTeacher = true;
	
	ImageView badge1, badge2, badge3;
	
	ProgressBar competencyProgressPB;
	
	int progress=0;
	
	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_home1);

		isTeacher = getIntent().getIntExtra("PROFILE_KEY", 0) == 0;
		Log.d(TAG, "Is teacher ? : " + isTeacher);
		

		firebaseRefCompetency = new Firebase(FIREBASE_URL_COMPETENCY);
		competencyProgressPB = (ProgressBar) findViewById(R.id.competencyProgressPB);
		competencyProgressPB.setProgress(0);
		firebaseRefCompetency.addChildEventListener(new ChildEventListener() {
			
			@Override
			public void onChildRemoved(DataSnapshot snapshot) {}
			
			@Override
			public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
			
			@Override
			public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}
			
			@Override
			public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
				
				CompetencyProgress b = snapshot.getValue(CompetencyProgress.class);
				
				competencyProgressPB.setProgress(progress + b.getProgress());
			}

			@Override
			public void onCancelled() {}
		});
		
		
		badge1 = (ImageView) findViewById(R.id.badge1) ;
		badge2 = (ImageView) findViewById(R.id.badge2) ;
		badge3 = (ImageView) findViewById(R.id.badge3) ;
		
		firebaseRefBadges = new Firebase(FIREBASE_URL_BADGES);
		firebaseRefBadges.addChildEventListener(new ChildEventListener() {
			
			@Override
			public void onChildRemoved(DataSnapshot snapshot) {
				if (!isTeacher) update(snapshot);
			}
			
			@Override
			public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}
			
			@Override
			public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
				if (!isTeacher) update(snapshot);
			}
			
			@Override
			public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
				Badge b = snapshot.getValue(Badge.class);

				switch (b.getId()) {
				case 1:
					badge1.setVisibility(View.VISIBLE);
					break;

				case 2:
					badge2.setVisibility(View.VISIBLE);
					break;

				case 3:
					badge3.setVisibility(View.VISIBLE);
					break;

				default:
					break;
				}

			}
			
			@Override
			public void onCancelled() {}
			
			private void update(DataSnapshot snapshot){
				Badge b = snapshot.getValue(Badge.class);

				switch (b.getId()) {
				case 1:
					badge1.setVisibility(View.INVISIBLE);
					break;

				case 2:
					badge2.setVisibility(View.INVISIBLE);
					break;

				case 3:
					badge3.setVisibility(View.INVISIBLE);
					break;

				default:
					break;
				}

			}
		});
		
		if (isTeacher) {
			initMetrics();			

			badge1.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Badge b1 = new Badge();
					b1.setId(1);
					b1.setEnabled(true);
					firebaseRefBadges.push().setValue(b1);
				}
			});

			badge2.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Badge b2 = new Badge();
					b2.setId(2);
					b2.setEnabled(true);
					firebaseRefBadges.push().setValue(b2);
				}
			});

			badge3.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Badge b3 = new Badge();
					b3.setId(3);
					b3.setEnabled(true);
					firebaseRefBadges.push().setValue(b3);
				}
			});


		} else {
			findViewById(R.id.outcomeLL).setVisibility(View.GONE);
			badge1.setVisibility(View.INVISIBLE);
			badge2.setVisibility(View.INVISIBLE);
			badge3.setVisibility(View.INVISIBLE);
			
		}
		
		
		
		
		competencyMetricLV = (ExpandableListView) findViewById(R.id.competencyLV);
//		competencySubmitBtn = (Button) findViewById(R.id.competencySubmitBtn);

		firebaseRefCompetency = new Firebase(FIREBASE_URL_COMPETENCY);
		competencyMetricLV.setAdapter(new MetricAdapter());
//		competencyMetricLV.setGroupIndicator(null);
		// competencyMetricLV.setItemsCanFocus(false);
		// competencyMetricLV.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		firebaseRefWB = new Firebase(FIREBASE_URL_WHITEBOARD);
		firebaseCanvasFL = (LinearLayout) findViewById(R.id.firebaseCanvasFL);
		drawingView = new DrawingView(this, firebaseRefWB);
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
					ovxStatusTV.setText("Video ON");
					ovxStatusTV.setBackgroundColor(Color.GREEN);
				}

				@Override
				public void callTerminated(String arg0) {
					Log.d(TAG, "callTerminated : " + arg0);
					ovxStartCallBtn.setText("Start video chat");
					ovxStatusTV.setText("Video call inactive");
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
					ovxStatusTV.setText("Video call inactive");
					ovxStatusTV.setBackgroundColor(Color.RED);
				}

				@Override
				public void callEnded() {
					Log.d(TAG, "callEnded : ");
					ovxStartCallBtn.setText("Start video chat");
					ovxStatusTV.setText("Video call inactive");
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
		// Set up a notification to let us know when we're connected or
		// disconnected from the Firebase servers
		connectedListener = firebaseRefWB.getRoot().child(".info/connected")
				.addValueEventListener(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot dataSnapshot) {
						boolean connected = (Boolean) dataSnapshot.getValue();
						if (connected) {
							// Toast.makeText(HomeActivity.this,
							// "Connected to Firebase",
							// Toast.LENGTH_SHORT).show();
							whiteBoardStatusTV.setText("White board ONLINE");
							whiteBoardStatusTV.setBackgroundColor(Color.GREEN);
						} else {
							// Toast.makeText(HomeActivity.this,
							// "Disconnected from Firebase",
							// Toast.LENGTH_SHORT).show();
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
		firebaseRefWB.getRoot().child(".info/connected").removeEventListener(connectedListener);
		drawingView.cleanup();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ovxView.exitCall();
		// and free the resources used by OVX context
		ovxView.unregister();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
		menu.add(1, CLEAR_MENU_ID, 0, "Clear").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		// menu.add(2, VIDEO_MENU_ID, 0,
		// "Video Chat").setIcon(R.drawable.ic_video_chat).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
			firebaseRefBadges.setValue(null);
			return true;

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

	private void initMetrics() {

		groupData.put(0, "Cognitive");
		groupData.put(1, "Affective");
		groupData.put(2, "Psychomotor");

		groupDescMap.put("Cognitive", "learning is demonstrated by knowledge recall and the intellectual skills");
		groupDescMap
				.put("Affective",
						"learning is demonstrated by behaviors indicating attitudes of awareness, interest, attention, concern, and responsibility");
		groupDescMap
				.put("Psychomotor",
						"learning is demonstrated by physical skills: coordination, dexterity, manipulation, grace, strength, speed");

		Map<Integer, String> cogChildMap = new HashMap<Integer, String>();
		cogChildMap.put(0, "Knowledge");
		cogChildMap.put(1, "Application");
		cogChildMap.put(2, "Synthesis");

		childData.put(0, cogChildMap);

		Map<Integer, String> affChildMap = new HashMap<Integer, String>();
		affChildMap.put(0, "Receiving");
		affChildMap.put(1, "Responding");
		affChildMap.put(2, "Organization");

		childData.put(1, affChildMap);

		Map<Integer, String> psyChildMap = new HashMap<Integer, String>();
		psyChildMap.put(0, "Perception");
		psyChildMap.put(1, "Guided response");
		psyChildMap.put(2, "Adaptation");
		psyChildMap.put(3, "Origination");

		childData.put(2, psyChildMap);

		childLevelMap.put("Knowledge", "remembering previously learned information : Verb - define, identify");
		childLevelMap.put("Application", "applying knowledge to actual situations  : Verb - apply, illustrate");
		childLevelMap.put("Synthesis", "rearranging component ideas into a new whole  : Verb - arrange, modify");

		childLevelMap.put("Receiving", "willingness to receive or attend : Illustrates - points to, replies");
		childLevelMap.put("Responding",
				"active participation indicating positive response  : Illustrates - discuss, recites");
		childLevelMap.put("Organization",
				"organizing various values into an internalized system   : Illustrates - combines, alters");

		childLevelMap.put("Perception",
				"using sense organs to obtain cues needed to guide motor activity  : Illustrates - chooses, isolates");
		childLevelMap.put("Guided response",
				"performing under guidance of a model  : Illustrates - assembles, measures");
		childLevelMap.put("Adaptation",
				"using previously learned skills to perform new but related tasks  : Illustrates - adapts, changes");
		childLevelMap.put("Origination",
				"creating new performances after having developed skills   : Illustrates - composes, designs");
	}

	class MetricAdapter extends BaseExpandableListAdapter {

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return childData.get(groupPosition).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 100 + childPosition;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild,
				View convertView, ViewGroup parent) {
			View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_metric_child, null);
			TextView childTitle = (TextView) v.findViewById(R.id.childLevelTV);
			TextView childDesc = (TextView) v.findViewById(R.id.childLevelDescTV);
			CheckBox childCB = (CheckBox) v.findViewById(R.id.childLevelCB);

			childTitle.setText(childData.get(groupPosition).get(childPosition));

			childDesc.setText(childLevelMap.get(childData.get(groupPosition).get(childPosition)));

			childCB.setOnCheckedChangeListener(new OnCheckedChangeListener() {

				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					Log.d(TAG, "Check changed for : " + childData.get(groupPosition).get(childPosition));
					
					CompetencyProgress cp = new CompetencyProgress();
					cp.setProgress(progress+=5);
					firebaseRefCompetency.push().setValue(cp);
				}
			});

			return v;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return childData.get(groupPosition).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return groupData.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return groupData.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(final int groupPosition, boolean isExpanded, final View convertView, ViewGroup parent) {

			View v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.layout_metric_group, null);
			TextView groupTitle = (TextView) v.findViewById(R.id.groupIDTV);
			groupTitle.setText(groupData.get(groupPosition));

			TextView infoButton = (TextView) v.findViewById(R.id.groupDescTV);
			infoButton.setText(groupDescMap.get(groupData.get(groupPosition)));
			
			return v;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

	}

}
