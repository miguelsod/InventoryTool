package com.idbarcodesolutions.mainactivity.models;

import com.idbarcodesolutions.mainactivity.application.MyApplication;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class User extends RealmObject {
    @PrimaryKey
    private String username;
    @Required
    private String password;
    private int userID;
    private RealmList<Warehouse> warehouseList;

    public User(){
    }

    public User(String username, String password) {
        this.userID = MyApplication.userID.incrementAndGet();
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public RealmList<Warehouse> getWarehouseList() {
        return warehouseList;
    }

    public void setWarehouseList(RealmList<Warehouse> warehouseList) {
        this.warehouseList = warehouseList;
    }
}
