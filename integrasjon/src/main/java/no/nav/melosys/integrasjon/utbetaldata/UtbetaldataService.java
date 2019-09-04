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
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.ForespurtPeriode;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.Ident;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.Identroller;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.Periodetyper;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.HentUtbetalingsinformasjonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UtbetaldataService implements UtbetaldataFasade {
    private static final String UTBETAL_VERSJON = "1.0";

    private static final String BARNETRYGD = "BARNETRYGD";

    private static final String RETTIGHETSHAVER = "Rettighetshaver";

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
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Saksopplysning hentUtbetalingerBarnetrygd(String fnr, LocalDate fom, LocalDate tom) throws TekniskException, FunksjonellException {
        HentUtbetalingsinformasjonResponse response = filtrerYtelserAvTypeBarnetrygd(
                hentUtbetalingsinformasjon(lagRequest(fnr, fom, tom))
        );

        // Response -> xml
        StringWriter xmlWriter = lagXml(response);
        Saksopplysning saksopplysning = lagSaksopplysning(xmlWriter);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    private HentUtbetalingsinformasjonResponse hentUtbetalingsinformasjon(HentUtbetalingsinformasjonRequest request) throws FunksjonellException {
        try {
            return utbetalingConsumer.hentUtbetalingsinformasjon(request);
        } catch (HentUtbetalingsinformasjonPersonIkkeFunnet hentUtbetalingsinformasjonPersonIkkeFunnet) {
            throw new IkkeFunnetException("Oppgitt person ble ikke funnet");
        } catch (HentUtbetalingsinformasjonPeriodeIkkeGyldig hentUtbetalingsinformasjonPeriodeIkkeGyldig) {
            throw new FunksjonellException("Oppgitt periode er ikke gyldig", hentUtbetalingsinformasjonPeriodeIkkeGyldig);
        } catch (HentUtbetalingsinformasjonIkkeTilgang hentUtbetalingsinformasjonIkkeTilgang) {
            throw new SikkerhetsbegrensningException("Har ikke tilgang til å hente data for person", hentUtbetalingsinformasjonIkkeTilgang);
        }
    }

    private StringWriter lagXml(HentUtbetalingsinformasjonResponse response) throws IntegrasjonException {
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse xmlRoot =
                new no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse();
            xmlRoot.setHentUtbetalingsinformasjonResponse(response);
            jaxbContext.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        return xmlWriter;
    }

    private static Saksopplysning lagSaksopplysning(StringWriter xmlWriter) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokumentXml(xmlWriter.toString());
        saksopplysning.setKilde(SaksopplysningKilde.UTBETALDATA);
        saksopplysning.setType(SaksopplysningType.UTBETAL);
        saksopplysning.setVersjon(UTBETAL_VERSJON);
        return saksopplysning;
    }

    private static HentUtbetalingsinformasjonRequest lagRequest(String fnr, LocalDate fom, LocalDate tom) throws TekniskException {
        HentUtbetalingsinformasjonRequest request = new HentUtbetalingsinformasjonRequest();
        request.setId(lagIdent(fnr));
        request.setPeriode(lagPeriode(fom, tom));
        return request;
    }

    private static Ident lagIdent(String fnr) {
        Ident ident = new Ident();
        Identroller identrolle = new Identroller();
        identrolle.setValue(RETTIGHETSHAVER);
        ident.setRolle(identrolle);
        ident.setIdent(fnr);
        return ident;
    }

    private static ForespurtPeriode lagPeriode(LocalDate fom, LocalDate tom) throws TekniskException {
        ForespurtPeriode periode = new ForespurtPeriode();
        Periodetyper periodetype = new Periodetyper();
        periodetype.setValue(YTELSESPERIODE);
        periode.setPeriodeType(periodetype);

        try {
            periode.setFom(KonverteringsUtils.localDateToXMLGregorianCalendar(fom));
            periode.setTom(KonverteringsUtils.localDateToXMLGregorianCalendar(tom));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException(e);
        }

        return periode;
    }

    private static HentUtbetalingsinformasjonResponse filtrerYtelserAvTypeBarnetrygd(HentUtbetalingsinformasjonResponse response) {
        response.getUtbetalingListe().removeIf(utbetaling ->  utbetaling.getYtelseListe().stream()
            .noneMatch(ytelse -> ytelse.getYtelsestype().getValue().equalsIgnoreCase(BARNETRYGD)));

        return response;
    }
}
