/*
 * Copyright 2021 EricHaycraft.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mayaforce.packagebuilder;

/**
 *
 * @author EricHaycraft
 */
public class PbProperties {

    // command line static strings
    public static final String APIVERSION = "sf.apiversion";
    public static final String SKIPEMAIL = "skipemail";
    public static final String CONNECTPROPS = "connectprops";
    public static final String INCLUDEUSERNAME = "includeusername";
    public static final String SERVERURL = "sf.serverurl";
    public static final String URLBASE = "/services/Soap/u/";
    public static final String RESTURLBASE = "/services/data/u/";
    public static final String GITCOMMIT = "gitcommit";
    public static final String UNZIP = "unzip";
    public static final String RUNTYPE = "runtype";
    public static final String BASEDIRECTORY = "basedirectory";
    public static final String FROMDATE = "fromdate";
    public static final String INCLUDENAMESPACEDITEMS = "includenamespaceditems";
    public static final String INCLUDEEMAIL = "includeemail";
    public static final String RETAINTARGETDIR = "retaintargetdir";
    public static final String MAXITEMS = "maxitems";
    public static final String METADATAITEMS = "metadataitems";
    public static final String METADATAITEMSTOSKIP = "metadataitemstoskip";
    public static final String PASSWORD = "sf.password";
    public static final String LOGLEVEL = "loglevel";
    public static final String USERNAME = "sf.username";
    public static final String ACCESSTOKEN = "sf.accessToken";
    public static final String FORCEINCLUDEPATTERNS = "forceincludepatterns"; // When specified, will force the match to be included regardless of date, username, etc.
    public static final String SKIPPATTERNS = "skippatterns";
    public static final String DOWNLOAD = "download";
    public static final String TODATE = "todate";
    public static final String SKIPUSERNAME = "skipusername";
    public static final String INCLUDEPATTERNS = "includepatterns";
    public static final String METADATADIR = "metadatadir";
    public static final String TOKEN = "sf.token";
    public static final String BUILDPROPS = "buildprops";
    public static final String MANIFESTDIRECTORY = "destination";
    public static final String ZIPDIRECTORY = "zipdir";
    public static final String STRIPPROFILEUSERPERMISSIONS = "stripprofileuserpermissions";
    public static final String INCLUDECHANGEDATA = "includechangedata";
    public static final String LOCALONLY = "localonly";
    public static final String INCLUDENULLDATE = "includenulldate";
    public static final String SKIPMANAGEABLESTATEINSTALLED = "skipmanagedstateinstalled"; //Only used in MetadataType.skipmanagedstateinstalled setups.
    public static final String LIMITTOACTIVE = "limittoactive"; // Limits objects (mainly Flows) to only active flows. 
    public static final String VERIFYMETADATAREAD = "verifymetadataread"; // forces a read check on every item of metadata found. Not recommended for production use. 
    public static final String METADATASUBTYPEINCLUDEPATTERN = "metadatasubtypeincludepattern";
    public static final String FORCEINCLUDENAMES = "forceincludenames";
    public static final String INCLUDEALLVERSIONS = "includeallversions"; //For Flow backup
}
