package no.nav.melosys.integrasjon.inntk;

import java.io.StringWriter;
import java.time.YearMonth;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.soap.SOAPFaultException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XmlFormaterer;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektConsumer;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeBolkUgyldigInput;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InntektService implements InntektFasade {
    private static final String INNTEKT_VERSJON = "3.2";

    private final InntektConsumer inntektConsumer;
    private final DokumentFactory dokumentFactory;
    private final ObjectFactory objectFactory;

    public static final String FILTER = "MedlemskapA-inntekt";
    public static final String FILTER_URI = "http://nav.no/kodeverk/Kode/A-inntektsfilter/MedlemskapA-inntekt?v=6";

    public static final String FORMAALSKODE = "Medlemskap";
    public static final String FORMAALSKODE_URI = "http://nav.no/kodeverk/Kode/Formaal/Medlemskap?v=5";

    @Autowired
    public InntektService(InntektConsumer consumer, DokumentFactory dokumentFactory) {
        this.inntektConsumer = consumer;
        this.dokumentFactory = dokumentFactory;

        this.objectFactory = new ObjectFactory();
    }

    // Henter inntekter for én ident fra hentInntektListeBolk for å få opplysninger om frilansforhold (se MELOSYS-1453).
    @Override
    public Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom) throws SikkerhetsbegrensningException, IntegrasjonException {
        HentInntektListeBolkRequest request = new HentInntektListeBolkRequest();

        PersonIdent personIdent = objectFactory.createPersonIdent();
        personIdent.setPersonIdent(personID);
        request.getIdentListe().add(personIdent);

        Uttrekksperiode uttrekksperiode = objectFactory.createUttrekksperiode();
        try {
            uttrekksperiode.setMaanedFom(convertToXMLGregorianCalendar(fom));
            uttrekksperiode.setMaanedTom(convertToXMLGregorianCalendar(tom));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        request.setUttrekksperiode(uttrekksperiode);

        Ainntektsfilter ainntektsfilter = objectFactory.createAinntektsfilter();
        ainntektsfilter.setValue(FILTER);
        ainntektsfilter.setKodeRef(FILTER);
        ainntektsfilter.setKodeverksRef(FILTER_URI);
        request.setAinntektsfilter(ainntektsfilter);

        Formaal formaal = objectFactory.createFormaal();
        formaal.setValue(FORMAALSKODE);
        formaal.setKodeRef(FORMAALSKODE);
        formaal.setKodeverksRef(FORMAALSKODE_URI);
        request.setFormaal(formaal);

        // Kall til Inntektskomponenten
        HentInntektListeBolkResponse response;
        try {
            response = inntektConsumer.hentInntektListeBolk(request);
        } catch (HentInntektListeBolkHarIkkeTilgangTilOensketAInntektsfilter e) {
            throw new SikkerhetsbegrensningException(e);
        } catch (HentInntektListeBolkUgyldigInput | SOAPFaultException e) {
            throw new IntegrasjonException(e);
        }

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse xmlRoot = new no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse();
            xmlRoot.setResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        final String dokumentXml = XmlFormaterer.formaterXml(xmlWriter.toString());
        if (dokumentXml != null) {
            saksopplysning.setDokumentXml(dokumentXml);
        } else {
            throw new IntegrasjonException("DokumentXML er null!");
        }
        saksopplysning.setKilde(SaksopplysningKilde.INNTK);
        saksopplysning.setType(SaksopplysningType.INNTK);
        saksopplysning.setVersjon(INNTEKT_VERSJON);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    private static XMLGregorianCalendar convertToXMLGregorianCalendar(YearMonth yearMonth) throws DatatypeConfigurationException {
        if (yearMonth == null) {
            return null;
        }

        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED
        );
    }
}
