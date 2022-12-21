package no.nav.melosys.integrasjon.utbetaldata;

import java.io.StringWriter;
import java.time.LocalDate;
import javax.xml.bind.JAXBException;
import javax.xml.ws.WebServiceException;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.*;
import no.nav.melosys.integrasjon.utbetaling.UtbetalingServiceV2;
import no.nav.melosys.integrasjon.utbetaling.Ytelse;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonIkkeTilgang;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPeriodeIkkeGyldig;
import no.nav.tjeneste.virksomhet.utbetaling.v1.HentUtbetalingsinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.*;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonRequest;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse;
import org.springframework.stereotype.Service;

@Service
public class UtbetaldataService implements UtbetaldataFasade {
    private static final String BARNETRYGD = "BARNETRYGD";
    private static final String RETTIGHETSHAVER = "Rettighetshaver";
    private static final String YTELSESPERIODE = "Ytelsesperiode";

    private final UtbetalingConsumer utbetalingConsumer;
    private final DokumentFactory dokumentFactory;
    private final Unleash unleash;
    private final UtbetalingServiceV2 utbetalingServiceV2;

    public UtbetaldataService(UtbetalingConsumer utbetalingConsumer, DokumentFactory dokumentFactory, UtbetalingServiceV2 utbetalingServiceV2, Unleash unleash) {
        this.utbetalingConsumer = utbetalingConsumer;
        this.dokumentFactory = dokumentFactory;
        this.unleash = unleash;
        this.utbetalingServiceV2 = utbetalingServiceV2;
    }

    @Override
    public Saksopplysning hentUtbetalingerBarnetrygd(String fnr, LocalDate fom, LocalDate tom) {

        if (!unleash.isEnabled("ubetalinger.v2")) { //TODO lag featuretoggle
            WSHentUtbetalingsinformasjonResponse response;

            if (erUtbetalingsDataStoettet(tom)) {
                response = new WSHentUtbetalingsinformasjonResponse();
            } else {
                response = filtrerYtelserAvTypeBarnetrygd(
                    hentUtbetalingsinformasjon(lagRequest(fnr, fom, tom))
                );
            }

            return UtbetaldataMapper.tilSaksopplysning(response, lagXml(response).toString());

        } else {
            System.out.println("Utbetalinger v2 brukes:");
            return utbetalingServiceV2.hentSaksopplysningForUtbetaling(fnr, fom, tom);
        }
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

    private WSHentUtbetalingsinformasjonRequest lagRequest(String fnr, LocalDate fom, LocalDate tom) {
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

    private WSForespurtPeriode lagPeriode(LocalDate fom, LocalDate tom) {
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

    private WSHentUtbetalingsinformasjonResponse filtrerYtelserAvTypeBarnetrygd(WSHentUtbetalingsinformasjonResponse response) {
        taVekkUtbetalingerUtenBarnetrygd(response);
        taVekkYtelserFraUtbetalingerSomIkkeErBarnetrygd(response);
        return response;
    }

    private void taVekkYtelserFraUtbetalingerSomIkkeErBarnetrygd(WSHentUtbetalingsinformasjonResponse response) {
        response.getUtbetalingListe().forEach(utbetaling -> utbetaling.getYtelseListe()
            .removeIf(ytelse -> !erBarnetrygdytelse(ytelse)));
    }

    private void taVekkUtbetalingerUtenBarnetrygd(WSHentUtbetalingsinformasjonResponse response) {
        response.getUtbetalingListe().removeIf(utbetaling -> utbetaling.getYtelseListe().stream()
            .noneMatch(this::erBarnetrygdytelse));
    }

    private boolean erBarnetrygdytelse(WSYtelse ytelse) {
        return ytelse.getYtelsestype() != null
            && ytelse.getYtelsestype().getValue() != null
            && ytelse.getYtelsestype().getValue().trim().equalsIgnoreCase(BARNETRYGD);
    }

    private boolean erBarnetrygdytelse(Ytelse ytelse) {
        return ytelse.getYtelsestype().trim().equalsIgnoreCase(BARNETRYGD);
    }

    private boolean erUtbetalingsDataStoettet(LocalDate tom) {
        return tom != null && datoErEldreEnnTreÅr(tom);
    }

    private boolean datoErEldreEnnTreÅr(LocalDate dato) {
        return dato.isBefore(LocalDate.now().minusYears(3));
    }
}
