package com.fileinsights.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "file_metadata", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"path", "name"})
})
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // Ensure non-null for unique constraints
    private String path; // Full file path

    @Column(nullable = false) // Ensure non-null for unique constraints
    private String name; // Original file name

    private long size; // File size in bytes
    private long ctime; // File creation time
    private long mtime; // File modification time
    private long atime; // File access time

    // Getters and Setters for all fields...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public long getMtime() {
        return mtime;
    }

    public void setMtime(long mtime) {
        this.mtime = mtime;
    }

    public long getAtime() {
        return atime;
    }

    public void setAtime(long atime) {
        this.atime = atime;
    }
}
