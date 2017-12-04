package no.nav.melosys.sikkerhet.sts;

import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.cxf.ws.security.trust.STSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NAVSTSClient extends STSClient {
    private static final Logger logger = LoggerFactory.getLogger(NAVSTSClient.class);
    private static TokenStore tokenStore;
    private StsClientType type;

    public enum StsClientType {
        SYSTEM_SAML,
        SECURITYCONTEXT_TIL_SAML
    }

    public NAVSTSClient(Bus bus, StsClientType type) {
        super(bus);
        this.type = type;
    }

    @Override
    protected boolean useSecondaryParameters() {
        return false;
    }

    @Override
    public SecurityToken requestSecurityToken(String appliesTo, String action, String requestType, String binaryExchange) throws Exception {
        ensureTokenStoreExists();

        final String userId = getUserId();
        final String key = getTokenStoreKey();
        if (key == null) {
            throw new RuntimeException("Cannot retrieve SAML without security token!");
        }

        SecurityToken token = tokenStore.getToken(key);
        if (token == null) {
            logger.debug("Missing token for user {}, fetching it from STS", userId);
            token = super.requestSecurityToken(appliesTo, action, requestType, binaryExchange);
            tokenStore.add(key, token);
        } else {
            logger.debug("Retrieved token for user {} from tokenStore", userId);
        }
        return token;
    }

    private String getTokenStoreKey() {
        return getUserKey();
    }

    private String getUserKey() {
        String jwt = null;

        switch (type) {
            case SECURITYCONTEXT_TIL_SAML:
                jwt = SpringSubjectHandler.getOidcTokenString();
                break;
            case SYSTEM_SAML:
                jwt = "systemSAML";
                break;
            default:
                throw new RuntimeException("STS client with type: " + type + " is not supported.");
        }

        return jwt;
    }

    private String getUserId() {
        return SpringSubjectHandler.getUserID();
    }

    private void ensureTokenStoreExists() {
        if (tokenStore == null) {
            createTokenStore();
        }
    }

    private synchronized void createTokenStore() {
        if (tokenStore == null) {
            logger.debug("Creating tokenStore");
            tokenStore = TokenStoreFactory.newInstance().newTokenStore(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message);
        }
    }
}