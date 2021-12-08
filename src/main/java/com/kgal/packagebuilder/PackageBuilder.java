package com.kgal.packagebuilder;

import com.kgal.migrationtoolutils.Utils;
import com.kgal.packagebuilder.inventory.InventoryItem;
import com.kgal.packagebuilder.output.GitOutputManager;
import com.kgal.packagebuilder.output.LogFormatter;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.ManageableState;
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

    // Static values that don't change
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String URLBASE = "/services/Soap/u/";
    public static final int DEFAULT_MAXITEMSINPACKAGE = 30000;
    public static final double DEFAULT_API_VERSION = 53.0;
    public static final boolean DEFAULT_INCLUDECHANGEDATA = false;
    public static final int CONCURRENT_THREADS = 8;

    // Logging
    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private Level thisLogLevel;

    private static final String[] STANDARDVALUETYPESARRAY = new String[]{"AccountContactMultiRoles",
        "AccountContactRole", "AccountOwnership", "AccountRating", "AccountType", "AddressCountryCode",
        "AddressStateCode",
        "AssetStatus", "CampaignMemberStatus", "CampaignStatus", "CampaignType", "CaseContactRole", "CaseOrigin",
        "CasePriority", "CaseReason",
        "CaseStatus", "CaseType", "ContactRole", "ContractContactRole", "ContractStatus", "EntitlementType",
        "EventSubject", "EventType",
        "FiscalYearPeriodName", "FiscalYearPeriodPrefix", "FiscalYearQuarterName", "FiscalYearQuarterPrefix",
        "IdeaCategory1",
        "IdeaMultiCategory", "IdeaStatus", "IdeaThemeStatus", "Industry", "InvoiceStatus", "LeadSource",
        "LeadStatus", "OpportunityCompetitor",
        "OpportunityStage", "OpportunityType", "OrderStatus1", "OrderType", "PartnerRole", "Product2Family",
        "QuestionOrigin1", "QuickTextCategory",
        "QuickTextChannel", "QuoteStatus", "SalesTeamRole", "Salutation", "ServiceContractApprovalStatus",
        "SocialPostClassification",
        "SocialPostEngagementLevel", "SocialPostReviewedStatus", "SolutionStatus", "TaskPriority", "TaskStatus",
        "TaskSubject", "TaskType",
        "WorkOrderLineItemStatus", "WorkOrderPriority", "WorkOrderStatus"};

    private static final String[] ADDITIONALTYPESTOADD = new String[]{"CustomLabel", "AssignmentRule",
        "BusinessProcess", "CompactLayout", "CustomField", "FieldSet", "Index", "ListView", "NamedFilter", "RecordType", "SharingReason", "ValidationRule", "WebLink", // CustomObject components
        "WorkflowActionReference", "WorkflowAlert", "WorkflowEmailRecipient", "WorkflowFieldUpdate", "WorkflowFlowAction", "WorkflowFlowActionParameter", // Workflow components
        "WorkflowKnowledgePublish", "WorkflowOutboundMessage", "WorkflowRule", "WorkflowTask", "WorkflowTimeTrigger" // Workflow components
};

    private static final String[] ITEMSTOINCLUDEWITHPROFILESPERMSETS = new String[]{"ApexClass", "CustomApplication", "CustomField",
        "CustomObject", "CustomTab", "ExternalDataSource", "RecordType", "ApexPage"};

    private static final String[] SPECIALTREATMENTPERMISSIONTYPES = new String[]{"Profile", "PermissionSet"};

    // Collections
    private final HashSet<String> existingTypes = new HashSet<>();
    private final Properties parameters = new Properties();

    // Variables changing per parameter or properties
    private double myApiVersion;
    private String skipItems;
    private HashMap<String, DescribeMetadataObject> describeMetadataObjectsMap;
    String authEndPoint = "";
    private MetadataConnection srcMetadataConnection;
    private String srcUrl;
    private String srcUser;
    private String srcPwd;
    private String metaSourceDownloadDir = "src";
    private final long totalTimeStart = System.currentTimeMillis();
    //	private String            dbFilename;
    private String targetDir = "";
    private OperationMode mode;
    private PartnerConnection srcPartnerConnection;

    private boolean includeChangeData = false;
    private boolean includeNamespacedItems = false;
    private boolean includeManagedPackagesOnly = false;
    private boolean downloadData = false;
    private boolean gitCommit = false;
    private boolean simulateDataDownload = false;
    private int maxItemsInRegularPackage = PackageBuilder.DEFAULT_MAXITEMSINPACKAGE;
    private boolean unzipDownload = false;
    private int itemCount;

    // Constructor that gets all settings as map
    public PackageBuilder(final Properties parameters) {
        this.parameters.putAll(parameters);

    }

    public void run() throws RemoteException, Exception {

        // set loglevel based on parameters
        String paramLogLevel = parameters.getProperty("loglevel");
        thisLogLevel = Level.INFO;
        if ("FINE".equals(paramLogLevel)) {
            thisLogLevel = Level.FINE;
        } else if ("FINER".equals(paramLogLevel)) {
            thisLogLevel = Level.FINER;
        } else if ("FINEST".equals(paramLogLevel)) {
            thisLogLevel = Level.FINEST;
        }

        logger.setLevel(thisLogLevel);
        logger.setUseParentHandlers(false);
        final LogFormatter formatter = new LogFormatter();
        final ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        handler.setLevel(thisLogLevel);
        logger.addHandler(handler);

        // Check what to do based on parameters
        this.includeChangeData = this.isParamTrue(PackageBuilderCommandLine.INCLUDECHANGEDATA_LONGNAME);
        this.downloadData = this.isParamTrue(PackageBuilderCommandLine.DOWNLOAD_LONGNAME);
        this.gitCommit = this.isParamTrue(PackageBuilderCommandLine.GITCOMMIT_LONGNAME);
        this.simulateDataDownload = this.isParamTrue(PackageBuilderCommandLine.LOCALONLY_LONGNAME);
        this.unzipDownload = this.isParamTrue(PackageBuilderCommandLine.UNZIP_LONGNAME);
        this.maxItemsInRegularPackage = Integer.valueOf(parameters.getProperty(PackageBuilderCommandLine.MAXITEMS_LONGNAME));
        this.includeNamespacedItems = this.isParamTrue(PackageBuilderCommandLine.INCLUDENAMESPACEDITEMS_LONGNAME);
        this.includeManagedPackagesOnly = this.isParamTrue(PackageBuilderCommandLine.INCLUDEMANAGEDPACKAGES_LONGNAME);

        // initialize inventory - it will be used in both types of operations
        // (connect to org or run local)
        final HashMap<String, ArrayList<InventoryItem>> inventory = new HashMap<>();

        this.myApiVersion = Double.parseDouble(parameters.getProperty(PackageBuilderCommandLine.APIVERSION_LONGNAME));
        this.targetDir = Utils.checkPathSlash(Utils.checkPathSlash(parameters.getProperty(PackageBuilderCommandLine.DESTINATION_LONGNAME)));
        this.metaSourceDownloadDir = Utils.checkPathSlash(
                Utils.checkPathSlash(parameters.getProperty(PackageBuilderCommandLine.METADATATARGETDIR_LONGNAME)));
        // handling for building a package from a directory
        // if we have a base directory set, ignore everything else and generate
        // from the directory

        if (parameters.getProperty(PackageBuilderCommandLine.BASEDIRECTORY_LONGNAME) != null) {
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
            for (String mdType : SPECIALTREATMENTPERMISSIONTYPES) {
                if (myCompleteInventory.containsKey(mdType)) {
                    exportingPermissions = true;
                    break;
                }
            }

            // if we are exporting permission, check if we're bringing anything that needs to go into the permissions file along
            boolean exportingPermissionsDependentItems = false;
            for (String mdType : ITEMSTOINCLUDEWITHPROFILESPERMSETS) {
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
                final HashMap<String, ArrayList<InventoryItem>> mySpecialTreatmentInventory = new HashMap<String, ArrayList<InventoryItem>>();

                for (final String mdType : ArrayUtils.addAll(SPECIALTREATMENTPERMISSIONTYPES, ITEMSTOINCLUDEWITHPROFILESPERMSETS)) {
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
        if (files.size() == 0) {
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
                ArrayList<InventoryItem> mdTypeListPartial = new ArrayList<InventoryItem>(mdTypeList.subList(0, maxItemsInRegularPackage - fileCount));
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

                ArrayList<InventoryItem> mdTypeListPartial = new ArrayList<InventoryItem>(mdTypeList.subList(0, maxItemsInRegularPackage));
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
            continuingPreviousFile = false;
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
        int itemCount = 0;
        try {
            ArrayList<ListMetadataQuery> queries = new ArrayList<ListMetadataQuery>();

            // check if what we have here is in folders
            final DescribeMetadataObject obj = this.describeMetadataObjectsMap.get(metadataType);
            if ((obj != null) && (obj.getInFolder() == true)) {
                ArrayList<FileProperties> foldersToProcess = new ArrayList<>();
                foldersToProcess = generateFolderListToProcess(packageInventoryList, metadataType);
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
                final ListMetadataQuery query = new ListMetadataQuery();

                query.setType(metadataType);

                ListMetadataQuery[] queryArray = new ListMetadataQuery[3];
                int queriesInArray = 0;
                while (queryIterator.hasNext() && queriesInArray < 3) {
                    queryArray[queriesInArray++] = queryIterator.next();
                }

                // generate metadata inventory
                final FileProperties[] srcMd = this.srcMetadataConnection.listMetadata(queryArray, this.myApiVersion);
                itemCount += srcMd.length;

                if (itemCount > 0) {
                    this.existingTypes.add(metadataType);
                }

                if (((srcMd != null) && (srcMd.length > 0)) || metadataType.equals("StandardValueSet")) {
                    // hack alert - currently (API 45) listMetadata returns nothing for StandardValueSet
                    if (!metadataType.equals("StandardValueSet")) {
                        for (final FileProperties n : srcMd) {
                            if ((includeNamespacedItems || n.getNamespacePrefix() == null) || n.getNamespacePrefix().equals("") || (metadataType.equals("InstalledPackage") && includeManagedPackagesOnly)) {
                                // packageMap.add(n.getFullName());
                                InventoryItem i = new InventoryItem(n.getFullName(), n, this.describeMetadataObjectsMap.get(metadataType));
                                packageInventoryList.put(n.getFullName(), i);
                                logger.log(Level.FINE, "Adding item " + i.getExtendedName() + " to inventory.");
                            } else {
                                logger.log(Level.FINE, "skipping item " + n.getNamespacePrefix() + " from " + n.getFullName() + " inventory.");
                            }
                            //
                        }
                    } else {
                        for (final String s : PackageBuilder.STANDARDVALUETYPESARRAY) {
                            InventoryItem i = new InventoryItem(s, "standardValueSets");
                            packageInventoryList.put(s, i);
                            logger.log(Level.FINER, "Adding item " + i.getExtendedName() + " to inventory.");
                        }
                    }

                } else {
                    logger.log(Level.FINER, "No items of this type, skipping...");
                }

            } while (queryIterator.hasNext());

        } catch (final Exception e) {
            // ce.printStackTrace();
            logger.log(Level.INFO, "\nException processing: " + metadataType);
            logger.log(Level.INFO, "Error: " + e.getMessage());
        }
        this.endTiming(startTime, "");

        return packageInventoryList;
    }

    // inventory is a list of lists
    // keys are the metadata types
    // e.g. flow, customobject, etc.
    private ArrayList<FileProperties> generateFolderListToProcess(HashMap<String, InventoryItem> packageInventoryList, String metadataType)
            throws ConnectionException {
        logger.log(Level.FINE, metadataType + " is stored in folders. Getting folder list.");
        final ArrayList<FileProperties> foldersToProcess = new ArrayList<>();
        final ListMetadataQuery query = new ListMetadataQuery();
        // stupid hack for emailtemplate folder name
        String type;
        if (metadataType.toLowerCase().equals("emailtemplate")) {
            type = "EmailFolder";
        } else {
            type = metadataType + "Folder";
        }

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
                    foldersToProcess.add(n);
                    packageInventoryList.put(n.getFullName(), new InventoryItem(n.getFullName(), n, true, this.describeMetadataObjectsMap.get(metadataType)));
                    logger.log(Level.FINER, "Adding folder " + n.getFullName() + " to inventory.");
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
        final String basedir = parameters.getProperty(PackageBuilderCommandLine.BASEDIRECTORY_LONGNAME);

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
                    System.out.println("Created inventory record for type: " + mdType);
                }

                // check if there is a folder in the filename and it's aura -
                // then we need to leave the folder, skip the item
                if (filename.contains("/") && mdType.equals("AuraDefinitionBundle")) {
                    final String subFoldername = filename.substring(0, filename.indexOf("/"));
                    typeInventory.add(new InventoryItem(subFoldername, null, null));
                    logger.log(Level.FINE, "Added: " + mdType + " : " + subFoldername + ", to inventory, original path: " + s);
                    continue;
                }

                // check if there is a folder in the filename - then we need to
                // add the folder as well
                if (filename.contains("/")) {
                    final String subFoldername = filename.substring(0, filename.indexOf("/"));
                    typeInventory.add(new InventoryItem(subFoldername, null, null));
                }

                typeInventory.add(new InventoryItem(filename, null, null));
                logger.log(Level.FINE, "Added: " + mdType + " : " + filename + ", to inventory, original path: " + s);

                // convert myinventory to the right return type
            } catch (final Exception e) {
                // Something bad happened
                System.out.println("Something bad happened on file: " + s + ", skipping...");
            }

        }
        for (final String myMdType : myInventory.keySet()) {
            final ArrayList<InventoryItem> invType = new ArrayList<>();
            invType.addAll(myInventory.get(myMdType));
            inventory.put(myMdType, invType);
        }

        //
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
        this.srcUrl = parameters.getProperty(PackageBuilderCommandLine.SERVERURL_LONGNAME) + PackageBuilder.URLBASE + this.myApiVersion;
        this.srcUser = parameters.getProperty(PackageBuilderCommandLine.USERNAME_LONGNAME);
        this.srcPwd = parameters.getProperty(PackageBuilderCommandLine.PASSWORD_LONGNAME);
        this.skipItems = parameters.getProperty(PackageBuilderCommandLine.SKIPPATTERNS_LONGNAME);
        // Make a login call to source
        this.srcMetadataConnection = LoginUtil.mdLogin(this.srcUrl, this.srcUser, this.srcPwd, logger);

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

        int itemCount = 0;
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
                itemCount++;
            }
        }

        // if we're writing change telemetry into the package.xml, or skipping/including by email, need to get
        // user emails now
        if (includeChangeData
                || this.parameters.containsKey(PackageBuilderCommandLine.SKIPEMAIL_LONGNAME)
                || this.parameters.containsKey(PackageBuilderCommandLine.INCLUDEEMAIL_LONGNAME)) {
            this.populateUserEmails(myFile);
        }

        // now check if anything we have needs to be skipped
        skipCount = this.handleSkippingItems(myFile);

        // now break it up into files if needed
        final HashMap<String, HashMap<String, ArrayList<InventoryItem>>> files = this.createPackageFiles(myFile);

        writeAndDownloadPackages(files, myFile);

        if (downloadData) {

            if (this.parameters.containsKey(PackageBuilderCommandLine.STRIPUSERPERMISSIONS_LONGNAME)
                    && myFile.containsKey("Profile")) {
                logger.log(Level.INFO, "Asked to strip user permissions from Profiles - will do so now.");
                ProfileCompare pc = new ProfileCompare(thisLogLevel);
                pc.stripUserPermissionsFromProfiles(parameters.getProperty(PackageBuilderCommandLine.METADATATARGETDIR_LONGNAME));

            }
        }

        final ArrayList<String> typesFound = new ArrayList<>(this.existingTypes);
        Collections.sort(typesFound);

        logger.log(Level.INFO, "Types found in org: " + typesFound.toString());

        logger.log(Level.INFO, "Total items in package.xml: " + (itemCount - skipCount));
        logger.log(Level.FINE, "Total items overall: " + itemCount + ", items skipped: " + skipCount
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

        for (String metadataType : inventory.keySet()) {
            for (InventoryItem item : inventory.get(metadataType)) {
                String key = item.getFileName();

                // strip suffix from file name
                int idx = key.lastIndexOf(".");

                if (idx > 0) {
                    key = key.substring(0, idx);
                }

                totalInventory.put(key, item);
                logger.log(Level.FINE, "Added: " + key + " to git inventory");
            }
        }

        return totalInventory;
    }

    private void writeAndDownloadPackages(HashMap<String, HashMap<String, ArrayList<InventoryItem>>> files,
            HashMap<String, ArrayList<InventoryItem>> completeInventory) throws Exception {
        // USE THREADS TO speed things up
        // final int totalFiles = files.size();
        final ExecutorService WORKER_THREAD_POOL = Executors.newFixedThreadPool(PackageBuilder.CONCURRENT_THREADS);

        final Collection<PackageAndFilePersister> allPersisters = new ArrayList<>();

        // Add all XML Files to the download queue
        files.forEach((curFileName, members) -> {
            final PackageAndFilePersister pfp = new PackageAndFilePersister(this.myApiVersion,
                    this.targetDir,
                    this.metaSourceDownloadDir,
                    parameters.getProperty(PackageBuilderCommandLine.DESTINATION_LONGNAME),
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
            File targetDirectory = new File(parameters.getProperty(PackageBuilderCommandLine.DESTINATION_LONGNAME) + File.separator + this.metaSourceDownloadDir);
            if (!isParamTrue(PackageBuilderCommandLine.RETAINTARGETDIR_LONGNAME)) {
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
        final String mdTypesToExamine = parameters.getProperty(PackageBuilderCommandLine.METADATAITEMS_LONGNAME);

        // get a describe
        final DescribeMetadataResult dmr = this.srcMetadataConnection.describeMetadata(this.myApiVersion);
        this.describeMetadataObjectsMap = new HashMap<>();

        for (final DescribeMetadataObject obj : dmr.getMetadataObjects()) {
            this.describeMetadataObjectsMap.put(obj.getXmlName(), obj);
        }

        // if a metadataitems parameter was provided, we use that
        if (mdTypesToExamine != null) {
            for (final String s : mdTypesToExamine.split(",")) {
                typesToFetch.add(s.trim());
            }
        } else {
            // no directions on what to fetch - go get everything
            logger.log(Level.INFO, "No metadataitems (-mi) parameter found, will inventory the whole org");

            for (final String obj : this.describeMetadataObjectsMap.keySet()) {
                typesToFetch.add(obj.trim());
            }

            // now add the list of types to be added manually
            for (String manualType : ADDITIONALTYPESTOADD) {
                typesToFetch.add(manualType.trim());
            }

            // check API version - in 45+, remove FlowDefinition
            if (Double.valueOf(parameters.getProperty(PackageBuilderCommandLine.APIVERSION_LONGNAME)) >= 45) {
                typesToFetch.remove("FlowDefinition");
            }

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
        int lastItemCount = 0;
        int itemCount = 0;

        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");

        //Patterns used while matching (defined for each metadata type)
        ArrayList<Pattern> skipPatterns_r;
        ArrayList<Pattern> skipEmail_r;
        ArrayList<Pattern> includeEmail_r;
        ArrayList<Pattern> skipUsername_r;
        ArrayList<Pattern> includeUsername_r;
        ArrayList<Pattern> includePatterns_r;
        ArrayList<Pattern> forceIncludePatterns_r;

//Default Patterns
        ArrayList<Pattern> skipPatterns_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.SKIPPATTERNS_LONGNAME));
        ArrayList<Pattern> skipEmail_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.SKIPEMAIL_LONGNAME));
        ArrayList<Pattern> includePatterns_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.INCLUDEPATTERNS_LONGNAME));
        ArrayList<Pattern> includeEmail_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.INCLUDEEMAIL_LONGNAME));
        ArrayList<Pattern> skipUsername_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.SKIPUSERNAME_LONGNAME));
        ArrayList<Pattern> includeUsername_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.INCLUDEUSERNAME_LONGNAME));
        ArrayList<Pattern> forceIncludePatterns_d = initializePatternArray(parameters.getProperty(PackageBuilderCommandLine.FORCEINCLUDEUSERNAME_LONGNAME));

        // initialize date ranges, if any
        String fromDateString = parameters.getProperty(PackageBuilderCommandLine.FROMDATE_LONGNAME);
        String toDateString = parameters.getProperty(PackageBuilderCommandLine.TODATE_LONGNAME);
        Date fromDate = null;
        Date toDate = null;
        boolean skipManageableStateInstalled = false;

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
            lastItemCount = itemCount;
            logger.log(Level.INFO, "Skip pattern check for " + mdType);

            skipManageableStateInstalled = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.SKIPMANAGEABLESTATEINSTALLED) ? true : false;

            //Setup patterns for metadata types:
            skipPatterns_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.SKIPPATTERNS_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.SKIPPATTERNS_LONGNAME)) : skipPatterns_d;
            skipEmail_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.SKIPEMAIL_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.SKIPEMAIL_LONGNAME)) : skipEmail_d;
            includePatterns_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.INCLUDEPATTERNS_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.INCLUDEPATTERNS_LONGNAME)) : includePatterns_d;
            includeEmail_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.INCLUDEEMAIL_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.INCLUDEEMAIL_LONGNAME)) : includeEmail_d;
            skipUsername_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.SKIPUSERNAME_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.SKIPUSERNAME_LONGNAME)) : skipUsername_d;
            includeUsername_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.INCLUDEUSERNAME_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.INCLUDEUSERNAME_LONGNAME)) : includeUsername_d;
            forceIncludePatterns_r = parameters.containsKey(mdType + "." + PackageBuilderCommandLine.FORCEINCLUDEUSERNAME_LONGNAME) ? initializePatternArray(parameters.getProperty(mdType + "." + PackageBuilderCommandLine.FORCEINCLUDEUSERNAME_LONGNAME)) : includeUsername_d;

            boolean ignoreNullDate = false;
            final ArrayList<InventoryItem> items = myFile.get(mdType);
            for (Iterator<InventoryItem> i = items.iterator(); i.hasNext();) {
                //Setups for Metadata Object:
                final InventoryItem mdItem = i.next();
                String metadataObjectName = mdItem.getFullName() == null ? mdItem.getExtendedName() : mdItem.getFullName();

                ignoreNullDate = false;
                boolean itemSkipped = false;
                boolean forceInclude = false;
                logger.log(Level.FINEST, "Skip pattern check on: " + mdItem.getFullName() + " || " + mdItem.getExtendedName());

                for (Pattern p : forceIncludePatterns_r) {
                    final Matcher m = p.matcher(metadataObjectName);
                    if (m.matches()) {
                        logger.log(Level.INFO, "Force Include Pattern: " + p.pattern() + " matches the metadata item: " + metadataObjectName + ", item will be included.");
                        forceInclude = true;
                        break;
                    }
                }
                if (!forceInclude) {

                    for (Pattern p : skipPatterns_r) {
                        final Matcher m = p.matcher(metadataObjectName);
                        if (m.matches()) {
                            logger.log(Level.FINE, "Skip pattern: " + p.pattern() + " matches the metadata item: " + metadataObjectName + ", item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                            break;
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
                            logger.log(Level.FINE, "Metadata item: " + metadataObjectName + " does not match any item name include patterns, item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                        }
                    }
                    if (!itemSkipped) {
                        for (Pattern p : skipUsername_r) {
                            final Matcher m = p.matcher(mdItem.getLastModifiedByName());
                            if (m.matches()) {
                                logger.log(Level.FINE, "Skip pattern: " + p.pattern() + " matches the metadata item: " + mdItem.getExtendedName()
                                        + " (" + mdItem.getLastModifiedByName() + "), item will be skipped.");
                                skipCount++;
                                itemSkipped = true;
                                break;
                            }
                        }
                    }
                    if (!itemSkipped && includeUsername_r.size() > 0) {
                        boolean matchesPattern = false;
                        for (Pattern p : includeUsername_r) {
                            final Matcher m = p.matcher(mdItem.getLastModifiedByName());
                            if (m.matches()) {
                                matchesPattern = true;
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "Metadata item: " + mdItem.getFullName() + " (" + mdItem.getLastModifiedByName()
                                    + ") does not match any user name include patterns, item will be skipped.");
                            skipCount++;
                            itemSkipped = true;
                        }
                    }
                    if (!itemSkipped) {
                        for (Pattern p : skipEmail_r) {
                            final Matcher m = p.matcher(mdItem.lastModifiedByEmail);
                            if (m.matches()) {
                                logger.log(Level.FINE, "Skip pattern: " + p.pattern() + " matches the metadata item: " + mdItem.getExtendedName()
                                        + " (" + mdItem.lastModifiedByEmail + "), item will be skipped.");
                                skipCount++;
                                itemSkipped = true;
                                break;
                            }
                        }
                    }
                    if (!itemSkipped && includeEmail_r.size() > 0) {
                        boolean matchesPattern = false;
                        for (Pattern p : includeEmail_r) {
                            final Matcher m = p.matcher(mdItem.lastModifiedByEmail);
                            if (m.matches()) {
                                matchesPattern = true;
                            }
                        }
                        if (!matchesPattern) {
                            logger.log(Level.FINE, "Metadata item: " + mdItem.getExtendedName() + " (" + mdItem.lastModifiedByEmail
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
                        if (!itemSkipped) {
                            if (fromDate != null) {
                                if (itemLastModified == null || fromDate.after(itemLastModified.getTime())) {
                                    skipCount++;
                                    itemSkipped = true;
                                    logger.log(Level.FINE, "Item: " + mdItem.getExtendedName() + " last modified (" + (itemLastModified == null || itemLastModified.getTimeInMillis() == 0 ? "null" : format1.format(itemLastModified.getTime())) + ") before provided FromDate ("
                                            + fromDateString + "), item will be skipped.");
                                }
                            }
                        }
                        if (!itemSkipped) {
                            if (toDate != null) {
                                if (itemLastModified == null || toDate.before(itemLastModified.getTime())) {
                                    skipCount++;
                                    itemSkipped = true;
                                    logger.log(Level.FINE, "Item: " + mdItem.getExtendedName() + " last modified (" + (itemLastModified == null || itemLastModified.getTimeInMillis() == 0 ? "null" : format1.format(itemLastModified.getTime())) + ") after provided ToDate ("
                                            + toDateString + "), item will be skipped.");
                                }
                            }
                        }
                    }

                    //Check against manageability
                    if (mdItem.getFileProperties() != null && skipManageableStateInstalled) {
                        if (mdItem.getFileProperties().getManageableState() == null || mdItem.getFileProperties().getManageableState().equals(ManageableState.installed)) {
                            itemSkipped = true;
                            logger.log(Level.INFO, "Skip managed package file matches the metadata item: " + metadataObjectName + ", item will be skipped.");
                        }
                    }

                } //End forceInclude check
                if (itemSkipped) {
                    i.remove();
                } else {
                    itemCount++;
                }

            }
            logger.log(Level.INFO, "Summary (included/total) " + mdType + ": " + (itemCount - lastItemCount) + "/" + ((skipCount - lastSkipCount) + (itemCount - lastItemCount)));
        }

        return skipCount;
    }

    private ArrayList<Pattern> initializePatternArray(String parameter) {
        ArrayList<Pattern> retVal = new ArrayList<Pattern>();
        if (parameter != null) {
            for (final String p : parameter.split(",")) {
                try {
                    retVal.add(Pattern.compile(p));
                } catch (final PatternSyntaxException e) {
                    System.out.println("Tried to compile pattern: " + p + " but got exception: ");
                    e.printStackTrace();
                }
            }
        }
        return retVal;
    }

    private boolean isParamTrue(final String paramName) {
        return "true".equals(parameters.getProperty(paramName));
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

        // login
        this.srcPartnerConnection = LoginUtil.soapLogin(this.srcUrl, this.srcUser, this.srcPwd, logger);

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
            System.out.println("No records found.");
        }

        // now run through the InventoryItems again and update user data
        for (final String mdName : myFile.keySet()) {
            for (final InventoryItem i : myFile.get(mdName)) {
                final HashMap<String, String> userMap = usersBySalesforceID.get(i.getLastModifiedById());
                if (userMap != null) {
                    i.lastModifiedByEmail = userMap.get("Email");
                    i.lastModifiedByUsername = userMap.get("Username");
                } else {
                    i.lastModifiedByEmail = "null";
                    i.lastModifiedByUsername = "null";
                }

            }
        }

    }

    private long startTiming() {
        return System.currentTimeMillis();
    }

}
