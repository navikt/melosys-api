package no.nav.melosys.service.registeropplysninger;

import java.time.LocalDate;
import java.time.YearMonth;

import no.nav.melosys.domain.Behandling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RegisteropplysningerPeriodeFactory {
    private final Integer arbeidsforholdhistorikkAntallMåneder;
    private final Integer medlemskaphistorikkAntallÅr;
    private final Integer inntektshistorikkAntallMåneder;

    public RegisteropplysningerPeriodeFactory(@Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}") Integer arbeidsforholdhistorikkAntallMåneder,
                                              @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr,
                                              @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}") Integer inntektshistorikkAntallMåneder) {
        this.arbeidsforholdhistorikkAntallMåneder = arbeidsforholdhistorikkAntallMåneder;
        this.medlemskaphistorikkAntallÅr = medlemskaphistorikkAntallÅr;
        this.inntektshistorikkAntallMåneder = inntektshistorikkAntallMåneder;
    }

    DatoPeriode hentPeriodeForArbeidsforhold(LocalDate fom, LocalDate tom, Behandling behandling) {
        return behandling.erBehandlingAvSøknadGammel()
            ? hentPeriodeForArbeidsforholdBehandlingSøknad(fom, tom)
            : hentPeriodeForArbeidsforholdMottakSed(fom, tom);
    }

    DatoPeriode hentPeriodeForMedlemskap(LocalDate fom, LocalDate tom, Behandling behandling) {
        return behandling.erBehandlingAvSøknadGammel()
            ? hentPeriodeForMedlemskapBehandlingSøknad(fom, tom)
            : hentPeriodeForMedlemskapMottakSed(fom, tom);
    }

    Periode hentPeriodeForInntekt(LocalDate fom, LocalDate tom, Behandling behandling) {
        return behandling.erBehandlingAvSøknadGammel()
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
            tomDato = iDag;
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
        if (fom.isAfter(nå)) {
            fomMnd = YearMonth.from(nå.minusMonths(inntektshistorikkAntallMåneder));
        } else {
            fomMnd = YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder));
        }

        if (tom == null) {
            tomMnd = YearMonth.from(nå);
        } else if (tom.isAfter(nå)) {
            tomMnd = YearMonth.from(nå);
        } else {
            tomMnd = YearMonth.from(tom);
        }

        return new Periode(fomMnd, tomMnd);
    }

    private Periode hentPeriodeForInntektMottakSed(LocalDate fom, LocalDate tom) {
        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        //1. Periode påbegynt: utbetalinger periode med 2 mnd tilbake
        if (fom.isBefore(nå) && (tom == null || tom.isAfter(nå))) {
            fomMnd = YearMonth.from(fom.minusMonths(2L));
            tomMnd = YearMonth.from(nå);
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
        return new DatoPeriode(fom.minusYears(medlemskaphistorikkAntallÅr), tom == null ? LocalDate.now() : tom);
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
