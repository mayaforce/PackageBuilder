package com.kgal.packagebuilder.inventory;

import java.util.Calendar;

import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.FileProperties;

public class InventoryItem {

    private String itemName;
    public String itemExtendedName;
    public FileProperties fp;
    public boolean isFolder;
    public int itemVersion;
    public boolean isNew;
    public boolean isUpdated;
    public String lastModifiedByEmail;
    public String lastModifiedByUsername;
    public String localFileName;
    public DescribeMetadataObject describe;
    public String folderName;
    private String status = "Active";

    public DescribeMetadataObject getDescribeMetadataObject() {
        return describe;
    }

    public FileProperties getFileProperties() {
        return fp;
    }

    public String getItemName() {
        //Managed Package Layouts need to be renamed to include the namespace prefix before the layout name. 
        if (fp != null && fp.getType().equals("Layout") && fp.getNamespacePrefix() != null) {
            return itemName.replaceFirst("\\-", "-" + fp.getNamespacePrefix() + "__");
        }
        return itemName;
    }

    public InventoryItem(String i, FileProperties f, boolean isF, DescribeMetadataObject d) {
        initItem(i, f, d);
        this.isFolder = isF;
    }

    public InventoryItem(String i, FileProperties f, DescribeMetadataObject d) {
        initItem(i, f, d);
        this.isFolder = false;
    }

    private void initItem(String i, FileProperties f, DescribeMetadataObject d) {
        this.itemName = i;
        this.fp = f;
        if (fp != null && d != null) {
            folderName = d.getDirectoryName();
        } else {
            folderName = "";
        }
        this.describe = d;
    }

    // for StandardValueSets only
    public InventoryItem(String i, String folderName) {
        this.itemName = i;
        this.isFolder = false;
        this.folderName = folderName;
        this.describe = null;
    }

    public String getExtendedName() {
        if (fp == null) {
            return getItemName();
        } else {
            return fp.getType() + ":" + fp.getFullName();
        }
    }

    public String getId() {
        return fp == null ? null : fp.getId();
    }

    public String getCreatedById() {
        return fp == null ? null : fp.getCreatedById();
    }

    public String getCreatedByName() {
        return fp == null ? null : fp.getCreatedByName();
    }

    public String getFileName() {
        return fp == null ? folderName + '/' + getItemName() : fp.getFileName();
    }

    public String getFullName() {
        return fp == null ? null : fp.getFullName();
    }

    public String getLastModifiedById() {
        return fp == null ? null : fp.getLastModifiedById();
    }

    public String getLastModifiedByName() {
        return fp == null ? null : fp.getLastModifiedByName();
    }

    public Calendar getLastModifiedDate() {
        return fp == null ? null : fp.getLastModifiedDate();
    }

    public Calendar getCreatedDate() {
        return fp == null ? null : fp.getCreatedDate();
    }

    public String getType() {
        return fp == null ? null : fp.getType();
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String newStatus){
        status = newStatus;
    }

    public String toCSV() {
        return getType() + ","
                + getItemName() + ","
                + itemVersion + ","
                + getLastModifiedDate() + ","
                + getLastModifiedByName() + ","
                + getCreatedDate() + ","
                + getCreatedByName() + ","
                + fp == null ? null : fp.getId() + ",";

    }
}
