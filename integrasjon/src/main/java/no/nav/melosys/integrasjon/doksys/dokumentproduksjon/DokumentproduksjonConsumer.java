package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;
import org.springframework.retry.annotation.Retryable;

@Retryable
public class DokumentproduksjonConsumer {

    private final DokumentproduksjonV3 dokumentProduksjonV3Port;

    public DokumentproduksjonConsumer(DokumentproduksjonV3 dokumentProduksjonV3Port) {
        this.dokumentProduksjonV3Port = dokumentProduksjonV3Port;
    }

    public ProduserDokumentutkastResponse produserDokumentutkast(ProduserDokumentutkastRequest request) throws ProduserDokumentutkastBrevdataValideringFeilet, ProduserDokumentutkastInputValideringFeilet {
        return dokumentProduksjonV3Port.produserDokumentutkast(request);
    }

    public ProduserIkkeredigerbartDokumentResponse produserIkkeredigerbartDokument(ProduserIkkeredigerbartDokumentRequest request) throws ProduserIkkeredigerbartDokumentDokumentErRedigerbart, ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak, ProduserIkkeredigerbartDokumentSikkerhetsbegrensning, ProduserIkkeredigerbartDokumentBrevdataValideringFeilet, ProduserIkkeredigerbartDokumentDokumentErVedlegg, ProduserIkkeRedigerbartDokumentInputValideringFeilet {
        return dokumentProduksjonV3Port.produserIkkeredigerbartDokument(request);
    }
}
