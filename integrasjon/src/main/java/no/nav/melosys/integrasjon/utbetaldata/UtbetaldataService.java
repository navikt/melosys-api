package no.nav.melosys.integrasjon.utbetaldata;

import java.io.StringWriter;
import java.time.LocalDate;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingConsumer;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UtbetaldataService implements UtbetaldataFasade {

    private static final Logger log = LoggerFactory.getLogger(UtbetaldataService.class);

    private static final String UTBETAL_VERSJON = "1.0";

    private static final String RETTIGHETSHAVER = "Rettighetshaver";

    private static final String UTBETALINGSPERIODE = "Utbetalingsperiode";

    private static final String YTELSESPERIODE = "Ytelsesperiode";

    private UtbetalingConsumer utbetalingConsumer;

    private DokumentFactory dokumentFactory;

    private final JAXBContext jaxbContext;

    @Autowired
    public UtbetaldataService(UtbetalingConsumer utbetalingConsumer, DokumentFactory dokumentFactory) {
        this.utbetalingConsumer = utbetalingConsumer;
        this.dokumentFactory = dokumentFactory;

        try {
            jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse.class);
        } catch (JAXBException e) {
            log.error("", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Saksopplysning hentUtbetalingsinformasjon(String fnr, LocalDate fom, LocalDate tom) throws TekniskException, FunksjonellException {
        HentUtbetalingsinformasjonResponse response;
        try {
            response = utbetalingConsumer.hentUtbetalingsinformasjon(lagRequest(fnr, fom, tom));
        } catch (HentUtbetalingsinformasjonPersonIkkeFunnet hentUtbetalingsinformasjonPersonIkkeFunnet) {
            throw new IkkeFunnetException("Oppgitt person ble ikke funnet");
        } catch (HentUtbetalingsinformasjonPeriodeIkkeGyldig hentUtbetalingsinformasjonPeriodeIkkeGyldig) {
            throw new FunksjonellException("Oppgitt periode er ikke gyldig", hentUtbetalingsinformasjonPeriodeIkkeGyldig);
        } catch (HentUtbetalingsinformasjonIkkeTilgang hentUtbetalingsinformasjonIkkeTilgang) {
            throw new SikkerhetsbegrensningException("Har ikke tilgang til å hente data for person", hentUtbetalingsinformasjonIkkeTilgang);
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException(e);
        }

        // Response -> xml
        StringWriter xmlWriter = lagXml(response);
        Saksopplysning saksopplysning = lagSaksopplysning(xmlWriter);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    private StringWriter lagXml(HentUtbetalingsinformasjonResponse response) throws IntegrasjonException {
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse xmlRoot =
                new no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse();
            xmlRoot.setHentUtbetalingsinformasjonResponse(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            log.error("", e);
            throw new IntegrasjonException(e);
        }

        return xmlWriter;
    }

    private Saksopplysning lagSaksopplysning(StringWriter xmlWriter) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.UTBETALDATA);
        saksopplysning.setType(SaksopplysningType.UTBETAL);
        saksopplysning.setVersjon(UTBETAL_VERSJON);
        return saksopplysning;
    }

    private HentUtbetalingsinformasjonRequest lagRequest(String fnr, LocalDate fom, LocalDate tom) throws DatatypeConfigurationException {
        HentUtbetalingsinformasjonRequest request = new HentUtbetalingsinformasjonRequest();
        request.setId(lagIdent(fnr));
        //request.setPeriode(lagPeriode(fom, tom));

        Ytelsestyper ytelsestype = new Ytelsestyper();
        ytelsestype.setValue("Barnetrygd");
        request.getYtelsestypeListe().add(ytelsestype);

        return request;
    }

    private Ident lagIdent(String fnr) {
        Ident ident = new Ident();
        Identroller identrolle = new Identroller();
        identrolle.setValue(RETTIGHETSHAVER);
        ident.setRolle(identrolle);
        ident.setIdent(fnr);
        return ident;
    }

    private ForespurtPeriode lagPeriode(LocalDate fom, LocalDate tom) throws DatatypeConfigurationException {
        ForespurtPeriode periode = new ForespurtPeriode();
        Periodetyper periodetype = new Periodetyper();
        periodetype.setKodeRef(YTELSESPERIODE);
        periode.setPeriodeType(periodetype);
        periode.setFom(KonverteringsUtils.localDateToXMLGregorianCalendar(fom));
        periode.setTom(KonverteringsUtils.localDateToXMLGregorianCalendar(tom));
        return periode;
    }
}
