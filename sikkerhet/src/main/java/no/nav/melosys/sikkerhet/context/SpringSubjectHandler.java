package no.nav.melosys.sikkerhet.context;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSubjectHandler {

    public static String getOidcTokenString() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return auth.getIdToken().getParsedString();
        //return auth.getAccessTokenValue();
    }

    public static String getUserID() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        return auth.getSub();
    }

}