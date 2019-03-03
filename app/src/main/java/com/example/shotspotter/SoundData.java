package com.example.shotspotter;

public class SoundData {
    public static float dbCount = 40;

    private static float lastDbCount = dbCount;
    private static float min = 0.5f;
    private static float value = 0;
    public static String coords = "";

    public static String APP_TAG = "shotspotter";
    public static void setDbCount(float dbValue) {
        if (dbValue > lastDbCount) {
            value = dbValue - lastDbCount > min ? dbValue - lastDbCount : min;
        }else{
            value = dbValue - lastDbCount < -min ? dbValue - lastDbCount : -min;
        }
        dbCount = lastDbCount + value; // prevents massive fluctuatio in sound
        lastDbCount = dbCount;
    }
    public static void setArbitraryDB(float dbValue){
        dbCount = dbValue;
    }
}
