package no.nav.melosys.sikkerhet.oidc;

import org.springframework.security.core.AuthenticationException;

public class OidcRefreshException extends AuthenticationException {
    public OidcRefreshException(String msg) {
        super(msg);
    }

    public OidcRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}