package no.nav.melosys.service.kontroll;

import java.util.Arrays;
import java.util.function.Predicate;

import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.RepresentantIUtlandet;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public final class ArbeidsstedKontroller {
    private static final String[] BYER_FRA_SVALBARD = {"Ny-Ålesund", "Ny-Alesund", "Svalbard", "Sveagruva", "Hopen",
        "Bjørnøya", "Bjornoya", "Spitsbergen", "Longyearbyen"};

    private static final Predicate<Arbeidssted> ARBEIDSSTED_SVALBARD_JAN_MAIEN =
        arbeidssted -> StringUtils.equals(arbeidssted.adresse.land, Landkoder.SJ.getKode())
            || matchAnyIgnoreCase(arbeidssted.adresse.by, BYER_FRA_SVALBARD);

    private ArbeidsstedKontroller() {
    }

    public static boolean representantIUtlandetMangler(RepresentantIUtlandet representantIUtlandet) {
        return representantIUtlandet == null || representantIUtlandet.representantNavn == null;
    }

    public static boolean arbeidstedSvalbardOgJanMayen(SedDokument sedDokument) {
        return sedDokument.getArbeidssteder().stream().anyMatch(ARBEIDSSTED_SVALBARD_JAN_MAIEN);
    }

    private static boolean matchAnyIgnoreCase(final String input, final String... searchedStrings) {
        if (StringUtils.isEmpty(input) || ArrayUtils.isEmpty(searchedStrings)) {
            return false;
        }
        return Arrays.stream(searchedStrings)
            .anyMatch(searchString -> input.trim().equalsIgnoreCase(searchString));
    }
}
