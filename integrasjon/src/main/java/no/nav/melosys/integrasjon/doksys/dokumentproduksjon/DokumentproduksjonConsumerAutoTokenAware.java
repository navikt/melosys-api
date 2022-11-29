package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsWrapper;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;

public class DokumentproduksjonConsumerAutoTokenAware implements DokumentproduksjonConsumer {

    private final StsWrapper stsWrapper;
    private final DokumentproduksjonV3 systemPort;

    public DokumentproduksjonConsumerAutoTokenAware(DokumentproduksjonConsumerConfig config, StsWrapper stsWrapper) {
        this.stsWrapper = stsWrapper;
        systemPort = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
    }

    @Override
    public ProduserDokumentutkastResponse produserDokumentutkast(ProduserDokumentutkastRequest request) throws ProduserDokumentutkastBrevdataValideringFeilet, ProduserDokumentutkastInputValideringFeilet {
        // Bruk system token frem til vi får saksbehandler azure token til å virke med doksys
        return systemPort.produserDokumentutkast(request);
    }

    @Override
    public ProduserIkkeredigerbartDokumentResponse produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest request) throws ProduserIkkeredigerbartDokumentDokumentErRedigerbart, ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak, ProduserIkkeredigerbartDokumentSikkerhetsbegrensning, ProduserIkkeredigerbartDokumentBrevdataValideringFeilet, ProduserIkkeredigerbartDokumentDokumentErVedlegg, ProduserIkkeRedigerbartDokumentInputValideringFeilet {
        // Bruk system token frem til vi får saksbehandler azure token til å virke med doksys
        return systemPort.produserIkkeredigerbartDokument(request);
    }

    private DokumentproduksjonV3 wrapWithSts(DokumentproduksjonV3 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return stsWrapper.wrapWithSts(port, oidcTilSaml);
    }

}
