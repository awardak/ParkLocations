package com.aman.parklocations.model;

public class Park {
    private String name;
    private String managerName;
    private String email;
    private String phone;
    private String latitude;
    private String longitude;
    private float distanceFromUser;

    public Park(String name, String managerName, String email, String phone, String latitude, String longitude) {
        this.name = name;
        this.managerName = managerName;
        this.email = email;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLatitude() { return latitude; }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public float getDistanceFromUser() { return distanceFromUser; }

    public void setDistanceFromUser(float distanceFromUser) { this.distanceFromUser = distanceFromUser; }
}
