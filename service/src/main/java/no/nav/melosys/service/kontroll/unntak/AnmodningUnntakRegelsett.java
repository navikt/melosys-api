package no.nav.melosys.service.kontroll.unntak;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.service.kontroll.arbeidutland.ArbeidUtlandKontroller;
import no.nav.melosys.service.validering.Kontrollfeil;

public class AnmodningUnntakRegelsett {

    static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> hentRegler() {
        return REGLER_ANMODNING_UNNTAK;
    }

    private static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> REGLER_ANMODNING_UNNTAK = Set.of(
        AnmodningUnntakKontroller::harRegistrertAdresse,
        AnmodningUnntakKontroller::anmodningsperiodeManglerSluttdato,
        AnmodningUnntakKontroller::kunEnArbeidsgiver,
        kontrollData -> ArbeidUtlandKontroller.arbeidsstedManglerFelter(kontrollData.getBehandlingsgrunnlagData()),
        kontrollData -> ArbeidUtlandKontroller.foretakUtlandManglerFelter(kontrollData.getBehandlingsgrunnlagData())
    );
}
