package com.mayaforce.packagebuilder.inventory;

import java.util.Calendar;

import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.FileProperties;

public class InventoryItem {

    /**
     * @return the createdByEmail
     */
    public String getCreatedByEmail() {
        return createdByEmail;
    }

    /**
     * @param createdByEmail the createdByEmail to set
     */
    public void setCreatedByEmail(String createdByEmail) {
        this.createdByEmail = createdByEmail;
    }


    /**
     * @return the excludeFromInventory
     */
    public boolean isExcludeFromInventory() {
        return excludeFromInventory;
    }

    /**
     * @param excludeFromInventory the excludeFromInventory to set
     */
    public void setExcludeFromInventory(boolean excludeFromInventory) {
        this.excludeFromInventory = excludeFromInventory;
    }

    /**
     * @return the excludeReason
     */
    public String getExcludeReason() {
        return excludeReason;
    }

    /**
     * @param excludeReason the excludeReason to set
     */
    public void setExcludeReason(String excludeReason) {
        this.excludeReason = excludeReason;
        this.excludeFromInventory = true;
    }

    /**
     * @return the customFieldType
     */
    public String getMetadataSubType() {
        return metadataSubType;
    }

    /**
     * @param metadataSubType the customFieldType to set
     */
    public void setMetadataSubType(String metadataSubType) {
        this.metadataSubType = metadataSubType;
    }

    /**
     * @param itemName the itemName to set
     */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /**
     * @return the itemExtendedName
     */
    public String getItemExtendedName() {
        return itemExtendedName;
    }

    /**
     * @param itemExtendedName the itemExtendedName to set
     */
    public void setItemExtendedName(String itemExtendedName) {
        this.itemExtendedName = itemExtendedName;
    }

    /**
     * @return the fp
     */
    public FileProperties getFp() {
        return fp;
    }

    /**
     * @param fp the fp to set
     */
    public void setFp(FileProperties fp) {
        this.fp = fp;
    }

    /**
     * @return the isFolder
     */
    public boolean isIsFolder() {
        return isFolder;
    }

    /**
     * @param isFolder the isFolder to set
     */
    public void setIsFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }

    /**
     * @return the itemVersion
     */
    public int getItemVersion() {
        return itemVersion;
    }

    /**
     * @param itemVersion the itemVersion to set
     */
    public void setItemVersion(int itemVersion) {
        this.itemVersion = itemVersion;
    }

    /**
     * @return the isNew
     */
    public boolean isIsNew() {
        return isNew;
    }

    /**
     * @param isNew the isNew to set
     */
    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * @return the isUpdated
     */
    public boolean isIsUpdated() {
        return isUpdated;
    }

    /**
     * @param isUpdated the isUpdated to set
     */
    public void setIsUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    /**
     * @return the lastModifiedByEmail
     */
    public String getLastModifiedByEmail() {
        return lastModifiedByEmail;
    }

    /**
     * @param lastModifiedByEmail the lastModifiedByEmail to set
     */
    public void setLastModifiedByEmail(String lastModifiedByEmail) {
        this.lastModifiedByEmail = lastModifiedByEmail;
    }

    /**
     * @return the lastModifiedByUsername
     */
    public String getLastModifiedByUsername() {
        return lastModifiedByUsername;
    }

    /**
     * @param lastModifiedByUsername the lastModifiedByUsername to set
     */
    public void setLastModifiedByUsername(String lastModifiedByUsername) {
        this.lastModifiedByUsername = lastModifiedByUsername;
    }

    /**
     * @return the localFileName
     */
    public String getLocalFileName() {
        return localFileName;
    }

    /**
     * @param localFileName the localFileName to set
     */
    public void setLocalFileName(String localFileName) {
        this.localFileName = localFileName;
    }

    /**
     * @return the describe
     */
    public DescribeMetadataObject getDescribe() {
        return describe;
    }

    /**
     * @param describe the describe to set
     */
    public void setDescribe(DescribeMetadataObject describe) {
        this.describe = describe;
    }

    /**
     * @return the folderName
     */
    public String getFolderName() {
        return folderName;
    }

    /**
     * @param folderName the folderName to set
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /**
     * @return the folderPath
     */
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * @param folderPath the folderPath to set
     */
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    /**
     * @return the inFolder
     */
    public boolean isInFolder() {
        return inFolder;
    }

    /**
     * @param inFolder the inFolder to set
     */
    public void setInFolder(boolean inFolder) {
        this.inFolder = inFolder;
    }

    /**
     * @return the typeOverride
     */
    public String getTypeOverride() {
        return typeOverride;
    }

    /**
     * @param typeOverride the typeOverride to set
     */
    public void setTypeOverride(String typeOverride) {
        this.typeOverride = typeOverride;
    }

    /**
     * @return the fullNameAndDirOverride
     */
    public String getFullNameAndDirOverride() {
        return fullNameAndDirOverride;
    }

    /**
     * @param fullNameAndDirOverride the fullNameAndDirOverride to set
     */
    public void setFullNameAndDirOverride(String fullNameAndDirOverride) {
        this.fullNameAndDirOverride = fullNameAndDirOverride;
    }

    private String itemName;
    private String itemExtendedName;
    private FileProperties fp;
    private boolean isFolder;
    private int itemVersion;
    private boolean isNew;
    private boolean isUpdated;
    private String lastModifiedByEmail;
    private String lastModifiedByUsername;
    private String createdByUsername;
    private String createdByEmail;
    private String localFileName;
    private DescribeMetadataObject describe;
    private String folderName;
    private String folderPath;
    private String status = "Active";
    private boolean inFolder = false;
    private String typeOverride;
    private String metadataSubType = "notSet";
    private boolean excludeFromInventory=false;
    private String excludeReason="";
    private String includeReason="";

    public InventoryItem(String i, FileProperties f, DescribeMetadataObject d, boolean isF, String fullNameAndDirOverride) {
        
        this.fullNameAndDirOverride = fullNameAndDirOverride;

        initItem(i, f, d);
        this.isFolder = isF;
    }

    public InventoryItem(String i, FileProperties f, DescribeMetadataObject d) {
        initItem(i, f, d);
        this.isFolder = false;

    }

    // for StandardValueSets only
    public InventoryItem(String i, String folderName, String type) {
        this.itemName = i;
        this.isFolder = false;
        this.folderName = folderName;
        if (folderName.equalsIgnoreCase("standardValueSets")){
            this.fullNameAndDirOverride = i;
        }
        this.describe = null;
        this.typeOverride = type;
    }

    public DescribeMetadataObject getDescribeMetadataObject() {
        return getDescribe();
    }

    private void initItem(String i, FileProperties f, DescribeMetadataObject d) {
        this.setItemName(i);
        this.setFp(f);

        if (getFp() != null && d != null) {
            setFolderName(d.getDirectoryName());
        } else {
            setFolderName("");
        }
        this.setDescribe(d);
    }

    public FileProperties getFileProperties() {
        return getFp();
    }

    public String getExtendedName() {
        if (getFp() == null) {
            return getItemName();
        } else {
            return getFp().getType() + ":" + getFp().getFullName();
        }
    }

    public String getId() {
        return getFp() == null ? null : getFp().getId();
    }

    public String getCreatedById() {
        return getFp() == null ? null : getFp().getCreatedById();
    }

    public String getCreatedByName() {
        return getFp() == null ? null : getFp().getCreatedByName();
    }

    public String getCreatedByName(int charLimit) {
        String temp = getCreatedByName();
        if (temp != null && temp.length() > charLimit) {
            return temp.substring(0, charLimit);
        }
        return temp;

    }

    public String getItemName() {
        //Managed Package Layouts need to be renamed to include the namespace prefix before the layout name. 
        if (this.getFullNameAndDirOverride() != null) {
            return getFullNameAndDirOverride();
        }
        if (getFp() != null && getFp().getType().equals("Layout") && getFp().getNamespacePrefix() != null) {
            return itemName.replaceFirst("\\-", "-" + getFp().getNamespacePrefix() + "__");
        }        
        if (getFp() != null && getFp().getType().equals("CustomMetadata") && getFp().getNamespacePrefix() != null) {
            return itemName.replaceFirst("\\.", "." + getFp().getNamespacePrefix() + "__");
        }
        return itemName;
    }

    public String getFolderAndFileName() {

        return getFp() == null ? getFolderName() + '/' + getItemName() : getFp().getFileName();

    }
    
    public String getPathAndFilename() {
        if (describe != null) {
            return describe.getDirectoryName() + "/" + getItemName();
        } if (getFp() != null) {
            return getFp().getType() + "/" + getItemName();
        } else {
            return getItemName();
        }
    }

    public String getFullName() {
        if (this.getFullNameAndDirOverride() != null) {
            return getFullNameAndDirOverride();
        }
        return getFp() == null ? null : getFp().getFullName();
    }

    public String getLastModifiedById() {
        return getFp() == null ? null : getFp().getLastModifiedById();
    }

    public String getLastModifiedByName() {
        return getFp() == null ? null : getFp().getLastModifiedByName();
    }
    
    

    public String getLastModifiedByName(int charLimit) {
        String temp = getLastModifiedByName();
        if (temp != null && temp.length() > charLimit) {
            return temp.substring(0, charLimit);
        }
        return temp;

    }

    public Calendar getLastModifiedDate() {
        return getFp() == null ? null : getFp().getLastModifiedDate();
    }

    public Calendar getCreatedDate() {
        return getFp() == null ? null : getFp().getCreatedDate();
    }

    public String getType() {
        if (getTypeOverride() != null) {
            return getTypeOverride();
        }
        return getFp() == null ? null : getFp().getType();
    }

    public String getStatus() {
        return status;
    }
    
   

    public void setStatus(String newStatus) {
        status = newStatus;
    }

    private String fullNameAndDirOverride;

    public void setfullNameAndDirOverride(String fullNameAndDirOverride) {
        this.setFullNameAndDirOverride(fullNameAndDirOverride);

    }

    /**
     * @return the includeReason
     */
    public String getIncludeReason() {
        return includeReason;
    }

    /**
     * @param includeReason the includeReason to set
     */
    public void setIncludeReason(String includeReason) {
        this.includeReason = includeReason;
    }

    /**
     * @return the createdByUsername
     */
    public String getCreatedByUsername() {
        return createdByUsername;
    }

    /**
     * @param createdByUsername the createdByUsername to set
     */
    public void setCreatedByUsername(String createdByUsername) {
        this.createdByUsername = createdByUsername;
    }
}
