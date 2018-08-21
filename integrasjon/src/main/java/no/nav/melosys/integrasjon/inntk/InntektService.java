package no.nav.melosys.integrasjon.inntk;

import java.io.StringWriter;
import java.time.YearMonth;
import javax.xml.bind.JAXBContext;
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
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektConsumer;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.*;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ObjectFactory;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeBolkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InntektService implements InntektFasade {

    private static final Logger log = LoggerFactory.getLogger(InntektService.class);

    private static final String INNTEKT_VERSJON = "3.2";

    private InntektConsumer inntektConsumer;

    private DokumentFactory dokumentFactory;

    private ObjectFactory objectFactory;

    private final JAXBContext jaxbContext;

    @Autowired
    public InntektService(InntektConsumer consumer, DokumentFactory dokumentFactory) {
        this.inntektConsumer = consumer;
        this.dokumentFactory = dokumentFactory;

        this.objectFactory = new ObjectFactory();

        try {
            jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeBolkResponse.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    // Henter inntekter for én ident fra hentInntektListeBolk for å få opplysninger om frilansforhold (se MELOSYS-1453).
    @Override
    public Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom) throws SikkerhetsbegrensningException {
        HentInntektListeBolkRequest request = new HentInntektListeBolkRequest();

        PersonIdent personIdent = objectFactory.createPersonIdent();
        personIdent.setPersonIdent(personID);
        request.getIdentListe().add(personIdent);

        Uttrekksperiode uttrekksperiode = objectFactory.createUttrekksperiode();
        try {
            uttrekksperiode.setMaanedFom(convertToXMLGregorianCalendar(fom));
            uttrekksperiode.setMaanedTom(convertToXMLGregorianCalendar(tom));
        } catch (DatatypeConfigurationException e) {
            log.error("", e);
            throw new RuntimeException(e);
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
        HentInntektListeBolkResponse response = null;
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
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.INNTK);
        saksopplysning.setType(SaksopplysningType.INNTEKT);
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
