package no.nav.melosys.service.dokument.brev;

import no.nav.dok.brevdata.felles.v1.navfelles.Saksbehandler;
import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

@Service
public class BrevDataService {
    static final String MELOSYS_ENHET_ID = "4530";

    static final String PLASSHOLDER_TEKST = "-";
    static final String PLASSHOLDER_POSTNUMMER = "0000";

    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final PersondataFasade persondataFasade;
    private final SaksbehandlerService saksbehandlerService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    public BrevDataService(BehandlingsresultatRepository behandlingsresultatRepository,
                           PersondataFasade persondataFasade,
                           SaksbehandlerService saksbehandlerService,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.persondataFasade = persondataFasade;
        this.saksbehandlerService = saksbehandlerService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
    }

    public DokumentbestillingMetadata lagBestillingMetadata(Produserbaredokumenter produserbartDokument, no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling, BrevData brevData) {
        Fagsak fagsak = behandling.getFagsak();

        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = DokumenttypeIdMapper.hentID(produserbartDokument);
        metadata.mottaker = mottaker;
        metadata.mottakerID = avklarMottakerId(mottaker, kontaktopplysning);
        metadata.brukerID = persondataFasade.hentFolkeregisterident(fagsak.hentBrukersAktørID());

        metadata.journalsakID = Long.toString(fagsak.getGsakSaksnummer());
        metadata.fagområde = Tema.MED.getKode();
        metadata.saksbehandler = brevData.saksbehandler;
        metadata.berik = true;

        if (mottaker.getRolle() == Mottakerroller.BRUKER) {
            if (personManglerAdresseFraRegister(behandling.getFagsak().hentBrukersAktørID())) {
                MottatteOpplysningerData grunnlagData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
                StrukturertAdresse oppgittAdresse = grunnlagData.bosted.oppgittAdresse;
                if (oppgittAdresse.erGyldig()) {
                    metadata.berik = false;
                    metadata.postadresse = oppgittAdresse;
                    metadata.brukerNavn = persondataFasade.hentSammensattNavn(metadata.brukerID);
                }
            }
        } else if (mottaker.erUtenlandskMyndighet()) {
            metadata.berik = false;
            metadata.utenlandskMyndighet = hentUtenlandskTrygdemyndighetFraMottaker(mottaker);
            metadata.brukerNavn = persondataFasade.hentSammensattNavn(metadata.brukerID);
        }
        return metadata;
    }

    private String avklarMottakerId(no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning) {
        return switch (mottaker.getRolle()) {
            case ARBEIDSGIVER -> avklarMottakerIDForOrg(mottaker, kontaktopplysning);
            case FULLMEKTIG -> mottaker.erOrganisasjon() ? avklarMottakerIDForOrg(mottaker, kontaktopplysning) : mottaker.getPersonIdent();
            case BRUKER -> persondataFasade.hentFolkeregisterident(mottaker.getAktørId());
            case UTENLANDSK_TRYGDEMYNDIGHET ->
                mottaker.erUtenlandskMyndighet() ? mottaker.getInstitusjonID() : mottaker.getOrgnr();
            case NORSK_MYNDIGHET -> mottaker.getOrgnr();
            default -> throw new TekniskException(mottaker.getRolle() + " støttes ikke.");
        };
    }

    private String avklarMottakerIDForOrg(no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning) {
        return (kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null) ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
    }

    UtenlandskMyndighet hentUtenlandskTrygdemyndighetFraMottaker(no.nav.melosys.domain.brev.Mottaker mottaker) {
        Land_iso2 landkode = mottaker.hentMyndighetLandkode();
        return utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
    }

    /**
     * Genererer XML i hensyn til mal og validere mot xsd.
     */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Element lagBrevXML(Produserbaredokumenter produserbartDokument, no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling, BrevData brevData) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Finner ingen behandlingsresultat for behandlingid " + behandling.getId()));

        Element brevXmlElement;
        try {
            FellesType fellesType = mapFellesType(mottaker, kontaktopplysning, behandling);
            MelosysNAVFelles navFelles = mapNAVFelles(mottaker, kontaktopplysning, behandling, brevData);
            String brevXml = BrevDataMapperRuter.brevDataMapper(produserbartDokument).mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(brevXml));
            Document doc = db.parse(is);
            brevXmlElement = doc.getDocumentElement();
        } catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
            throw new TekniskException("XML genereringsfeil " + behandling.getFagsak().getSaksnummer(), e);
        } catch (Exception e) {
            throw new TekniskException("Feil ved bygging av data til XML-generering " + behandling.getFagsak().getSaksnummer(), e);
        }
        return brevXmlElement;
    }

    private FellesType mapFellesType(no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling) {
        final FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer(behandling.getFagsak().getSaksnummer());
        if (mottaker.getRolle() == Mottakerroller.FULLMEKTIG) {
            fellesType.setFullmektig(mottaker.erOrganisasjon() ? mottaker.getOrgnr() : mottaker.getPersonIdent());
        }
        if (kontaktopplysning != null) {
            fellesType.setKontaktperson(kontaktopplysning.getKontaktNavn());
        }

        return fellesType;
    }

    private MelosysNAVFelles mapNAVFelles(no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling, BrevData brevData) {
        final MelosysNAVFelles navFelles = new MelosysNAVFelles();

        navFelles.setBehandlendeEnhet(lagNavEnhet());
        navFelles.setKontaktinformasjon(BrevDataUtils.lagKontaktInformasjon());
        navFelles.setMottaker(lagMottaker(mottaker, kontaktopplysning));
        navFelles.setSakspart(lagSakspart(behandling));

        Saksbehandler saksbehandler = lagSaksbehandler(brevData.saksbehandler);
        navFelles.setSignerendeBeslutter(saksbehandler);
        navFelles.setSignerendeSaksbehandler(saksbehandler);
        return navFelles;
    }

    Mottaker lagMottaker(no.nav.melosys.domain.brev.Mottaker mottaker, Kontaktopplysning kontaktopplysning) {
        String mottakerID = avklarMottakerId(mottaker, kontaktopplysning);

        return switch (mottaker.getRolle()) {
            case BRUKER -> lagMottakerForBruker(mottakerID);
            case ARBEIDSGIVER, NORSK_MYNDIGHET -> lagMottakerForOrganisasjon(mottakerID);
            case UTENLANDSK_TRYGDEMYNDIGHET ->
                mottaker.erUtenlandskMyndighet() ? lagMottakerForUtenlandskTrygdemyndighet(mottaker) : lagMottakerForOrganisasjon(mottakerID);
            case FULLMEKTIG ->
                mottaker.erOrganisasjon() ? lagMottakerForOrganisasjon(mottakerID) : lagMottakerForRepresentantPerson(mottakerID);
            default -> throw new TekniskException(mottaker.getRolle() + " støttes ikke.");
        };
    }

    private Mottaker lagMottakerForBruker(String mottakerID) {
        Mottaker mottaker = new Person();
        mottaker.setTypeKode(AktoerType.PERSON);
        mottaker.setSpraakkode(Spraakkode.NB);
        mottaker.setId(mottakerID);

        mottaker.setMottakeradresse(lagNorskPostadresse());
        mottaker.setBerik(true);
        mottaker.setNavn(PLASSHOLDER_TEKST);
        mottaker.setKortNavn(PLASSHOLDER_TEKST);
        return mottaker;
    }

    private Mottaker lagMottakerForOrganisasjon(String mottakerID) {
        Mottaker mottaker = new Organisasjon();
        mottaker.setId(mottakerID);
        mottaker.setTypeKode(AktoerType.ORGANISASJON);

        mottaker.setNavn(PLASSHOLDER_TEKST);
        mottaker.setKortNavn(PLASSHOLDER_TEKST);
        mottaker.setMottakeradresse(lagNorskPostadresse());
        mottaker.setSpraakkode(Spraakkode.NB);
        return mottaker;
    }

    private Mottaker lagMottakerForRepresentantPerson(String mottakerID) {
        Mottaker mottaker = new Person();
        mottaker.setTypeKode(AktoerType.PERSON);
        mottaker.setSpraakkode(Spraakkode.NB);
        mottaker.setId(mottakerID);

        mottaker.setMottakeradresse(lagNorskPostadresse());
        mottaker.setBerik(true);
        mottaker.setNavn(PLASSHOLDER_TEKST);
        mottaker.setKortNavn(PLASSHOLDER_TEKST);
        return mottaker;
    }

    private Mottaker lagMottakerForUtenlandskTrygdemyndighet(no.nav.melosys.domain.brev.Mottaker mottakerUtenlandskTrygdemyndighet) {
        Mottaker mottaker = new Person();
        mottaker.setBerik(false);
        mottaker.setTypeKode(AktoerType.PERSON);
        mottaker.setId(mottakerUtenlandskTrygdemyndighet.getInstitusjonID());

        UtenlandskMyndighet utenlandskMyndighet = hentUtenlandskTrygdemyndighetFraMottaker(mottakerUtenlandskTrygdemyndighet);
        mottaker.setNavn(utenlandskMyndighet.navn);
        mottaker.setKortNavn(utenlandskMyndighet.navn);
        mottaker.setSpraakkode(Spraakkode.NB);
        mottaker.setMottakeradresse(lagUtendlanskAdresse(utenlandskMyndighet));
        return mottaker;
    }

    private Saksbehandler lagSaksbehandler(String ident) {
        Saksbehandler saksbehandler = new Saksbehandler();
        saksbehandler.setNavEnhet(lagNavEnhet());
        var saksbehandlerNavn = ident != null ? saksbehandlerService.hentNavnForIdent(ident) : "N/A";
        saksbehandler.setNavAnsatt(lagNavAnsatt(ident, saksbehandlerNavn));
        return saksbehandler;
    }

    private Sakspart lagSakspart(Behandling behandling) {
        Sakspart sakspart = new Sakspart();
        Aktoer aktør = behandling.getFagsak().hentBruker();

        if (aktør == null || aktør.getAktørId() == null) {
            throw new TekniskException("Det finnes ingen bruker på sak " + behandling.getFagsak().getSaksnummer());
        }
        try {
            sakspart.setId(persondataFasade.hentFolkeregisterident(aktør.getAktørId()));
        } catch (IkkeFunnetException e) {
            throw new TekniskException("Det finnes ingen ident for aktørID " + aktør.getAktørId());
        }

        sakspart.setTypeKode(AktoerType.PERSON);
        sakspart.setNavn(PLASSHOLDER_TEKST);
        return sakspart;
    }

    private boolean personManglerAdresseFraRegister(String id) {
        return persondataFasade.hentPerson(id).manglerGyldigRegistrertAdresse();
    }
}
