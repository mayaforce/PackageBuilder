package com.mayaforce.packagebuilder;

import com.mayaforce.migrationtoolutils.Utils;
import com.mayaforce.packagebuilder.inventory.InventoryItem;
import com.mayaforce.packagebuilder.output.GitOutputManager;
import com.mayaforce.packagebuilder.output.LogFormatter;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.Flow;
import com.sforce.soap.metadata.CustomField;
import com.sforce.soap.metadata.InstalledPackage;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.ManageableState;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.QueryResult;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class PackageBuilder {

    public enum OperationMode {
        DIR(0), ORG(1);

        private final int level;

        OperationMode(final int level) {
            this.level = level;
        }

        int getLevel() {
            return this.level;
        }
    }

    // Logging
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private Level thisLogLevel;

    // Collections
    private final HashSet<String> existingTypes = new HashSet<>();
    private final Properties parameters = new Properties();

    // Variables changing per parameter or properties
    private double myApiVersion;
    private String skipItems;
    private HashMap<String, DescribeMetadataObject> describeMetadataObjectsMap;

    private HashMap<String, String> folderRecursivePath = new HashMap<>(); //String1: Metadata Type|Folder Name String2: Full path
    String authEndPoint = "";
    private MetadataConnection srcMetadataConnection;
    private String srcUrl;
    private String srcUser;
    private String srcPwd;
    private String srcToken;
    private String metaSourceDownloadDir = "src";
    private final long totalTimeStart = System.currentTimeMillis();
    private String targetDir = "";
    private OperationMode mode;
    private PartnerConnection srcPartnerConnection;

    private boolean includeChangeData = false;
    private boolean includeNamespacedItems = false;
    private boolean skipManageableStateInstalled = false;
    private boolean includeNullDate = false;
    private boolean downloadData = false;
    private boolean gitCommit = false;
    private boolean simulateDataDownload = false;
    private int maxItemsInRegularPackage = PbConstants.DEFAULT_MAXITEMSINPACKAGE;
    private boolean unzipDownload = false;
    private int itemCount;
    private boolean verifyMetadataRead = false;

    // Constructor that gets all settings as map
    public PackageBuilder(final Properties parameters) {
        this.parameters.putAll(parameters);

    }

    public void run() throws RemoteException, Exception {

        // set loglevel based on parameters
        String paramLogLevel = parameters.getProperty("loglevel");
        thisLogLevel = Level.INFO;
        if (null != paramLogLevel) {
            switch (paramLogLevel) {
                case "FINE":
                    thisLogLevel = Level.FINE;
                    break;
                case "FINER":
                    thisLogLevel = Level.FINER;
                    break;
                case "FINEST":
                    thisLogLevel = Level.FINEST;
                    break;
                default:
                    break;
            }
        }

        logger.setLevel(thisLogLevel);
        logger.setUseParentHandlers(false);
        final LogFormatter formatter = new LogFormatter(thisLogLevel);
        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        handler.setLevel(thisLogLevel);
        logger.addHandler(handler);

        // Check what to do based on parameters
        this.includeChangeData = this.isParamTrue(PbProperties.INCLUDECHANGEDATA);
        this.downloadData = this.isParamTrue(PbProperties.DOWNLOAD);
        this.gitCommit = this.isParamTrue(PbProperties.GITCOMMIT);
        this.simulateDataDownload = this.isParamTrue(PbProperties.LOCALONLY);
        this.unzipDownload = this.isParamTrue(PbProperties.UNZIP);
        this.maxItemsInRegularPackage = Integer.valueOf(parameters.getProperty(PbProperties.MAXITEMS));
        this.includeNamespacedItems = this.isParamTrue(PbProperties.INCLUDENAMESPACEDITEMS);
        this.verifyMetadataRead = this.isParamTrue(PbProperties.VERIFYMETADATAREAD);
        this.skipManageableStateInstalled = this.isParamTrue(PbProperties.SKIPMANAGEABLESTATEINSTALLED);
        this.includeNullDate = this.isParamTrue(PbProperties.INCLUDENULLDATE);

        // initialize inventory - it will be used in both types of operations
        // (connect to org or run local)
        final HashMap<String, ArrayList<InventoryItem>> inventory = new HashMap<>();

        this.myApiVersion = Double.parseDouble(parameters.getProperty(PbProperties.APIVERSION));
        this.targetDir = Utils.checkPathSlash(Utils.checkPathSlash(parameters.getProperty(PbProperties.DESTINATION)));
        this.metaSourceDownloadDir = Utils.checkPathSlash(
                Utils.checkPathSlash(parameters.getProperty(PbProperties.METADATATARGETDIR)));
        // handling for building a package from a directory
        // if we have a base directory set, ignore everything else and generate
        // from the directory

        if (parameters.getProperty(PbProperties.BASEDIRECTORY) != null) {
            this.generateInventoryFromDir(inventory);
            this.mode = OperationMode.DIR;
        } else {
            this.generateInventoryFromOrg(inventory);
            this.mode = OperationMode.ORG;
        }
        // This is where the actual work happens creating Package[].xml AND download/unzip assets
        this.generatePackageXML(inventory);

        if (this.gitCommit) {

            // if we are doing git commit, have to prepare an inventory map so we can properly set each file's last modified date, etc
            final HashMap<String, InventoryItem> totalInventory = generateTotalInventory(inventory);

            // now walk the contents of the folder we've unzipped into and fix any of the dates 
            // need to get all the files for InventoryItems that translate to multiple files
            // so classes, etc. that have a -meta.xml, aura that have child directories, etc.
            new ZipAndFileFixer(totalInventory, logger).adjustFileDates(this.metaSourceDownloadDir);

            final GitOutputManager gom = new GitOutputManager(this.parameters, logger);
            gom.commitToGit(totalInventory);
        }
        this.endTiming(this.totalTimeStart, "Complete run");

    }

    public static InventoryItem getInventoryItemForFile(Map<String, InventoryItem> totalInventory, File f, String targetDirName) throws IOException {

        String key = f.getCanonicalPath();
        String basePath = new File(targetDirName).getCanonicalPath();
        key = key.replace(basePath, "");
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        // first check if there's a -meta.xml on the file name, and remove if there is
        if (key.endsWith("-meta.xml")) {
            key = key.replace("-meta.xml", "");
        }

        // now remove any suffix left on the file, if any?
        int index = key.lastIndexOf(".");
        if (index > 0) {
            key = key.substring(0, index);
        }

        // check if this maps to a key in the inventory
        InventoryItem item = totalInventory.get(key);

        while (item == null && key.contains("/")) {
            // let's try to cut the last piece of the path off and see if that gives us a usable key,
            // i.e. go from lwc/MyComp/MyCompHelper to lwc/MyComp

            key = key.substring(0, key.lastIndexOf("/"));
            item = totalInventory.get(key);

        }

        if (item == null) {
            logger.log(Level.INFO, "Found no inventory match for file " + f.getCanonicalPath());
        } else {
            logger.log(Level.FINER, "Found an inventory match for file " + f.getCanonicalPath() + " with key " + key);
        }

        // TODO Auto-generated method stub
        return item;
    }

    private HashMap<String, HashMap<String, ArrayList<InventoryItem>>> createPackageFiles(final HashMap<String, ArrayList<InventoryItem>> myCompleteInventory) {

        final ArrayList<HashMap<String, ArrayList<InventoryItem>>> files = new ArrayList<HashMap<String, ArrayList<InventoryItem>>>();

        // first, check how many items we have
        // if no more than one file, con't bother with any special treatment
        if (itemCount > maxItemsInRegularPackage) {

            // we'll get more than one package, need to check if we need special treatment
            // for profiles/permission sets
            // first, check if we have any items that contain permissions
            // if we do, put them in the first package, then add all items that
            // add permissions
            boolean exportingPermissions = false;
            for (String mdType : PbConstants.SPECIALTREATMENTPERMISSIONTYPES) {
                if (myCompleteInventory.containsKey(mdType)) {
                    exportingPermissions = true;
                    break;
                }
            }

            // if we are exporting permission, check if we're bringing anything that needs to go into the permissions file along
            boolean exportingPermissionsDependentItems = false;
            for (String mdType : PbConstants.ITEMSTOINCLUDEWITHPROFILESPERMSETS) {
                if (myCompleteInventory.containsKey(mdType)) {
                    exportingPermissionsDependentItems = true;
                    break;
                }
            }

            // so now, if we're actually exporting permissions and dependent items, we need to process them first
            // else, just continue as normal
            if (exportingPermissions && exportingPermissionsDependentItems) {
                // try to add all the special treatment types first and see if we get one file back or more
                // if 1, process the rest as normal
                // if not, warn that security items (Profiles/PermissionSets) may be incomplete, warn and 
                // process everything else

                logger.log(Level.INFO, "Asked to export permission settings (Profiles/PermSets). "
                        + "Will now try to bundle them and dependent items in one package.");

                // create inventory with all the special treatment items
                final HashMap<String, ArrayList<InventoryItem>> mySpecialTreatmentInventory = new HashMap<>();

                for (final String mdType : ArrayUtils.addAll(PbConstants.SPECIALTREATMENTPERMISSIONTYPES, PbConstants.ITEMSTOINCLUDEWITHPROFILESPERMSETS)) {
                    if (myCompleteInventory.get(mdType) != null && myCompleteInventory.get(mdType).size() > 0) {
                        mySpecialTreatmentInventory.put(mdType, myCompleteInventory.get(mdType));
                        myCompleteInventory.remove(mdType);
                    }
                }

                // now create files with just the special treatment types
                breakInventoryIntoFiles(files, mySpecialTreatmentInventory);

                // check if we got 1 file only, log if not
                if (files.size() != 1) {
                    logger.log(Level.INFO, "Permission settings (Profiles/PermSets) with dependent items over " + maxItemsInRegularPackage
                            + " items - won't fit in one package. Please note that contents of profiles/permission sets may be incomplete.");
                }

            }
        }
        // now add all the rest of the inventory

        breakInventoryIntoFiles(files, myCompleteInventory);

        // now generate file names if more than one package
        final HashMap<String, HashMap<String, ArrayList<InventoryItem>>> breakIntoFilesResult = new HashMap<>();

        int packageCounter = 1;
        for (final HashMap<String, ArrayList<InventoryItem>> singleFile : files) {
            if (packageCounter == 1) {
                breakIntoFilesResult.put("package.xml", singleFile);
                packageCounter++;
            } else {
                breakIntoFilesResult.put("package." + packageCounter++ + ".xml", singleFile);
            }
        }

        return breakIntoFilesResult;
    }

    // need to break the inventory into packages of less than MAXITEMS items
    // also need to ensure that if we have profiles or permission sets, they ideally go in the same package as:
    // ApexClass, CustomApplication, CustomField, CustomObject, CustomTab, ExternalDataSource, RecordType, ApexPage
    private ArrayList<HashMap<String, ArrayList<InventoryItem>>> breakInventoryIntoFiles(
            ArrayList<HashMap<String, ArrayList<InventoryItem>>> files,
            final HashMap<String, ArrayList<InventoryItem>> myFile) {

        int fileIndex = 1;
        int fileCount = 0;
        boolean continuingPreviousFile = false;

        HashMap<String, ArrayList<InventoryItem>> currentFile;
        if (files.isEmpty()) {
            currentFile = new HashMap<>();
        } else {
            currentFile = files.get(files.size() - 1);
            fileCount = countItemsInFile(currentFile);
            continuingPreviousFile = true;
            fileIndex = files.size();
        }

        for (final String mdType : myFile.keySet()) {
            final ArrayList<InventoryItem> mdTypeList = myFile.get(mdType);
            final int mdTypeSize = mdTypeList.size();

            // do we have room in this file for the
            if ((fileCount + mdTypeSize) > maxItemsInRegularPackage) {
                // no, we don't, finish file off, add to list, create new and
                // add to that

                logger.log(Level.FINE, "Type " + mdType + ", won't fit into this file - #items: " + mdTypeSize + ".");

                //put part of this type into this file
                ArrayList<InventoryItem> mdTypeListPartial = new ArrayList<>(mdTypeList.subList(0, maxItemsInRegularPackage - fileCount));
                currentFile.put(mdType, mdTypeListPartial);
                mdTypeList.removeAll(mdTypeListPartial);
                fileCount += mdTypeListPartial.size();
                logger.log(Level.FINE,
                        "Adding type: " + mdType + "(" + mdTypeListPartial.size() + " items) to file " + fileIndex + ", total count now: "
                        + fileCount);
                if (!continuingPreviousFile) {
                    files.add(currentFile);
                }
                logger.log(Level.FINE, "Finished composing file " + fileIndex + ", total count: " + fileCount + " items.");
                continuingPreviousFile = false;

                // finish and start new file
                currentFile = new HashMap<>();
                fileCount = 0;
                fileIndex++;
            }
            // now add this type to this file and continue
            // but need to check that this type isn't more than maxItems
            // if yes, then split this type into multiple pieces

            while (mdTypeList.size() > maxItemsInRegularPackage) {
                // too much even for a single file just with that, 
                // break up into multiple files

                ArrayList<InventoryItem> mdTypeListPartial = new ArrayList<>(mdTypeList.subList(0, maxItemsInRegularPackage));
                currentFile.put(mdType, mdTypeListPartial);
                fileCount += mdTypeListPartial.size();
                if (!continuingPreviousFile) {
                    files.add(currentFile);
                    continuingPreviousFile = false;
                    logger.log(Level.FINE, "Finished composing file " + fileIndex + ", total count: " + fileCount + " items.");
                }
                currentFile = new HashMap<>();
                mdTypeList.removeAll(mdTypeListPartial);
                logger.log(Level.FINE,
                        "Adding type: " + mdType + "(" + mdTypeListPartial.size() + " items) to file " + fileIndex + ", total count now: "
                        + fileCount);

                fileCount = 0;
                fileIndex++;

            }

            currentFile.put(mdType, mdTypeList);
            fileCount += mdTypeList.size();
            logger.log(Level.FINE,
                    "Adding type: " + mdType + "(" + mdTypeList.size() + " items) to file " + fileIndex + ", total count now: "
                    + fileCount);
        }

        // finish off any last file
        if (!continuingPreviousFile) {
            files.add(currentFile);
            logger.log(Level.FINE, "Finished composing file " + fileIndex + ", total count: " + fileCount + " items.");
        }

        return files;
    }

    private int countItemsInFile(HashMap<String, ArrayList<InventoryItem>> fileToCount) {
        int retval = 0;

        for (String mdType : fileToCount.keySet()) {
            if (fileToCount.get(mdType) != null) {
                retval += fileToCount.get(mdType).size();
            }
        }

        return retval;
    }

    private void endTiming(final long start, final String message) {
        final long end = System.currentTimeMillis();
        final long diff = ((end - start));
        final String hms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(diff),
                TimeUnit.MILLISECONDS.toMinutes(diff) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diff)),
                TimeUnit.MILLISECONDS.toSeconds(diff)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(diff)));
        logger.log(Level.FINE, message + " Duration: " + hms);
    }

    /*
	 * returns a Hashmap of InventoryItems, keyed by item.getFullName
     */
    private HashMap<String, InventoryItem> fetchMetadataType(final String metadataType)
            throws RemoteException, Exception {
        long startTime = this.startTiming();
        //		final MetadataFetchReturnResult fetchResult = new MetadataFetchReturnResult(metadataType);
        final HashMap<String, InventoryItem> packageInventoryList = new HashMap<>();
        int metadataItemCount = 0;
        try {
            ArrayList<ListMetadataQuery> queries = new ArrayList<>();

            // check if what we have here is in folders
            final DescribeMetadataObject obj = this.describeMetadataObjectsMap.get(metadataType);
            if ((obj != null) && (obj.getInFolder() == true)) {
                ArrayList<FileProperties> foldersToProcess = new ArrayList<>();
                foldersToProcess = generateFolderListToProcess(packageInventoryList, obj);
                final Iterator<FileProperties> folder = foldersToProcess.iterator();

                // generate queries array
                while (folder.hasNext()) {
                    ListMetadataQuery q = new ListMetadataQuery();
                    q.setFolder(folder.next().getFullName());
                    q.setType(metadataType);

                    queries.add(q);
                }
            } else {
                ListMetadataQuery q = new ListMetadataQuery();
                q.setType(metadataType);
                queries.add(q);
            }

            // now we have our queries list. Run through it no more than 3 at a time
            // query and store, keep going until nothing left
            Iterator<ListMetadataQuery> queryIterator = queries.iterator();

            do {

                ListMetadataQuery[] queryArray = new ListMetadataQuery[3];
                int queriesInArray = 0;
                while (queryIterator.hasNext() && queriesInArray < 3) {
                    queryArray[queriesInArray++] = queryIterator.next();
                }

                // generate metadata inventory
                final FileProperties[] srcMd = this.srcMetadataConnection.listMetadata(queryArray, this.myApiVersion);
                metadataItemCount += srcMd.length;

                ArrayList<String> forceIncludeNames_r = null;
                //Setup patterns for metadata types:

                if (parameters.containsKey(metadataType + "." + PbProperties.FORCEINCLUDENAMES)) {
                    forceIncludeNames_r = initializeStringArray(parameters.getProperty(metadataType + "." + PbProperties.FORCEINCLUDENAMES));
                }

                if (metadataItemCount > 0 || forceIncludeNames_r != null || metadataType.equals("StandardValueSet")) {
                    this.existingTypes.add(metadataType);
                }

                if (forceIncludeNames_r != null) {
                    String folderName = Character.toLowerCase(metadataType.charAt(0)) + metadataType.substring(1);
                    if (!folderName.endsWith("s")) {
                        folderName += "s";
                    }

                    Iterator it = forceIncludeNames_r.iterator();
                    while (it.hasNext()) {
                        String forceIncludeItem = (String) it.next();
                        InventoryItem i = new InventoryItem(forceIncludeItem, folderName, metadataType);
                        packageInventoryList.put(forceIncludeItem, i);
                        logger.log(Level.FINER, "Adding item " + i.getExtendedName() + " to inventory. - Filename: " + i.getFullName() + " itemName: " + i.getItemName());
                    }
                }

                if (((srcMd != null) && (srcMd.length > 0)) || metadataType.equals("StandardValueSet")) {
                    // hack alert - currently (API 45) listMetadata returns nothing for StandardValueSet
                    if (!metadataType.equals("StandardValueSet")) { //List setup in PbConstants.java
                        for (final FileProperties n : srcMd) {
                            boolean skipFile = false;
                            //Always look at InstalledPackages if they are included as a metadata type regardless of the namespace. 
                            if ((includeNamespacedItems || n.getNamespacePrefix() == null) || n.getNamespacePrefix().equals("") || (metadataType.equals("InstalledPackage"))) {
                                // packageMap.add(n.getFullName());
                                InventoryItem i = new InventoryItem(n.getFullName(), n, this.describeMetadataObjectsMap.get(metadataType));

                                if (obj != null && obj.getInFolder()) {
                                    logger.log(Level.FINE, "Folder Detected " + obj.getXmlName() + " filename: " + i.getFileProperties().getFullName());
                                    int indexOfFwdSlash = i.getFileProperties().getFullName().indexOf("/");
                                    String directory = i.getFileProperties().getFullName().substring(0, indexOfFwdSlash);
                                    String newDirectory = folderRecursivePath.get(obj.getDirectoryName() + '/' + directory);

                                    String newFilename = newDirectory + i.getFileProperties().getFullName().substring(indexOfFwdSlash);
                                    i.setfullNameAndDirOverride(newFilename);

                                }
                                if (!skipFile) {
                                    packageInventoryList.put(n.getFullName(), i);
                                    logger.log(Level.FINER, "Adding item " + i.getExtendedName() + " to inventory. - Filename: " + i.getFullName() + " itemName: " + i.getItemName());
                                }
                            } else {
                                logger.log(Level.FINE, "skipping item " + n.getNamespacePrefix() + " from " + n.getFullName() + " inventory.");
                            }
                            //
                        }
                    } else {

                        for (final String s : PbConstants.STANDARDVALUETYPESARRAY) {
                            InventoryItem i = new InventoryItem(s, "standardValueSets", metadataType);
                            packageInventoryList.put(s, i);
                            logger.log(Level.FINER, "Adding item extendedName: " + i.getExtendedName() + " to inventory. FullName: " + i.getFullName());
                        }
                    }

                } else {
                    logger.log(Level.FINER, "No items of this type, skipping...");
                }

            } while (queryIterator.hasNext());

        } catch (final ConnectionException e) {
            // ce.printStackTrace();
            logger.log(Level.INFO, "\nException processing: " + metadataType);
            logger.log(Level.INFO, "Error: " + e.getMessage());
        }
        this.endTiming(startTime, "");

        return packageInventoryList;
    }

    private String getFullFolderPath(DescribeMetadataObject metadataType, String lookupKey, String folderId, int level) {

        final String soqlQry = "select Id, ParentId, DeveloperName, AccessType, IsReadonly, Type, NamespacePrefix from Folder WHERE Id IN (%s)";
        QueryResult qResult;

        try {
            qResult = this.srcPartnerConnection.query(String.format(soqlQry, "'" + folderId + "'"));
            if (qResult.getSize() == 1) {

                final SObject[] records = qResult.getRecords();
                SObject o = records[0];
                String parentId = (String) o.getField("ParentId");
                String developerName = (String) o.getField("DeveloperName");
                logger.log(Level.FINER, String.format("getFullFolderPath metadataType: %s || folderId: %s || parentId %s || developerName: %s", metadataType.getXmlName(), folderId, parentId, developerName));

                if (parentId == null && folderRecursivePath.containsKey(lookupKey)) {
                    logger.log(Level.FINER, "key found: " + lookupKey);
                    return folderRecursivePath.get(lookupKey);
                } else {
                    String fullPath = developerName;
                    if (parentId != null && parentId.length() > 0) {
                        // Folder has a parent, need to go to the parent to get the full path (recursive)
                        logger.log(Level.FINER, "Parent found. Recursive call. fullPath: " + fullPath + " parentId: " + parentId);
                        String parentPath = getFullFolderPath(metadataType, lookupKey, parentId, level + 1);
                        fullPath = parentPath + '/' + developerName;

                    }

                    if (level == 1) {
                        logger.log(Level.FINER, "Adding " + lookupKey + " with value: " + fullPath);
                        folderRecursivePath.put(lookupKey, fullPath);
                    }

                    return fullPath;
                }
            } else {

                folderRecursivePath.put(lookupKey, "unfiled$public");
                return "unfiled$public";
            }

            // run the query
        } catch (ConnectionException ex) {
            Logger.getLogger(PackageBuilder.class.getName()).log(Level.SEVERE, null, ex);
            return "RecordLookupError";

        }

    }
    // inventory is a list of lists
    // keys are the metadata types
    // e.g. flow, customobject, etc.

    private ArrayList<FileProperties> generateFolderListToProcess(HashMap<String, InventoryItem> packageInventoryList, DescribeMetadataObject metadataType)
            throws ConnectionException {
        logger.log(Level.FINE, metadataType.getXmlName() + " is stored in folders. Getting folder list.");
        final ArrayList<FileProperties> foldersToProcess = new ArrayList<>();
        final ListMetadataQuery query = new ListMetadataQuery();
        // stupid hack for emailtemplate folder name
        String type;
        if (metadataType.getXmlName().toLowerCase().equals("emailtemplate")) {
            type = "EmailFolder";
        } else {
            type = metadataType.getXmlName() + "Folder";
        }

        ArrayList<Pattern> skipPatterns_d = initializePatternArray(parameters.getProperty(PbProperties.SKIPPATTERNS));
        ArrayList<Pattern> includePatterns_d = initializePatternArray(parameters.getProperty(PbProperties.INCLUDEPATTERNS));
        ArrayList<Pattern> forceIncludePatterns_d = initializePatternArray(parameters.getProperty(PbProperties.FORCEINCLUDEPATTERNS));

        //Setup patterns for metadata types:
        ArrayList<Pattern> skipPatterns_r;
        ArrayList<Pattern> includePatterns_r;
        ArrayList<Pattern> forceIncludePatterns_r;

        //Setup patterns for metadata types:
        skipPatterns_r = parameters.containsKey(type + "." + PbProperties.SKIPPATTERNS) ? initializePatternArray(parameters.getProperty(type + "." + PbProperties.SKIPPATTERNS)) : skipPatterns_d;
        includePatterns_r = parameters.containsKey(type + "." + PbProperties.INCLUDEPATTERNS) ? initializePatternArray(parameters.getProperty(type + "." + PbProperties.INCLUDEPATTERNS)) : includePatterns_d;
        forceIncludePatterns_r = parameters.containsKey(type + "." + PbProperties.FORCEINCLUDEPATTERNS) ? initializePatternArray(parameters.getProperty(type + "." + PbProperties.FORCEINCLUDEPATTERNS)) : forceIncludePatterns_d;

        String activeSkipPattern = "\n*\n************************";
        activeSkipPattern += "\n* skippatterns                   {" + skipPatterns_r.size() + "} " + skipPatterns_r;
        activeSkipPattern += "\n* includepatterns                {" + includePatterns_r.size() + "} " + includePatterns_r;
        activeSkipPattern += "\n* forceincludepatterns           {" + forceIncludePatterns_r.size() + "} " + forceIncludePatterns_r;
        activeSkipPattern += "\n************************";
        logger.log(Level.INFO, "\n***************************\n* Skip check\n* \n* " + type + activeSkipPattern);

        query.setType(type);
        final FileProperties[] srcMd = this.srcMetadataConnection.listMetadata(
                new ListMetadataQuery[]{query},
                this.myApiVersion);
        if ((srcMd != null) && (srcMd.length > 0)) {
            for (final FileProperties n : srcMd) {

                // add folder to final inventory
                if (n.getManageableState().name().equals("installed")) {
                    logger.log(Level.FINER, "Skipping folder " + n.getFullName() + " because it is managed.");
                } else {
//                    String tempArray[] = { n.getFullName()};
//                    ReadResult rr =  this.srcMetadataConnection.readMetadata(type, tempArray);
                    boolean itemSkipped = false;
                    boolean forceInclude = false;

                    if (!forceInclude && !itemSkipped && forceIncludePatterns_r.size() > 0) {
                        for (Pattern p : forceIncludePatterns_r) {
                            final Matcher m = p.matcher(n.getFullName());
                            if (m.matches()) {
                                logger.log(Level.FINE, "forceincludepatterns: " + p.pattern() + " matches the metadata item: " + n.getFullName() + ", item will be skipped.");
                                forceInclude = true;
                                break;
                            }
                        }
                    }

                    if (!forceInclude && !itemSkipped && skipPatterns_r.size() > 0) {
                        for (Pattern p : skipPatterns_r) {
                            final Matcher m = p.matcher(n.getFullName());
                            if (m.matches()) {
                                logger.log(Level.FINE, "skippatterns: " + p.pattern() + " matches the metadata item: " + n.getFullName() + ", item will be skipped.");
                                itemSkipped = true;
                                break;
                            }
                        }
                    }

                    if (!forceInclude && !itemSkipped && includePatterns_r.size() > 0) {
                        boolean matchesPattern = false;
                        for (Pattern p : includePatterns_r) {
                            final Matcher m = p.matcher(n.getFullName());
                            if (m.matches()) {
                                matchesPattern = true;
                                break;
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "includepatterns (no match): " + type + " does not match the metadata item: " + n.getFullName() + ", item will be skipped");
                            itemSkipped = true;
                        }
                    }
                    if (!itemSkipped) {
                        foldersToProcess.add(n);

                        String lookupKey = metadataType.getDirectoryName() + '/' + n.getFullName();
                        String fullPath = getFullFolderPath(metadataType, lookupKey, n.getId(), 1);

                        packageInventoryList.put(n.getFullName(), new InventoryItem(n.getFullName(), n, metadataType, true, fullPath));

                        logger.log(Level.FINER, "Adding folder " + n.getFullName() + " to inventory. Path: " + n.getFileName() + " fullPath " + fullPath);
                    }
                }

                itemCount++;
            }
            logger.log(Level.FINE, foldersToProcess.size() + " folders found. Adding to retrieve list.");
        }
        return foldersToProcess;
    }

    private Collection<String> generateFileList(final File node, final String baseDir) {

        final Collection<String> retval = new ArrayList<>();
        // add file only
        if (node.isFile()) {
            retval.add(this.generateZipEntry(node.getAbsoluteFile().toString(), baseDir));
            // retval.add(baseDir + "/" + node.getAbsoluteFile().toString());
            // retval.add(node.getName());
        } else if (node.isDirectory()) {
            final String[] subNote = node.list();
            for (final String filename : subNote) {
                retval.addAll(this.generateFileList(new File(node, filename), baseDir));
            }
        }
        return retval;
    }

    private void generateInventoryFromDir(final HashMap<String, ArrayList<InventoryItem>> inventory)
            throws IOException {
        final String basedir = parameters.getProperty(PbProperties.BASEDIRECTORY);

        // check if the directory is valid
        final HashMap<String, HashSet<InventoryItem>> myInventory = new HashMap<>();

        if (!Utils.checkIsDirectory(basedir)) {
            // log error and exit

            logger.log(Level.INFO, "Base directory parameter provided: " + basedir
                    + " invalid or is not a directory, cannot continue.");
            System.exit(1);
        }

        // directory valid - enumerate and generate inventory
        final Collection<String> filelist = this.generateFileList(new File(basedir), basedir);

        // so now we have a list of folders/files
        // need to convert to inventory for package.xml generator
        for (final String s : filelist) {
            // ignore -meta.xml

            if (s.contains("-meta.xml")) {
                continue;
            }

            // split into main folder + rest
            try {

                // ignore anything which doesn't have a path separator (i.e. not
                // a folder)
                final int separatorLocation = s.indexOf(File.separator);

                if (separatorLocation == -1) {
                    logger.log(Level.INFO, "No folder in: " + s + ",skipping...");
                    continue;
                }

                final String foldername = s.substring(0, separatorLocation);
                String filename = s.substring(separatorLocation + 1);

                // split off file name suffix
                filename = filename.substring(0, filename.lastIndexOf("."));

                // ignore anything starting with a .
                if (filename.startsWith(".")) {
                    continue;
                }

                // figure out based on foldername what the metadatatype is
                String mdType = Utils.getMetadataTypeForDir(foldername);

                // if not found, try lowercase
                if (mdType == null) {
                    mdType = Utils.getMetadataTypeForDir(foldername.toLowerCase());
                }

                if (mdType == null) {
                    logger.log(Level.INFO, "Couldn't find type mapping for item : " + mdType + " : " + filename + ", original path: "
                            + s + ",skipping...");
                    continue;
                }

                // generate inventory entry
                HashSet<InventoryItem> typeInventory = myInventory.get(mdType);
                if (typeInventory == null) {
                    typeInventory = new HashSet<>();
                    myInventory.put(mdType, typeInventory);
                    logger.log(Level.INFO, "Created inventory record for type: " + mdType);
                }

                // check if there is a folder in the filename and it's aura -
                // then we need to leave the folder, skip the item
                if (filename.contains("/") && mdType.equals("AuraDefinitionBundle")) {
                    final String subFoldername = filename.substring(0, filename.indexOf("/"));
                    typeInventory.add(new InventoryItem(subFoldername, null, mdType));
                    logger.log(Level.FINE, "Added: " + mdType + " : " + subFoldername + ", to inventory, original path: " + s);
                    continue;
                }

                // check if there is a folder in the filename - then we need to
                // add the folder as well
                if (filename.contains("/")) {
                    final String subFoldername = filename.substring(0, filename.indexOf("/"));
                    typeInventory.add(new InventoryItem(subFoldername, null, mdType));
                }

                typeInventory.add(new InventoryItem(filename, null, mdType));
                logger.log(Level.FINE, "Added: " + mdType + " : " + filename + ", to inventory, original path: " + s);

                // convert myinventory to the right return type
            } catch (final IOException e) {
                // Something bad happened
                logger.log(Level.INFO, "Something bad happened on file: " + s + ", skipping...");
            }

        }
        myInventory.keySet().forEach(myMdType -> {
            final ArrayList<InventoryItem> invType = new ArrayList<>();
            invType.addAll(myInventory.get(myMdType));
            inventory.put(myMdType, invType);
        }); //
    }

    /*
	 *
	 * this method will populate username (Salesforce user name in email format)
	 * and user email fields on the inventoryItems for use when outputting
	 * change telemetry
	 *
     */
    private void generateInventoryFromOrg(final HashMap<String, ArrayList<InventoryItem>> inventory)
            throws RemoteException, Exception {

        // Initialize the metadata connection we're going to need
        this.srcUrl = parameters.getProperty(PbProperties.SERVERURL) + PbProperties.URLBASE + this.myApiVersion;
        this.srcUser = parameters.getProperty(PbProperties.USERNAME);
        this.srcPwd = parameters.getProperty(PbProperties.PASSWORD);
        this.srcToken = parameters.getProperty(PbProperties.TOKEN);
        this.skipItems = parameters.getProperty(PbProperties.SKIPPATTERNS);
        // Make a login call to source
        this.srcPartnerConnection = LoginUtil.soapLogin(this.srcUrl, this.srcUser, this.srcPwd, this.srcToken, logger);

        this.srcMetadataConnection = LoginUtil.mdLogin(this.srcUrl, this.srcUser, this.srcPwd, this.srcToken, logger);

        // Figure out what we are going to be fetching
        final ArrayList<String> workToDo = new ArrayList<>(this.getTypesToFetch());
        Collections.sort(workToDo);

        logger.log(Level.INFO, "Will fetch: " + String.join(", ", workToDo) + " from: " + this.srcUrl);
        logger.log(Level.FINE, "Using user: " + this.srcUser + " skipping: " + this.skipItems);

        logger.log(Level.INFO, "target directory: " + this.targetDir);

        Utils.checkDir(this.targetDir);

        final Iterator<String> i = workToDo.iterator();
        int counter = 0;
        while (i.hasNext()) {
            counter++;
            final String mdType = i.next();

            if (logger.getLevel() == Level.FINE) {
                logger.log(Level.FINE, "*********************************************");
            }
            logger.log(Level.INFO, "Processing type " + counter + " out of " + workToDo.size() + ": " + mdType + (Level.INFO == thisLogLevel ? "\\" : ""));

            final ArrayList<InventoryItem> mdTypeItemList = new ArrayList<>(this.fetchMetadataType(mdType).values());
            Collections.sort(mdTypeItemList, (o1, o2) -> o1.getItemName().compareTo(o2.getItemName()));
            inventory.put(mdType, mdTypeItemList);

            logger.log(Level.INFO, " items: " + mdTypeItemList.size());

            if (logger.getLevel() == Level.FINE) {
                logger.log(Level.FINE, "Finished processing: " + mdType);
                logger.log(Level.FINE, "*********************************************");
                logger.log(Level.FINE, "");
            }

        }
    }

    private HashMap<String, HashMap<String, ArrayList<InventoryItem>>> generatePackageXML(
            final HashMap<String, ArrayList<InventoryItem>> inventory)
            throws Exception {

        int pkgItemCount = 0;
        int skipCount = 0;

        final HashMap<String, ArrayList<InventoryItem>> myFile = new HashMap<>();

        final ArrayList<String> types = new ArrayList<>();
        types.addAll(inventory.keySet());
        Collections.sort(types);

        for (final String mdType : types) {

            // check if we have any items in this category
            final ArrayList<InventoryItem> items = inventory.get(mdType);
            if (items.size() < 1) {
                continue;
            }

            myFile.put(mdType, new ArrayList<InventoryItem>());

            Collections.sort(items, (o1, o2) -> o1.getItemName().compareTo(o2.getItemName()));
            for (final InventoryItem item : items) {
                myFile.get(mdType).add(item);
                pkgItemCount++;
            }
        }

        this.populateUserEmails(myFile);

        // now check if anything we have needs to be skipped
        skipCount = this.handleSkippingItems(myFile);

        // now break it up into files if needed
        final HashMap<String, HashMap<String, ArrayList<InventoryItem>>> files = this.createPackageFiles(myFile);

        writeAndDownloadPackages(files);

        if (downloadData) {

            if (this.parameters.containsKey(PbProperties.STRIPPROFILEUSERPERMISSIONS)
                    && myFile.containsKey("Profile")) {
                logger.log(Level.INFO, "Asked to strip user permissions from Profiles - will do so now.");
                ProfileCompare pc = new ProfileCompare(thisLogLevel);
                pc.stripUserPermissionsFromProfiles(parameters.getProperty(PbProperties.METADATATARGETDIR));

            }
        }

        final ArrayList<String> typesFound = new ArrayList<>(this.existingTypes);
        Collections.sort(typesFound);

        logger.log(Level.INFO, "Types found in org: " + typesFound.toString());

        logger.log(Level.INFO, "Total items in package.xml: " + (pkgItemCount - skipCount));
        logger.log(Level.INFO, "Total items overall: " + pkgItemCount + ", items skipped: " + skipCount
                + " (excludes count of items in type where entire type was skipped)");

        return files;

    }

    /*
	 * this method will take the total inventory by type HashMap<String, ArrayList<InventoryItem>>
	 * and generate a single map keyed by a unique key which is the filename 
	 * 
     */
    private HashMap<String, InventoryItem> generateTotalInventory(HashMap<String, ArrayList<InventoryItem>> inventory) {
        HashMap<String, InventoryItem> totalInventory = new HashMap<>();

        inventory.keySet().forEach(metadataType -> {
            inventory.get(metadataType).stream().map(item -> {
                String key = item.getFolderAndFileName();
                // strip suffix from file name
                int idx = key.lastIndexOf(".");
                if (idx > 0) {
                    key = key.substring(0, idx);
                }
                totalInventory.put(key, item);
                return key;
            }).forEachOrdered(key -> {
                logger.log(Level.FINE, "Added: " + key + " to git inventory");
            });
        });

        return totalInventory;
    }

    private void writeAndDownloadPackages(HashMap<String, HashMap<String, ArrayList<InventoryItem>>> files) throws Exception {
        // USE THREADS TO speed things up
        // final int totalFiles = files.size();
        final ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(PbConstants.CONCURRENT_THREADS);

        final Collection<PackageAndFilePersister> allPersisters = new ArrayList<>();

        // Add all XML Files to the download queue
        files.forEach((curFileName, members) -> {
            final PackageAndFilePersister pfp = new PackageAndFilePersister(this.myApiVersion,
                    this.targetDir,
                    this.metaSourceDownloadDir,
                    parameters.getProperty(PbProperties.DESTINATION),
                    members, curFileName,
                    this.includeChangeData,
                    this.downloadData,
                    this.unzipDownload,
                    this.srcMetadataConnection);
            if (this.simulateDataDownload) {
                pfp.setLocalOnly();
            }
            allPersisters.add(pfp);
        });

        // new feature here
        // if doing git commit, clean out the target directory before starting (unless requested not to)
        // but first check that someplace above it actually has the GIT stuff in it
        if (gitCommit) {
            File targetDirectory = new File(parameters.getProperty(PbProperties.DESTINATION) + File.separator + this.metaSourceDownloadDir);
            if (!isParamTrue(PbProperties.RETAINTARGETDIR)) {
                FileUtils.deleteDirectory(targetDirectory);
            }
        }

        WORKER_THREAD_POOL.invokeAll(allPersisters).stream().map(future -> {
            String result = null;
            try {
                final PersistResult pr = future.get();
                result = "Completion of " + pr.getName() + ": "
                        + String.valueOf(pr.getStatus());

            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage());
                result = e.getMessage();
            }
            return result;

        }).forEach(result -> {
            logger.log(Level.INFO, result);
        });

        if (!WORKER_THREAD_POOL.awaitTermination(1, TimeUnit.SECONDS)) {
            //			logger.log(Level.SEVERE, "Threads not terminated within 15 sec");
            WORKER_THREAD_POOL.shutdownNow();
        }

    }

    private String generateZipEntry(final String file, final String sourceFolder) {
        final int indexOfSourceFolder = file.lastIndexOf(sourceFolder);
        return file.substring(indexOfSourceFolder + sourceFolder.length() + 1, file.length());
    }

    private HashSet<String> getTypesToFetch() throws ConnectionException {

        final HashSet<String> typesToFetch = new HashSet<>();
        final String mdTypesToExamine = parameters.getProperty(PbProperties.METADATAITEMS);
        final String mdTypesToSkip = parameters.getProperty(PbProperties.METADATAITEMSTOSKIP);

        // get a describe
        final DescribeMetadataResult dmr = this.srcMetadataConnection.describeMetadata(this.myApiVersion);
        this.describeMetadataObjectsMap = new HashMap<>();

        for (final DescribeMetadataObject obj : dmr.getMetadataObjects()) {
            this.describeMetadataObjectsMap.put(obj.getXmlName(), obj);
            if (obj.getChildXmlNames() != null && obj.getChildXmlNames().length > 0) {
                for (final String childName : obj.getChildXmlNames()) {
                    this.describeMetadataObjectsMap.put(childName, obj);
                }
            }
        }
        logger.finest(describeMetadataObjectsMap.toString());
        // if a metadataitems parameter was provided, we use that
        if (mdTypesToExamine != null) {
            for (final String s : mdTypesToExamine.split(",")) {
                typesToFetch.add(s.trim());
            }
        } else {
            // no directions on what to fetch - go get everything
            logger.log(Level.INFO, "No metadataitems (-mi) parameter found, will inventory the whole org");

            this.describeMetadataObjectsMap.keySet().forEach(obj -> {
                typesToFetch.add(obj.trim());

            });

            // now add the list of types to be added manually
            for (String manualType : PbConstants.ADDITIONALTYPESTOADD) {
                typesToFetch.add(manualType.trim());
            }

            if (mdTypesToSkip != null) {
                for (final String s : mdTypesToSkip.split(",")) {
                    typesToFetch.remove(s.trim());
                }
            }
            // check API version - in 45+, remove FlowDefinition
//            if (Double.valueOf(parameters.getProperty(PbProperties.APIVERSION)) >= 45) {
//                typesToFetch.remove("FlowDefinition");
//            }

        }
        return typesToFetch;
    }


    /* function to handle removing items from the result
	 * 
	 * will handle 	-sp (skipPatterns_d), -ip (includepatterns), -su (skipusers) -iu (includeusers)
	 * 				-se (skipEmail_r) -ie (includeemail) parameters to filter the list
     */
    private int handleSkippingItems(final HashMap<String, ArrayList<InventoryItem>> myFile) {

        int skipCount = 0;
        int lastSkipCount = 0;
        int lastUnskippedItemCount = 0;
        int unskippedItemCount = 0;

        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");

        //Patterns used while matching (defined for each metadata type)
        ArrayList<Pattern> skipPatterns_r;
        ArrayList<Pattern> skipEmail_r;
        ArrayList<Pattern> includeEmail_r;
        ArrayList<Pattern> skipUsername_r;
        ArrayList<Pattern> includeUsername_r;
        ArrayList<Pattern> includePatterns_r;
        ArrayList<Pattern> forceIncludePatterns_r;
        ArrayList<Pattern> metadataSubTypeIncludePatterns_r;

//Default Patterns
        ArrayList<Pattern> skipPatterns_d = initializePatternArray(parameters.getProperty(PbProperties.SKIPPATTERNS));
        ArrayList<Pattern> skipEmail_d = initializePatternArray(parameters.getProperty(PbProperties.SKIPEMAIL));
        ArrayList<Pattern> includePatterns_d = initializePatternArray(parameters.getProperty(PbProperties.INCLUDEPATTERNS));
        ArrayList<Pattern> includeEmail_d = initializePatternArray(parameters.getProperty(PbProperties.INCLUDEEMAIL));
        ArrayList<Pattern> skipUsername_d = initializePatternArray(parameters.getProperty(PbProperties.SKIPUSERNAME));
        ArrayList<Pattern> includeUsername_d = initializePatternArray(parameters.getProperty(PbProperties.INCLUDEUSERNAME));
        ArrayList<Pattern> forceIncludePatterns_d = initializePatternArray(parameters.getProperty(PbProperties.FORCEINCLUDEPATTERNS));

        // initialize date ranges, if any
        String fromDateString = parameters.getProperty(PbProperties.FROMDATE);
        String toDateString = parameters.getProperty(PbProperties.TODATE);
        Date fromDate = null;
        Date toDate = null;
        boolean skipManageableStateInstalled_r = false;
        boolean includeNullDate_r = false;
        boolean limitToActive_r = false;
        boolean verifyMetadataRead_r = false;

        if (fromDateString != null && fromDateString.length() >= 8) {
            try {
                fromDate = Date.valueOf(fromDateString);
            } catch (IllegalArgumentException e) {
                logger.log(Level.FINE, "FromDate value: " + fromDateString + " cannot be parsed to a proper date. Required format: YYYY-[M]M-[D]D. Continuing without FromDate parameter.");
            }
        }

        if (toDateString != null && toDateString.length() >= 8) {
            try {
                toDate = Date.valueOf(toDateString);
            } catch (IllegalArgumentException e) {
                logger.log(Level.FINE, "ToDate value: " + toDateString + " cannot be parsed to a proper date. Required format: YYYY-[M]M-[D]D. Continuing without ToDate parameter.");
            }
        }
        SortedSet<String> sortedSet = new TreeSet<String>(myFile.keySet());
        for (final String mdType : sortedSet) { //Loop through Metadata Types a-z
            lastSkipCount = skipCount;
            lastUnskippedItemCount = unskippedItemCount;

            skipManageableStateInstalled_r = parameters.containsKey(mdType + "." + PbProperties.SKIPMANAGEABLESTATEINSTALLED) ? isParamTrue(mdType + "." + PbProperties.SKIPMANAGEABLESTATEINSTALLED) : this.skipManageableStateInstalled;
            includeNullDate_r = parameters.containsKey(mdType + "." + PbProperties.INCLUDENULLDATE) ? isParamTrue(mdType + "." + PbProperties.INCLUDENULLDATE) : this.includeNullDate;
            limitToActive_r = parameters.containsKey(mdType + "." + PbProperties.LIMITTOACTIVE) ? isParamTrue(mdType + "." + PbProperties.LIMITTOACTIVE) : false;
            verifyMetadataRead_r = parameters.containsKey(mdType + "." + PbProperties.VERIFYMETADATAREAD) ? isParamTrue(mdType + "." + PbProperties.VERIFYMETADATAREAD) : this.verifyMetadataRead;

            //Setup patterns for metadata types:
            skipPatterns_r = parameters.containsKey(mdType + "." + PbProperties.SKIPPATTERNS) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.SKIPPATTERNS)) : skipPatterns_d;
            skipEmail_r = parameters.containsKey(mdType + "." + PbProperties.SKIPEMAIL) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.SKIPEMAIL)) : skipEmail_d;
            includePatterns_r = parameters.containsKey(mdType + "." + PbProperties.INCLUDEPATTERNS) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.INCLUDEPATTERNS)) : includePatterns_d;
            includeEmail_r = parameters.containsKey(mdType + "." + PbProperties.INCLUDEEMAIL) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.INCLUDEEMAIL)) : includeEmail_d;
            skipUsername_r = parameters.containsKey(mdType + "." + PbProperties.SKIPUSERNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.SKIPUSERNAME)) : skipUsername_d;
            includeUsername_r = parameters.containsKey(mdType + "." + PbProperties.INCLUDEUSERNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.INCLUDEUSERNAME)) : includeUsername_d;
            forceIncludePatterns_r = parameters.containsKey(mdType + "." + PbProperties.FORCEINCLUDEPATTERNS) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.FORCEINCLUDEPATTERNS)) : forceIncludePatterns_d;
            metadataSubTypeIncludePatterns_r = parameters.containsKey(mdType + "." + PbProperties.METADATASUBTYPEINCLUDEPATTERN) ? initializePatternArray(parameters.getProperty(mdType + "." + PbProperties.METADATASUBTYPEINCLUDEPATTERN)) : forceIncludePatterns_d;

            //Type specific overrides
            if (mdType.equals("InstalledPackage")) {
                logger.log(Level.INFO, "Setting override for " + mdType + " skipmanagedstateinstalled = false");
                skipManageableStateInstalled_r = false;
                logger.log(Level.INFO, "Setting override for " + mdType + " verifymetadataread = true");
                verifyMetadataRead_r = true;
            }
            if (mdType.equals("Flow") && !verifyMetadataRead_r) {
                logger.log(Level.INFO, "Setting override for " + mdType + " verifymetadataread = true");
                verifyMetadataRead_r = true;
            }

            String activeSkipPattern = "\n*\n************************";
            activeSkipPattern += "\n* skipmanagedstateinstalled       " + skipManageableStateInstalled_r;

            if (mdType.equals("Flow")) {
                activeSkipPattern += "\n* limittoactive                   " + limitToActive_r;
            }
            activeSkipPattern += "\n* includenulldate                 " + includeNullDate_r;
            activeSkipPattern += "\n* verifymetadataread              " + verifyMetadataRead_r;
            activeSkipPattern += "\n* skippatterns                   {" + skipPatterns_r.size() + "} " + skipPatterns_r;
            activeSkipPattern += "\n* includepatterns                {" + includePatterns_r.size() + "} " + includePatterns_r;
            activeSkipPattern += "\n* metadatasubtypeincludepattern  {" + metadataSubTypeIncludePatterns_r.size() + "} " + metadataSubTypeIncludePatterns_r;
            activeSkipPattern += "\n* forceincludepatterns           {" + forceIncludePatterns_r.size() + "} " + forceIncludePatterns_r;
            activeSkipPattern += "\n* skipemail                      {" + skipEmail_r.size() + "} " + skipEmail_r;
            activeSkipPattern += "\n* includeemail                   {" + includeEmail_r.size() + "} " + includeEmail_r;
            activeSkipPattern += "\n* skipusername                   {" + skipUsername_r.size() + "} " + skipUsername_r;
            activeSkipPattern += "\n* includeusername                {" + includeUsername_r.size() + "} " + includeUsername_r;
            activeSkipPattern += "\n************************";
            logger.log(Level.INFO, "\n***************************\n* Skip check\n* \n* " + mdType + activeSkipPattern);

            boolean ignoreNullDate;
            final ArrayList<InventoryItem> items = myFile.get(mdType);
            for (Iterator<InventoryItem> i = items.iterator(); i.hasNext();) {
                //Setups for Metadata Object:
                final InventoryItem mdItem = i.next();
                String metadataObjectName = mdItem.getFullName() == null ? mdItem.getExtendedName() : mdItem.getFullName();

                ignoreNullDate = includeNullDate_r;
                boolean itemSkipped = false;
                boolean forceInclude = false;
                logger.log(Level.FINEST, "Skip pattern check on: " + metadataObjectName);

                for (Pattern p : forceIncludePatterns_r) {
                    final Matcher m = p.matcher(metadataObjectName);
                    if (m.matches()) {
                        logger.log(Level.FINE, "forceincludepatterns : " + p.pattern() + " matches the metadata item: " + metadataObjectName + ", item will be included.");
                        forceInclude = true;
                        break;
                    }
                }
                if (!forceInclude) {

                    if (!itemSkipped && skipPatterns_r.size() > 0) {
                        for (Pattern p : skipPatterns_r) {
                            final Matcher m = p.matcher(metadataObjectName);
                            if (m.matches()) {
                                logger.log(Level.FINE, "skippatterns: " + p.pattern() + " matches the metadata item: " + metadataObjectName + ", item will be skipped.");
                                skipCount++;
                                itemSkipped = true;
                                break;
                            }
                        }
                    }

                    if (!itemSkipped && includePatterns_r.size() > 0) {
                        boolean matchesPattern = false;
                        for (Pattern p : includePatterns_r) {
                            final Matcher m = p.matcher(metadataObjectName);
                            if (m.matches()) {
                                matchesPattern = true;
                                ignoreNullDate = true;
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "includepatterns (no match): " + metadataObjectName + " does not match any item name include patterns, item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                        }
                    }
                    if (!itemSkipped && skipUsername_r.size() > 0) {
                        for (Pattern p : skipUsername_r) {
                            final Matcher m = p.matcher(mdItem.getLastModifiedByName());
                            if (m.matches()) {
                                logger.log(Level.FINE, "skipusername: " + p.pattern() + " matches the metadata item: " + metadataObjectName
                                        + " (" + mdItem.getLastModifiedByName() + "), item will be skipped.");
                                skipCount++;
                                itemSkipped = true;
                                break;
                            }
                        }
                    }
                    if (!itemSkipped && includeUsername_r.size() > 0) {
                        boolean matchesPattern = false;
                        String lastModUsername = mdItem.getLastModifiedByUsername() == null ? "null" : mdItem.getLastModifiedByUsername();
                        for (Pattern p : includeUsername_r) {
                            final Matcher m = p.matcher(lastModUsername);
                            if (m.matches()) {
                                matchesPattern = true;
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "includeusername (no match): " + metadataObjectName + " (" + mdItem.getLastModifiedByName()
                                    + ") does not match any user name include patterns, item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                        }
                    }
                    if (!itemSkipped && skipEmail_r.size() > 0) {
                        String lastModEmail = mdItem.getLastModifiedByEmail() == null ? "null" : mdItem.getLastModifiedByEmail();
                        for (Pattern p : skipEmail_r) {

                            final Matcher m = p.matcher(lastModEmail);
                            if (m.matches()) {
                                logger.log(Level.FINE, "skipemail: " + p.pattern() + " matches the metadata item: " + metadataObjectName
                                        + " (" + mdItem.getLastModifiedByEmail() + "), item will be skipped.");
                                skipCount++;
                                itemSkipped = true;
                                break;
                            }
                        }
                    }
                    if (!itemSkipped && includeEmail_r.size() > 0) {
                        boolean matchesPattern = false;
                        for (Pattern p : includeEmail_r) {
                            final Matcher m = p.matcher(mdItem.getLastModifiedByEmail());
                            if (m.matches()) {
                                matchesPattern = true;
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "includeemail (no match): " + metadataObjectName + " (" + mdItem.getLastModifiedByEmail()
                                    + ") does not match any email include patterns, item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                        }
                    }

                    // check against dates now, if defined
                    Calendar itemLastModified = mdItem.getLastModifiedDate();
                    if (ignoreNullDate && (itemLastModified == null || itemLastModified.getTimeInMillis() == 0)) {
                        logger.log(Level.FINE, "Item lacks lastModifiedDate but was included in the includepattern. Overriding and adding. ");
                    } else {
                        if (!itemSkipped && fromDate != null) {

                            if (itemLastModified == null || fromDate.after(itemLastModified.getTime())) {
                                skipCount++;
                                itemSkipped = true;
                                logger.log(Level.FINE, "fromdate: " + metadataObjectName + " last modified (" + (itemLastModified == null || itemLastModified.getTimeInMillis() == 0 ? "null" : format1.format(itemLastModified.getTime())) + ") before provided FromDate ("
                                        + fromDateString + "), item will be skipped.");
                            }

                        }
                        if (!itemSkipped && toDate != null) {
                            if (itemLastModified == null || toDate.before(itemLastModified.getTime())) {
                                skipCount++;
                                itemSkipped = true;
                                logger.log(Level.FINE, "todate: " + metadataObjectName + " last modified (" + (itemLastModified == null || itemLastModified.getTimeInMillis() == 0 ? "null" : format1.format(itemLastModified.getTime())) + ") after provided ToDate ("
                                        + toDateString + "), item will be skipped.");
                            }

                        }
                    }

                    //Check against manageability
                    if (!itemSkipped && mdItem.getFileProperties() != null && skipManageableStateInstalled_r) {
                        if (mdItem.getFileProperties().getManageableState() == null || mdItem.getFileProperties().getManageableState().equals(ManageableState.installed)) {
                            itemSkipped = true;
                            logger.log(Level.FINE, "skipmanagedstateinstalled: Skip managed package file matches the metadata item: " + metadataObjectName + ", item will be skipped.");
                        }
                    }
                    if (!itemSkipped && (verifyMetadataRead_r || metadataSubTypeIncludePatterns_r.size() > 0)) { //TODO add flag for picklist
                        //Can't do the following check on some types:
                        String type = mdItem.getType();
                        if (!type.matches("(ApexClass|ApexTrigger|xxx)")) {
                            try {

                                Metadata[] mdt = srcMetadataConnection.readMetadata(mdItem.getType(), new String[]{mdItem.getFullName()}).getRecords();
                                if (mdt.length == 1 && mdt[0] != null) {

                                    if (mdItem.getType().equals("Flow")) {
                                        Flow flow = (Flow) mdt[0];
                                        mdItem.setStatus(flow.getStatus() == null ? "Managed" : flow.getStatus().toString());
                                        logger.log(Level.FINE, "Flow " + metadataObjectName + " status: " + mdItem.getStatus());
                                    } else if (mdItem.getType().equals("CustomField")) {
                                        CustomField cf = (CustomField) mdt[0];
                                        mdItem.setMetadataSubType(cf.getType() == null ? "Unknown" : cf.getType().toString());
                                        logger.log(Level.FINE, "CustomField " + metadataObjectName + " Sub-Type: " + mdItem.getMetadataSubType());
                                    }

                                } else {
                                    //Can't read metadata. Skip
                                    logger.log(Level.FINE, "verifymetadataread Metadata " + metadataObjectName + " skipped - can't read: ");
                                    itemSkipped = true;
                                }

                            } catch (final ConnectionException mdtLookup) {
                                logger.log(Level.FINEST, "\nException processing: " + mdItem.getType() + " msg: " + mdtLookup.getMessage());
                                logger.log(Level.FINE, "Metadata Lookup Error on file: " + metadataObjectName);
                            }
                        }
                    }

                    //Check for active (Flows) 
                    if (limitToActive_r && !mdItem.getStatus().equals("Active")) {
                        itemSkipped = true;
                        logger.log(Level.INFO, "Skip non-active or managed metadata: " + metadataObjectName + ", item will be skipped.");
                    }

                    //Check for Sub Type (CustoField Types)
                    if (!itemSkipped && metadataSubTypeIncludePatterns_r.size() > 0) {
                        boolean matchesPattern = false;
                        String lastModUsername = mdItem.getMetadataSubType() == null ? "null" : mdItem.getMetadataSubType();
                        for (Pattern p : metadataSubTypeIncludePatterns_r) {
                            final Matcher m = p.matcher(lastModUsername);
                            if (m.matches()) {
                                matchesPattern = true;
                                logger.log(Level.FINEST, "Metadata Sub Type Filter MATCH: " + metadataObjectName + " (" + mdItem.getMetadataSubType()
                                        + ") ");
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "Metadata Sub Type Filter (no match): " + metadataObjectName + " (" + mdItem.getMetadataSubType()
                                    + ") does not match any user name include patterns, item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                        }
                    }

                } //End forceInclude check
                if (itemSkipped) {
                    i.remove();
                } else {
                    unskippedItemCount++;
                }

            }
            logger.log(Level.INFO, "Summary (included/total) " + mdType + ": " + (unskippedItemCount - lastUnskippedItemCount) + "/" + ((skipCount - lastSkipCount) + (unskippedItemCount - lastUnskippedItemCount)) + "\n\n\n");
        }

        return skipCount;
    }

    private ArrayList<Pattern> initializePatternArray(String parameter) {
        ArrayList<Pattern> retVal = new ArrayList<>();
        if (parameter != null) {
            for (final String p : parameter.split(",")) {
                if (p != null && p.length() > 0) {
                    try {
                        retVal.add(Pattern.compile(p));
                    } catch (final PatternSyntaxException e) {
                        logger.log(Level.SEVERE, "Tried to compile pattern: " + p + " but got exception: ", e);
                    }
                }
            }
        }
        return retVal;
    }

    private ArrayList<String> initializeStringArray(String parameter) {
        ArrayList<String> retVal = new ArrayList<>();
        if (parameter != null) {
            for (final String p : parameter.split(",")) {
                if (p != null && p.length() > 0) {
                    retVal.add(p);
                }
            }
        }
        return retVal;
    }

    private boolean isParamTrue(final String paramName) {
        if (parameters.containsKey(paramName)) {
            String value = parameters.getProperty(paramName);

            return !"false".equals(value);
        }
        return false;
    }

    private void populateUserEmails(final HashMap<String, ArrayList<InventoryItem>> myFile) throws ConnectionException {

        final Set<String> userIDs = new HashSet<>();

        for (final String mdName : myFile.keySet()) {
            for (final InventoryItem i : myFile.get(mdName)) {
                userIDs.add(i.getLastModifiedById());
            }
        }

        // remove the null ID if it appears
        userIDs.remove(null);

        // now call salesforce to get the emails and usernames
        final HashMap<String, HashMap<String, String>> usersBySalesforceID = new HashMap<>();

        // build the query
        final String queryStart = "SELECT Id, Name, Username, Email FROM User WHERE ID IN(";
        final String queryEnd = ")";
        final String[] myIDs = userIDs.toArray(new String[userIDs.size()]);
        final String queryMid = "'" + String.join("','", myIDs) + "'";

        final String query = queryStart + queryMid + queryEnd;

        logger.log(Level.INFO, "Looking for emails for " + userIDs.size() + " users.");
        logger.log(Level.FINE, "Query: " + query);

        // run the query
        QueryResult qResult = this.srcPartnerConnection.query(query);

        boolean done = false;
        if (qResult.getSize() > 0) {
            logger.log(Level.FINE, "Logged-in user can see a total of " + qResult.getSize() + " contact records.");
            while (!done) {
                final SObject[] records = qResult.getRecords();
                for (final SObject o : records) {
                    final HashMap<String, String> userMap = new HashMap<>();
                    userMap.put("Name", (String) o.getField("Name"));
                    userMap.put("Email", (String) o.getField("Email"));
                    userMap.put("Username", (String) o.getField("Username"));
                    usersBySalesforceID.put((String) o.getField("Id"), userMap);
                }
                if (qResult.isDone()) {
                    done = true;
                } else {
                    qResult = this.srcPartnerConnection.queryMore(qResult.getQueryLocator());
                }
            }
        } else {
            logger.log(Level.INFO, "No records found.");
        }

        // now run through the InventoryItems again and update user data
        for (final String mdName : myFile.keySet()) {
            for (final InventoryItem i : myFile.get(mdName)) {
                final HashMap<String, String> userMap = usersBySalesforceID.get(i.getLastModifiedById());
                if (userMap != null) {
                    i.setLastModifiedByEmail(userMap.get("Email"));
                    i.setLastModifiedByUsername(userMap.get("Username"));
                } else {
                    i.setLastModifiedByEmail("null");
                    i.setLastModifiedByUsername("null");
                }

            }
        }

    }

    private long startTiming() {
        return System.currentTimeMillis();
    }

}
