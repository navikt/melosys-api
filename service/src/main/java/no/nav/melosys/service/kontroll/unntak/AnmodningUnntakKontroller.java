package no.nav.melosys.service.kontroll.unntak;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.regler.PersonRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public class AnmodningUnntakKontroller {

    static Kontrollfeil harRegistrertAdresse(AnmodningUnntakKontrollData kontrollData) {
        return PersonRegler.harRegistrertAdresse(kontrollData.getPersonDokument(), kontrollData.getBehandlingsgrunnlagData())
            ? null : new Kontrollfeil(Kontroll_begrunnelser.MANGLENDE_REGISTRERTE_ADRESSE);
    }

    static Kontrollfeil anmodningsperiodeManglerSluttdato(AnmodningUnntakKontrollData kontrollData) {
        return kontrollData.getAnmodningsperiode().getTom() == null
            ? new Kontrollfeil(Kontroll_begrunnelser.INGEN_SLUTTDATO) : null;
    }

    static Kontrollfeil kunEnArbeidsgiver(AnmodningUnntakKontrollData kontrollData) {
        return (kontrollData.getAntallArbeidsgivere() != 1)
            ? new Kontrollfeil(Kontroll_begrunnelser.IKKE_KUN_EN_VIRKSOMHET) : null;
    }
}
