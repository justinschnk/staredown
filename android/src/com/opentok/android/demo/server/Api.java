package com.opentok.android.demo.server;

public class Api {
    public class User {
        public String id;
        public String name;
        public int wins;
        public int losses;
        public int stareTime;
    }

    public class Game {
        public String id;
        public String uid1;
        public String uid2;
        public int duration;
        public int winner;
    }



}
