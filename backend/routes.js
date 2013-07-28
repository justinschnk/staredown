var _ = require('underscore');
var OpenTok = require('opentok');
var Firebase = require('firebase');
var queueRef = new Firebase('https://staredown.firebaseIO.com/queue');
var userRef = new Firebase('https://staredown.firebaseIO.com/users');

exports.home = function(req, res) {
    //Sample code that needs to be replicated in java to check if session is available to join
    queueRef.on('value', function(snapshot) {
      if(!snapshot.val()){
        console.log("No sessions available, adding self to queue");
        var current = queueRef.push(
          {
            sessionID: 'test',
            user1: 'jasdev',
            winner: 0,
            duration: 0
          }
        );
      } else {
          snapshot.forEach(function(child){
            console.log("Session ID is " + JSON.stringify(child.val()));
          });
      }
    });
    res.render('home');
}

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

exports.leaderboard = function(req, res){
  var users = [];

  userRef.on('value', function(snapshot) {
    res.send(JSON.stringify(snapshot));
  });

  //res.send(JSON.stringify(_.sortBy(users, 'wins').reverse()));
}