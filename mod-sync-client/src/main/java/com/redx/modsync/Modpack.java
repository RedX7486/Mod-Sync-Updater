package com.redx.modsync;

public class Modpack {
    private String name;
    private int modCount;
    private String totalSize;
    private String joinCode;
    private boolean isAdmin;

    public Modpack(String name, int modCount, String totalSize, String joinCode, boolean isAdmin) {
        this.name = name;
        this.modCount = modCount;
        this.totalSize = totalSize;
        this.joinCode = joinCode;
        this.isAdmin = isAdmin;
    }

    public String getName() { return name; }
    public int getModCount() { return modCount; }
    public String getTotalSize() { return totalSize; }
    public String getJoinCode() { return joinCode; }
    public boolean isAdmin() { return isAdmin; }
}