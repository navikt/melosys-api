package no.nav.melosys.sikkerhet.oidc;

import com.nimbusds.jwt.JWT;
import org.joda.time.DateTime;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.Date;

public class OidcTokenRefreshingFilter extends AbstractAuthenticationProcessingFilter {
    static final String OIDC_REFRESH_FAULT_MESSAGE = "Failed to create JWT from refreshed OIDC token";

    private final OidcRefresher refresher;
    private final ServerConfiguration serverConfiguration;
    private final OidcTokenValidator validator;
    private final JwtParser jwtParser;

    public OidcTokenRefreshingFilter(OidcRefresher refresher, ServerConfiguration serverConfiguration, OidcTokenValidator validator, JwtParser jwtParser) {
        super("/me");
        this.refresher = refresher;
        this.serverConfiguration = serverConfiguration;
        this.validator = validator;
        this.jwtParser = jwtParser;
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof OIDCAuthenticationToken && super.requiresAuthentication(request, response)) {
            OIDCAuthenticationToken oidcAuth = (OIDCAuthenticationToken) auth;
            try {
                Date expiryAsDate = oidcAuth.getIdToken().getJWTClaimsSet().getExpirationTime();
                DateTime expiry = new DateTime(expiryAsDate);
                return DateTime.now().isAfter(expiry.minusMinutes(5));
            } catch (ParseException e) {
                throw new OidcRefreshException("Failed to check expiry date for OIDC token", e);
            }
        }
        return false;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        OIDCAuthenticationToken auth = (OIDCAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String newOidc = refresher.refreshOidcToken(auth.getRefreshTokenValue());

        try {
            validator.validate(newOidc);
            JWT newJwt = jwtParser.parse(newOidc);

            PendingOIDCAuthenticationToken pendingAuth = new PendingOIDCAuthenticationToken(
                    newJwt.getJWTClaimsSet().getSubject(),
                    newJwt.getJWTClaimsSet().getIssuer(),
                    serverConfiguration,
                    newJwt,
                    auth.getAccessTokenValue(),
                    auth.getRefreshTokenValue());
            return getAuthenticationManager().authenticate(pendingAuth);
        } catch (ParseException | MalformedClaimException | InvalidJwtException e) {
            throw new OidcRefreshException(OIDC_REFRESH_FAULT_MESSAGE, e);
        }
    }
}
