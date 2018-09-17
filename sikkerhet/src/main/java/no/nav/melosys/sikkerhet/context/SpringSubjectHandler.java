package no.nav.melosys.sikkerhet.context;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSubjectHandler extends SubjectHandler {

    @Override
    public String getOidcTokenString() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            return auth.getIdToken().getParsedString();
        } else {
            return null;
        }
    }

    @Override
    public String getOidcTokenBody() {
        final String[] tokenParts = getOidcTokenString().split("\\.");
        return tokenParts.length == 1 ? tokenParts[0] : tokenParts[1];
    }

    @Override
    public String getUserID() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            return auth.getSub();
        } else {
            return null;
        }
    }

}