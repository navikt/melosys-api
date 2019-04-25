package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.ObjectFactory;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.informasjon.*;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserDokumentutkastResponse;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentRequest;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.meldinger.ProduserIkkeredigerbartDokumentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import static no.nav.melosys.integrasjon.Fagsystem.GSAK_I_JOARK;
import static no.nav.melosys.integrasjon.Fagsystem.MELOSYS;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

@Service
@Primary
public class DoksysService implements DoksysFasade {

    private static final Logger log = LoggerFactory.getLogger(DoksysService.class);

    private static final String FALSK_MOTTAKER_ID = "11111111111";
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final DokumentproduksjonConsumer dokumentproduksjonConsumer;

    private ObjectFactory objectFactory;

    @Autowired
    public DoksysService(DokumentproduksjonConsumer dokumentproduksjonConsumer) {
        this.dokumentproduksjonConsumer = dokumentproduksjonConsumer;
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public byte[] produserDokumentutkast(Dokumentbestilling dokumentbestilling) throws IntegrasjonException {
        ProduserDokumentutkastRequest wsRequest = new ProduserDokumentutkastRequest();
        DokumentbestillingMetadata metadata = dokumentbestilling.getMetadata();

        wsRequest.setUtledRegisterInfo(metadata.utledRegisterInfo);
        wsRequest.setDokumenttypeId(metadata.dokumenttypeID);
        wsRequest.setBrevdata(dokumentbestilling.getBrevData());

        try {
            if (log.isDebugEnabled()) {
                log.debug("Sender request:{} {}", LINE_SEPARATOR, wsRequest);
                log.debug("Bestiller utkast:{} {}", LINE_SEPARATOR, xmlToString(dokumentbestilling.getBrevData()));
            }
            ProduserDokumentutkastResponse wsResponse = dokumentproduksjonConsumer.produserDokumentutkast(wsRequest);
            return wsResponse.getDokumentutkast();
        } catch (ProduserDokumentutkastBrevdataValideringFeilet | ProduserDokumentutkastInputValideringFeilet e) {
            log.error("Henting av dokumentutkast feilet", e);
            throw new IntegrasjonException(e);
        }
    }

    @Override
    public DokumentbestillingResponse produserIkkeredigerbartDokument(Dokumentbestilling dokumentbestilling)
        throws FunksjonellException, TekniskException {
        ProduserIkkeredigerbartDokumentRequest wsRequest = new ProduserIkkeredigerbartDokumentRequest();
        Dokumentbestillingsinformasjon info = new Dokumentbestillingsinformasjon();

        DokumentbestillingMetadata metadata = dokumentbestilling.getMetadata();
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
        if (!metadata.utledRegisterInfo) {
            bruker.setNavn(metadata.brukerNavn);
        }
        bruker.setIdent(metadata.brukerID);
        info.setBruker(bruker);

        info.setMottaker(lagMottaker(metadata));

        info.setJournalsakId(metadata.journalsakID);

        Fagomraader fagområde = objectFactory.createFagomraader();
        fagområde.setKodeRef(metadata.fagområde);
        fagområde.setValue(metadata.fagområde);
        info.setDokumenttilhoerendeFagomraade(fagområde);

        info.setJournalfoerendeEnhet(Integer.toString(MELOSYS_ENHET_ID));
        info.setSaksbehandlernavn(metadata.saksbehandler);

        if (!metadata.utledRegisterInfo) {
            info.setAdresse(lagAdresse(metadata));
        }

        wsRequest.setDokumentbestillingsinformasjon(info);
        wsRequest.setBrevdata(dokumentbestilling.getBrevData());

        try {
            if (log.isDebugEnabled()) {
                log.debug("Sender request:{} {}", LINE_SEPARATOR, wsRequest);
                log.debug("Bestiller dokument:{} {}", LINE_SEPARATOR, xmlToString(dokumentbestilling.getBrevData()));
            }
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

    private Adresse lagAdresse(DokumentbestillingMetadata metadata) throws TekniskException {
        if (metadata.mottaker.erUtenlandskMyndighet()) {
            return lagUtenlandskAdresse(metadata.utenlandskMyndighet);
        } else {
            throw new TekniskException("Det er ikke planlagt å lage en adresse for mottakerRolle: " + metadata.mottaker.getRolle());
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
        if (metadata.mottaker == null) {
            throw new FunksjonellException("Brev kan ikke sendes, mottaker er ikke satt.");
        }

        Aktoersroller mottakerRolle = metadata.mottaker.getRolle();
        String mottakerID = metadata.mottakerID;

        switch (mottakerRolle) {
            case BRUKER:
                return lagPerson(mottakerID);
            case ARBEIDSGIVER:
            case REPRESENTANT:
                Organisasjon organisasjon = objectFactory.createOrganisasjon();
                organisasjon.setOrgnummer(mottakerID);
                return organisasjon;
            case MYNDIGHET:
                if (metadata.mottaker.erUtenlandskMyndighet()) {
                    // Dokprod støtter ikke utenlandske myndigheter så vi lager en falsk person
                    // med mottakerId="11111111111" og dermed blir AvsendMottakId i Joark tom.
                    return lagPerson(FALSK_MOTTAKER_ID, metadata.utenlandskMyndighet.navn);
                } else {
                    Organisasjon myndighet = objectFactory.createOrganisasjon();
                    myndighet.setOrgnummer(mottakerID);
                    return myndighet;
                }
            default:
                log.warn("MottakersRolle {} er ukjent. PERSON brukes som standard.", mottakerRolle);
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

    private static String xmlToString(Node node) {
        Document document = node.getOwnerDocument();
        DOMImplementationLS domImplLS = (DOMImplementationLS) document
            .getImplementation();
        LSSerializer serializer = domImplLS.createLSSerializer();
        return serializer.writeToString(node);
    }
}
