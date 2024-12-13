package com.fileinsights.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path; // Full file path
    private String name; // Original file name
    private long size; // File size in bytes
    private long ctime; // File creation time
    private long mtime; // File modification time
    private long atime; // File access time

    // Getter and Setter for 'id'
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter and Setter for 'path'
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // Getter and Setter for 'name'
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter and Setter for 'size'
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    // Getter and Setter for 'ctime'
    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    // Getter and Setter for 'mtime'
    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    // Getter and Setter for 'atime'
    public long getAtime() {
        return atime;
    }

    public void setAtime(long atime) {
        this.atime = atime;
    }
}
