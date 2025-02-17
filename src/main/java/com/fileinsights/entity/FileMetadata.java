package com.fileinsights.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "file_metadata", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"path", "name"})
})
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1024)
    @NotBlank
    @Size(max = 1024)
    private String path; // Full file path

    @Column(nullable = false, length = 255)
    @NotBlank
    @Size(max = 255)
    private String name; // Original file name

    @Column(nullable = false)
    private long size; // File size in bytes

    @Temporal(TemporalType.TIMESTAMP)
    private Date ctime; // File creation time

    @Temporal(TemporalType.TIMESTAMP)
    private Date mtime; // File modification time

    @Temporal(TemporalType.TIMESTAMP)
    private Date atime; // File access time

    // New fields for NFS/SMB support
    @Column(nullable = true, length = 255)
    private String nfsHost; // To store the NFS host

    @Column(nullable = true, length = 255)
    private String smbHost; // To store the SMB host

    @Column(nullable = true, length = 255)
    private String username; // To store the username for NFS/SMB

    @Column(nullable = true, length = 255)
    private String password; // To store the password for NFS/SMB

    @Column(nullable = true, length = 1024)
    private String sharePath; // To store the shared path for NFS/SMB

    @Column(nullable = true, length = 50)
    private String type; // Type of the file (Local, NFS, SMB)

    // Getters and Setters
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
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative.");
        }
        this.size = size;
    }

    public Date getCtime() {
        return ctime;
    }

    public void setCtime(Date ctime) {
        this.ctime = ctime;
    }

    public Date getMtime() {
        return mtime;
    }

    public void setMtime(Date mtime) {
        this.mtime = mtime;
    }

    public Date getAtime() {
        return atime;
    }

    public void setAtime(Date atime) {
        this.atime = atime;
    }

    // New Getters and Setters for NFS/SMB fields
    public String getNfsHost() {
        return nfsHost;
    }

    public void setNfsHost(String nfsHost) {
        this.nfsHost = nfsHost;
    }

    public String getSmbHost() {
        return smbHost;
    }

    public void setSmbHost(String smbHost) {
        this.smbHost = smbHost;
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

    public String getSharePath() {
        return sharePath;
    }

    public void setSharePath(String sharePath) {
        this.sharePath = sharePath;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // Override toString, equals, and hashCode
    @Override
    public String toString() {
        return "FileMetadata{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", ctime=" + ctime +
                ", mtime=" + mtime +
                ", atime=" + atime +
                ", nfsHost='" + nfsHost + '\'' +
                ", smbHost='" + smbHost + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", sharePath='" + sharePath + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equals(path, that.path) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name);
    }
}
