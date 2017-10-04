package no.nav.melosys.integrasjon.inntk;

import java.io.StringWriter;
import java.time.YearMonth;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.integrasjon.inntk.inntekt.InntektConsumer;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.HentInntektListeUgyldigInput;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Ainntektsfilter;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Formaal;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.ObjectFactory;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.PersonIdent;
import no.nav.tjeneste.virksomhet.inntekt.v3.informasjon.inntekt.Uttrekksperiode;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeRequest;
import no.nav.tjeneste.virksomhet.inntekt.v3.meldinger.HentInntektListeResponse;

@Service
public class InntektService implements InntektFasade {

    private static final Logger log = LoggerFactory.getLogger(InntektService.class);

    private static final String INNTEKT_VERSJON = "3.2";

    private InntektConsumer inntektConsumer;

    private DokumentFactory dokumentFactory;

    private ObjectFactory objectFactory;

    private Marshaller marshaller;

    @Autowired
    public InntektService(InntektConsumer consumer, DokumentFactory dokumentFactory) {
        this.inntektConsumer = consumer;
        this.dokumentFactory = dokumentFactory;

        this.objectFactory = new ObjectFactory();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse.class);
            marshaller = jaxbContext.createMarshaller();
        } catch (JAXBException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Saksopplysning hentInntektListe(String personID, YearMonth fom, YearMonth tom) throws HentInntektListeSikkerhetsbegrensning, HentInntektListeUgyldigInput, HentInntektListeHarIkkeTilgangTilOensketAInntektsfilter {
        HentInntektListeRequest request = new HentInntektListeRequest();

        PersonIdent personIdent = objectFactory.createPersonIdent();
        personIdent.setPersonIdent(personID);
        request.setIdent(personIdent);

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
        HentInntektListeResponse response = inntektConsumer.hentInntektListe(request);

        // Response -> xml
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse xmlRoot = new no.nav.tjeneste.virksomhet.inntekt.v3.HentInntektListeResponse();
            xmlRoot.setResponse(response);
            marshaller.marshal(xmlRoot, xmlWriter);
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
