package no.nav.melosys.sikkerhet.context;

import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

public class SpringSubjectHandler extends SubjectHandler {

    private static final String ISSO = "isso";

    private final SpringTokenValidationContextHolder contextHolder;

    public SpringSubjectHandler(SpringTokenValidationContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    public String getOidcTokenString() {
        return hasValidToken() ? issoToken().getTokenAsString() : null;
    }

    @Override
    public String getUserID() {
        return hasValidToken() ? issoToken().getSubject() : null;
    }

    private boolean hasValidToken() {
        //contextHolder.getTokenValidationContext() kaster exception om det ikke finnes en request-context
        return RequestContextHolder.getRequestAttributes() != null && context().hasTokenFor(ISSO);
    }

    private JwtToken issoToken() {
        return context().getJwtToken(ISSO);
    }

    private TokenValidationContext context() {
        return contextHolder.getTokenValidationContext();
    }
}
