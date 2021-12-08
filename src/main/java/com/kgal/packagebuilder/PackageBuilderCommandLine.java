/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Kim Galant  <kgalant@salesforce.com>                          *
 * @version     1.0                                                           *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== *
 */
package com.kgal.packagebuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;  

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.kgal.migrationtoolutils.Utils;
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

	// command line static strings


	public static final String APIVERSION_LONGNAME = "sf.apiversion";
	public static final String METADATAITEMS_LONGNAME = "metadataitems";
	public static final String CONNPROPS_LONGNAME = "connectprops";
	public static final String BUILDPROPS_LONGNAME = "buildprops";
	public static final String USERNAME_LONGNAME = "sf.username";
	public static final String PASSWORD_LONGNAME = "sf.password";
	public static final String SERVERURL_LONGNAME = "sf.serverurl";
	public static final String SKIPPATTERNS_LONGNAME = "skippatterns";
	public static final String INCLUDEPATTERNS = "i";
	public static final String INCLUDEPATTERNS_LONGNAME = "includepatterns";
	public static final String SKIPEMAIL_LONGNAME = "skipemail";
	public static final String INCLUDEEMAIL_LONGNAME = "includeemail";
	public static final String SKIPUSERNAME_LONGNAME = "skipusername";
	public static final String INCLUDEUSERNAME_LONGNAME = "includeusername";
	public static final String INCLUDENAMESPACEDITEMS_LONGNAME = "includenamespaceditems";
	public static final String INCLUDEMANAGEDPACKAGES_LONGNAME = "includemanagedpackages";
	public static final String DESTINATION_LONGNAME = "destination";
	public static final String BASEDIRECTORY_LONGNAME = "basedirectory";
	public static final String METADATATARGETDIR_LONGNAME = "metadatatargetdir";
	public static final String LOGLEVEL_LONGNAME = "loglevel";
	public static final String INCLUDECHANGEDATA_LONGNAME = "includechangedata";
	public static final String DOWNLOAD_LONGNAME = "download";
	public static final String GITCOMMIT_LONGNAME = "gitcommit";
	public static final String MAXITEMS_LONGNAME = "maxitems";
	public static final String FROMDATE_LONGNAME = "fromdate";
	public static final String TODATE_LONGNAME = "todate";
	public static final String STRIPUSERPERMISSIONS_LONGNAME = "stripprofileuserpermissions";
	public static final String LOCALONLY_LONGNAME = "localonly";
	public static final String UNZIP_LONGNAME = "unzip";
	public static final String RETAINTARGETDIR_LONGNAME = "retaintargetdir";
        public static final String SKIPMANAGEABLESTATEINSTALLED="skipmanagedstateinstalled"; //Only used in MetadataType.skipmanagedstateinstalled setups. 
        public static final String FORCEINCLUDEUSERNAME_LONGNAME="forceincludepatterns"; // When specified, will force the match to be included regardless of date, username, etc. 
	
	public static  String     VERSIONNUMBER       = "";

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
        
        public Properties getParameters(){
            return buildProps;
        }



	public boolean parseCommandLine(final String[] args) throws FileNotFoundException {

		boolean canProceed = false;

		// put in default parameters
		buildProps.put(APIVERSION_LONGNAME, "" + PackageBuilder.DEFAULT_API_VERSION);

		// now parse the command line

		final CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
              

                
		try {
			// parse the command line arguments
			line = parser.parse(this.options, args);
                        Iterator<Option> i = line.iterator();
                        while( i.hasNext() ) {
                            Option opt = i.next();
                            if (opt.hasOptionalArg()) {
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
                    
                    if (line.getOptionValue(BUILDPROPS_LONGNAME) != null){
                        fs = new FileInputStream(line.getOptionValue(BUILDPROPS_LONGNAME));
                        buildProps.load(fs);
                    }
                    if (line.getOptionValue(CONNPROPS_LONGNAME) != null){
                        fs = new FileInputStream(line.getOptionValue(CONNPROPS_LONGNAME));
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
		//		if (isOptionSet(LOGLEVEL_LONGNAME)) {
		//			this.parameters.put("loglevel", LOGLEVEL_LONGNAME);
		//		}        

		// add default to current directory if no target directory given
		if (!buildProps.containsKey(DESTINATION_LONGNAME)) {
			System.out.println("No target directory provided, will default to current directory.");
			buildProps.put(DESTINATION_LONGNAME, ".");
		}
                
		// GIT needs download and changedata
		if (buildProps.containsKey(GITCOMMIT_LONGNAME)) {
			buildProps.put(INCLUDECHANGEDATA_LONGNAME, "true");
			buildProps.put(DOWNLOAD_LONGNAME, "true");
			buildProps.put(UNZIP_LONGNAME, "true");
		}

		// default download target to current directory if no explicit destination provided
		if ((buildProps.containsKey(GITCOMMIT_LONGNAME) || buildProps.containsKey(DOWNLOAD_LONGNAME)) && !buildProps.containsKey(METADATATARGETDIR_LONGNAME)) {
			System.out.println("No directory provided as download destination, will default to current directory");
			buildProps.put(METADATATARGETDIR_LONGNAME, ".");
		}     

		// set maxitems to default value if nothing provided
		if (!buildProps.containsKey(MAXITEMS_LONGNAME)) {
			//System.out.println("No maxitems parameter provided, will default to " + PackageBuilder.DEFAULT_MAXITEMSINPACKAGE + ".");
			buildProps.put(MAXITEMS_LONGNAME, String.valueOf(PackageBuilder.DEFAULT_MAXITEMSINPACKAGE));
		} 

		////////////////////////////////////////////////////////////////////////
		//
		// now check that we have minimum parameters needed to run
		//
		////////////////////////////////////////////////////////////////////////

		// check that we have the minimum parameters
		// either b(asedir) and d(estinationdir)
		// or s(f_url), p(assword), u(sername)

		if (buildProps.containsKey(BASEDIRECTORY_LONGNAME) && buildProps.containsKey(DESTINATION_LONGNAME)) {
                    canProceed = true;
		} if (buildProps.containsKey(SERVERURL_LONGNAME) &&
                                    buildProps.containsKey(USERNAME_LONGNAME) &&
                                    buildProps.containsKey(PASSWORD_LONGNAME)) {
                        canProceed = true;
                    } else {
                        System.out.println("Mandatory parameters not provided in files or commandline -"
                                            + " either basedir and destination or serverurl, username and password required as minimum");
                    }
		
                //Print the options & properties
                List<String> propNameList = new ArrayList<String>(buildProps.stringPropertyNames());
                Collections.sort(propNameList);
                
                
		for (String key : propNameList) {
			if (key.equals("sf.password")) {
                            String val = (String) buildProps.get(key);
			    System.out.println(key + ":" +  val.replaceAll(".", "*"));
			} else {
				System.out.println(key + ":" + buildProps.get(key));
			}
		}

		if (!canProceed) {
			this.printHelp();
		}
		return canProceed;
	}

	private void printHelp() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);

		formatter.printHelp(
				"java -jar PackageBuilder.jar [-b basedirectory] [-o <parameter file1>,<parameter file2>] [-u <SF username>] [-p <SF password>]",
				this.options);
	}

        
	// add any new parameters here only
	private void setupOptions() {

		setupParameter(BUILDPROPS_LONGNAME, 		"b", 		BUILDPROPS_LONGNAME, 		"file containing org parameters (see below)", true);
                setupParameter(CONNPROPS_LONGNAME, 		"c", 		CONNPROPS_LONGNAME, 		"file containing environment connection parameters. sf.username, sf.password, sf.apiversion, sf.serverurl(see below)", true);
		setupParameter(APIVERSION_LONGNAME, 		"a", 	APIVERSION_LONGNAME, 	"api version to use, will default to " + PackageBuilder.DEFAULT_API_VERSION +"\nsf.apiversion in property file", true);
		setupParameter(SERVERURL_LONGNAME, 			"s", 		SERVERURL_LONGNAME, 	"server URL for the org (https://login.salesforce.com) \nsf.serverurl in property file", true);
		setupParameter(PASSWORD_LONGNAME, 			"p", 		PASSWORD_LONGNAME,		"password with token for the org (t0pSecr3t) \nsf.password in property file",true);
		setupParameter(USERNAME_LONGNAME, 			"u", 		USERNAME_LONGNAME,		"username for the org (someuser@someorg.com) \nsf.username in property file\"", true);
		setupParameter(DESTINATION_LONGNAME, 	"t",  	DESTINATION_LONGNAME,	"directory where the generated package.xml will be written", true);
		setupParameter(METADATATARGETDIR_LONGNAME, "m",  METADATATARGETDIR_LONGNAME, "Directory to download meta data source (different to where package.xml will go) to", true);
		setupParameter(LOGLEVEL_LONGNAME, 		"o",  		LOGLEVEL_LONGNAME,		"output log level (INFO, FINE, FINER make sense) - defaults to INFO if not provided", true);
		setupParameter(DOWNLOAD_LONGNAME, 		"d",  		DOWNLOAD_LONGNAME,		"directly download assets, removing the need for ANT or MDAPI call", false);
		setupParameter(GITCOMMIT_LONGNAME, 		"g",  	GITCOMMIT_LONGNAME,		"commits the changes to git. Requires -d -c options", false);
		setupParameter(LOCALONLY_LONGNAME, 		"l",  	LOCALONLY_LONGNAME, 	"Don't re-download package.zip files, but process existing ones", false);
		setupParameter(UNZIP_LONGNAME, 			"f",  		UNZIP_LONGNAME,			"unzip any retrieved package(s)", false);
		setupParameter(BASEDIRECTORY_LONGNAME, 	"w",  BASEDIRECTORY_LONGNAME,	"base directory from which to generate package.xml", true);
		setupParameter(RETAINTARGETDIR_LONGNAME,"k",RETAINTARGETDIR_LONGNAME,"do not clear the metadatatargetdir provided when unzipping", false);
	}

	private void setupParameter(String propFileParamName, String shortParamName, String longParamName, String paramDescription,	boolean hasArgs) {
		if (hasArgs) {
			this.options.addOption(Option.builder(shortParamName).longOpt(longParamName)
					.desc(paramDescription)
					.hasArg()
					.build());		
		} else {
			this.options.addOption(Option.builder(shortParamName).longOpt(longParamName)
					.desc(paramDescription)
					.build());
		}
	}

	private static void displayVersionNumber() throws IOException, XmlPullParserException {
		if (VERSIONNUMBER.equals("")) {
			MavenXpp3Reader reader = new MavenXpp3Reader();
			Model model;
			if ((new File("pom.xml")).exists())
				model = reader.read(new FileReader("pom.xml"));
			else
				model = reader.read(
						new InputStreamReader(
								PackageBuilderCommandLine.class.getResourceAsStream(
										"/META-INF/maven/com.kgal/PackageBuilder/pom.xml"
										)
								)
			);
			VERSIONNUMBER = model.getArtifactId() + " " + model.getVersion();
		}
		
		System.out.println(VERSIONNUMBER);

	}

}
