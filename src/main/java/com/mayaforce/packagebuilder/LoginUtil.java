package com.mayaforce.packagebuilder;

import java.util.Properties;
import java.util.logging.Level;
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
    public static MetadataConnection mdLogin(final Properties props, Logger logger) throws ConnectionException {
        final String username = props.getProperty(PbProperties.USERNAME);
        final String password = props.getProperty(PbProperties.PASSWORD);
        final String token = props.getProperty(PbProperties.TOKEN);
        String url = props.getProperty(PbProperties.URLBASE);

        if ((username != null) && (password != null) && (url != null)) {

            // check if it's the full url (contains /services/Soap/u/API), add
            // if necessary
            if (!(url.contains("/services/Soap/u/"))) {
                url += "/services/Soap/u/" + props.getProperty(PbProperties.APIVERSION, PbConstants.DEFAULT_API_VERSION);
            }

            return LoginUtil.mdLogin(url, username, password, token, logger);
        } else {
            return null;
        }
    }

    /*
     * Creates a MetadataConnection based on a properties object
    This is used
     */
    public static MetadataConnection mdLogin(final String url, final String user, final String pwd, final String token, Logger logger)
            throws ConnectionException {
        final LoginResult loginResult = LoginUtil.loginToSalesforce(user, pwd + token, url);
        return LoginUtil.createMetadataConnection(loginResult);
    }

    public static PartnerConnection soapLogin(final String url, final String user, final String pwd, final String token, Logger logger) {

        PartnerConnection conn = null;

        try {
            final ConnectorConfig config = new ConnectorConfig();
            config.setUsername(user);
            config.setPassword(pwd + token);

            logger.log(Level.INFO, "AuthEndPoint: " + url);
            config.setAuthEndpoint(url);

            conn = new PartnerConnection(config);

        } catch (final ConnectionException ce) {
            ce.printStackTrace();
        }

        return conn;
    }

    private static MetadataConnection createMetadataConnection(final LoginResult loginResult)
            throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setServiceEndpoint(loginResult.getMetadataServerUrl());
        config.setSessionId(loginResult.getSessionId());
        return new MetadataConnection(config);
    }

    private static LoginResult loginToSalesforce(
            final String username,
            final String password,
            final String loginUrl) throws ConnectionException {
        final ConnectorConfig config = new ConnectorConfig();
        config.setAuthEndpoint(loginUrl);
        config.setServiceEndpoint(loginUrl);
        config.setManualLogin(true);
        return (new PartnerConnection(config)).login(username, password);
    }
}
