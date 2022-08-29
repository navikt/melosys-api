package no.nav.melosys.sikkerhet.context;

import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

public class SpringSubjectHandler extends SubjectHandler {

    private static final String AAD = "aad";
    private static final String JWT_TOKEN_CLAIM_NAVIDENT = "NAVident";

    private final SpringTokenValidationContextHolder contextHolder;

    public SpringSubjectHandler(SpringTokenValidationContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    public String getOidcTokenString() {
        return hasValidToken() ? aadToken().getTokenAsString() : null;
    }

    @Override
    public String getUserID() {
        return hasValidToken() ? aadToken().getJwtTokenClaims().get(JWT_TOKEN_CLAIM_NAVIDENT).toString() : null;
    }

    private boolean hasValidToken() {
        //contextHolder.getTokenValidationContext() kaster exception om det ikke finnes en request-context
        return RequestContextHolder.getRequestAttributes() != null && context().hasTokenFor(AAD);
    }

    private JwtToken aadToken() {
        return context().getJwtToken(AAD);
    }

    private TokenValidationContext context() {
        return contextHolder.getTokenValidationContext();
    }
}
