package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

import java.util.Set;
import java.util.function.Function;

public class FerdigbehandlingKontrollsett {

    public static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentRegelsettForVedtak(Sakstyper sakstype,
                                                                                                   boolean harRegistreringUnntakFraMedlemskapFlyt,
                                                                                                   boolean harIkkeYrkesaktivFlyt) {
        if (harRegistreringUnntakFraMedlemskapFlyt) {
            return REGELSETT_UNNTAKSREGISTRERING;
        }
        if (harIkkeYrkesaktivFlyt) {
            return REGELSETT_IKKE_YRKESAKTIV;
        }
        return switch (sakstype) {
            case EU_EOS -> REGELSETT_EU_EOS;
            case FTRL -> REGELSETT_FTRL;
            case TRYGDEAVTALE -> REGELSETT_TRYGDEAVTALER;
        };
    }

    public static Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> hentRegelsettForAvslagOgHenleggelse() {
        return REGELSETT_AVSLAG_HENLEGGELSE;
    }

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_EU_EOS = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendePeriode,
        FerdigbehandlingKontroll::periodeOver24Mnd,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::arbeidsstedManglerFelter,
        FerdigbehandlingKontroll::foretakUtlandManglerFelter,
        FerdigbehandlingKontroll::orgnrErOpphørt
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_FTRL = Set.of(
            FerdigbehandlingKontroll::adresseRegistrert,
            FerdigbehandlingKontroll::overlappendeMedlemskapsperioder
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_TRYGDEAVTALER = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendePeriode,
        FerdigbehandlingKontroll::periodeOver12Måneder,
        FerdigbehandlingKontroll::periodeOverTreÅr,
        FerdigbehandlingKontroll::periodeOverFemÅr,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::arbeidsstedManglerFelter,
        FerdigbehandlingKontroll::representantIUtlandetMangler
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_UNNTAKSREGISTRERING = Set.of(
        FerdigbehandlingKontroll::overlappendeLovvalgsperiode,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::overlappendeUnntaksperiode
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_IKKE_YRKESAKTIV = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendeLovvalgsperiode,
        FerdigbehandlingKontroll::periodeManglerSluttdato
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_AVSLAG_HENLEGGELSE = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert
    );
}
