
// CheeseBot FB Id
var CHEESE_BOT_FB_ID = "369050949920159";

/* retrieve cheese counts of user's friends */
var getFriendsCheeseCounts = function(friendFacebookIds, response, thiefFacebookId)
	{
		var query = new Parse.Query("cheese");
		var finalCheesUpdates = {};
		console.log("getFriendsCheeseCounts received friendsFacebookIds: " + friendFacebookIds);
		query.containedIn("facebookId", friendFacebookIds);
		query.find().then(function(usersFriends){	
				
				for(var i = 0; i < usersFriends.length; i++){
					var currFBId = usersFriends[i].get("facebookId");
					finalCheesUpdates[currFBId] = {
						facebookId: currFBId,
                         showMe: null,
                         cheeseCount: usersFriends[i].get("cheeseCount")
					};
					
				}
				console.log("INITIAL WRAPPERR IS ...");
				console.log(finalCheesUpdates);
				
				var findWhereThiefIsVictimQuery = new Parse.Query("theftdirection");
				findWhereThiefIsVictimQuery.equalTo("victimFBId", thiefFacebookId);
				return findWhereThiefIsVictimQuery.find();
		
			}).then(function(thiefs){
				for(var i = 0; i < thiefs.length; i++){
					var fbId = thiefs[i].get("thiefFBId");
					console.log("WhereThiefIsVictim id is " + fbId);
					finalCheesUpdates[fbId].showMe = true;
            	}
			
				var d = new Date(); // gets today
    			var dMinus = new Date(d - 1000 * 60 * 60 * 24 *.25); // gets 6  hrs ago
    			console.log("Half a day..." + dMinus);
    			
    			//var dMinus1 = new Date("November 8, 2014 10:10:00");
    		
				var findWhereThiefIsThief = new Parse.Query("theftdirection");
				findWhereThiefIsThief.equalTo("thiefFBId", thiefFacebookId);
				findWhereThiefIsThief.containedIn("victimFBId", friendFacebookIds);
				findWhereThiefIsThief.greaterThanOrEqualTo("updatedAt", dMinus);
				return findWhereThiefIsThief.find();
			
			}).then(function(currThiefs){
				for(var i = 0; i < currThiefs.length; i++){
					var fbId = currThiefs[i].get("victimFBId");
					console.log("WhereThiefIsThief id is " + fbId);
					finalCheesUpdates[fbId].showMe = false;
			}
			
			console.log("FINAL WRAPPERR IS ...");
			console.log(finalCheesUpdates);
			
			var finalCheesUpdatesList = [];
   			for(var key in finalCheesUpdates){
   				
   				// check for 0 cheese count, and disabling the image
   				var localCheeseCount = finalCheesUpdates[key].cheeseCount;
   				if(localCheeseCount < 1){
   					finalCheesUpdates[key].showMe = false;
   				}
   				if(finalCheesUpdates[key].showMe == null){
   					finalCheesUpdates[key].showMe = true;
   				}
   				
      			finalCheesUpdatesList.push(finalCheesUpdates[key]);
   			}
   				
   			console.log(finalCheesUpdatesList);
   			response.success(finalCheesUpdatesList);
		
		});	
			
		
	}
	
	



/* cloud code to steal cheese */
Parse.Cloud.define("onCheeseTheft", function(request, response)
{
	console.log(request);
	var thiefFacebookId = request.params.thiefFacebookId;
	var victimFacebookId = request.params.victimFacebookId;
	var thiefUserCheese;
	var victimUserCheese;
	console.log(thiefFacebookId + " is stealing cheese from " + victimFacebookId);
	var query = new Parse.Query("cheese");
	var facebookIds = [thiefFacebookId, victimFacebookId];
	
	query.containedIn("facebookId", facebookIds);
	query.find().then(
					function(cheeseRows)
					{
						console.log("cheese query response is: " + cheeseRows);
						findVictimThiefCheeseRows(cheeseRows);
						victimUserCheese.increment("cheeseCount", -1);
						return victimUserCheese.save();
					})
				.then(
					function()
					{
						console.log("I'm in success");
						thiefUserCheese.increment("cheeseCount");	
						return thiefUserCheese.save();
					},
					function(error)
					{
						response.error(error);
					})
				.then(function(){insertTheftHistory();})
				.then(function(){updateTheftDirection();})
				.then(function(){return getUserFriendsFacebookIds();})
				.then(function(friendFacebookIds){getFriendsCheeseCounts(friendFacebookIds, response, thiefFacebookId);});
							
	
	/* find and set victim and thief cheese rows */
	var findVictimThiefCheeseRows = function(cheeseRows)
	{					
		for(var ii=0; ii< cheeseRows.length; ii++)
		{
			var userFacebookId = cheeseRows[ii].get("facebookId");
			var cheeseCount = cheeseRows[ii].get("cheeseCount");
			
			console.log("user " + userFacebookId + " has cheese: " + cheeseCount);
			
			if (userFacebookId === thiefFacebookId)
			{
				thiefUserCheese = cheeseRows[ii];
			}
			else
			{
				victimUserCheese = cheeseRows[ii];
			}
		}
	}	
	
	/* add theft record to thefthistory table */
	var insertTheftHistory = function(response)
	{
		var TheftHistoryClass = Parse.Object.extend("thefthistory");
		var theftHistory = new TheftHistoryClass();
		theftHistory.set("thiefFBId", thiefFacebookId);
		theftHistory.set("victimFBId", victimFacebookId);
		
		console.log("adding theft history for thief " + thiefFacebookId + " and victim " + victimFacebookId);
		
		theftHistory.save(null,{
		  success:function(theftResponse) { 
			console.log(theftResponse);
		  },
		  error:function(error) {
			response.error(error);
		  }
		});
	}
	
	var updateTheftDirection = function(){
		
		console.log("Inside theft direction...");
		Parse.Cloud.useMasterKey();
		var fwdDirection = new Parse.Query("theftdirection");
		fwdDirection.equalTo("thiefFBId", thiefFacebookId);
		fwdDirection.equalTo("victimFBId", victimFacebookId);
		
		var reverseDirection = new Parse.Query("theftdirection");
		reverseDirection.equalTo("thiefFBId", victimFacebookId);
		reverseDirection.equalTo("victimFBId", thiefFacebookId);
		
		var directionQuery = Parse.Query.or(fwdDirection, reverseDirection);
		directionQuery.find({
			success: function(theftVictimCombination)
			{
				console.log("Combination size : " + theftVictimCombination.length);
				if(theftVictimCombination.length < 1){
					var TheftDirectionClass = Parse.Object.extend("theftdirection");
					var theftDir = new TheftDirectionClass();
					theftDir.set("thiefFBId", thiefFacebookId);
					theftDir.set("victimFBId", victimFacebookId);
					theftDir.save();
					console.log("Inserted new combination");
				}else {
					console.log("updating existing combination...");
					theftVictimCombination[0].set("thiefFBId", thiefFacebookId);
					theftVictimCombination[0].set("victimFBId", victimFacebookId);
					theftVictimCombination[0].save();
				}
				
			},
			error: function(error)
			{	
				console.log("Not able to find any row..." + error);
				
			}	
		}); 
	}
	
	
	
	
	/* return all facebook ids of user's friends */
	var getUserFriendsFacebookIds = function()
	{
		var query = new Parse.Query(Parse.User);
		query.equalTo("facebookId", thiefFacebookId);
		var results = 
			query.find().then(
				function(user)
				{
					console.log(user);
					var friendFacebookIds = user[0].get("friends");
					console.log("User has friends: " + friendFacebookIds);
					friendFacebookIds.push(thiefFacebookId);
					return friendFacebookIds;
				},
				function(error)
				{
					console.log("Cannot find user friends facebookIds");
					response.error("Cannot find user friends facebookIds");
				}	
			
			);
		console.log("getUserFriendsFacebookIds results:  " + results);
		return results;
	}
	
	
	
	
	
	
});

 



Parse.Cloud.define("onLoginActivity", function(request, response) {

    console.log(request);
    Parse.Cloud.useMasterKey();
	     
    var isNewUser = request.params.isNewUserFlag;
    var passedInUser = request.user;
    console.log("START Passed In USER ...");
    console.log(passedInUser);
    console.log("END Passed In USER ...");
    var fbaccessToken = passedInUser.get('authData').facebook.access_token;
    console.log("fbaccessToken " + fbaccessToken);
    var currentFBUserId = passedInUser.get("facebookId");
    
    var existingUserSteps = function(request, response) {
    	console.log("Inside performExistingUser Steps...");
    	console.log(fbaccessToken);
    	var updatedFriendsList = [];
    	Parse.Cloud.httpRequest({
               url: 'https://graph.facebook.com/me/friends?access_token=' + fbaccessToken
    	}).then(function(httpResponse){
                    console.log("Fb Reponse " + httpResponse.text);
                    var fbResponse = httpResponse['data'].data;
                    console.log(fbResponse);
                    var friendsList = [CHEESE_BOT_FB_ID];
                    for(var i = 0; i < fbResponse.length; i++) {
                        var fbId = fbResponse[i].id;
                        friendsList.push(fbId);
                    }
                     return friendsList;
                         
                    }, function(errorResponse){
                        // Throw error
                        console.error('Request failed with response code ' + errorResponse.status);
                 
                }).then(function(allFriendslist){
                    console.log("in updating user friends list");
                    passedInUser.set("friends", allFriendslist);
                    updatedFriendsList = allFriendslist;
                    return passedInUser.save();
                 
                }).then(function(savedUser){
                        console.log("In final stage");
                        updatedFriendsList.push(passedInUser.get("facebookId"));
                        console.log(updatedFriendsList);
                       // var query = new Parse.Query("cheese");
                        var query = new Parse.Query(Parse.User);
                        query.containedIn("facebookId", updatedFriendsList);
                        return query.find();
                     
                }).then(function(allCheeseCountObjects){
                            for(var i = 0; i<allCheeseCountObjects.length; i++){
                                console.log(allCheeseCountObjects[i]);
                            }
                            response.success(allCheeseCountObjects);
                        },function(errorHandler){
                            response.error("Not able to complete this operation");
                        }
                );
	}   
     
 	
 	var query = new Parse.Query(Parse.User);
	query.equalTo("facebookId", CHEESE_BOT_FB_ID);
	query.find().then(function(botUser){
			console.log("Bot USER IS ...");
			console.log(botUser[0]);
			botUser[0].addUnique("friends", passedInUser.get("facebookId"));
		 	return botUser[0].save();
	
	}).then(function(saveUser){
		
		if(isNewUser) {
       		console.log("Inside NEW USER BLOCK...");
       		var CheeseCountClass = Parse.Object.extend("cheese");
       		var cheeseCount = new CheeseCountClass();
       		cheeseCount.set("facebookId", passedInUser.get("facebookId"));
       		cheeseCount.set("cheeseCount", 20);
       		cheeseCount.save().then(function(cheeseCount){
                        return existingUserSteps(request, response);
                });
    	} else {
    		console.log("Inside else part...");
        	return existingUserSteps(request, response);
    	}
 	});
 	
});



Parse.Cloud.define("getAllCheeseCounts", function(request, response) {
	console.log("In getAllCheeseCounts...");
	console.log(request);
	var friendsList = request.user.get("friends");
	var query = new Parse.Query("cheese");
	friendsList.push(request.user.get("facebookId"));
	console.log("getFriendsCheeseCounts received friendsFacebookIds: " + friendsList);
	getFriendsCheeseCounts(friendsList, response, request.user.get("facebookId"));
});




/* beforeSave function for the cheese table to check victim cheese count */
Parse.Cloud.beforeSave("cheese", function(re, response){
	var myCount = re.object.get("cheeseCount");
	console.log("Player cheese count is: " + myCount);
	if(myCount >= 0)
	{
		console.log("cheese table beforeSave success");
		response.success();
	}
	else 
	{
		console.log("cheese table beforeSave error");
		response.error("Victim has no cheese! Cannot steal from victim");
	}
});


