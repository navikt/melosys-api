package no.nav.melosys.service.kontroll.feature.unntak.kontroll;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.unntak.data.AnmodningUnntakKontrollData;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public class AnmodningUnntakKontroll {

    private AnmodningUnntakKontroll() {
    }

    static Kontrollfeil harRegistrertAdresse(AnmodningUnntakKontrollData kontrollData) {
        return PersonRegler.harRegistrertAdresse(kontrollData.persondata(), kontrollData.behandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    static Kontrollfeil anmodningsperiodeManglerSluttdato(AnmodningUnntakKontrollData kontrollData) {
        return kontrollData.anmodningsperiode().getTom() == null
            ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil kunEnArbeidsgiver(AnmodningUnntakKontrollData kontrollData) {
        return (kontrollData.antallArbeidsgivere() != 1)
            ? new Kontrollfeil(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET) : null;
    }
}
