package no.nav.melosys.service.dokument.brev;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.dok.brevdata.felles.v1.navfelles.Organisasjon;
import no.nav.dok.brevdata.felles.v1.navfelles.Saksbehandler;
import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

/**
 * BrevDataService er ansvarlig for å mappe saksopplysninger i brevmalene.
 */
@Service
public class BrevDataService {

    static final String MELOSYS_ENHET_ID = "4530";

    static final String PLASSHOLDER_TEKST = "-";
    static final String PLASSHOLDER_POSTNUMMER = "0000";

    private final TpsFasade tpsFasade;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    @Autowired
    public BrevDataService(@Qualifier("system") TpsFasade tpsFasade,
                           BehandlingsresultatRepository behandlingsresultatRepository,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
    }

    /**
     * Genererer metada til doksys angående dokumentbestillingen.
     */
    public DokumentbestillingMetadata lagBestillingMetadata(Produserbaredokumenter produserbartDokument,
                                                            Aktoer mottaker, Kontaktopplysning kontaktopplysning,
                                                            Behandling behandling, BrevData brevData)
        throws TekniskException, FunksjonellException {
        Fagsak fagsak = behandling.getFagsak();

        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = DokumenttypeIdMapper.hentID(produserbartDokument);
        metadata.mottaker = mottaker;
        metadata.mottakerID = avklarMottakerId(mottaker, kontaktopplysning);
        metadata.brukerID = tpsFasade.hentIdentForAktørId(fagsak.hentBruker().getAktørId());

        metadata.journalsakID = Long.toString(fagsak.getGsakSaksnummer());
        // Fagområde=MED for alle dokumenter til bruker/arbeidsgiver, men kan være UFM for papir-SED til ikke-elektroniske land
        metadata.fagområde = Tema.MED.getKode();
        metadata.saksbehandler = brevData.saksbehandler;
        metadata.berik = true;

        if (mottaker.getRolle() == BRUKER) {
            if (brukerHarIkkeAdresseiTps(behandling)) {
                BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
                StrukturertAdresse oppgittAdresse = grunnlagData.bosted.oppgittAdresse;
                if (!oppgittAdresse.erTom()) {
                    metadata.berik = false;
                    metadata.postadresse = oppgittAdresse;
                    metadata.brukerNavn = tpsFasade.hentSammensattNavn(metadata.brukerID);
                }
            }
        } else if (mottaker.erUtenlandskMyndighet()) {
            metadata.berik = false;
            metadata.utenlandskMyndighet = hentMyndighetFraAktoer(mottaker);
            metadata.brukerNavn = tpsFasade.hentSammensattNavn(metadata.brukerID);
        }
        return metadata;
    }

    private String avklarMottakerId(Aktoer mottaker, Kontaktopplysning kontaktopplysning) throws TekniskException {
        Aktoersroller mottakerRolle = mottaker.getRolle();

        if (mottakerRolle == ARBEIDSGIVER || mottakerRolle == REPRESENTANT) {
            return (kontaktopplysning != null && kontaktopplysning.getKontaktOrgnr() != null) ? kontaktopplysning.getKontaktOrgnr() : mottaker.getOrgnr();
        } else if (mottakerRolle == BRUKER) {
            try {
                return tpsFasade.hentIdentForAktørId(mottaker.getAktørId());
            } catch (IkkeFunnetException e) {
                throw new TekniskException(e);
            }
        } else if (mottakerRolle == MYNDIGHET) {
            if (mottaker.erUtenlandskMyndighet()) {
                return mottaker.getInstitusjonId();
            } else {
                return mottaker.getOrgnr();
            }
        }

        throw new TekniskException(mottakerRolle + " støttes ikke.");
    }

    UtenlandskMyndighet hentMyndighetFraAktoer(Aktoer aktoer) throws TekniskException {
        Landkoder landkode = aktoer.hentMyndighetLandkode();
        return utenlandskMyndighetRepository.findByLandkode(landkode)
            .orElseThrow(() -> new TekniskException("Finner ikke utenlandskMyndighet for " + landkode.getKode() + "."));
    }

    /**
     * Genererer XML i hensyn til mal og validere mot xsd.
     */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Element lagBrevXML(Produserbaredokumenter produserbartDokument, Aktoer mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling, BrevData brevData) throws TekniskException, FunksjonellException {
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
        }
        return brevXmlElement;
    }

    private FellesType mapFellesType(Aktoer mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling) {
        final FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer(behandling.getFagsak().getSaksnummer());
        if (mottaker.getRolle() == REPRESENTANT) {
            fellesType.setFullmektig(mottaker.getOrgnr());
        }
        if (kontaktopplysning != null) {
            fellesType.setKontaktperson(kontaktopplysning.getKontaktNavn());
        }

        return fellesType;
    }

    private MelosysNAVFelles mapNAVFelles(Aktoer mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling, BrevData brevData)
        throws TekniskException, FunksjonellException {
        final MelosysNAVFelles navFelles = new MelosysNAVFelles();

        navFelles.setBehandlendeEnhet(lagNavEnhet());
        navFelles.setKontaktinformasjon(BrevDataUtils.lagKontaktInformasjon());
        navFelles.setMottaker(lagMottaker(mottaker, kontaktopplysning, behandling));
        navFelles.setSakspart(lagSakspart(behandling));
        navFelles.setSignerendeBeslutter(lagSaksbehandler(brevData.saksbehandler));
        navFelles.setSignerendeSaksbehandler(lagSaksbehandler(brevData.saksbehandler));

        return navFelles;
    }

    Mottaker lagMottaker(Aktoer mottaker, Kontaktopplysning kontaktopplysning, Behandling behandling) throws TekniskException, FunksjonellException {
        Aktoersroller mottakerRolle = mottaker.getRolle();
        String mottakerID = avklarMottakerId(mottaker, kontaktopplysning);

        Mottaker mottakerBrev;
        if (mottakerRolle == BRUKER) {
            return lagMottakerForBruker(behandling, mottakerID);
        } else if (mottakerRolle == ARBEIDSGIVER || mottakerRolle == REPRESENTANT) {
            mottakerBrev = new Organisasjon();
            mottakerBrev.setTypeKode(AktoerType.ORGANISASJON);
            mottakerBrev.setId(mottakerID);
        } else if (mottakerRolle == MYNDIGHET) {
            if (mottaker.erUtenlandskMyndighet()) {
                mottakerBrev = new Person();
                mottakerBrev.setBerik(false);
                mottakerBrev.setTypeKode(AktoerType.PERSON);
                mottakerBrev.setId(mottaker.getInstitusjonId());

                UtenlandskMyndighet utenlandskMyndighet = hentMyndighetFraAktoer(mottaker);
                mottakerBrev.setNavn(utenlandskMyndighet.navn);
                mottakerBrev.setKortNavn(utenlandskMyndighet.navn);
                mottakerBrev.setSpraakkode(Spraakkode.NB);
                mottakerBrev.setMottakeradresse(lagUtendlanskAdresse(utenlandskMyndighet));
                return mottakerBrev;
            } else {
                mottakerBrev = new Organisasjon();
                mottakerBrev.setTypeKode(AktoerType.ORGANISASJON);
                mottakerBrev.setId(mottakerID);
            }
        } else {
            throw new TekniskException(mottakerRolle + " støttes ikke.");
        }

        mottakerBrev.setNavn(PLASSHOLDER_TEKST);
        mottakerBrev.setKortNavn(PLASSHOLDER_TEKST);
        mottakerBrev.setMottakeradresse(lagNorskPostadresse());
        mottakerBrev.setSpraakkode(Spraakkode.NB);

        return mottakerBrev;
    }

    private Mottaker lagMottakerForBruker(Behandling behandling, String mottakerID) throws TekniskException, FunksjonellException {
        Mottaker mottakerBrev;
        mottakerBrev = new Person();
        mottakerBrev.setTypeKode(AktoerType.PERSON);
        mottakerBrev.setSpraakkode(Spraakkode.NB);
        mottakerBrev.setId(mottakerID);

        String navn = tpsFasade.hentSammensattNavn(mottakerID);
        if (brukerHarIkkeAdresseiTps(behandling)) {
            BehandlingsgrunnlagData grunnlagData = behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata();
            StrukturertAdresse oppgittAdresse = grunnlagData.bosted.oppgittAdresse;
            if (oppgittAdresse.erTom()) {
                throw new TekniskException("Bruker har verken adresse i TPS eller oppgitt adresse i søknad");
            }
            mottakerBrev.setMottakeradresse(lagAdresse(oppgittAdresse));
            mottakerBrev.setBerik(false);
            mottakerBrev.setNavn(navn);
            mottakerBrev.setKortNavn(navn);
        } else {
            mottakerBrev.setMottakeradresse(lagNorskPostadresse());
            mottakerBrev.setBerik(true);
            mottakerBrev.setNavn(PLASSHOLDER_TEKST);
            mottakerBrev.setKortNavn(PLASSHOLDER_TEKST);
        }
        return mottakerBrev;
    }

    private Saksbehandler lagSaksbehandler(String userId) {
        Saksbehandler saksbehandler = new Saksbehandler();
        saksbehandler.setNavEnhet(lagNavEnhet());
        saksbehandler.setNavAnsatt(lagNavAnsatt(userId));
        return saksbehandler;
    }

    private Sakspart lagSakspart(Behandling behandling) throws TekniskException {
        Sakspart sakspart = new Sakspart();
        Aktoer aktør = behandling.getFagsak().hentBruker();

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

    private boolean brukerHarIkkeAdresseiTps(Behandling behandling) throws TekniskException {
        PersonDokument person = SaksopplysningerUtils.hentPersonDokument(behandling);
        return person.harIkkeRegistrertAdresse();
    }
}