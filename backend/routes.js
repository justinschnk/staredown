var Firebase = require('firebase');
var queueRef = new Firebase('https://staredown.firebaseIO.com/queue');
var userRef = new Firebase('https://staredown.firebaseIO.com/users');

exports.home = function(req, res) {
    //Sample code that needs to be replicated in java to check if session is available to join
    queueRef.on('value', function(snapshot) {
      if(!snapshot.val()){
        console.log("No sessions available, adding self to queue");
        current = queueRef.push(
          {
            sessionID: 'test',
            user1: 'jasdev',
            winner: 0,
            duration: 0
          }
        );
      } else {
        console.log("Session ID is " + JSON.stringify(current.val()));
      }
    });
    res.render('home');
}

exports.leaderboard = function(req, res){
  res.send('leaderboard');
}