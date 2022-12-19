package no.nav.melosys.integrasjon.utbetaldata;

import java.time.LocalDate;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.utbetaling.Periode;
import no.nav.melosys.domain.dokument.utbetaling.Utbetaling;
import no.nav.melosys.domain.dokument.utbetaling.UtbetalingDokument;
import no.nav.melosys.domain.dokument.utbetaling.Ytelse;
import no.nav.melosys.integrasjon.KonverteringsUtils;
import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingResponse;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.WSUtbetaling;
import no.nav.tjeneste.virksomhet.utbetaling.v1.informasjon.WSYtelse;
import no.nav.tjeneste.virksomhet.utbetaling.v1.meldinger.WSHentUtbetalingsinformasjonResponse;
import org.joda.time.LocalDateTime;

final class UtbetaldataMapper {

    private UtbetaldataMapper() {}

    private static final String UTBETAL_VERSJON = "1.0";
    private static final String UTBETAL_VERSJON2 = "2.0";

    static Saksopplysning tilSaksopplysning(WSHentUtbetalingsinformasjonResponse utbetalingResponse, String mottattDokument) {
        var saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.UTBETAL);
        saksopplysning.setVersjon(UTBETAL_VERSJON);
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.UTBETALDATA, mottattDokument);
        saksopplysning.setDokument(tilUtbetalingDokument(utbetalingResponse));
        return saksopplysning;
    }

    static Saksopplysning tilSaksopplysning(UtbetalingResponse utbetalingResponse, String mottattDokument) {
        var saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.UTBETAL);
        saksopplysning.setVersjon(UTBETAL_VERSJON2);
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.UTBETALDATA, mottattDokument);
        saksopplysning.setDokument(tilUtbetalingDokument(utbetalingResponse));
        return saksopplysning;
    }

    private static UtbetalingDokument tilUtbetalingDokument(WSHentUtbetalingsinformasjonResponse utbetalingResponse) {
        var utbetalingDokument = new UtbetalingDokument();
        utbetalingDokument.utbetalinger = utbetalingResponse.getUtbetalingListe()
            .stream()
            .map(UtbetaldataMapper::tilUtbetalling)
            .collect(Collectors.toList());

        return utbetalingDokument;
    }

    private static UtbetalingDokument tilUtbetalingDokument(UtbetalingResponse utbetalingResponse) {
        var utbetalingDokument = new UtbetalingDokument();
        utbetalingDokument.utbetalinger = utbetalingResponse.getUtbetalingListe()
            .stream()
            .map(UtbetaldataMapper::tilUtbetalling)
            .collect(Collectors.toList());

        return utbetalingDokument;
    }

    private static Utbetaling tilUtbetalling(WSUtbetaling wsUtbetaling) {
        var utbetaling = new Utbetaling();
        utbetaling.ytelser = wsUtbetaling.getYtelseListe().stream()
            .map(UtbetaldataMapper::tilYtelse)
            .collect(Collectors.toList());

        return utbetaling;
    }

    private static Utbetaling tilUtbetalling(no.nav.melosys.integrasjon.utbetaldata.utbetaling.Utbetaling utbetalingResponse) {
        var utbetaling = new Utbetaling();
        utbetaling.ytelser = utbetalingResponse.getYtelseListe().stream()
            .map(UtbetaldataMapper::tilYtelse)
            .collect(Collectors.toList());

        return utbetaling;
    }

    private static Ytelse tilYtelse(WSYtelse wsYtelse) {
        var ytelse = new Ytelse();
        ytelse.periode = new Periode(
            KonverteringsUtils.jodaDateTimeToJavaLocalDate(wsYtelse.getYtelsesperiode().getFom()),
            KonverteringsUtils.jodaDateTimeToJavaLocalDate(wsYtelse.getYtelsesperiode().getTom())
        );
        ytelse.type = wsYtelse.getYtelsestype().getValue();
        return ytelse;
    }

    private static Ytelse tilYtelse(no.nav.melosys.integrasjon.utbetaldata.utbetaling.Ytelse ytelseRes) {
        var ytelse = new Ytelse();
        ytelse.periode = new Periode(
            LocalDate.parse(ytelseRes.getYtelsesperiode().getFom()),
            LocalDate.parse(ytelseRes.getYtelsesperiode().getTom())
        );
        ytelse.type = ytelseRes.getYtelsestype();
        return ytelse;
    }
}
