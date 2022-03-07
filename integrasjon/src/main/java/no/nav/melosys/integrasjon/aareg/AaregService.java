package no.nav.melosys.integrasjon.aareg;

import java.time.LocalDate;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningKildesystem;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdKonverter;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdQuery;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdResponse;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AaregService implements AaregFasade {
    private static final String ARBEIDSFORHOLD_REST_VERSJON = "REST 1.0";

    private final ArbeidsforholdRestConsumer arbeidsforholdRestConsumer;
    private final KodeOppslag kodeOppslag;

    @Autowired
    AaregService(ArbeidsforholdRestConsumer arbeidsforholdRestConsumer, KodeOppslag kodeOppslag) {
        this.arbeidsforholdRestConsumer = arbeidsforholdRestConsumer;
        this.kodeOppslag = kodeOppslag;
    }

    @Override
    public Saksopplysning finnArbeidsforholdPrArbeidstaker(String ident, LocalDate fom, LocalDate tom) {
        return finnArbeidsforholdPrArbeidstakerRest(ident, fom, tom);
    }

    private Saksopplysning finnArbeidsforholdPrArbeidstakerRest(String ident, LocalDate fom, LocalDate tom) {
        ArbeidsforholdQuery arbeidsforholdQuery = new ArbeidsforholdQuery
            .Builder()
            .arbeidsforholdType(ArbeidsforholdQuery.ArbeidsforholdType.ALLE)
            .regelverk(ArbeidsforholdQuery.Regelverk.A_ORDNINGEN)
            .ansettelsesperiodeFom(fom)
            .ansettelsesperiodeTom(tom)
            .build();

        ArbeidsforholdResponse response = arbeidsforholdRestConsumer.finnArbeidsforholdPrArbeidstaker(ident, arbeidsforholdQuery);
        ArbeidsforholdKonverter arbeidsforholdKonverter = new ArbeidsforholdKonverter(response, kodeOppslag);

        Saksopplysning saksopplysning = arbeidsforholdKonverter.createSaksopplysning();
        saksopplysning.leggTilKildesystemOgMottattDokument(
            SaksopplysningKildesystem.AAREG, response.tilSaksopplysning());
        saksopplysning.setType(SaksopplysningType.ARBFORH);
        saksopplysning.setVersjon(ARBEIDSFORHOLD_REST_VERSJON);

        return saksopplysning;
    }

}
