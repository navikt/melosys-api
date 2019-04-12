package no.nav.melosys.sikkerhet.context;


import java.util.Optional;

import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.authentication.Pac4jAuthentication;
import org.pac4j.springframework.security.authentication.Pac4jAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSubjectHandler extends SubjectHandler {

    @Override
    public String getOidcTokenString() {
        return oidcProfile().map(OidcProfile::getIdTokenString).orElse(null);
    }

    @Override
    public String getUserID() {
        return oidcProfile()
            .map(OidcProfile::getSubject)
            .map(String::toUpperCase)
            .orElse(null);
    }

    private static Optional<OidcProfile> oidcProfile() {
        return authentication()
            .map(Pac4jAuthentication::getProfile)
            .map(OidcProfile.class::cast);
    }

    private static Optional<Pac4jAuthenticationToken> authentication() {
        return Optional.ofNullable((Pac4jAuthenticationToken) SecurityContextHolder.getContext().getAuthentication());
    }
}