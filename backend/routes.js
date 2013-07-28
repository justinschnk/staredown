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

  queueRef.once('value', function(snapshot) {
  if(!snapshot.val()){
    console.log("No sessions available, adding self to queue");

    //Create Opentok session and add self
    var location = '127.0.0.1';
    opentok.createSession(location, function(result){
      var sessionId = result;
      var token = opentok.generateToken(sessionId);

      var newSession = queueRef.push();

      queueRef.set({
        user1: user,
        session: sessionId,
        user1Token: token,
        url: newSession.toString()
      });

      res.send(JSON.stringify({status: 'waiting', sessionId: sessionId, token: token, firebase: newSession.toString()}));
    });

  } else {
    console.log('Session found, adding user to game');
    var queueData = snapshot.val();

    var qSession = queueData.session;
    var qUser1 = queueData.user1;
    var qUser1Token = queueData.user1Token;
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

    queueRef.remove();

    res.send(JSON.stringify({status: 'connected', sessionId: qSession, token: user2Token, firebase: newGame.toString()}));
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

//Low priority
exports.leaderboard = function(req, res){
  var users = [];

  userRef.on('value', function(snapshot) {
    res.send(JSON.stringify(snapshot));
  });

  //res.send(JSON.stringify(_.sortBy(users, 'wins').reverse()));
}

//Test stub
exports.addUser = function(req, res){
  userRef.push({
    uid: '1',
    username: 'Vivek',
    stareTime: 10,
    wins: 3,
    loses: 0,
    ratio: 1
  });
  res.send('added user');
}