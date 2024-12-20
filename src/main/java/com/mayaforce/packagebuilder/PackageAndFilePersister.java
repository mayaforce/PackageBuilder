/** ========================================================================= *
 * Copyright (C)  2017, 2019 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>
 *
 * @notessensei
 *
 * @version 1.0 *
 * ========================================================================== *
 * * Licensed under the Apache License, Version 2.0 (the "License"). You may *
 * not use this file except in compliance with the License. You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>. * *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the *
 * License for the specific language governing permissions and limitations *
 * under the License. * *
 * ========================================================================== *
 */
package com.mayaforce.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.mayaforce.packagebuilder.PersistResult.Status;
import com.mayaforce.packagebuilder.inventory.InventoryItem;
import com.mayaforce.migrationtoolutils.Utils;
import com.sforce.soap.metadata.MetadataConnection;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.util.Calendar;

/**
 * Download packageXML and eventually files in a background thread
 *
 * @author swissel
 *
 */
public class PackageAndFilePersister implements Callable<PersistResult> {

    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private final Map<String, ArrayList<InventoryItem>> theMap;
    private final String filename;
    private final boolean includeChangeData;
    private final boolean downloadData;
    private final boolean unzipDownload;
    private final double myApiVersion;
    private final String destManifestDir;
    private final String metaSourceDownloadDir;
    private final String zipFileDir;
    private final MetadataConnection metadataConnection;
    private final PersistResult result;

    private String zipFileName;
    private File zipResult;

    private OrgRetrieve myRetrieve = null;
    private boolean localOnly = false;

    public PackageAndFilePersister(final double myApiVersion,
            final String destManifestDir_p,
            final String metaSourceDownloadDir,
            final String zipFileDir_p,
            final Map<String, ArrayList<InventoryItem>> theMap,
            final String filename,
            final boolean includeChangeData, final boolean download,
            final boolean unzip,
            final MetadataConnection metadataConnection) {
        this.myApiVersion = myApiVersion;
        this.destManifestDir = destManifestDir_p;
        this.metaSourceDownloadDir = metaSourceDownloadDir;
        this.zipFileDir = zipFileDir_p;
        this.theMap = theMap;
        this.filename = filename;
        this.includeChangeData = includeChangeData;
        this.downloadData = download;
        this.unzipDownload = unzip;
        this.metadataConnection = metadataConnection;
        this.result = new PersistResult(filename);
    }

    /**
     * Switch the persister to local only operation mainly used when you have
     * both a local ZIP and XML
     */
    public void setLocalOnly() {
        this.localOnly = true;
    }

    /**
     * @see java.lang.Callable#call()
     */
    @Override
    public PersistResult call() throws Exception {
        boolean itworked = true;
        try {
            writePackageXML();
        } catch (Exception e) {
            this.result.setStatus(PersistResult.Status.FAILURE);
            itworked = false;
            logger.log(Level.SEVERE, "Unable to writePackageXML", e);
        }

        if (itworked && this.downloadData && !this.localOnly) {
            try {
                this.downloadPackage();
            } catch (Exception e) {
                this.result.setStatus(PersistResult.Status.FAILURE);
                itworked = false;
                logger.log(Level.SEVERE, "Unable to writePackageXML", e);
            }
        }

        if (itworked && (this.downloadData || this.localOnly) && this.unzipDownload) {
            try {
                this.unzipPackage();
            } catch (Exception e) {
                this.result.setStatus(PersistResult.Status.FAILURE);
                itworked = false;
                logger.log(Level.SEVERE, "Unable to writePackageXML", e);
            }
        }

        if (itworked) {
            this.result.setStatus(Status.SUCCESS);
        } else {
            this.logger.log(Level.INFO, "Cancel requested or download ZIP file doesn''t exist:{0}", zipFileName);
            this.result.setStatus(Status.FAILURE);
        }

        return this.result;
    }

    private void unzipPackage() throws Exception {
        final String zipFileNameWithPath = this.zipFileDir + File.separator + zipFileName;
        zipResult = new File(zipFileNameWithPath);

        if (zipResult.exists()) {
            Utils.unzip(zipFileNameWithPath, this.metaSourceDownloadDir);
        } else {
            throw new Exception("Asked to unzip " + zipFileNameWithPath + " but file not found, something is wrong.");
        }
    }

    private void downloadPackage() throws Exception {
        zipFileName = this.filename.replace("xml", "zip");

        this.logger.log(Level.INFO, "Asked to retrieve this package {0} from org - will do so now.", this.filename);
        myRetrieve = new OrgRetrieve(Level.FINE);
        myRetrieve.setMetadataConnection(this.metadataConnection);
        Utils.checkDir(this.zipFileDir);
        myRetrieve.setZipFile(this.zipFileDir + File.separator + zipFileName);
        myRetrieve.setManifestFile(this.destManifestDir + this.filename);
        myRetrieve.setApiVersion(this.myApiVersion);
        myRetrieve.retrieveZip();

    }

    private void writePackageXML() throws ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, IOException {
        final SimpleDateFormat format1 = new SimpleDateFormat(PbConstants.DEFAULT_DATE_FORMAT);

        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

        Document document = documentBuilder.newDocument();

        Element root = document.createElement("Package");
        root.setAttribute("xmlns", "http://soap.sforce.com/2006/04/metadata");
        document.appendChild(root);

        final ArrayList<String> mdTypes = new ArrayList<>(theMap.keySet());
        Collections.sort(mdTypes);

        // get list of types for comment line
        ArrayList<String> typesInPackage = new ArrayList<>();
        int count = 0;
        for (final String mdType : mdTypes) {
            if (theMap.get(mdType).isEmpty()) {
            } else {
                typesInPackage.add(mdType + "(" + theMap.get(mdType).size() + ")");
                count += theMap.get(mdType).size();
            }
        }

        String[] typesArray = new String[typesInPackage.size()];

        typesArray = typesInPackage.toArray(typesArray);
        Package ver = PackageBuilderCommandLine.class.getPackage();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        Comment blurb = document.createComment("Package.xml generated by " + ver.getImplementationTitle() + "\nVersion: " + ver.getImplementationVersion() + " on " + timeStamp + " - check out https://github.com/mayaforce/PackageBuilder ");
        root.appendChild(blurb);
        Comment comment = document.createComment("Types packaged: " + String.join(", ", typesArray));
        root.appendChild(comment);
        Comment comment2 = document.createComment("Items packaged total: " + count);
        root.appendChild(comment2);

        Element version = document.createElement("version");
        version.setTextContent(String.valueOf(this.myApiVersion));
        root.appendChild(version);

        for (final String mdType : mdTypes) {
            if (theMap.get(mdType).isEmpty()) {
                continue;
            }

            Element types = document.createElement("types");
            root.appendChild(types);
            Element name = document.createElement("name");
            name.setTextContent(mdType);
            types.appendChild(name);

            for (final InventoryItem item : theMap.get(mdType)) {

                Element member = document.createElement("members");
                member.setTextContent(item.getItemName());
                String createdDate = item.getCreatedDate() == null || item.getCreatedDate().getTimeInMillis() == 0 ? String.format("%-16s", "") : format1.format(item.getCreatedDate().getTime());
                String modifiedDate = item.getLastModifiedDate() == null || item.getLastModifiedDate().getTimeInMillis() == 0 ? String.format("%-16s", "") : format1.format(item.getLastModifiedDate().getTime());

                if (this.includeChangeData) {
                    member.setAttribute("cd", createdDate);
                    member.setAttribute("cb", String.format("%-20s", item.getCreatedByName(20)));
                    member.setAttribute("mb", String.format("%-20s", item.getLastModifiedByName(20)));
                    member.setAttribute("md", modifiedDate);
                    member.setAttribute("mdt", item.getType());
                }
                types.appendChild(member);
            }
        }

        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.METHOD, "xml");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        Writer out = new StringWriter();
        tf.transform(new DOMSource(document), new StreamResult(out));

        Utils.writeFile(this.destManifestDir + filename, out.toString());
        this.logger.log(Level.INFO, "Writing {0}", new File(this.destManifestDir + filename).getCanonicalPath());
    }

}
