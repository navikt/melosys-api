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
    public String getUserID() {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            // BrukerID er case sensitive men Gosys bruker store bokstaver.
            return auth.getSub().toUpperCase();
        } else {
            return null;
        }
    }

}