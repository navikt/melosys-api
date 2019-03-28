package no.nav.melosys.service.dokument.brev;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

/**
 * BrevDataService er ansvarlig for å mappe saksopplysninger i brevmalene.
 */
@Service
public class BrevDataService {

    private static final Set<Produserbaredokumenter> DOKUMENTER_TIL_BRUKER = Collections.unmodifiableSet(EnumSet.of(MELDING_FORVENTET_SAKSBEHANDLINGSTID,
        AVSLAG_YRKESAKTIV, ORIENTERING_ANMODNING_UNNTAK, MELDING_MANGLENDE_OPPLYSNINGER, MELDING_HENLAGT_SAK, INNVILGELSE_YRKESAKTIV));

    private final TpsFasade tpsFasade;

    static final String MELOSYS_ENHET_ID = "4530";

    private static final String FALSK_MOTTAKER_ID = "11111111111";
    static final String PLASSHOLDER_TEKST = "-";
    static final String PLASSHOLDER_POSTNUMMER = "0000";

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    private final KontaktopplysningService kontaktopplysningService;

    @Autowired
    public BrevDataService(@Qualifier("system") TpsFasade tpsFasade,
                           BehandlingsresultatRepository behandlingsresultatRepository,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                           KontaktopplysningService kontaktopplysningService) {
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.kontaktopplysningService = kontaktopplysningService;
    }

    /**
     * Genererer metada til doksys angående dokumentbestillingen.
     */
    public DokumentbestillingMetadata lagBestillingMetadata(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Assert.notNull(produserbartDokument, "Ingen gyldig produserbartDokument");
        Fagsak fagsak = behandling.getFagsak();

        Aktoersroller mottakersRolle = brevData.mottaker != null ? brevData.mottaker : avklarMottakerRolleFraProduserbartDokument(produserbartDokument);
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = DokumenttypeIdMapper.hentID(produserbartDokument);
        metadata.mottakersRolle = mottakersRolle;
        metadata.mottakerID = avklarMottakerId(fagsak, mottakersRolle);
        metadata.brukerID = tpsFasade.hentFagsakIdentMedRolleType(fagsak, BRUKER);

        metadata.journalsakID = Long.toString(fagsak.getGsakSaksnummer());
        // Fagområde=MED for alle dokumenter til bruker/arbeidsgiver, men kan være UFM for papir-SED til ikke-elektroniske land
        metadata.fagområde = Tema.MED.getKode();
        metadata.saksbehandler = brevData.saksbehandler;
        metadata.utenlandskMyndighet = (mottakersRolle == Aktoersroller.MYNDIGHET) ? hentMyndighetFraSak(fagsak) : null;
        metadata.utledRegisterInfo = dokprodUtlederRegisterInfo(metadata);

        if (!metadata.utledRegisterInfo) {
            Saksopplysning tpsOpplysning = tpsFasade.hentPerson(metadata.brukerID);
            PersonDokument tpsDokument = (PersonDokument) tpsOpplysning.getDokument();
            metadata.brukerNavn = tpsDokument.sammensattNavn;
        }
        return metadata;
    }

    UtenlandskMyndighet hentMyndighetFraSak(Fagsak fagsak) throws TekniskException {
        return utenlandskMyndighetRepository.findByLandkode(fagsak.hentMyndighetLandkode());
    }

    // Dokprod kan utlede registerinfo når Melosys ikke trenger å sette adressen sammen.
    // Melosys setter adressen sammen for kontaktpersoner og utelandske myndigheter.
    private boolean dokprodUtlederRegisterInfo(DokumentbestillingMetadata metadata) {
        return Aktoersroller.MYNDIGHET != metadata.mottakersRolle;
    }

    /**
     * Genererer XML i hensyn til mal og validere mot xsd.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Element lagBrevXML(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws TekniskException {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findById(behandling.getId())
            .orElseThrow(() -> new TekniskException("Finner ingen behandlingsresultat for behandlingid"));

        Element brevXmlElement;
        try {
            FellesType fellesType = mapFellesType(behandling);
            MelosysNAVFelles navFelles = mapNAVFelles(produserbartDokument, behandling, brevData);
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
        }
        return brevXmlElement;
    }

    private FellesType mapFellesType(Behandling behandling) {
        final FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer(behandling.getFagsak().getSaksnummer());

        return fellesType;
    }

    private MelosysNAVFelles mapNAVFelles(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws TekniskException {
        final MelosysNAVFelles navFelles = new MelosysNAVFelles();

        navFelles.setSakspart(lagSakspart(behandling));
        navFelles.setMottaker(lagMottaker(produserbartDokument, behandling, brevData));
        navFelles.setBehandlendeEnhet(lagNavEnhet());
        navFelles.setSignerendeSaksbehandler(lagSaksbehandler(brevData.saksbehandler));
        navFelles.setSignerendeBeslutter(lagSaksbehandler(brevData.saksbehandler));
        navFelles.setKontaktinformasjon(BrevDataUtils.lagKontaktInformasjon());

        return navFelles;
    }

    private Sakspart lagSakspart(Behandling behandling) throws TekniskException {
        Sakspart sakspart = new Sakspart();
        Aktoer aktør = behandling.getFagsak().hentAktørMedRolleType(BRUKER);

        if (aktør == null || aktør.getAktørId() == null) {
            throw new TekniskException("Det finnes ingen bruker på sak " + behandling.getFagsak().getSaksnummer());
        }
        try {
            sakspart.setId(tpsFasade.hentIdentForAktørId(aktør.getAktørId()));
        } catch (IkkeFunnetException e) {
            throw new TekniskException("Det finnes ingen ident for aktørID " + aktør.getAktørId());
        }

        sakspart.setTypeKode(AktoerType.PERSON);
        sakspart.setNavn(PLASSHOLDER_TEKST);
        return sakspart;
    }

    Mottaker lagMottaker(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws TekniskException {
        Aktoersroller mottakersRolle = brevData.mottaker != null ? brevData.mottaker : avklarMottakerRolleFraProduserbartDokument(produserbartDokument);
        Fagsak fagsak = behandling.getFagsak();
        Aktoer aktør = fagsak.hentAktørMedRolleType(mottakersRolle);
        TekniskException ingenAktør = new TekniskException("Det finnes ingen mottaker på sak " + behandling.getFagsak().getSaksnummer());
        if (aktør == null) {
            throw ingenAktør;
        }

        String mottakerID = avklarMottakerId(fagsak, mottakersRolle);
        Mottaker mottaker;

        if (mottakersRolle == BRUKER) {
            mottaker = new Person();
            mottaker.setTypeKode(AktoerType.PERSON);
            mottaker.setId(mottakerID);
        } else if (mottakersRolle == ARBEIDSGIVER || mottakersRolle == REPRESENTANT) {
            mottaker = new Organisasjon();
            mottaker.setTypeKode(AktoerType.ORGANISASJON);
            mottaker.setId(mottakerID);
        } else if (mottakersRolle == MYNDIGHET) {
            mottaker = new Person();
            mottaker.setBerik(false);
            mottaker.setTypeKode(AktoerType.PERSON);
            mottaker.setId(FALSK_MOTTAKER_ID);

            UtenlandskMyndighet utenlandskMyndighet = hentMyndighetFraSak(behandling.getFagsak());
            mottaker.setNavn(utenlandskMyndighet.navn);
            mottaker.setKortNavn(aktør.getInstitusjonId());
            mottaker.setSpraakkode(Spraakkode.NB);
            mottaker.setMottakeradresse(lagUtendlanskAdresse(utenlandskMyndighet));
            return mottaker;
        } else {
            throw ingenAktør;
        }

        Optional<String> mottakerNavn = hentKontaktNavnForOrgnr(fagsak, mottakerID);
        mottaker.setNavn(mottakerNavn.orElse(PLASSHOLDER_TEKST));
        mottaker.setKortNavn(PLASSHOLDER_TEKST);
        mottaker.setSpraakkode(Spraakkode.NB);
        mottaker.setMottakeradresse(lagNorskPostadresse());

        return mottaker;
    }

    private Saksbehandler lagSaksbehandler(String userId) {
        Saksbehandler saksbehandler = new Saksbehandler();
        saksbehandler.setNavEnhet(lagNavEnhet());
        saksbehandler.setNavAnsatt(lagNavAnsatt(userId));
        return saksbehandler;
    }

    private Aktoersroller avklarMottakerRolleFraProduserbartDokument(Produserbaredokumenter produserbartDokument) throws TekniskException {
        Aktoersroller mottakerRolle;
        if (DOKUMENTER_TIL_BRUKER.contains(produserbartDokument)) {
            mottakerRolle = BRUKER;
        } else if (produserbartDokument == INNVILGELSE_ARBEIDSGIVER || produserbartDokument == AVSLAG_ARBEIDSGIVER) {
            mottakerRolle = ARBEIDSGIVER;
        } else if (produserbartDokument == ANMODNING_UNNTAK || produserbartDokument == ATTEST_A1) {
            mottakerRolle = MYNDIGHET;
        } else {
            throw new TekniskException("Produserbartdokument ikke støttet for å velge mottakerRolle");
        }
        return mottakerRolle;
    }

    private String avklarMottakerId(Fagsak fagsak, Aktoersroller mottakerRolle) throws TekniskException {

        Aktoer mottaker = fagsak.hentAktørMedRolleType(mottakerRolle);
        Aktoer representant = fagsak.hentAktørMedRolleType(REPRESENTANT); // FIXME MEL-2182 Det kan være flere representanter på en sak

        if (representant != null) {
            return kontaktopplysningService.hentKontaktopplysning(fagsak.getSaksnummer(), representant.getOrgnr())
                .map(Kontaktopplysning::getKontaktOrgnr).orElse(representant.getOrgnr());
        } else if (mottakerRolle == ARBEIDSGIVER) {
            return kontaktopplysningService.hentKontaktopplysning(fagsak.getSaksnummer(), mottaker.getOrgnr())
                .map(Kontaktopplysning::getKontaktOrgnr).orElse(mottaker.getOrgnr());
        } else if (mottakerRolle == MYNDIGHET) {
            return mottaker.getInstitusjonId();
        } else if (mottakerRolle == BRUKER) {
            try {
                return tpsFasade.hentIdentForAktørId(mottaker.getAktørId());
            } catch (IkkeFunnetException e) {
                throw new TekniskException(e);
            }
        }

        throw new TekniskException("Det finnes ingen mottaker på sak " + fagsak.getSaksnummer());
    }

    private Optional<String> hentKontaktNavnForOrgnr(Fagsak fagsak, String orgNr) {
        return kontaktopplysningService.hentKontaktopplysning(fagsak.getSaksnummer(), orgNr)
            .map(Kontaktopplysning::getKontaktNavn);
    }
}