package no.nav.melosys.sikkerhet.sts;


public interface StsWrapper {
    <T> T wrapWithSts(T port, NAVSTSClient.StsClientType samlTokenType);
}
