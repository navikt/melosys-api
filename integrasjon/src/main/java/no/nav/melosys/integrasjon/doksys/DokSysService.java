package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
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

    private static final String FALSK_MOTTAKER_ID = "11111111111";

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

        wsRequest.setUtledRegisterInfo(dokprodUtlederRegisterInfo(metadata));
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
        throws FunksjonellException, TekniskException {
        ProduserIkkeredigerbartDokumentRequest wsRequest = new ProduserIkkeredigerbartDokumentRequest();
        Dokumentbestillingsinformasjon info = new Dokumentbestillingsinformasjon();

        info.setDokumenttypeId(metadata.dokumenttypeID);
        // Parameter som settes for å angi om registerInfo skal utledes i Dokprod for dokumentet som bestilles.
        boolean adresseUtledes = dokprodUtlederRegisterInfo(metadata);
        info.setUtledRegisterInfo(adresseUtledes);
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

        info.setMottaker(lagMottaker(metadata));

        info.setJournalsakId(metadata.journalsakID);

        Fagomraader fagområde = objectFactory.createFagomraader();
        fagområde.setKodeRef(metadata.fagområde);
        fagområde.setValue(metadata.fagområde);
        info.setDokumenttilhoerendeFagomraade(fagområde);

        info.setJournalfoerendeEnhet(Integer.toString(MELOSYS_ENHET_ID));
        info.setSaksbehandlernavn(metadata.saksbehandler);

        if (!adresseUtledes) {
            info.setAdresse(lagAdresse(metadata));
        }

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

    // Dokprod kan utlede registerinfo når Melosys ikke trenger å sette adressen sammen.
    // Melosys setter adressen sammen for kontaktpersoner og utelandske myndigheter.
    private boolean dokprodUtlederRegisterInfo(DokumentbestillingMetadata metadata) {
        return Aktoersroller.MYNDIGHET != metadata.mottakersRolle;
    }

    private Adresse lagAdresse(DokumentbestillingMetadata metadata) throws TekniskException {

        if (Aktoersroller.MYNDIGHET == metadata.mottakersRolle) {
            return lagUtenlandskAdresse(metadata.utenlandskMyndighet);
        } else {
            throw new TekniskException("Det er ikke planlagt å lage en adresse for mottakersRolle: " + metadata.mottakersRolle);
        }
    }

    private UtenlandskPostadresse lagUtenlandskAdresse(UtenlandskMyndighet utenlandskMyndighet) {
        UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
        utenlandskPostadresse.setAdresselinje1(utenlandskMyndighet.gateadresse);
        utenlandskPostadresse.setAdresselinje2(utenlandskMyndighet.postnummer + " " + utenlandskMyndighet.poststed);
        utenlandskPostadresse.setLand(new Landkoder().withValue(utenlandskMyndighet.landkode.getKode()));
        return utenlandskPostadresse;
    }

    private Aktoer lagMottaker(DokumentbestillingMetadata metadata) throws FunksjonellException {
        Aktoersroller mottakersRolle = metadata.mottakersRolle;
        String mottakerID = metadata.mottakerID;

        if (mottakersRolle == null) {
            log.error("Brev bør ikke sendes, mottakersRolle er ikke satt.");
            metadata.mottakersRolle = Aktoersroller.BRUKER;
        }

        switch (mottakersRolle) {
            case BRUKER:
                return lagPerson(mottakerID);
            case ARBEIDSGIVER:
            case REPRESENTANT:
                Organisasjon organisasjon = objectFactory.createOrganisasjon();
                organisasjon.setOrgnummer(mottakerID);
                return organisasjon;
            case MYNDIGHET:
                // Dokprod støtter ikke utenlandske myndigheter så vi lager en falsk person
                // med mottakerId="11111111111" og dermed blir AvsendMottakId i Joark tom.
                return lagPerson(FALSK_MOTTAKER_ID, metadata.utenlandskMyndighet.navn);
            default:
                log.warn("MottakersRolle {} er ukjent. PERSON brukes som standard.", mottakersRolle);
                return lagPerson(mottakerID);
        }
    }

    private Aktoer lagPerson(String personID) {
        return lagPerson(personID, null);
    }

    private Aktoer lagPerson(String personID, String navn) {
        Person person = objectFactory.createPerson();
        person.setIdent(personID);
        person.setNavn(navn);
        return person;
    }
}
