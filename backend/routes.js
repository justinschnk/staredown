var _ = require('underscore');
var OpenTok = require('opentok');

var Firebase = require('firebase');
var queueRef = new Firebase('https://staredown.firebaseIO.com/queue');
var userRef = new Firebase('https://staredown.firebaseIO.com/users');
var gameRef = new Firebase('https://staredown.firebaseIO.com/games');

//Move to env variables before open sourcing
var key = '36528402';
var secret = '3bb3b5484081d03b70e984939cbcdf2e418e4e46';
var opentok = new OpenTok.OpenTokSDK(key, secret);

exports.home = function(req, res) {
  res.render('home');
}

exports.queue = function(req, res){
  var user = req.query.id;
  var name = req.query.name;

  userRef.child(user).on('value', function(snapshot){
    if(!snapshot.val()){
      userRef.child(user).set({name: name, stareTime: 0, wins: 0, loses: 0});
    }
  });

  queueRef.once('value', function(snapshot) {
  if(!snapshot.val()){
    console.log("No sessions available, adding self to queue");

    //Create Opentok session and add self
    var location = '127.0.0.1';
    opentok.createSession(location, function(result){
      var sessionId = result;
      var token = opentok.generateToken(sessionId);

      var newSession = queueRef.push();

      newSession.set({
        user1: user,
        session: sessionId,
        user1Token: token,
        url: newSession.toString()
      });

      res.send(JSON.stringify({status: 'waiting', sessionId: sessionId, token: token, firebase: newSession.toString()}));
    });

  } else {
      console.log('Session found, adding user to game');

      snapshot.forEach(function(childSnapshot) {
        var queueData = childSnapshot.val();

        var qSession = queueData.session;
        var qUser1 = queueData.user1;
        var qUser1Token = queueData.user1Token;
        var qURL = queueData.url;
        var user2Token = opentok.generateToken({session_id: qSession});

        var newGame = gameRef.push();

        newGame.set({
          session: qSession,
          user1Token: qUser1Token,
          user2Token: user2Token,
          user1: qUser1,
          user2: user,
          winner: 0,
          url: newGame.toString()
        });

        var oldQueueRef = new Firebase(qURL.toString());
        console.log("qurl " + qURL);
        oldQueueRef.child('url').set(newGame.toString(), function(){setTimeout(function(){oldQueueRef.remove();}, 3000)});

        res.send(JSON.stringify({status: 'connected', sessionId: qSession, token: user2Token, firebase: newGame.toString()}));
      });
  }
  });
}


exports.startgame = function(req, res){
  var onComplete = function(error) {
    if (error) res.send({status: 'failed to start'});
    else res.send({status: 'started game'});
  };

  var gameId = req.query.game;

  var gameQuery = gameRef.child(gameId);
  gameQuery.update({startedAt: Firebase.ServerValue.TIMESTAMP, winner: 0}, onComplete);
}

exports.blink = function(req, res){
  var gameId = req.query.game;
  var blinkee = req.query.id;

  var gameQuery = gameRef.child(gameId);

  gameQuery.once('value', function(snapshot){
    var result = snapshot.val();
    var elapsed = (new Date).getTime() - result.startedAt;
    var onComplete = function(){
      //Update wins/loses of both users
      userRef.child(blinkee === result.user1 ? result.user2 : result.user1).child('wins').transaction(function(currentData){
        return  currentData + 1;
      });
      userRef.child(blinkee).child('loses').transaction(function(currentData){
        return  currentData + 1;
      });

      //Update times for both users
      userRef.child(result.user1).child('stareTime').transaction(function(currentData){
        return  currentData + elapsed;
      });
      userRef.child(result.user2).child('stareTime').transaction(function(currentData){
        return  currentData + elapsed;
      });
      res.send({status: 'status updated'});
    }

    gameQuery.update({endedAt: (new Date).getTime(), elapsed: elapsed, winner: blinkee === result.user1 ? 2 : 1}, onComplete);
  });
}

exports.leaderboard = function(req, res){
  var json = req.query.json;

  var users = [];

  userRef.once('value', function(snapshot) {
    for (var key in snapshot.val()) {
      if (snapshot.val().hasOwnProperty(key)) {
        users.push(snapshot.val()[key]);
      }
    }
    if(json){
      res.send(JSON.stringify(_.sortBy(users, 'wins').reverse()));
    }
    else{
      res.render('leaderboard', {users: users});
    }
  });
}