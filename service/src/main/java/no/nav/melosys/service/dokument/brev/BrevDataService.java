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
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.DokumentType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingMetadata;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.*;

/**
 * BrevDataService er ansvarlig for å mappe saksopplysninger i brevmalene.
 */
@Service
public class BrevDataService {

    private TpsFasade tpsFasade;

    static String MELOSYS_ENHET_ID = "4530";

    @Autowired
    public BrevDataService(TpsFasade tpsFasade) {
        this.tpsFasade = tpsFasade;
    }

    /**
     * Genererer metada til doksys angående dokumentbestillingen.
     */
    public DokumentbestillingMetadata lagBestillingMetadata(DokumentType dokumentType, Behandling behandling) {
        DokumentbestillingMetadata metadata = new DokumentbestillingMetadata();

        Fagsak fagsak = behandling.getFagsak();
        Aktoer bruker = fagsak.getBruker();
        if (bruker != null) {
            try {
                metadata.bruker = tpsFasade.hentIdentForAktørId(bruker.getAktørId());
            } catch (IkkeFunnetException e) {
                throw new TekniskException("Det finnes ingen ident for aktørID " + bruker.getAktørId());
            }
        } else {
            throw new TekniskException("Det finnes ingen bruker på sak " + fagsak.getSaksnummer());
        }

        metadata.dokumenttypeID = dokumentType.getKode();
        metadata.journalsakID = Long.toString(fagsak.getGsakSaksnummer());
        // FIXME Mottaker er avhengig av dokumentTypen men kan også sendes som parameter
        metadata.mottaker = null;
        // FIXME Fagområde er avhengig av dokumentTypen men kan også sendes som parameter.
        metadata.fagområde = null;

        return metadata;
    }

    /**
     * Genererer XML i hensyn til mal og validere mot xsd.
     */
    public Element lagBrevXML(DokumentType dokumentType, Behandling behandling) {
        Element brevXmlElement;
        try {
            FellesType fellesType = mapFellesType(behandling);
            MelosysNAVFelles navFelles = mapNAVFelles(behandling);
            String brevXml = BrevDataMapperRuter.brevDataMapper(dokumentType).mapTilBrevXML(fellesType, navFelles, behandling);

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(brevXml));
            Document doc = db.parse(is);
            brevXmlElement = doc.getDocumentElement();
        } catch (JAXBException | SAXException | ParserConfigurationException | IOException e) {
            throw new TekniskException("XML genereringsfeil " + behandling.getFagsak().getSaksnummer(), e);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new TekniskException("Annen teknisk feil", e);
        }
        return brevXmlElement;
    }

    private FellesType mapFellesType(Behandling behandling) {
        final FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer(behandling.getFagsak().getSaksnummer());

        return fellesType;
    }

    private MelosysNAVFelles mapNAVFelles(Behandling behandling) {
        final MelosysNAVFelles navFelles = new MelosysNAVFelles();

        navFelles.setSakspart(lagSakspart(behandling));
        navFelles.setMottaker(lagMottaker(behandling));
        navFelles.setBehandlendeEnhet(lagNavEnhet());
        navFelles.setSignerendeSaksbehandler(lagSaksbehandler());
        navFelles.setSignerendeBeslutter(lagSaksbehandler());
        navFelles.setKontaktinformasjon(BrevDataUtils.lagKontaktInformasjon());

        return navFelles;
    }

    private Sakspart lagSakspart(Behandling behandling) {
        Sakspart sakspart = new Sakspart();
        Aktoer aktør = behandling.getFagsak().getBruker();

        if (aktør == null || aktør.getAktørId() == null) {
            throw new TekniskException("Det finnes ingen bruker på sak " + behandling.getFagsak().getSaksnummer());
        }
        try {
            sakspart.setId(tpsFasade.hentIdentForAktørId(aktør.getAktørId()));
        } catch (IkkeFunnetException e) {
            throw new TekniskException("Det finnes ingen ident for aktørID " + aktør.getAktørId());
        }

        sakspart.setTypeKode(AktoerType.PERSON);
        sakspart.setBerik(true);
        sakspart.setNavn("Sakspart-Navn");
        return sakspart;
    }

    private Mottaker lagMottaker(Behandling behandling) {
        Mottaker mottaker = new Person(); // FIXME mottaker kan være Organisasjon
        Aktoer aktør = behandling.getFagsak().getRepresentant();
        if (aktør == null) {
            aktør = behandling.getFagsak().getBruker();
        }

        if (aktør == null || aktør.getAktørId() == null) {
            throw new TekniskException("Det finnes ingen representant/bruker på sak " + behandling.getFagsak().getSaksnummer());
        }
        try {
            mottaker.setId(tpsFasade.hentIdentForAktørId(aktør.getAktørId()));
        } catch (IkkeFunnetException e) {
            throw new TekniskException("Det finnes ingen ident for aktørID " + aktør.getAktørId());
        }

        mottaker.setTypeKode(AktoerType.PERSON);
        mottaker.setBerik(true); // Gjør oppslag mot EREG/TPS

        mottaker.setNavn("Mottaker-Navn");
        mottaker.setKortNavn("Mottaker-Kortnavn");
        mottaker.setSpraakkode(Spraakkode.NB);
        mottaker.setMottakeradresse(lagNorskPostadresse());

        return mottaker;
    }

    // FIXME Bør være private
    Saksbehandler lagSaksbehandler() {
        Saksbehandler saksbehandler = new Saksbehandler();
        saksbehandler.setNavEnhet(lagNavEnhet());

        String userID = SubjectHandler.getInstance().getUserID();
        if (userID != null) {
            saksbehandler.setNavAnsatt(lagNavAnsatt(userID));
        }
        return saksbehandler;
    }

}
