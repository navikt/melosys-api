package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokSysService implements DokSysFasade {

    private static final Logger log = LoggerFactory.getLogger(DokSysService.class);

    private final DokumentproduksjonConsumer dokumentproduksjonConsumer;

    @Autowired
    public DokSysService(DokumentproduksjonConsumer dokumentproduksjonConsumer) {
        this.dokumentproduksjonConsumer = dokumentproduksjonConsumer;
    }

    @Override
    public byte[] produserDokumentutkast() throws IntegrasjonException {
        ProduserDokumentutkastRequest request = new ProduserDokumentutkastRequest();
        try {
            ProduserDokumentutkastResponse response = dokumentproduksjonConsumer.produserDokumentutkast(request);
            return response.getDokumentutkast();
        } catch (ProduserDokumentutkastBrevdataValideringFeilet | ProduserDokumentutkastInputValideringFeilet e) {
            log.error("Henting av dokumentutkast feilet", e);
            throw new IntegrasjonException(e);
        }
    }

    @Override
    public String produserIkkeredigerbartDokument() throws SikkerhetsbegrensningException, IntegrasjonException {
        ProduserIkkeredigerbartDokumentRequest request = new ProduserIkkeredigerbartDokumentRequest();
        try {
            ProduserIkkeredigerbartDokumentResponse response = dokumentproduksjonConsumer.produserIkkeredigerbartDokument(request);
            // FIXME: Får også journalpostId fra tjenesten
            return response.getDokumentId();
        } catch (ProduserIkkeredigerbartDokumentSikkerhetsbegrensning e) {
            log.error("Produksjon av dokument feilet", e);
            throw new SikkerhetsbegrensningException(e);
        } catch (ProduserIkkeredigerbartDokumentDokumentErRedigerbart | ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak
            | ProduserIkkeredigerbartDokumentBrevdataValideringFeilet | ProduserIkkeredigerbartDokumentDokumentErVedlegg
            | ProduserIkkeRedigerbartDokumentInputValideringFeilet e) {
            log.error("Produksjon av dokument feilet", e);
            throw new IntegrasjonException(e);
        }
    }
}
