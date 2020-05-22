package no.nav.melosys.service.registeropplysninger;

import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.inntk.InntektService;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.integrasjon.utbetaldata.UtbetaldataService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@Qualifier("jfr")
public class RegisteropplysningerJfrService extends RegisteropplysningerService {

    private final Integer defaultSluttdatoAntallÅr = 1;

    @Autowired
    public RegisteropplysningerJfrService(@Qualifier("system") TpsFasade tpsFasade,
                                          MedlPeriodeService medlPeriodeService, @Qualifier("system") EregFasade eregFasade,
                                          AaregFasade aaregFasade,
                                          BehandlingService behandlingService,
                                          SakOgBehandlingFasade sakOgBehandlingFasade,
                                          InntektService inntektService,
                                          UtbetaldataService utbetaldataService,
                                          SaksopplysningerService saksopplysningerService,
                                          @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallMåneder}") Integer arbeidsforholdhistorikkAntallMåneder,
                                          @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr,
                                          @Value("${melosys.service.fagsak.inntektshistorikk.antallMåneder}") Integer inntektshistorikkAntallMåneder
    ) {
        super(tpsFasade, medlPeriodeService, eregFasade, aaregFasade, behandlingService,
            sakOgBehandlingFasade, inntektService, utbetaldataService, saksopplysningerService,
            arbeidsforholdhistorikkAntallMåneder, medlemskaphistorikkAntallÅr, inntektshistorikkAntallMåneder);
    }

    @Override
    RegisteropplysningerService.DatoPeriode hentPeriodeForArbeidsforhold(LocalDate fom, LocalDate tom) {

        LocalDate fomDato = fom;
        LocalDate tomDato = tom;

        final LocalDate iDag = LocalDate.now();
        if (fomDato.isAfter(iDag)) {
            fomDato = iDag.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        } else {
            fomDato = fomDato.minusMonths(arbeidsforholdhistorikkAntallMåneder);
        }

        if (tomDato == null) {
            tomDato = fom.plusYears(defaultSluttdatoAntallÅr);
        }
        if (tomDato.isAfter(iDag)) {
            tomDato = iDag;
        }
        return new DatoPeriode(fomDato, tomDato);
    }

    @Override
    RegisteropplysningerService.Periode hentPeriodeForYtelser(LocalDate fom, LocalDate tom) {

        YearMonth fomMnd;
        YearMonth tomMnd;

        LocalDate nå = LocalDate.now();
        if (tom == null) {
            fomMnd = YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder));
            tomMnd = YearMonth.from(fom.plusYears(defaultSluttdatoAntallÅr));
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
}
