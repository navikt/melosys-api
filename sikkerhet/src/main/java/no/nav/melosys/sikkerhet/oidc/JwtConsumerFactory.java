package no.nav.melosys.sikkerhet.oidc;

import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtConsumerFactory {

    private JwtConsumerBuilder jwtConsumerBuilder;

    public JwtConsumerFactory() {
        this.jwtConsumerBuilder = new JwtConsumerBuilder();
    }

    public JwtConsumerFactory(JwtConsumerBuilder jwtConsumerBuilder) {
        this.jwtConsumerBuilder = jwtConsumerBuilder;
    }

    JwtConsumer createNonValidatingConsumer() {
        return jwtConsumerBuilder
                .setSkipAllValidators()
                .setRelaxVerificationKeyValidation()
                .setRelaxDecryptionKeyValidation()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();
    }

    JwtConsumer createValidatingConsumer(Key key, String expectedIssuer) {
        return jwtConsumerBuilder
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(30) // Default er tatt fra Foreldrepenger, som har ref. implementasjon.
                .setRequireSubject()
                .setSkipDefaultAudienceValidation() // Audience skippes i NAV ettersom man skal kunne bruke den på tvers.
                .setExpectedIssuer(expectedIssuer)
                .setVerificationKey(key)
                .build();
    }
}