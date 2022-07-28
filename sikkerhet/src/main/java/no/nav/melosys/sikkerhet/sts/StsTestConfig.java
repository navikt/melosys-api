package no.nav.melosys.sikkerhet.sts;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local-mock", "test"})
public class StsTestConfig implements StsConfig {

    @Override
    public <T> T wrapWithSts(T port, NAVSTSClient.StsClientType samlTokenType) {
        return port;
    }
}
