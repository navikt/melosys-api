package no.nav.melosys.sikkerhet.context;

import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSubjectHandler extends SubjectHandler {

    public String getOidcTokenString() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            return auth.getIdToken().getParsedString();
        } else {
            return null;
        }
    }

    public String getUserID() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            return auth.getSub();
        } else {
            return null;
        }
    }

}