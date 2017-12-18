package no.nav.melosys.sikkerhet.oidc;

import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JwtConsumerFactoryTest {

    private JwtConsumerBuilder builder;

    private JwtConsumerFactory factory;

    @Before
    public void setUp() {
        builder = spy(JwtConsumerBuilder.class);
        factory = new JwtConsumerFactory(builder);
    }

    @Test
    public void createNonValidatingConsumer() throws Exception {
        JwtConsumer result = factory.createNonValidatingConsumer();

        verify(builder).setSkipAllValidators();
        verify(builder).setRelaxVerificationKeyValidation();
        verify(builder).setRelaxDecryptionKeyValidation();
        verify(builder).setDisableRequireSignature();
        verify(builder).setSkipSignatureVerification();
        verify(builder).build();

        assertThat(result, is(notNullValue()));
    }

    @Test
    public void createValidatingConsumer() throws Exception {
        Key key = mock(Key.class);
        String expectedIssuer = "issuer";

        JwtConsumer result = factory.createValidatingConsumer(key, expectedIssuer);

        verify(builder).setRequireExpirationTime();
        verify(builder).setAllowedClockSkewInSeconds(30);
        verify(builder).setRequireSubject();
        verify(builder).setExpectedIssuer("issuer");
        verify(builder).setSkipDefaultAudienceValidation();
        verify(builder).setVerificationKey(key);
        verify(builder).build();

        assertThat(result, is(notNullValue()));
    }

}