package com.example.keelinofarrell.taxiapp.HistoryRecyclerViewInfo;

/**
 * Created by keelin.ofarrell on 02/02/2018.
 */

public class HistoryObject {

    private String driveId, time;

    public HistoryObject(String driveId, String time){
        this.driveId = driveId;
        this.time = time;
    }

    public String getDriveId(){
        return driveId;
    }
    public String getTime(){
        return time;
    }

    public void setDriveId(String driveId){
        this.driveId = driveId;
    }

    public void setTime(String time){
        this.time = time;
    }
}
