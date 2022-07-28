package no.nav.melosys.sikkerhet.sts;


public interface StsConfig {
    <T> T wrapWithSts(T port, NAVSTSClient.StsClientType samlTokenType);
}
