package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.DistribuerJournalpostConsumer;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.Adresse;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
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

import static no.nav.melosys.domain.Fagsystem.GSAK_I_JOARK;
import static no.nav.melosys.domain.Fagsystem.MELOSYS;
import static no.nav.melosys.domain.adresse.Adresse.sammenslå;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static org.springframework.util.StringUtils.hasText;

@Service
@Primary
public class DoksysService implements DoksysFasade {
    private static final Logger log = LoggerFactory.getLogger(DoksysService.class);

    private static final String FALSK_MOTTAKER_ID = "11111111111";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String SYS_AVSENDER = "Melosys";

    private final DokumentproduksjonConsumer dokumentproduksjonConsumer;
    private final DistribuerJournalpostConsumer distribuerJournalpostConsumer;

    private final ObjectFactory objectFactory;

    @Autowired
    public DoksysService(DokumentproduksjonConsumer dokumentproduksjonConsumer, DistribuerJournalpostConsumer distribuerJournalpostConsumer) {
        this.dokumentproduksjonConsumer = dokumentproduksjonConsumer;
        this.distribuerJournalpostConsumer = distribuerJournalpostConsumer;
        this.objectFactory = new ObjectFactory();
    }

    @Override
    public byte[] produserDokumentutkast(Dokumentbestilling dokumentbestilling) {
        ProduserDokumentutkastRequest wsRequest = new ProduserDokumentutkastRequest();
        DokumentbestillingMetadata metadata = dokumentbestilling.getMetadata();

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
            throw new IntegrasjonException(e);
        }
    }

    @Override
    public DokumentbestillingResponse produserIkkeredigerbartDokument(Dokumentbestilling dokumentbestilling) {
        ProduserIkkeredigerbartDokumentRequest wsRequest = new ProduserIkkeredigerbartDokumentRequest();
        Dokumentbestillingsinformasjon info = new Dokumentbestillingsinformasjon();

        DokumentbestillingMetadata metadata = dokumentbestilling.getMetadata();
        info.setDokumenttypeId(metadata.dokumenttypeID);

        // UtledRegisterInfo er utdatert.
        // Deaktivering av registerutledning gjøres ved "setBerik" på mottaker

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
        bruker.setNavn(metadata.brukerNavn);
        bruker.setIdent(metadata.brukerID);
        info.setBruker(bruker);

        info.setMottaker(lagMottaker(metadata));

        info.setJournalsakId(metadata.journalsakID);

        Fagomraader fagområde = objectFactory.createFagomraader();
        fagområde.setKodeRef(metadata.fagområde);
        fagområde.setValue(metadata.fagområde);
        info.setDokumenttilhoerendeFagomraade(fagområde);

        info.setJournalfoerendeEnhet(Integer.toString(MELOSYS_ENHET_ID));
        info.setSaksbehandlernavn(metadata.saksbehandler !=null ? metadata.saksbehandler : SYS_AVSENDER);

        if (metadata.mottaker.erUtenlandskMyndighet()) {
            info.setAdresse(lagUtenlandskAdresse(metadata.utenlandskMyndighet.getAdresse()));
        } else if (metadata.postadresse != null) {
            info.setAdresse(lagUtenlandskAdresse(metadata.postadresse));
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
            throw new SikkerhetsbegrensningException(e);
        } catch (ProduserIkkeredigerbartDokumentDokumentErRedigerbart | ProduserIkkeRedigerbartDokumentJoarkForretningsmessigUnntak
            | ProduserIkkeredigerbartDokumentBrevdataValideringFeilet | ProduserIkkeredigerbartDokumentDokumentErVedlegg
            | ProduserIkkeRedigerbartDokumentInputValideringFeilet e) {
            throw new IntegrasjonException(e);
        }
    }

    @Override
    public String distribuerJournalpost(String journalpostId, StrukturertAdresse mottakeradresse) {
        DistribuerJournalpostRequest request = DistribuerJournalpostRequest.builder()
            .journalpostId(journalpostId)
            .bestillendeFagsystem(MELOSYS.getKode())
            .dokumentProdApp(MELOSYS.getKode())
            .adresse("NO".equalsIgnoreCase(mottakeradresse.landkode)
                ? norskAdresse(mottakeradresse)
                : utenlandskAdresse(mottakeradresse))
            .build();

        return distribuerJournalpostConsumer.distribuerJournalpost(request).getBestillingsId();
    }

    @Override
    public String distribuerJournalpost(String journalpostId, StrukturertAdresse mottakeradresse, Kontaktopplysning kontaktopplysning, String kontaktpersonNavn) {
        DistribuerJournalpostRequest request = DistribuerJournalpostRequest.builder()
            .journalpostId(journalpostId)
            .bestillendeFagsystem(MELOSYS.getKode())
            .dokumentProdApp(MELOSYS.getKode())
            .adresse(mapAdresse(mottakeradresse, kontaktopplysning, kontaktpersonNavn))
            .build();

        return distribuerJournalpostConsumer.distribuerJournalpost(request).getBestillingsId();
    }

    @Override
    public String distribuerJournalpost(String journalpostId) {
        DistribuerJournalpostRequest request = DistribuerJournalpostRequest.builder()
            .journalpostId(journalpostId)
            .bestillendeFagsystem(MELOSYS.getKode())
            .dokumentProdApp(MELOSYS.getKode())
            .build();

        return distribuerJournalpostConsumer.distribuerJournalpost(request).getBestillingsId();
    }

    private static Adresse norskAdresse(StrukturertAdresse strukturertAdresse) {
        return Adresse.builder()
            .adressetype("norskPostadresse")
            .adresselinje1(sammenslå(strukturertAdresse.gatenavn, strukturertAdresse.husnummer))
            .adresselinje2(strukturertAdresse.region)
            .postnummer(strukturertAdresse.postnummer)
            .poststed(strukturertAdresse.poststed)
            .land(strukturertAdresse.landkode)
            .build();
    }

    private static Adresse utenlandskAdresse(StrukturertAdresse strukturertAdresse) {
        return Adresse.builder()
            .adressetype("utenlandskPostadresse")
            .adresselinje1(sammenslå(strukturertAdresse.gatenavn, strukturertAdresse.husnummer))
            .adresselinje2(sammenslå(strukturertAdresse.postnummer, strukturertAdresse.poststed))
            .adresselinje3(strukturertAdresse.region)
            .land(strukturertAdresse.landkode)
            .build();
    }

    private Adresse mapAdresse(StrukturertAdresse strukturertAdresse, Kontaktopplysning kontaktopplysning, String kontaktpersonNavn) {
        Adresse.AdresseBuilder adresseBuilder = Adresse.builder()
            .land(strukturertAdresse.landkode);

        if(hasText(kontaktpersonNavn)) {
            adresseBuilder
                .adresselinje1("Att: " + kontaktpersonNavn)
                .adresselinje2(strukturertAdresse.gatenavn + ((strukturertAdresse.husnummer == null) ? "" : " " + strukturertAdresse.husnummer));
        } else if (kontaktopplysning != null) {
            adresseBuilder
                .adresselinje1("Att: " + kontaktopplysning.getKontaktNavn())
                .adresselinje2(strukturertAdresse.gatenavn + ((strukturertAdresse.husnummer == null) ? "" : " " + strukturertAdresse.husnummer));
        } else {
            adresseBuilder
                .adresselinje1(strukturertAdresse.gatenavn + ((strukturertAdresse.husnummer == null) ? "" : " " + strukturertAdresse.husnummer));
        }

        if (strukturertAdresse.erNorsk()) {
            adresseBuilder.adressetype("norskPostadresse")
            .postnummer(strukturertAdresse.postnummer)
            .poststed(strukturertAdresse.poststed);
        } else {
            adresseBuilder.adressetype("utenlandskPostadresse");
        }

        return adresseBuilder.build();
    }

    private UtenlandskPostadresse lagUtenlandskAdresse(StrukturertAdresse postadresse) {
        UtenlandskPostadresse utenlandskPostadresse = new UtenlandskPostadresse();
        utenlandskPostadresse.setAdresselinje1(sammenslå(postadresse.gatenavn, postadresse.husnummer));
        utenlandskPostadresse.setAdresselinje3(postadresse.region);
        utenlandskPostadresse.setAdresselinje2(sammenslå(postadresse.postnummer, postadresse.poststed));
        utenlandskPostadresse.setLand(new Landkoder().withValue(postadresse.landkode));
        return utenlandskPostadresse;
    }

    private Aktoer lagMottaker(DokumentbestillingMetadata metadata) {
        if (metadata.mottaker == null) {
            throw new FunksjonellException("Brev kan ikke sendes, mottaker er ikke satt.");
        }

        Aktoersroller mottakerRolle = metadata.mottaker.getRolle();
        String mottakerID = metadata.mottakerID;
        String brukerNavn = metadata.brukerNavn;
        boolean berik = metadata.berik;

        switch (mottakerRolle) {
            case BRUKER:
                return lagPerson(mottakerID, brukerNavn, berik);
            case ARBEIDSGIVER:
            case REPRESENTANT:
                Organisasjon organisasjon = objectFactory.createOrganisasjon();
                organisasjon.setOrgnummer(mottakerID);
                return organisasjon;
            case MYNDIGHET:
                if (metadata.mottaker.erUtenlandskMyndighet()) {
                    // Dokprod støtter ikke utenlandske myndigheter så vi lager en falsk person
                    // med mottakerId="11111111111" og dermed blir AvsendMottakId i Joark tom.
                    return lagPerson(FALSK_MOTTAKER_ID, metadata.utenlandskMyndighet.navn, false);
                } else {
                    Organisasjon myndighet = objectFactory.createOrganisasjon();
                    myndighet.setOrgnummer(mottakerID);
                    return myndighet;
                }
            default:
                log.warn("MottakersRolle {} er ukjent. PERSON brukes som standard.", mottakerRolle);
                return lagPerson(mottakerID, brukerNavn, berik);
        }
    }

    private Aktoer lagPerson(String personID, String navn, boolean berik) {
        Person person = objectFactory.createPerson();
        person.setIdent(personID);
        if (!berik) {
            person.setNavn(navn);
        }
        person.setBerik(berik);
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
