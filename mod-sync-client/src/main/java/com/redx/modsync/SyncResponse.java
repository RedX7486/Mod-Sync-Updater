package com.redx.modsync;

import java.util.List;

public class SyncResponse {
    // These variable names will become the JSON keys
    private List<String> toDownload;
    private List<String> toDelete;

    // Spring Boot NEEDS these "Getter" methods to convert the JSON to data
    public List<String> getToDownload() { return toDownload; }
    public List<String> getToDelete() { return toDelete; }
}