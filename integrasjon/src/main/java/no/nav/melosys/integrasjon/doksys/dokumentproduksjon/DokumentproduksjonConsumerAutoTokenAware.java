package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfig;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;

public class DokumentproduksjonConsumerAutoTokenAware implements DokumentproduksjonConsumer {

    private final StsConfig stsConfig;
    private final DokumentproduksjonV3 systemPort;
    private final DokumentproduksjonV3 saksbehandlerPort;

    public DokumentproduksjonConsumerAutoTokenAware(DokumentproduksjonConsumerConfig config, StsConfig stsConfig) {
        this.stsConfig = stsConfig;
        saksbehandlerPort = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML);
        systemPort = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
    }

    @Override
    public ProduserDokumentutkastResponse produserDokumentutkast(ProduserDokumentutkastRequest request) throws ProduserDokumentutkastBrevdataValideringFeilet, ProduserDokumentutkastInputValideringFeilet {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return systemPort.produserDokumentutkast(request);
        }
        return saksbehandlerPort.produserDokumentutkast(request);
    }

    @Override
    public ProduserIkkeredigerbartDokumentResponse produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest request) throws ProduserIkkeredigerbartDokumentDokumentErRedigerbart, ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak, ProduserIkkeredigerbartDokumentSikkerhetsbegrensning, ProduserIkkeredigerbartDokumentBrevdataValideringFeilet, ProduserIkkeredigerbartDokumentDokumentErVedlegg, ProduserIkkeRedigerbartDokumentInputValideringFeilet {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return systemPort.produserIkkeredigerbartDokument(request);
        }
        return saksbehandlerPort.produserIkkeredigerbartDokument(request);
    }

    private DokumentproduksjonV3 wrapWithSts(DokumentproduksjonV3 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return stsConfig.wrapWithSts(port, oidcTilSaml);
    }

}
