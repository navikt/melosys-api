package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

public class FerdigbehandlingKontrollsett {

    public static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentRegelsettForVedtak(Sakstyper sakstype,
                                                                                                   Behandlingstema behandlingstema) {
        return switch (sakstype) {
            case EU_EOS -> REGELSETT_EU_EOS;
            case FTRL -> Collections.emptySet();
            case TRYGDEAVTALE -> {
                if (behandlingstema.equals(Behandlingstema.REGISTRERING_UNNTAK)) {
                    yield REGELSETT_TRYGDEAVTALE_UNNTAK;
                }
                yield REGELSETT_TRYGDEAVTALER;
            }
        };
    }
    public static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentRegelsettForAvslagOgHenleggelse() {
        return REGELSETT_AVSLAG_HENLEGGELSE;
    }

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_EU_EOS = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendeMedlemsperiode,
        FerdigbehandlingKontroll::periodeOver24Mnd,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::arbeidsstedManglerFelter,
        FerdigbehandlingKontroll::foretakUtlandManglerFelter,
        FerdigbehandlingKontroll::orgnrErOpphørt
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_TRYGDEAVTALER = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendeMedlemsperiode,
        FerdigbehandlingKontroll::periodeOver12Måneder,
        FerdigbehandlingKontroll::periodeOverTreÅr,
        FerdigbehandlingKontroll::periodeOverFemÅr,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::arbeidsstedManglerFelter,
        FerdigbehandlingKontroll::representantIUtlandetMangler
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_TRYGDEAVTALE_UNNTAK = Set.of(
        FerdigbehandlingKontroll::overlappendeMedlemsperiode,
        FerdigbehandlingKontroll::periodeManglerSluttdato
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_AVSLAG_HENLEGGELSE = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert
    );
}
