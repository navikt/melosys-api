package no.nav.melosys.service.kontroll.feature.ferdigbehandling.kontroll;

import java.util.Set;
import java.util.function.Function;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.FerdigbehandlingKontrollData;
import no.nav.melosys.service.validering.Kontrollfeil;

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
        FerdigbehandlingKontroll::arbeidsstedLandManglerFelter,
        FerdigbehandlingKontroll::arbeidsstedMaritimtManglerFelter,
        FerdigbehandlingKontroll::arbeidsstedOffshoreManglerFelter,
        FerdigbehandlingKontroll::arbeidsstedLuftfartManglerFelter,
        FerdigbehandlingKontroll::foretakUtlandManglerFelter,
        FerdigbehandlingKontroll::selvstendigUtlandManglerFelter,
        FerdigbehandlingKontroll::orgnrErOpphørt,
        FerdigbehandlingKontroll::åpentUtkastFinnes
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_FTRL = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendePeriode,
        FerdigbehandlingKontroll::åpentUtkastFinnes
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_TRYGDEAVTALER = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendePeriode,
        FerdigbehandlingKontroll::periodeOver12Måneder,
        FerdigbehandlingKontroll::periodeOverTreÅr,
        FerdigbehandlingKontroll::periodeOverFemÅr,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::arbeidsstedLandManglerFelter,
        FerdigbehandlingKontroll::representantIUtlandetMangler,
        FerdigbehandlingKontroll::åpentUtkastFinnes
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_UNNTAKSREGISTRERING = Set.of(
        FerdigbehandlingKontroll::overlappendeMedlemskapsperiode,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::overlappendeUnntaksperiode,
        FerdigbehandlingKontroll::åpentUtkastFinnes
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_IKKE_YRKESAKTIV = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::overlappendePeriode,
        FerdigbehandlingKontroll::periodeManglerSluttdato,
        FerdigbehandlingKontroll::åpentUtkastFinnes
    );

    private static final Set<Function<FerdigbehandlingKontrollData, Kontrollfeil>> REGELSETT_AVSLAG_HENLEGGELSE = Set.of(
        FerdigbehandlingKontroll::adresseRegistrert,
        FerdigbehandlingKontroll::åpentUtkastFinnes
    );
}
