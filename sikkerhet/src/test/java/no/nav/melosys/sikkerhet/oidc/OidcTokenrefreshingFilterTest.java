package no.nav.melosys.sikkerhet.oidc;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import org.joda.time.DateTime;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mitre.openid.connect.model.OIDCAuthenticationToken;
import org.mitre.openid.connect.model.PendingOIDCAuthenticationToken;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OidcTokenrefreshingFilterTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OidcRefresher refresherMock;

    @Mock
    private ServerConfiguration serverConfigurationMock;

    @Mock
    private OidcTokenValidator validatorMock;

    @Mock
    private JwtParser jwtParserMock;

    @Mock
    private AuthenticationManager authManagerMock;

    @Mock
    private OIDCAuthenticationToken authMock;

    @Mock
    private JWT refreshedJwtMock;

    @Mock
    private HttpServletRequest requestMock;

    @Mock
    private HttpServletResponse responseMock;

    private OidcTokenRefreshingFilter filter;

    @Before
    public void before() throws Exception {
        filter = new OidcTokenRefreshingFilter(refresherMock, serverConfigurationMock, validatorMock, jwtParserMock);
        filter.setAuthenticationManager(authManagerMock);

        when(requestMock.getServletPath()).thenReturn("/me");
        SecurityContextHolder.getContext().setAuthentication(authMock);

        setTokenExpiry(DateTime.now());

        when(refresherMock.refreshOidcToken(anyString())).thenReturn("blerp.blerpzon.blerpi");

        when(refreshedJwtMock.getJWTClaimsSet()).thenReturn(new JWTClaimsSet.Builder().subject("subject").issuer("issuer").build());
        when(jwtParserMock.parse(anyString())).thenReturn(refreshedJwtMock);
    }

    @After
    public void after() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void requiresAuthenticationReturnsFalseWhenNoAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(null);

        boolean result = filter.requiresAuthentication(requestMock, responseMock);

        assertThat(result, is(false));
    }

    @Test
    public void requiresAuthenticationReturnsFalseWhenNotOidcAuthenticationToken() {
        Authentication auth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(auth);

        boolean result = filter.requiresAuthentication(requestMock, responseMock);

        assertThat(result, is(false));
    }

    @Test
    public void requiresAuthenticationReturnsFalseWhenPathNotMe() {
        when(requestMock.getServletPath()).thenReturn("/test");

        boolean result = filter.requiresAuthentication(requestMock, responseMock);

        assertThat(result, is(false));
    }

    @Test
    public void requiresAuthenticationReturnsFalseWhenExpiryIn6Minutes() throws Exception {
        setTokenExpiry(DateTime.now().plusMinutes(6));

        boolean result = filter.requiresAuthentication(requestMock, responseMock);

        assertThat(result, is(false));
    }

    @Test
    public void requiresAuthenticationReturnsTrueWhenExpiryIn4Minutes() throws Exception {
        setTokenExpiry(DateTime.now().plusMinutes(4));

        boolean result = filter.requiresAuthentication(requestMock, responseMock);

        assertThat(result, is(true));
    }

    @Test
    public void requiresAuthenticationThrowsExceptionWhenParseExceptionIsThrown() throws Exception {
        ParseException ex = new ParseException(null, 0);
        JWT jwt = mock(JWT.class);
        when(jwt.getJWTClaimsSet()).thenThrow(ex);
        when(authMock.getIdToken()).thenReturn(jwt);

        expectedException.expect(OidcRefreshException.class);
        expectedException.expectMessage("Failed to check expiry date for OIDC token");
        expectedException.expectCause(sameInstance(ex));

        filter.requiresAuthentication(requestMock, responseMock);
    }

    @Test
    public void attemptAuthenticationCallsRefresherWithRefreshToken() {
        when(authMock.getRefreshTokenValue()).thenReturn("refresh");

        filter.attemptAuthentication(requestMock, responseMock);

        verify(refresherMock).refreshOidcToken("refresh");
    }

    @Test
    public void attemptAuthenticationCallsJwtParserWithNewOidcToken() throws Exception {
        when(refresherMock.refreshOidcToken(isNull())).thenReturn("newToken");

        filter.attemptAuthentication(requestMock, responseMock);

        verify(jwtParserMock).parse("newToken");
    }

    @Test
    public void attemptAuthenticationSetsValuesAndAuthenticatesWithAuthenticationManager() {
        when(authMock.getAccessTokenValue()).thenReturn("accessToken");
        when(authMock.getRefreshTokenValue()).thenReturn("refreshToken");

        filter.attemptAuthentication(requestMock, responseMock);

        ArgumentCaptor<PendingOIDCAuthenticationToken> captor = ArgumentCaptor.forClass(PendingOIDCAuthenticationToken.class);
        verify(authManagerMock).authenticate(captor.capture());

        assertThat(captor.getValue().getSub(), is("subject"));
        assertThat(captor.getValue().getIssuer(), is("issuer"));
        assertThat(captor.getValue().getServerConfiguration(), is(serverConfigurationMock));
        assertThat(captor.getValue().getIdToken(), is(refreshedJwtMock));
        assertThat(captor.getValue().getAccessTokenValue(), is("accessToken"));
        assertThat(captor.getValue().getRefreshTokenValue(), is("refreshToken"));
    }

    @Test
    public void attemptAuthenticationThrowsExceptionWhenParseExceptionIsThrown() throws Exception {
        when(authMock.getRefreshTokenValue()).thenReturn("refreshToken");

        ParseException ex = new ParseException(null, 0);
        when(refreshedJwtMock.getJWTClaimsSet()).thenThrow(ex);

        expectedException.expect(OidcRefreshException.class);
        expectedException.expectMessage(OidcTokenRefreshingFilter.OIDC_REFRESH_FAULT_MESSAGE);
        expectedException.expectCause(sameInstance(ex));

        filter.attemptAuthentication(requestMock, responseMock);
    }

    @Test
    public void attemptAuthenticationThrowsExceptionWhenMalformedClaimExceptionIsThrown() throws Exception {
        MalformedClaimException ex = new MalformedClaimException(null);
        doThrow(ex).when(validatorMock).validate(isNull());

        expectedException.expect(OidcRefreshException.class);
        expectedException.expectMessage(OidcTokenRefreshingFilter.OIDC_REFRESH_FAULT_MESSAGE);
        expectedException.expectCause(sameInstance(ex));

        filter.attemptAuthentication(requestMock, responseMock);
    }

    @Test
    public void attemptAuthenticationThrowsExceptionWhenInvalidJwtExceptionIsThrown() throws Exception {
        InvalidJwtException ex = new InvalidJwtException(null, null, null);
        doThrow(ex).when(validatorMock).validate(isNull());

        expectedException.expect(OidcRefreshException.class);
        expectedException.expectMessage(OidcTokenRefreshingFilter.OIDC_REFRESH_FAULT_MESSAGE);
        expectedException.expectCause(sameInstance(ex));

        filter.attemptAuthentication(requestMock, responseMock);
    }

    private void setTokenExpiry(DateTime expiry) throws Exception {
        JWT jwt = mock(JWT.class);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().expirationTime(expiry.toDate()).build();
        when(jwt.getJWTClaimsSet()).thenReturn(claimsSet);
        when(authMock.getIdToken()).thenReturn(jwt);
    }
}
