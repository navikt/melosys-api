package no.nav.melosys.sikkerhet.oidc;

import jwks.JwksCache;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Key;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OidcTokenValidatorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private JwksCache jwksCacheMock;

    @Mock
    private JwtConsumerFactory consumerFactoryMock;

    @Mock
    private JwtConsumer nonValidatingJwtConsumerMock;

    @Mock
    private JwtConsumer validatingJwtConsumerMock;

    @Mock
    private JsonWebStructure jwsMock;

    @Mock
    private Key keyMock;

    @Mock
    private JwtClaims jwtClaimsMock;

    private OidcTokenValidator validator;

    @Before
    public void before() throws Exception {
        when(consumerFactoryMock.createNonValidatingConsumer()).thenReturn(nonValidatingJwtConsumerMock);
        JwtContext nonValidatingContextMock = mock(JwtContext.class);
        when(nonValidatingJwtConsumerMock.process(anyString())).thenReturn(nonValidatingContextMock);

        when(jwsMock.getKeyIdHeaderValue()).thenReturn("kid");
        when(jwsMock.getAlgorithmHeaderValue()).thenReturn("alg");
        when(nonValidatingContextMock.getJoseObjects()).thenReturn(singletonList(jwsMock));

        when(consumerFactoryMock.createValidatingConsumer(any(Key.class), any(String.class))).thenReturn(validatingJwtConsumerMock);
        when(jwksCacheMock.getValidationKey(anyString(), anyString())).thenReturn(keyMock);
        when(validatingJwtConsumerMock.processToClaims(anyString())).thenReturn(jwtClaimsMock);

        when(jwtClaimsMock.getClaimValue("azp", String.class)).thenReturn("azp");

        ServerConfiguration serverConfiguration = new ServerConfiguration();
        serverConfiguration.setIssuer("issuer");
        validator = new OidcTokenValidator(jwksCacheMock, consumerFactoryMock, serverConfiguration);
    }

    @Test
    public void throwsExceptionWhenOidcIsNull() throws Exception {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("OIDC Token cannot be null");

        validator.validate(null);
    }

    @Test
    public void callsNonValidatingConsumerWithOidcToken() throws Exception {
        validator.validate("oidc");

        verify(nonValidatingJwtConsumerMock).process("oidc");
    }

    @Test
    public void callsJwksCacheWithKeyIdAndAlgFromOidcToken() throws Exception {
        validator.validate("oidc");

        verify(jwksCacheMock).getValidationKey("kid", "alg");
    }

    @Test
    public void callsJwksCacheWithKeyIdEmptyAndAlgFromOidcTokenWhenNoKeyId() throws Exception {
        when(jwsMock.getKeyIdHeaderValue()).thenReturn(null);

        validator.validate("oidc");

        verify(jwksCacheMock).getValidationKey("", "alg");
    }

    @Test
    public void throwsExceptionWhenNoKeyReturnedFromJwksCache() throws Exception {
        when(jwksCacheMock.getValidationKey(anyString(), anyString())).thenReturn(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Jwt with keyId kid and algorithm alg is not in JWKs");

        validator.validate("oidc");
    }

    @Test
    public void callsValidatingConsumerFactoryWithKey() throws Exception {
        validator.validate("oidc");

        verify(consumerFactoryMock).createValidatingConsumer(keyMock, "issuer");
    }

    @Test
    public void callsProcessToClaimsOnValidatingJwtConsumer() throws Exception {
        validator.validate("oidc");

        verify(validatingJwtConsumerMock).processToClaims("oidc");
    }

    @Test
    // Tok med denne testen her ettersom det er viktig at man ikke catcher feilen! Det er 90% av valideringen til OIDC.
    public void throwsInvalidJwtExceptionFromValidatingJwtConsumer() throws Exception {
        InvalidJwtException ex = new InvalidJwtException(null, null, null);
        when(validatingJwtConsumerMock.processToClaims(anyString())).thenThrow(ex);

        expectedException.expect(sameInstance(ex));

        validator.validate("oidc");
    }

    @Test
    public void throwsExceptionWhenNoAzpPresent() throws Exception {
        when(jwtClaimsMock.getClaimValue("azp", String.class)).thenReturn(null);

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("An Azp claim is required");

        validator.validate("oidc");
    }

}