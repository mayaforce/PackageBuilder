package com.mayaforce.packagebuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.mayaforce.migrationtoolutils.Utils;
import com.mayaforce.packagebuilder.profilecompare.TagComparer;

public class ProfileCompare {

    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final HashMap<String, String> TAGSTOBECOMPARED = new HashMap<String, String>() {
        /**
         *
         */
        private static final long serialVersionUID = 576428229572675412L;

        {
            put("userPermissions", "com.mayaforce.packagebuilder.profilecompare.UserPermissionsComparer");
        }
    };

    private static final String PROFILEFOLDERNAME = "profiles";

    public ProfileCompare(final Level level) {
        this.logger.setLevel(level);
    }

    public static void main(String[] args)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, SAXException, IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, XPathExpressionException {
        // TODO Auto-generated method stub
        ProfileCompare pc = new ProfileCompare(Level.INFO);
        pc.diffTwoProfileFiles(new File("profilecompare/clean/Sysadmclean.profile"),
                new File("profilecompare/dirty/Sysadmdirty.profile"),
                "profilecompare", "result.xml");

    }

    /*
	 * This function will take two XML Documents (which are expected to be the same profile)
	 * And remove all permissions that appear in both and are identical
	 * 
	 * This is intended to remove all the boilerplate stuff from profiles and leave only the actual relevant 
	 * content (i.e. field permissions, etc.) so no changes that weren't expected are moved
	 * inadvertently
     */
    public Document diffTwoProfiles(Document clean, Document dirty)
            throws ParserConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException, XPathExpressionException {

        // we're going to try to remove all the userPermission tags from the profile if they match across clean and dirty
        //    <userPermissions>
        //        <enabled>true</enabled>
        //        <name>ActivateOrder</name>
        //    </userPermissions>
        // go through clean, for each tag configured to be compared, run the comparer 
        // and remove the tag from the final document if matching
        for (String t : TAGSTOBECOMPARED.keySet()) {

            // initialize comparer
            TagComparer c = (TagComparer) Class.forName(TAGSTOBECOMPARED.get(t)).newInstance();

            NodeList tagsDirty = dirty.getDocumentElement().getElementsByTagName(t);

            // remove all these tags from the dirty document for now,
            // we'll add back all the ones which are not identical later
            NodeList tagsClean = clean.getDocumentElement().getElementsByTagName(t);
            for (int i = 0; i < tagsDirty.getLength(); i++) {

                for (int j = 0; j < tagsClean.getLength(); j++) {
                    if (c.isIdentical(tagsDirty.item(i), tagsClean.item(j))) {
                        // remove it from the target
                        dirty.getDocumentElement().removeChild(tagsDirty.item(i));
                    }
                }
            }
        }

        // now clean up any leftover whitespace in the dirty document after removing all differences
        dirty.normalize();
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                dirty,
                XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }
        return dirty;
    }

    /*
	 * This function will pick up two files (which are expected to the the same profile, with different
	 * content, convert them into XML Documents and run the comparison
     */
    public void diffTwoProfileFiles(File clean, File dirty, String targetDir, String filename)
            throws SAXException, IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException, InstantiationException, IllegalAccessException, ClassNotFoundException, XPathExpressionException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        documentFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document diff = diffTwoProfiles(documentBuilder.parse(clean), documentBuilder.parse(dirty));

        Utils.writeXMLFile(diff, targetDir + File.separator + filename);

    }

    /*
	 * This method will take a zip file, unzip it and then remove the userPermissions tags
	 * from all profiles so that only actual field/object/class/page/etc. permissions are contained 
	 * in the profile
     */
    public void stripUserPermissionsFromProfiles(String metadataTargetDirectory)
            throws ParserConfigurationException, SAXException, IOException {

        // get the profile folder
        File profileFolder = new File(metadataTargetDirectory + File.separator + PROFILEFOLDERNAME);

        if (!profileFolder.exists() && !profileFolder.isDirectory()) {
            this.logger.log(Level.INFO, "Something wrong: cannot locate profiles folder in the unzipped directory. Cannot continue stripping profiles.");
            return;
        } else {
            // get XML parsing ready

            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

            // for each profile
            for (File profile : profileFolder.listFiles()) {
                // open, get list of tags to remove
                this.logger.log(Level.FINER, "Stripping profile " + profile.getName());
                Document myProfileDocument = documentBuilder.parse(profile);

                for (String t : TAGSTOBECOMPARED.keySet()) {

                    NodeList tags = myProfileDocument.getDocumentElement().getElementsByTagName(t);

                    // remove all these tags from the document for now
                    for (int i = tags.getLength() - 1; i >= 0; i--) {
                        myProfileDocument.getDocumentElement().removeChild(tags.item(i));
                    }
                }

                // write the file back
                Utils.writeXMLFile(myProfileDocument, profile.getAbsolutePath());
            }
        }

    }

}
