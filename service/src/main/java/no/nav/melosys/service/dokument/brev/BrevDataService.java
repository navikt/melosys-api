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
import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    static final String PLASSHOLDER_TEKST = "-";
    static final String PLASSHOLDER_POSTNUMMER = "0000";

    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Autowired
    public BrevDataService(TpsFasade tpsFasade, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.tpsFasade = tpsFasade;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    /**
     * Genererer metada til doksys angående dokumentbestillingen.
     */
    public DokumentbestillingMetadata lagBestillingMetadata(Produserbaredokumenter produserbartDokument, Behandling behandling, BrevData brevData) throws TekniskException {
        Assert.notNull(produserbartDokument, "Ingen gyldig produserbartDokument");

        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();

        Fagsak fagsak = behandling.getFagsak();
        metadata.bruker = tpsFasade.hentFagsakIdentMedRolleType(fagsak, BRUKER);
        Aktoer representant = fagsak.hentAktørMedRolleType(REPRESENTANT);

        switch (produserbartDokument) {
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK: {
                if (representant != null) {
                    metadata.mottaker = tpsFasade.hentFagsakIdentMedRolleType(fagsak, REPRESENTANT);
                } else {
                    metadata.mottaker = metadata.bruker;
                }
                break;
            }
            case ATTEST_A1:
            case ANMODNING_UNNTAK:
            case INNVILGELSE_YRKESAKTIV:
            case MELDING_MANGLENDE_OPPLYSNINGER:
            case MELDING_HENLAGT_SAK: {
                if (brevData.mottaker == null) {
                    throw new TekniskException("Det finnes ingen mottaker på sak " + fagsak.getSaksnummer());
                }
                metadata.mottaker = tpsFasade.hentFagsakIdentMedRolleType(fagsak, brevData.mottaker);
                break;
            }
            default:
                throw new TekniskException("Produserbaredokumenter ikke støttet");
        }

        metadata.dokumenttypeID = DokumenttypeIdMapper.hentID(produserbartDokument);
        metadata.journalsakID = Long.toString(fagsak.getGsakSaksnummer());
        // Fagområde=MED for alle dokumenter til bruker/arbeidsgiver, men kan være UFM for papir-SED til ikke-elektroniske land
        metadata.fagområde = Tema.MED.getKode();
        // Default=true i DOKKAT, men kan settes til false for å ikke utlede, eller berik=false for å overstyre enkeltelementer
        metadata.utledRegisterInfo = true;
        metadata.saksbehandler = brevData.saksbehandler;

        return metadata;
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
        Mottaker mottaker;
        Aktoer aktør = behandling.getFagsak().hentAktørMedRolleType(REPRESENTANT);
        if (brevData.mottaker != null) {
            aktør = behandling.getFagsak().hentAktørMedRolleType(brevData.mottaker);
        } else if (aktør == null) {
            aktør = behandling.getFagsak().hentAktørMedRolleType(BRUKER);
        }

        if (aktør != null && aktør.getAktørId() != null) {
            try {
                mottaker = new Person();
                mottaker.setId(tpsFasade.hentIdentForAktørId(aktør.getAktørId()));
                mottaker.setTypeKode(AktoerType.PERSON);
            } catch (IkkeFunnetException e) {
                throw new TekniskException("Det finnes ingen ident for aktørID " + aktør.getAktørId());
            }
        } else if (aktør != null && aktør.getOrgnr() != null) {
            mottaker = new Organisasjon();
            mottaker.setId(aktør.getOrgnr());
            mottaker.setTypeKode(AktoerType.ORGANISASJON);
        } else {
            throw new TekniskException("Det finnes ingen mottaker på sak " + behandling.getFagsak().getSaksnummer());
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
