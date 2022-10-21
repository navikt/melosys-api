package no.nav.melosys.sikkerhet.context;

import java.util.ArrayList;
import java.util.List;

import com.nimbusds.jose.shaded.json.JSONArray;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

public class SpringSubjectHandler extends SubjectHandler {

    private static final String AZURE_ACTIVE_DIRECTORY = "aad";
    private static final String JWT_TOKEN_CLAIM_NAVIDENT = "NAVident";
    private static final String JWT_TOKEN_CLAIM_NAME = "name";
    private static final String JWT_TOKEN_CLAIM_GROUPS = "groups";

    private final SpringTokenValidationContextHolder contextHolder;

    public SpringSubjectHandler(SpringTokenValidationContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    public String getOidcTokenString() {
        return hasValidToken() ? azureActiveDirectoryToken().getTokenAsString() : null;
    }

    @Override
    public String getUserID() {
        return hasValidToken() ? azureActiveDirectoryToken().getJwtTokenClaims().get(JWT_TOKEN_CLAIM_NAVIDENT).toString() : null;
    }

    @Override
    public String getName() {
        return hasValidToken() ? azureActiveDirectoryToken().getJwtTokenClaims().get(JWT_TOKEN_CLAIM_NAME).toString() : null;
    }

    @Override
    public List<String> getGroups() {
        ArrayList<String> groups = new ArrayList<>();
        JSONArray jArray = (JSONArray) azureActiveDirectoryToken().getJwtTokenClaims().get(JWT_TOKEN_CLAIM_GROUPS);

        if (jArray != null) {
            for (Object o : jArray) {
                groups.add(o.toString());
            }
        }
        return groups;
    }

    private boolean hasValidToken() {
        //contextHolder.getTokenValidationContext() kaster exception om det ikke finnes en request-context
        return RequestContextHolder.getRequestAttributes() != null && getValidationContext().hasTokenFor(AZURE_ACTIVE_DIRECTORY);
    }

    private JwtToken azureActiveDirectoryToken() {
        return getValidationContext().getJwtToken(AZURE_ACTIVE_DIRECTORY);
    }

    private TokenValidationContext getValidationContext() {
        return contextHolder.getTokenValidationContext();
    }
}
