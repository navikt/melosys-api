package no.nav.melosys.integrasjon.utbetaldata;

import java.io.StringWriter;
import java.time.LocalDate;
import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceException;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingConsumer;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UtbetaldataService implements UtbetaldataFasade {
    private static final String UTBETAL_VERSJON = "1.0";
    private static final String BARNETRYGD = "BARNETRYGD";
    private static final String RETTIGHETSHAVER = "Rettighetshaver";
    private static final String YTELSESPERIODE = "Ytelsesperiode";

    private final UtbetalingConsumer utbetalingConsumer;
    private final DokumentFactory dokumentFactory;

    @Autowired
    public UtbetaldataService(UtbetalingConsumer utbetalingConsumer, DokumentFactory dokumentFactory) {
        this.utbetalingConsumer = utbetalingConsumer;
        this.dokumentFactory = dokumentFactory;
    }

    @Override
    public Saksopplysning hentUtbetalingerBarnetrygd(String fnr, LocalDate fom, LocalDate tom) {
        WSHentUtbetalingsinformasjonResponse response;

        // Utbetldata støtter ikke uthenting av data for lenger tilbake enn 3 år
        if (tom != null && datoErEldreEnnTreÅr(tom)) {
            response = new WSHentUtbetalingsinformasjonResponse();
        } else {
            response = filtrerYtelserAvTypeBarnetrygd(
                    hentUtbetalingsinformasjon(lagRequest(fnr, fom, tom))
            );
        }

        // Response -> xml
        StringWriter xmlWriter = lagXml(response);
        Saksopplysning saksopplysning = lagSaksopplysning(xmlWriter);

        // xml -> java objekter
        dokumentFactory.lagDokument(saksopplysning);

        return saksopplysning;
    }

    private WSHentUtbetalingsinformasjonResponse hentUtbetalingsinformasjon(WSHentUtbetalingsinformasjonRequest request) {
        try {
            return utbetalingConsumer.hentUtbetalingsinformasjon(request);
        } catch (HentUtbetalingsinformasjonPersonIkkeFunnet hentUtbetalingsinformasjonPersonIkkeFunnet) {
            throw new IkkeFunnetException("Oppgitt person ble ikke funnet");
        } catch (HentUtbetalingsinformasjonPeriodeIkkeGyldig hentUtbetalingsinformasjonPeriodeIkkeGyldig) {
            throw new FunksjonellException("Oppgitt periode er ikke gyldig", hentUtbetalingsinformasjonPeriodeIkkeGyldig);
        } catch (HentUtbetalingsinformasjonIkkeTilgang hentUtbetalingsinformasjonIkkeTilgang) {
            throw new SikkerhetsbegrensningException("Har ikke tilgang til å hente data for person", hentUtbetalingsinformasjonIkkeTilgang);
        } catch (WebServiceException e) {
            throw new IntegrasjonException(e);
        }
    }

    private StringWriter lagXml(WSHentUtbetalingsinformasjonResponse response) {
        StringWriter xmlWriter = new StringWriter();
        try {
            no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse xmlRoot =
                new no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonResponse();
            xmlRoot.setHentUtbetalingsinformasjonResponse(response);
            dokumentFactory.createMarshaller().marshal(xmlRoot, xmlWriter);
        } catch (JAXBException e) {
            throw new IntegrasjonException(e);
        }

        return xmlWriter;
    }

    private static Saksopplysning lagSaksopplysning(StringWriter xmlWriter) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.UTBETALDATA, xmlWriter.toString());
        saksopplysning.setType(SaksopplysningType.UTBETAL);
        saksopplysning.setVersjon(UTBETAL_VERSJON);
        return saksopplysning;
    }

    private static WSHentUtbetalingsinformasjonRequest lagRequest(String fnr, LocalDate fom, LocalDate tom) {
        var request = new WSHentUtbetalingsinformasjonRequest();
        request.setId(lagIdent(fnr));
        request.setPeriode(lagPeriode(fom, tom));
        return request;
    }

    private static WSIdent lagIdent(String fnr) {
        var ident = new WSIdent();
        var identrolle = new WSIdentroller();
        identrolle.setValue(RETTIGHETSHAVER);
        ident.setRolle(identrolle);
        ident.setIdent(fnr);
        return ident;
    }

    private static WSForespurtPeriode lagPeriode(LocalDate fom, LocalDate tom) {
        var periode = new WSForespurtPeriode();
        var periodetype = new WSPeriodetyper();
        periodetype.setValue(YTELSESPERIODE);
        periode.setPeriodeType(periodetype);

        if (datoErEldreEnnTreÅr(fom)) {
            fom = LocalDate.now().minusYears(3);
        }

        periode.setFom(KonverteringsUtils.javaLocalDateToJodaDateTime(fom));
        periode.setTom(KonverteringsUtils.javaLocalDateToJodaDateTime(tom));

        return periode;
    }

    private static WSHentUtbetalingsinformasjonResponse filtrerYtelserAvTypeBarnetrygd(WSHentUtbetalingsinformasjonResponse response) {
        // Fjerner utbetalinger uten barnetrygd
        response.getUtbetalingListe().removeIf(utbetaling ->  utbetaling.getYtelseListe().stream()
            .noneMatch(UtbetaldataService::erBarnetrygdytelse));

        // Fjerner ytelser i utbetalinger som ikke er barnetrygd
        response.getUtbetalingListe().forEach(utbetaling -> utbetaling.getYtelseListe()
            .removeIf(ytelse -> !erBarnetrygdytelse(ytelse)));

        return response;
    }

    private static boolean erBarnetrygdytelse(WSYtelse ytelse) {
        return ytelse.getYtelsestype() != null
            && ytelse.getYtelsestype().getValue() != null
            && ytelse.getYtelsestype().getValue().trim().equalsIgnoreCase(BARNETRYGD);
    }

    private static boolean datoErEldreEnnTreÅr(LocalDate dato) {
        return dato.isBefore(LocalDate.now().minusYears(3));
    }
}
