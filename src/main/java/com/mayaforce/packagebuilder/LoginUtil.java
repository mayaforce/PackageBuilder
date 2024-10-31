package com.mayaforce.packagebuilder;

import java.util.logging.Logger;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

public class LoginUtil {

    /*
     * Creates a MetadataConnection based on url, credentials
     */
//    public static MetadataConnection mdLogin(final Properties props, Logger logger) throws ConnectionException {
//        final String username = props.getProperty(PbProperties.USERNAME);
//        final String password = props.getProperty(PbProperties.PASSWORD);
//        final String token = props.getProperty(PbProperties.TOKEN);
//        String url = props.getProperty(PbProperties.URLBASE);
//
//        if ((username != null) && (password != null) && (url != null)) {
//
//            // check if it's the full url (contains /services/Soap/u/API), add
//            // if necessary
//            if (!(url.contains("/services/Soap/u/"))) {
//                url += "/services/Soap/u/" + props.getProperty(PbProperties.APIVERSION, PbConstants.DEFAULT_API_VERSION);
//            }
//
//            return LoginUtil.mdLogin(url, username, password, token, logger);
//        } else {
//            return null;
//        }
//    }

    /*
     * Creates a MetadataConnection based on a properties object
    This is used
     */
    private static PartnerConnection partnerConn;
    private static ConnectorConfig partnerConfig;
    private static ConnectorConfig metadataConfig;
    private static MetadataConnection metadataConn;

    public static MetadataConnection mdLogin(final String url, final String accessToken, final String user, final String pwd, final String token, Logger logger)
            throws ConnectionException {
        if (metadataConn == null || metadataConn.getConfig().getSessionId() == null) {
            metadataConn = new MetadataConnection(getConnectorConfig(url, accessToken, user, pwd, token, logger, false));
        }
        return metadataConn;
    }

    public static PartnerConnection soapLogin(final String url, final String accessToken, final String user, final String pwd, final String token, Logger logger) throws ConnectionException {
        if (partnerConn == null || partnerConn.getConfig().getSessionId() == null) {
            partnerConn = new PartnerConnection(getConnectorConfig(url, accessToken, user, pwd, token, logger, true));
        }
        return partnerConn;
    }

    private static ConnectorConfig getConnectorConfig(final String url, final String accessToken, final String user, final String pwd, final String token, Logger logger, boolean isPartner) throws ConnectionException {
        if (partnerConfig == null) {
            partnerConfig = new ConnectorConfig();
            metadataConfig = new ConnectorConfig();
            partnerConfig.setAuthEndpoint(url);
            metadataConfig.setAuthEndpoint(url);
            partnerConfig.setServiceEndpoint(url);
            metadataConfig.setServiceEndpoint(url.replaceAll("/u/", "/m/"));
            partnerConfig.setManualLogin(true);
            metadataConfig.setManualLogin(true);
            if (accessToken != null && accessToken.length() > 80) {
                partnerConfig.setSessionId(accessToken);
                metadataConfig.setSessionId(accessToken);
            } else {

            }
        }

        //Login if needed:
        if (partnerConfig.getSessionId() == null) {
            if (partnerConn == null) {
                partnerConn = new PartnerConnection(partnerConfig);
            }
            LoginResult lr = partnerConn.login(user, pwd + token);
            logger.fine("Url before: " + partnerConfig.getServiceEndpoint());
            logger.fine("New url: " + lr.getMetadataServerUrl());
            metadataConfig.setServiceEndpoint(lr.getMetadataServerUrl());
            metadataConfig.setUsername(lr.getUserInfo().getUserName());
            partnerConfig.setSessionId(lr.getSessionId());
            metadataConfig.setSessionId(lr.getSessionId());
        }
        if (isPartner) {
            return partnerConfig;
        } else {
            return metadataConfig;
        }
    }

}
