package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.ObjectFactory;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static no.nav.melosys.integrasjon.Fagsystem.GSAK_I_JOARK;
import static no.nav.melosys.integrasjon.Fagsystem.MELOSYS;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
@Primary
public class DokSysService implements DokSysFasade {

    private static final Logger log = LoggerFactory.getLogger(DokSysService.class);

    private final DokumentproduksjonConsumer dokumentproduksjonConsumer;

    private ObjectFactory objectFactory;

    @Autowired
    public DokSysService(DokumentproduksjonConsumer dokumentproduksjonConsumer) {
        this.dokumentproduksjonConsumer = dokumentproduksjonConsumer;

        this.objectFactory = new ObjectFactory();
    }

    @Override
    public byte[] produserDokumentutkast(DokumentbestillingMetadata metadata, Object brevdata) throws IntegrasjonException {
        ProduserDokumentutkastRequest wsRequest = new ProduserDokumentutkastRequest();

        wsRequest.setUtledRegisterInfo(metadata.utledRegisterInfo);
        wsRequest.setDokumenttypeId(metadata.dokumenttypeID);
        wsRequest.setBrevdata(brevdata);

        try {
            ProduserDokumentutkastResponse wsResponse = dokumentproduksjonConsumer.produserDokumentutkast(wsRequest);
            return wsResponse.getDokumentutkast();
        } catch (ProduserDokumentutkastBrevdataValideringFeilet | ProduserDokumentutkastInputValideringFeilet e) {
            log.error("Henting av dokumentutkast feilet", e);
            throw new IntegrasjonException(e);
        }
    }

    @Override
    public DokumentbestillingResponse produserIkkeredigerbartDokument(DokumentbestillingMetadata metadata, Object brevdata) 
        throws SikkerhetsbegrensningException, IntegrasjonException {
        ProduserIkkeredigerbartDokumentRequest wsRequest = new ProduserIkkeredigerbartDokumentRequest();
        Dokumentbestillingsinformasjon info = new Dokumentbestillingsinformasjon();

        info.setDokumenttypeId(metadata.dokumenttypeID);
        info.setUtledRegisterInfo(metadata.utledRegisterInfo);
        // Hvis vedlegg skal sendes, må denne settes først når vedleggene har blitt sendt
        info.setFerdigstillForsendelse(true);

        Fagsystemer bestillendeFagsystem = objectFactory.createFagsystemer();
        bestillendeFagsystem.setKodeRef(MELOSYS.getKode());
        bestillendeFagsystem.setValue(MELOSYS.getKode());
        info.setBestillendeFagsystem(bestillendeFagsystem);

        Fagsystemer sakstilhørendeFagsystem = objectFactory.createFagsystemer();
        sakstilhørendeFagsystem.setKodeRef(GSAK_I_JOARK.getKode());
        sakstilhørendeFagsystem.setValue(GSAK_I_JOARK.getKode());
        info.setSakstilhoerendeFagsystem(sakstilhørendeFagsystem);

        Person bruker = objectFactory.createPerson();
        bruker.setIdent(metadata.bruker);
        info.setBruker(bruker);

        Person mottaker = objectFactory.createPerson();
        mottaker.setIdent(metadata.mottaker);
        info.setMottaker(mottaker);

        info.setJournalsakId(metadata.journalsakID);

        Fagomraader fagområde = objectFactory.createFagomraader();
        fagområde.setKodeRef(metadata.fagområde);
        fagområde.setValue(metadata.fagområde);
        info.setDokumenttilhoerendeFagomraade(fagområde);

        info.setJournalfoerendeEnhet(Integer.toString(MELOSYS_ENHET_ID));
        info.setSaksbehandlernavn(metadata.saksbehandler);

        wsRequest.setDokumentbestillingsinformasjon(info);
        wsRequest.setBrevdata(brevdata);

        try {
            ProduserIkkeredigerbartDokumentResponse wsResponse = dokumentproduksjonConsumer.produserIkkeredigerbartDokument(wsRequest);

            DokumentbestillingResponse response = new DokumentbestillingResponse();
            response.dokumentId = wsResponse.getDokumentId();
            response.journalpostId = wsResponse.getJournalpostId();

            return response;
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
