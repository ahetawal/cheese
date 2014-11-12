package com.stealthecheese.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.stealthecheese.R;
import com.stealthecheese.adapter.FriendsListAdapter;
import com.stealthecheese.adapter.HistoryListAdapter;
import com.stealthecheese.adapter.UserViewAdapter;
import com.stealthecheese.application.StealTheCheeseApplication;
import com.stealthecheese.enums.UpdateType;
import com.stealthecheese.util.AnimationHandler;
import com.stealthecheese.util.CircularImageView;
import com.stealthecheese.viewmodel.HistoryViewModel;
import com.stealthecheese.viewmodel.PlayerViewModel;

public class TheftActivity extends Activity {
	ListView historyListView;
	ListView friendsListView;
	ArrayList<HistoryViewModel> historyList = new ArrayList<HistoryViewModel>();
	ArrayList<PlayerViewModel> friendsList = new ArrayList<PlayerViewModel>();
	HistoryListAdapter historyListAdapter;
	FriendsListAdapter friendsListAdapter;
	UserViewAdapter userViewAdapter;
	CircularImageView userProfileImageView; 
	TextView userCheeseTextView;
	ImageView refreshImageView;
	TextView refreshFinishedTextView;
	ParseUser currentUser;
	private HashMap<String, Integer> localCountMap = new HashMap<String, Integer>();
	private HashMap<String, String> facebookIdFirstNameMap = new HashMap<String, String>();
	private HashMap<String, Boolean> localShowMeMap = new HashMap<String, Boolean>();
	AnimationHandler animationHandler;
	private UpdateType updateType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//String updateTypeName = getResources().getString(R.string.update_type);
		//updateType = (UpdateType)getIntent().getSerializableExtra(updateTypeName);
		
			setContentView(R.layout.activity_theft);
			currentUser = ParseUser.getCurrentUser();
			
			initializeUtilities();
			initializeUIControls();
			initializeHistoryListView(getResources());
			initializeFriendListVIew(getResources());
		updateType = UpdateType.LOGIN;
	}
	
	private void initializeUtilities()
	{
		this.animationHandler = new AnimationHandler(this);
	}
	
	@Override
	public void onBackPressed() {
		    finish();
            super.onBackPressed();
        }
	
	
	@Override
	public void onStart() {
		if (updateType == UpdateType.LOGIN)
		{
			updatePage();
		}
		else
		{
			updateCheeseCountData(refreshImageView);
		}
		super.onStart();
		updateType = UpdateType.REFRESH;
	}
	
	@Override
	protected void onResume() {
	  super.onResume();
	  StealTheCheeseApplication.activityResumed();
	}

	@Override
	protected void onPause() {
	  super.onPause();
	  StealTheCheeseApplication.activityPaused();
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		updateCheeseCountData(refreshImageView);
		  super.onNewIntent(intent);
	}
	
	/* update page when logging in */
	private void updatePage() {
		try {
			List<ParseUser> friendUsers = ParseUser.getQuery()
													.fromLocalDatastore()
													.whereNotEqualTo("facebookId", currentUser.getString("facebookId"))
													.find();
			
			populateViews(friendUsers);
		} 
		catch (ParseException e) {
			Log.e(StealTheCheeseApplication.LOG_TAG, "Fetch friends from localstore failed with message: " + e);
		}
	}
	
	/* update page when user refreshes */
	private void updatePage(List<HashMap<String, Object>> cheeseCounts)
	{
        populateUserView();
		refreshFriendsListview(cheeseCounts);
		populateHistoryListView();
	}
	
	/**
	 * Populates user and friends views
	 * @param friendUsers
	 */
	private void populateViews(List<ParseUser> friendUsers){
        retrieveCheeseCountsLocally();
        populateUserView();
        populateFriendsListView(friendUsers);
        populateHistoryListView();
	}
	
	
	
	private void populateHistoryListView() {
		new HistoryViewTask().execute();
	}

	
	private void retrieveCheeseCountsLocally() {
		ParseQuery<ParseObject> query = ParseQuery.getQuery("cheeseCountObj");
		query.fromLocalDatastore();
		try {
			List<ParseObject> cheeseUpdates = query.find();
			for(ParseObject cheese : cheeseUpdates){
				localCountMap.put(cheese.getString("facebookId"), cheese.getInt("cheeseCount"));
				localShowMeMap.put(cheese.getString("facebookId"), cheese.getBoolean("showMe"));
			}
			
		} catch (ParseException e) {
			Log.e(StealTheCheeseApplication.LOG_TAG, "Error getting cheese locally ", e);
		}
		
	}

	/* set display properties for user */
	private void populateUserView()
	{
		/* create dummy user properties, throw away later */
		PlayerViewModel userViewModel = new PlayerViewModel(currentUser.getString("facebookId"), 
										currentUser.getString("profilePicUrl")+"?type=large", 
										localCountMap.get(currentUser.getString("facebookId")), true);
		
		/* create adapter for user view */
		userCheeseTextView = (TextView) findViewById(R.id.cheeseCountTextView);
		//userProfileImageView = (ImageView) findViewById(R.id.userProfileImageView);
		userProfileImageView = (CircularImageView) findViewById(R.id.userProfileImageView);
		userViewAdapter = new UserViewAdapter(this, userCheeseTextView, userProfileImageView);
		
		/* set display values via adapter */
		userViewAdapter.setUser(userViewModel);		
	}
	
	private void initializeFriendListVIew(Resources resources) {
		friendsListView= ( ListView )findViewById( R.id.friendsListView );   
		friendsListAdapter = new FriendsListAdapter( this, friendsList, resources );
		friendsListView.setAdapter( friendsListAdapter );
	}
	
	
	private void initializeUIControls()
	{
		refreshFinishedTextView = (TextView)findViewById(R.id.refreshFinishedMessage);
		initializeImageButtons();
	}
	
	/* hook up image button clicks */
	private void initializeImageButtons()
	{
		/* hook up refresh button to fetch data from Parse and populate views */
		refreshImageView = (ImageView)findViewById(R.id.refreshImageView);
		refreshImageView.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				updateCheeseCountData(v);
			}
			
		});
	}
	
	/* get most updated cheese data from database and pin to localstore
	 * need to refactor these into separate class
	 */
	private void updateCheeseCountData(final View v){
		final Map<String,Object> params = new HashMap<String,Object>();
		animationHandler.startAnimateRefresh(v);
		ParseCloud.callFunctionInBackground("getAllCheeseCounts", params, new FunctionCallback<List<HashMap<String, Object>>>() {

			@Override
			public void done(final List<HashMap<String, Object>> cheeseCounts, ParseException ex) {
				if(ex == null){
					List<ParseObject> allCountList = new ArrayList<ParseObject>();
					for(HashMap<String, Object> eachCount : cheeseCounts){
						String friendFacebookId = (String)eachCount.get("facebookId");
						int cheeseCount = (Integer)eachCount.get("cheeseCount");
						boolean showMe = (Boolean)eachCount.get("showMe");
						
						ParseObject tempObject = new ParseObject("cheeseCountObj");
						tempObject.put("facebookId", friendFacebookId);
						tempObject.put("cheeseCount", cheeseCount);
						tempObject.put("showMe", showMe);
						allCountList.add(tempObject);
						
						/* update local hashmap to contain the newest mappings */
						localCountMap.put(friendFacebookId, cheeseCount);
					}
					
					
					ParseObject.pinAllInBackground(StealTheCheeseApplication.PIN_TAG, allCountList, new SaveCallback() {
						@Override
						public void done(ParseException ex) {
							updatePage(cheeseCounts);
							animationHandler.stopAnimateRefresh(v);
							animationHandler.fadeInOutView(refreshFinishedTextView);
						}
					});
				}
			}
		});
	}
	
	/* set history list view adapter */
	private void initializeHistoryListView(Resources res) {
        historyListView= ( ListView )findViewById( R.id.historyListView ); 
        historyListAdapter=new HistoryListAdapter( this, historyList,res );
        historyListView.setAdapter( historyListAdapter );
	}
	
	/* set friends list view adapter and handle onClick events */
	private void populateFriendsListView(List<ParseUser> userFriends) {
		friendsList.clear();
		facebookIdFirstNameMap.clear();
		for(ParseUser friend : userFriends){
			String imageUrl = String.format(StealTheCheeseApplication.FRIEND_CHEESE_COUNT_PIC_URL, friend.getString("facebookId"));
			friendsList.add(new PlayerViewModel(friend.getString("facebookId"), 
												imageUrl,
												localCountMap.get(friend.getString("facebookId")), 
												localShowMeMap.get(friend.getString("facebookId"))));
			
			facebookIdFirstNameMap.put(friend.getString("facebookId"), friend.getString("firstName"));
		}
		
		friendsListAdapter.notifyDataSetChanged();      
	}
	
	
	@SuppressWarnings("unchecked")
	private void refreshFriendsListview(List<HashMap<String, Object>> friendCheeseObjects) {
		new RefreshFriendsViewTask().execute(friendCheeseObjects);
	}
	
	
	class RefreshFriendsViewTask extends AsyncTask<List<HashMap<String, Object>>, Void, Void> {
		@Override
		protected Void doInBackground(List<HashMap<String, Object>>... friendCheeseObjects) {
			//friendsList.clear();
			for(HashMap<String, Object> eachCount : friendCheeseObjects[0]){
				String friendFacebookId = (String)eachCount.get("facebookId");
				if (friendFacebookId.equals(currentUser.getString("facebookId"))){
					continue;
				}
				else {
					/* check match with old friendsList, diff and update */
					Boolean showMe = (Boolean)eachCount.get("showMe");
					String imageUrl = String.format(StealTheCheeseApplication.FRIEND_CHEESE_COUNT_PIC_URL, (String)eachCount.get("facebookId"));
					int cheeseCount = (Integer)eachCount.get("cheeseCount");
					
					PlayerViewModel existingFriend = findFriendInList(friendFacebookId);
					if (existingFriend == null) {
						friendsList.add(new PlayerViewModel(friendFacebookId, imageUrl , localCountMap.get(friendFacebookId), showMe));
					} else {
						existingFriend.setCheese(cheeseCount);
						//existingFriend.setImageString(imageUrl);
						existingFriend.setShowMe(showMe);
					}
					
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result){
			friendsListAdapter.notifyDataSetChanged();
		}
		
	}
	
	private PlayerViewModel findFriendInList(String facebookId) {
		for (PlayerViewModel friend : friendsList) {
			if (facebookId.equals(friend.getFacebookId())) {
				return friend;
			}
		}
		
		return null;
	}
	
	
	
	
	public void onCheeseTheft(View friendImageClicked, int position, ImageView movedCheeseImg, TextView cheeseCounter){
    	/* lock list item so you can't click it again before verifying > 0 */
		friendsListAdapter.lockImageClick((ImageView)friendImageClicked, cheeseCounter);
		
		String friendFacebookId = getFriendFacebookId(position);

		/* display animation and start cheese theft async process */
		animationHandler.animateCheeseTheft(friendImageClicked, movedCheeseImg, cheeseCounter, userProfileImageView, 0, 0);
    	performCheeseTheft(friendFacebookId, (ImageView)friendImageClicked, cheeseCounter);
	}

	
	private void performCheeseTheft(final String friendFacebookId, final ImageView friendImageClicked, final TextView cheeseCounter) {
		final Map<String,Object> params = new HashMap<String,Object>();
		params.put("victimFacebookId", friendFacebookId);
		params.put("thiefFacebookId", currentUser.getString("facebookId"));
		
	    ParseCloud.callFunctionInBackground("onCheeseTheft", params, new FunctionCallback<List<HashMap<String, Object>> >() {
	        public void done(List<HashMap<String, Object>> allUpdates, ParseException e) {
	          if (e == null){   
					localCountMap.clear();
					
			    	for(HashMap<String, Object> eachCount : allUpdates){
			    		localCountMap.put((String)eachCount.get("facebookId"), (Integer)eachCount.get("cheeseCount"));
			    	}
			    	
					int currentCheesCount = localCountMap.get(currentUser.getString("facebookId"));
					int frndCurrentCheeseCount = localCountMap.get(friendFacebookId);
					
			    	cheeseCounter.setText(Integer.toString(frndCurrentCheeseCount));
					((TextView)userCheeseTextView).setText("x " + Integer.toString(currentCheesCount));			    	
			    	
		    		View userCheeseCountContainer = findViewById(R.id.userCheeseCountContainer);
		    		animationHandler.bounceCheeseCounters(userCheeseCountContainer, cheeseCounter);
		    		
		    		refreshFriendsListview(allUpdates);
		    		
		    		/* populate theft history asynchronously after friend cheese counts are updated */
		    		populateHistoryListView();
	          }
	          else
	          {
	        	/* if friend has no cheese, update cheese count to 0 and display message */
	  			Log.e(StealTheCheeseApplication.LOG_TAG, "Cheese theft failed with message: ", e);
	  			cheeseCounter.setText(Integer.toString(0));
	  			Toast theftFailedToast = Toast.makeText(getApplicationContext(), R.string.cheese_theft_failed_message, 2);
	  			theftFailedToast.setGravity(Gravity.CENTER, 0, 0);
	  			theftFailedToast.show();
	          }
	        }
	    });
		}
	
	private String getFriendFacebookId(int position) {
		String facebookId;
		try {
			facebookId = friendsList.get(position).getFacebookId();
		}
		catch (Exception ex){
			Log.e(StealTheCheeseApplication.LOG_TAG, "Cannot find facebook Id of friend in list");
			facebookId = "";
		}
		
		return facebookId;
	}

	
	
	class HistoryViewTask extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			ParseQuery<ParseObject> histQuery = ParseQuery.getQuery("thefthistory");
			histQuery.whereEqualTo("victimFBId", currentUser.get("facebookId"));
			histQuery.orderByDescending("createdAt");
			histQuery.setLimit(5);
			
			try {
				List<ParseObject> objects = histQuery.find();
				historyList.clear();
				List<ParseObject> lastTenTrans = objects;
				int visible = ((View)historyListView.getParent()).getVisibility();
				for(ParseObject trans : lastTenTrans){
					String firstName = facebookIdFirstNameMap.get(trans.getString("thiefFBId"));
					historyList.add(new HistoryViewModel(firstName));
				}
				if(lastTenTrans.size() > 0 && (View.VISIBLE != visible)){
					return true;
				}
				
			} catch (ParseException e) {
				Log.e(StealTheCheeseApplication.LOG_TAG, "Error getting histview ", e);
			}
			return false;
		}
		
		
		@Override
		protected void onPostExecute(Boolean result){
			if(result){
				((View)historyListView.getParent()).setVisibility(View.VISIBLE);
				animationHandler.fadeIn((View)historyListView.getParent());
			}
			historyListAdapter.notifyDataSetChanged();
		}
	}
	
	
	@Override
	public void onDestroy() {
		System.out.println("Called destory...");
		ParseObject.unpinAllInBackground(StealTheCheeseApplication.PIN_TAG);
		StealTheCheeseApplication.activityPaused();
		super.onDestroy();
		
	}
	
}
