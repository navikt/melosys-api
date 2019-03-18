package no.nav.melosys.service.dokument.brev;

import java.io.IOException;
import java.io.StringReader;
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

import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.REPRESENTANT;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

/**
 * BrevDataService er ansvarlig for å mappe saksopplysninger i brevmalene.
 */
@Service
public class BrevDataService {

    private TpsFasade tpsFasade;

    static final String MELOSYS_ENHET_ID = "4530";

    private static final String FALSK_MOTTAKER_ID = "11111111111";
    static final String PLASSHOLDER_TEKST = "-";
    static final String PLASSHOLDER_POSTNUMMER = "0000";

    private BehandlingsresultatRepository behandlingsresultatRepository;

    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;

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
    public DokumentbestillingMetadata lagBestillingMetadata(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        Assert.notNull(produserbartDokument, "Ingen gyldig produserbartDokument");
        Fagsak fagsak = behandling.getFagsak();
        String saksnummer = fagsak.getSaksnummer();

        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();
        metadata.dokumenttypeID = DokumenttypeIdMapper.hentID(produserbartDokument);
        metadata.mottakersRolle = brevData.mottaker;

        metadata.brukerID = tpsFasade.hentFagsakIdentMedRolleType(fagsak, BRUKER);
        Aktoer representant = fagsak.hentAktørMedRolleType(REPRESENTANT);

        switch (produserbartDokument) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK: {
                if (representant != null) {
                    metadata.mottakerID = tpsFasade.hentFagsakIdentMedRolleType(fagsak, REPRESENTANT);
                } else {
                    metadata.mottakerID = metadata.brukerID;
                }
                break;
            }
            case INNVILGELSE_ARBEIDSGIVER:
            case AVSLAG_ARBEIDSGIVER: {
                if (representant != null) {
                    metadata.mottakerID = tpsFasade.hentFagsakIdentMedRolleType(fagsak, REPRESENTANT);
                } else {
                    if (brevData.mottaker == null) {
                        throw new TekniskException("Det finnes ingen mottaker på sak " + fagsak.getSaksnummer());
                    }
                    metadata.mottakerID = tpsFasade.hentFagsakIdentMedRolleType(fagsak, brevData.mottaker);
                }
                break;
            }
            case ATTEST_A1:
            case ANMODNING_UNNTAK:
            case INNVILGELSE_YRKESAKTIV:
                //Avklaring av brev mottaker blir utført i MELOSYS-2248
            case MELDING_MANGLENDE_OPPLYSNINGER:
            case MELDING_HENLAGT_SAK: {
                if (brevData.mottaker == null) {
                    throw new TekniskException("Det finnes ingen mottaker på sak " + saksnummer);
                }
                metadata.mottakerID = tpsFasade.hentFagsakIdentMedRolleType(fagsak, brevData.mottaker);
                break;
            }
            default:
                throw new TekniskException("Produserbaredokumenter ikke støttet");
        }

        metadata.journalsakID = Long.toString(fagsak.getGsakSaksnummer());
        // Fagområde=MED for alle dokumenter til bruker/arbeidsgiver, men kan være UFM for papir-SED til ikke-elektroniske land
        metadata.fagområde = Tema.MED.getKode();
        metadata.saksbehandler = brevData.saksbehandler;
        metadata.utenlandskMyndighet = (brevData.mottaker == Aktoersroller.MYNDIGHET) ? hentMyndighetFraSak(fagsak) : null;
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
            MelosysNAVFelles navFelles = mapNAVFelles(behandling, brevData);
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

    private MelosysNAVFelles mapNAVFelles(Behandling behandling, BrevData brevData) throws TekniskException {
        final MelosysNAVFelles navFelles = new MelosysNAVFelles();

        navFelles.setSakspart(lagSakspart(behandling));
        navFelles.setMottaker(lagMottaker(behandling, brevData));
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

    Mottaker lagMottaker(Behandling behandling, BrevData brevData) throws TekniskException {
        Aktoersroller mottakersRolle = brevData.mottaker != null ? brevData.mottaker : BRUKER;
        Aktoer aktør = behandling.getFagsak().hentAktørMedRolleType(mottakersRolle);
        TekniskException ingenAktør = new TekniskException("Det finnes ingen mottaker på sak " + behandling.getFagsak().getSaksnummer());
        if (aktør == null) {
            throw ingenAktør;
        }

        Mottaker mottaker;
        if (aktør.getAktørId() != null) {
            try {
                mottaker = new Person();
                mottaker.setTypeKode(AktoerType.PERSON);
                mottaker.setId(tpsFasade.hentIdentForAktørId(aktør.getAktørId()));
            } catch (IkkeFunnetException e) {
                throw new TekniskException("Det finnes ingen ident for aktørID " + aktør.getAktørId());
            }
        } else if (aktør.getOrgnr() != null) {
            mottaker = new Organisasjon();
            mottaker.setTypeKode(AktoerType.ORGANISASJON);
            mottaker.setId(aktør.getOrgnr());
        } else if (aktør.getInstitusjonId() != null) {
            mottaker = new Person();
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

        mottaker.setNavn(PLASSHOLDER_TEKST);
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

}
