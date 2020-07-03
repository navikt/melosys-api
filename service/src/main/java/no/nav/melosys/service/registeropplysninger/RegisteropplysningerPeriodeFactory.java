package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.domain.Behandling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class RegisteropplysningerPeriodeFactory {

    private final Integer arbeidsforholdhistorikkAntallMåneder;
    private final Integer medlemskaphistorikkAntallÅr;
    private final Integer inntektshistorikkAntallMåneder;
    public static final Integer REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR = 1;

    public RegisteropplysningerPeriodeFactory(@Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}") Integer arbeidsforholdhistorikkAntallMåneder,
                                              @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr,
                                              @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}") Integer inntektshistorikkAntallMåneder) {
        this.arbeidsforholdhistorikkAntallMåneder = arbeidsforholdhistorikkAntallMåneder;
        this.medlemskaphistorikkAntallÅr = medlemskaphistorikkAntallÅr;
        this.inntektshistorikkAntallMåneder = inntektshistorikkAntallMåneder;
    }

    DatoPeriode hentPeriodeForArbeidsforhold(LocalDate fom, LocalDate tom, Behandling behandling) {
        return behandling.erBehandlingAvSøknad()
            ? hentPeriodeForArbeidsforholdBehandlingSøknad(fom, tom)
            : hentPeriodeForArbeidsforholdMottakSed(fom, tom);
    }

    DatoPeriode hentPeriodeForMedlemskap(LocalDate fom, LocalDate tom, Behandling behandling) {
        return behandling.erBehandlingAvSøknad()
            ? hentPeriodeForMedlemskapBehandlingSøknad(fom, tom)
            : hentPeriodeForMedlemskapMottakSed(fom, tom);
    }

    Periode hentPeriodeForInntekt(LocalDate fom, LocalDate tom, Behandling behandling) {
        return behandling.erBehandlingAvSøknad()
            ? hentPeriodeForInntektBehandlingSøknad(fom, tom)
            : hentPeriodeForInntektMottakSed(fom, tom);
    }

    private DatoPeriode hentPeriodeForArbeidsforholdBehandlingSøknad(LocalDate fom, LocalDate tom) {
        LocalDate fomDato = fom;
        LocalDate tomDato = tom;

        final LocalDate iDag = LocalDate.now();
        if (fomDato.isAfter(iDag)) {
            fomDato = iDag.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        } else {
            fomDato = fomDato.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        }

        if (tomDato == null) {
            tomDato = fom.plusYears(REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR);
        }
        if (tomDato.isAfter(iDag)) {
            tomDato = iDag;
        }
        return new DatoPeriode(fomDato, tomDato);
    }

    private DatoPeriode hentPeriodeForArbeidsforholdMottakSed(LocalDate fom, LocalDate tom) {
        LocalDate fomDato = fom;
        LocalDate tomDato = tom;

        final LocalDate iDag = LocalDate.now();
        if (fomDato.isAfter(iDag)) {
            fomDato = iDag.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        } else {
            fomDato = fomDato.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        }

        if (tomDato == null || tomDato.isAfter(iDag)) {
            tomDato = iDag;
        }

        return new DatoPeriode(fomDato, tomDato);
    }

    private Periode hentPeriodeForInntektBehandlingSøknad(LocalDate fom, LocalDate tom) {
        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if (tom == null) {
            fomMnd = YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder));
            tomMnd = YearMonth.from(fom.plusYears(REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR));
        } else if (fom.isBefore(nå) && tom.isAfter(nå)) { //1. Periode påbegynt: utbetalinger periode med 2 mnd tilbake
            fomMnd = YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder));
            tomMnd = YearMonth.from(tom);
        } else if (fom.isAfter(nå)) { //2. Periode ikke påbegynt. Inneværende mnd og 2 mnd tilbake
            fomMnd = YearMonth.from(nå.minusMonths(inntektshistorikkAntallMåneder));
            tomMnd = YearMonth.from(nå);
        } else { //3. Avsluttet: sjekker hele periode
            fomMnd = YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder));
            tomMnd = YearMonth.from(tom);
        }

        return new Periode(fomMnd, tomMnd);
    }

    private Periode hentPeriodeForInntektMottakSed(LocalDate fom, LocalDate tom) {
        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if (tom == null) {
            fomMnd = YearMonth.from(fom);
            tomMnd = YearMonth.from(fom.plusYears(2));
        } else if (fom.isBefore(nå) && tom.isAfter(nå)) { //1. Periode påbegynt: utbetalinger periode med 2 mnd tilbake
            fomMnd = YearMonth.from(fom.minusMonths(2L));
            tomMnd = YearMonth.from(tom);
        } else if (fom.isAfter(nå)) { //2. Periode ikke påbegynt. Inneværende mnd og 2 mnd tilbake
            fomMnd = YearMonth.from(nå.minusMonths(2L));
            tomMnd = YearMonth.from(nå);
        } else { //3. Avsluttet: sjekker hele periode
            fomMnd = YearMonth.from(fom);
            tomMnd = YearMonth.from(tom);
        }

        return new Periode(fomMnd, tomMnd);
    }

    private DatoPeriode hentPeriodeForMedlemskapBehandlingSøknad(LocalDate fom, LocalDate tom) {
        LocalDate fomDato = fom.minusYears(medlemskaphistorikkAntallÅr);
        LocalDate tomDato = tom == null ? fom.plusYears(REGISTEROPPLYSNINGER_DEFAULT_SLUTTDATO_ANTALL_ÅR) : tom;

        return new DatoPeriode(fomDato, tomDato);
    }

    private DatoPeriode hentPeriodeForMedlemskapMottakSed(LocalDate fom, LocalDate tom) {
        LocalDate fomDato = fom.minusYears(medlemskaphistorikkAntallÅr);

        return new DatoPeriode(fomDato, tom);
    }

    static final class Periode {
        YearMonth fom;
        YearMonth tom;

        Periode(YearMonth fom, YearMonth tom) {
            this.fom = fom;
            this.tom = tom;
        }
    }

    static final class DatoPeriode {
        LocalDate fom;
        LocalDate tom;

        DatoPeriode(LocalDate fom, LocalDate tom) {
            this.fom = fom;
            this.tom = tom;
        }
    }
}