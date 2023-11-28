/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Kim Galant  <kgalant@salesforce.com>
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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Command line and parameter frontend for package builder core application
 *
 */
public class PackageBuilderCommandLine {

    public static String VERSIONNUMBER = "";

    //Map<String, Map<String, String>> paramDefinitions = new HashMap<>();
    /**
     * @param args
     * @throws Exception
     * @throws RemoteException
     */
    public static void main(final String[] args) throws RemoteException, Exception {
        displayVersionNumber();
        System.out.println(LogManager.getLogManager().getLoggerNames().asIterator().next());
        final PackageBuilderCommandLine pbc = new PackageBuilderCommandLine();

        if (pbc.parseCommandLine(args)) {
            final PackageBuilder pb = new PackageBuilder(pbc.getParameters());
            pb.run();
            System.out.println("Done");
        }
        System.exit(0);
    }

    //private final Map<String, String> parameters = new HashMap<>();
    Properties buildProps = new Properties();

    private final Options options = new Options();

    public PackageBuilderCommandLine() {
        this.setupOptions();
    }

    public Properties getParameters() {
        return buildProps;
    }

    public boolean parseCommandLine(final String[] args) throws FileNotFoundException {

        boolean canProceed = false;

        // put in default parameters
        buildProps.put(PbProperties.APIVERSION, PbConstants.DEFAULT_API_VERSION);

        // now parse the command line
        final CommandLineParser parser = new DefaultParser();
        CommandLine line = null;

        try {
            // parse the command line arguments
            line = parser.parse(this.options, args);
            Iterator<Option> i = line.iterator();
            while (i.hasNext()) {
                Option opt = i.next();
                if (options.getOption(opt.getLongOpt()).hasArg()) {
                    buildProps.put(opt.getLongOpt(), opt.getValue());
                } else {
                    buildProps.put(opt.getLongOpt(), "true");
                }

            }

        } catch (final ParseException exp) {
            // oops, something went wrong
            System.err.println("Command line parsing failed.  Reason: " + exp.getMessage());
            System.exit(-1);
        }

        // first, add any parameters from any property files provided on command line
        try {
            InputStream fs;

            if (line.getOptionValue(PbProperties.BUILDPROPS) != null) {
                fs = new FileInputStream(line.getOptionValue(PbProperties.BUILDPROPS));
                buildProps.load(fs);
            }
            if (line.getOptionValue(PbProperties.CONNECTPROPS) != null) {
                fs = new FileInputStream(line.getOptionValue(PbProperties.CONNECTPROPS));
                buildProps.load(fs);
            }
        } catch (final FileNotFoundException ex) {
            Logger.getLogger(PackageBuilderCommandLine.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PackageBuilderCommandLine.class.getName()).log(Level.SEVERE, null, ex);
        }

        ////////////////////////////////////////////////////////////////////////
        //
        // from here on down, any special treatment for individual parameters
        //
        ////////////////////////////////////////////////////////////////////////
        //		skipping this, going to setting LOGLEVEL directly from command/property
        // 		if LOGLEVEL parameter is provided, set loglevel to LOGLEVEL, else it will default to normal
        //		if (isOptionSet(LOGLEVEL)) {
        //			this.parameters.put("loglevel", LOGLEVEL);
        //		}        
        // add default to current directory if no target directory given
        if (!buildProps.containsKey(PbProperties.DESTINATION)) {
            System.out.println("No target directory provided, will default to current directory.");
            buildProps.put(PbProperties.DESTINATION, ".");
        }

        // GIT needs download and changedata
        if (buildProps.containsKey(PbProperties.GITCOMMIT)) {
            buildProps.put(PbProperties.INCLUDECHANGEDATA, "true");
            buildProps.put(PbProperties.DOWNLOAD, "true");
            buildProps.put(PbProperties.UNZIP, "true");
        }

        // default download target to current directory if no explicit destination provided
        if ((buildProps.containsKey(PbProperties.GITCOMMIT) || buildProps.containsKey(PbProperties.DOWNLOAD)) && !buildProps.containsKey(PbProperties.METADATATARGETDIR)) {
            System.out.println("No directory provided as download destination, will default to current directory");
            buildProps.put(PbProperties.METADATATARGETDIR, ".");
        }

        // set maxitems to default value if nothing provided
        if (!buildProps.containsKey(PbProperties.MAXITEMS)) {
            //System.out.println("No maxitems parameter provided, will default to " + PackageBuilder.DEFAULT_MAXITEMSINPACKAGE + ".");
            buildProps.put(PbProperties.MAXITEMS, String.valueOf(PbConstants.DEFAULT_MAXITEMSINPACKAGE));
        }

        ////////////////////////////////////////////////////////////////////////
        //
        // now check that we have minimum parameters needed to run
        //
        ////////////////////////////////////////////////////////////////////////
        // check that we have the minimum parameters
        // either b(asedir) and d(estinationdir)
        // or s(f_url), p(assword), u(sername)
        if (buildProps.containsKey(PbProperties.BASEDIRECTORY) && buildProps.containsKey(PbProperties.DESTINATION)) {
            canProceed = true;
        }
        if (buildProps.containsKey(PbProperties.ACCESSTOKEN) && buildProps.containsKey(PbProperties.SERVERURL) || (buildProps.containsKey(PbProperties.USERNAME)
                && buildProps.containsKey(PbProperties.PASSWORD) && buildProps.containsKey(PbProperties.SERVERURL))) {
            canProceed = true;
        } else {
            System.out.println("Mandatory parameters not provided in files or commandline -"
                    + " either basedir and destination or serverurl, username and password required as minimum");
        }

        //Print the options & properties
        List<String> propNameList = new ArrayList<>(buildProps.stringPropertyNames());
        Collections.sort(propNameList);

        propNameList.forEach(key -> {
            if (key.equals(PbProperties.PASSWORD) || key.equals(PbProperties.TODATE)) {
                String val = (String) buildProps.get(key);
                System.out.println(key + ":" + val.replaceAll(".", "*"));
            } else {
                System.out.println(key + ":" + buildProps.get(key));
            }
        });

        if (!canProceed) {
            this.printHelp();
        }
        return canProceed;
    }

    private void printHelp() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);

        formatter.printHelp(
                "java -jar PackageBuilder.jar [-w basedirectory] [-b <parameter file1>] [-u <SF username>] [-p <SF password>]",
                this.options);
    }

    // add any new parameters here only
    private void setupOptions() {

        setupParameter("b", PbProperties.BUILDPROPS, "file containing org parameters (see below)", true);
        setupParameter("c", PbProperties.CONNECTPROPS, "file containing environment connection parameters. sf.username, sf.password, sf.apiversion, sf.serverurl(see below)", true);
        setupParameter("a", PbProperties.APIVERSION, "api version to use, will default to " + PbConstants.DEFAULT_API_VERSION + "\nsf.apiversion in property file", true);
        setupParameter("s", PbProperties.SERVERURL, "server URL for the org (https://login.salesforce.com) \nsf.serverurl in property file", true);
        setupParameter("a", PbProperties.ACCESSTOKEN, "SessionId or AccessToken from an already established session. sf org details for example", true);
        setupParameter("p", PbProperties.PASSWORD, "password for the org (t0pSecr3t) \nsf.password in property file", true);
        setupParameter("q", PbProperties.TOKEN, "token for the org (t0pSecr3t) \nsf.password in property file", true);
        setupParameter("u", PbProperties.USERNAME, "username for the org (someuser@someorg.com) \nsf.username in property file\"", true);
        setupParameter("t", PbProperties.DESTINATION, "directory where the generated package.xml will be written", true);
        setupParameter("m", PbProperties.METADATATARGETDIR, "Directory to download meta data source (different to where package.xml will go) to", true);
        setupParameter("o", PbProperties.LOGLEVEL, "output log level (INFO, FINE, FINER make sense) - defaults to INFO if not provided", true);
        setupParameter("d", PbProperties.DOWNLOAD, "directly download assets, removing the need for ANT or MDAPI call", false);
        setupParameter("g", PbProperties.GITCOMMIT, "commits the changes to git. Requires -d -c options", false);
        setupParameter("l", PbProperties.LOCALONLY, "Don't re-download package.zip files, but process existing ones", false);
        setupParameter("f", PbProperties.UNZIP, "unzip any retrieved package(s)", false);
        setupParameter("w", PbProperties.BASEDIRECTORY, "base directory from which to generate package.xml", true);
        setupParameter("k", PbProperties.RETAINTARGETDIR, "do not clear the metadatatargetdir provided when unzipping", false);
    }

    private void setupParameter(String shortParamName, String longParamName, String paramDescription, boolean hasArgs) {
        this.options.addOption(new Option(shortParamName, longParamName, hasArgs, paramDescription));
    }

    private static void displayVersionNumber() throws IOException, XmlPullParserException {
        Package ver = PackageBuilderCommandLine.class.getPackage();

        System.out.println("Title:   " + ver.getImplementationTitle() + "\nVersion: " + ver.getImplementationVersion());

    }

}
