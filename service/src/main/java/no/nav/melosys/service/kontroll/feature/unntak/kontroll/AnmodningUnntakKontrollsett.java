package no.nav.melosys.service.kontroll.feature.unntak.kontroll;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.service.kontroll.feature.arbeidutland.kontroll.ArbeidUtlandKontroll;
import no.nav.melosys.service.kontroll.feature.unntak.data.AnmodningUnntakKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

public class AnmodningUnntakKontrollsett {

    public static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> hentRegler() {
        return REGLER_ANMODNING_UNNTAK;
    }

    private static Set<Function<AnmodningUnntakKontrollData, Kontrollfeil>> REGLER_ANMODNING_UNNTAK = Set.of(
        AnmodningUnntakKontroll::harRegistrertAdresse,
        AnmodningUnntakKontroll::anmodningsperiodeManglerSluttdato,
        AnmodningUnntakKontroll::kunEnArbeidsgiver,
        kontrollData -> ArbeidUtlandKontroll.arbeidsstedManglerFelter(kontrollData.behandlingsgrunnlagData()),
        kontrollData -> ArbeidUtlandKontroll.foretakUtlandManglerFelter(kontrollData.behandlingsgrunnlagData())
    );
}
