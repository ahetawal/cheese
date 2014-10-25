package com.stealthecheese.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.model.GraphUser;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.stealthecheese.MainPageActivity;
import com.stealthecheese.R;
import com.stealthecheese.application.StealTheCheeseApplication;
import com.stealthecheese.model.User;


public class MainActivity extends Activity {
	
	private Button loginButton;
	private Button logoutButton;
	List<User> playersFriends;
	User playerUser;
	
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Test parse code
		//UnitOfWork uow = new UnitOfWork();
		//Transaction newActivity = new Transaction("testFromuserId", "testToUserId", 5);
		//uow.activityDAO.createTransaction(newActivity);
		playersFriends = new ArrayList<User>();
		
		loginButton = (Button) findViewById(R.id.loginButton);
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onLoginButtonClicked();
			}
		});
		
		logoutButton = (Button) findViewById(R.id.logoutButton);
		logoutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ParseFacebookUtils.getSession().closeAndClearTokenInformation();
				
				/*
				 *  Do we need to delete the current user from parse, in order to start fresh 
				 *  or just log them out and only update friend list when they log back in again
				 */
				ParseUser currentUser = ParseUser.getCurrentUser(); 
				currentUser.deleteInBackground();
				ParseUser.logOut();
				
			}
		});
		
		
		ParseUser currentUser = ParseUser.getCurrentUser();
		if ((currentUser != null) && ParseFacebookUtils.isLinked(currentUser)) {
			// Go to the user info activity
			//showUserDetailsActivity();
		}else {
			//onLoginButtonClicked();
		}
		
		
	}
	
	private void startMainPageActivity()
	{
		Bundle b = new Bundle();
		Intent intent = new Intent(MainActivity.this, MainPageActivity.class);		
		b.putParcelableArrayList("friends", (ArrayList<? extends Parcelable>) playersFriends);
		intent.putExtras(b);
		startActivity(intent);
	}
	
	private void onLoginButtonClicked() {

		List<String> permissions = Arrays.asList("public_profile", "user_friends");
		ParseFacebookUtils.logIn(permissions, this, new LogInCallback() {
			@Override
			public void done(ParseUser user, ParseException err) {

				if (user == null) {
					Log.i(StealTheCheeseApplication.LOG_TAG,
							"Uh oh. The user cancelled the Facebook login.");
				} else if (user.isNew()) {
					Log.i(StealTheCheeseApplication.LOG_TAG,
							"User signed up and logged in through Facebook!");
					getFBUserInfo(user);
				} else {
					Log.i(StealTheCheeseApplication.LOG_TAG,
							"User logged in through Facebook!");
					getFBUserFriendsInfo(user);
					//showUserDetailsActivity();
				}
			}
		});

	}
	
	
	private void getFBUserInfo(final ParseUser loggedInUser) {
		Request request = Request.newMeRequest(ParseFacebookUtils.getSession(),
				new Request.GraphUserCallback() {
			@Override
			public void onCompleted(GraphUser user, Response response) {
				loggedInUser.put("facebookId", user.getId());
				loggedInUser.put("firstName", user.getFirstName());
				loggedInUser.put("lastName", user.getLastName());
				// Use ProfilePictureView if needed for UI
				loggedInUser.put("profilePicUrl", String.format(StealTheCheeseApplication.PROFILE_PIC_URL, user.getId()));
				loggedInUser.put("cheeseCount", getResources().getInteger(R.integer.initialCheeseCount));
				getAndSaveFBUserFriendsInfo(loggedInUser);
			}
		});
		request.executeAsync();

	}

	private void getAndSaveFBUserFriendsInfo(final ParseUser loggedInUser) {

		// Returns only the list of friends which use the app also
		Request request = Request.newMyFriendsRequest(ParseFacebookUtils.getSession(),
				new Request.GraphUserListCallback() {
			@Override
			public void onCompleted(List<GraphUser> friends, Response response) {
				List<String> friendsList = new ArrayList<String>();
				if(friends.size() > 0){
					Log.i(StealTheCheeseApplication.LOG_TAG, "Friend list size: " + friends.size());
					for(GraphUser friend : friends){
						friendsList.add(friend.getId());
						/*storing data into playersFriends to pass over to Main Page Activity*/
						playersFriends.add(new User(friend.getId(), 20));
					}

				}
				loggedInUser.addAllUnique("friends", friendsList);
				
				//TODO: Do we need the callback version of this save in case of new friends updated ??
				loggedInUser.saveInBackground();
				
				
			}

		});
		request.executeAsync();

	}
	
	
	private void getFBUserFriendsInfo(final ParseUser loggedInUser) {

		// Returns only the list of friends which use the app also
		Request request = Request.newMyFriendsRequest(ParseFacebookUtils.getSession(),
				new Request.GraphUserListCallback() {
			@Override
			public void onCompleted(List<GraphUser> friends, Response response) {
				List<String> friendsList = new ArrayList<String>();
				if(friends.size() > 0){
					Log.i(StealTheCheeseApplication.LOG_TAG, "Friend list size: " + friends.size());
					for(GraphUser friend : friends){
						friendsList.add(friend.getId());
						/*storing data into playersFriends to pass over to Main Page Activity*/
						playersFriends.add(new User(friend.getId(), 20));
					}

				}
			
				startMainPageActivity();

			}

		});
		request.executeAsync();

	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	  super.onActivityResult(requestCode, resultCode, data);
	  ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
	}
	
}
