/** ========================================================================= *
 * Copyright (C)  2017, 2018 Salesforce Inc ( http://www.salesforce.com/      *
 *                            All rights reserved.                            *
 *                                                                            *
 *  @author     Stephan H. Wissel (stw) <swissel@salesforce.com>              *
 *                                       @notessensei                         *
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

import com.kgal.migrationtoolutils.Utils;
import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.kgal.packagebuilder.PackageBuilderCommandLine;
import java.io.FileNotFoundException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author swissel
 *
 */
public class PackageBuilderCommandLineTest {

    @Test
    public final void testParametersFromFile() {
        String workingDir = System.getProperty("user.dir");
        System.out.println("Current working directory : " + workingDir);
        PackageBuilderCommandLine pbc = new PackageBuilderCommandLine();
        String[] args = new String[1];
        args[0] = "--buildprops=properties/test.properties";
        try {
            pbc.parseCommandLine(args);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PackageBuilderCommandLineTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Properties result = pbc.getParameters();
        assertTrue(result.containsKey(PbProperties.SKIPPATTERNS));
        assertTrue(result.containsKey(PbProperties.PASSWORD));
        assertTrue(result.containsKey(PbProperties.BASEDIRECTORY));
        assertTrue(result.containsKey(PbProperties.DESTINATION));
        assertTrue(result.containsKey(PbProperties.SERVERURL));
        assertTrue(result.containsKey(PbProperties.METADATAITEMS));
        assertTrue(result.containsKey(PbProperties.INCLUDECHANGEDATA));
        assertTrue(result.containsKey(PbProperties.APIVERSION));
        assertTrue(result.containsKey(PbProperties.USERNAME));
        assertTrue(result.containsKey(PbProperties.LOGLEVEL));
        assertTrue(result.containsKey(PbProperties.FROMDATE));
        assertTrue(result.containsKey(PbProperties.TODATE));
        assertTrue(result.containsKey(PbProperties.DOWNLOAD));
        assertTrue(result.containsKey(PbProperties.METADATATARGETDIR));
    }

    @Test
    public final void testMissingParamsAlert() {
        PackageBuilderCommandLine pbc = new PackageBuilderCommandLine();
        String[] args = new String[1];
        args[0] = "";
        boolean result = false;
        try {
            result = pbc.parseCommandLine(args);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PackageBuilderCommandLineTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertTrue(!result);        
    }
    
    @Test
    public final void testGitParam() {
        PackageBuilderCommandLine pbc = new PackageBuilderCommandLine();
        String[] args = new String[2];
        args[0] = "-bproperties/test.properties";
        args[1] = "-g";
        try {
            pbc.parseCommandLine(args);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PackageBuilderCommandLineTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Properties result = pbc.getParameters();
        assertEquals("true", result.get(PbProperties.INCLUDECHANGEDATA));
        assertEquals("true", result.get(PbProperties.DOWNLOAD));
        assertEquals("true", result.get(PbProperties.GITCOMMIT));        
    }
    
}
