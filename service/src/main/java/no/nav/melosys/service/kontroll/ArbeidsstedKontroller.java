package no.nav.melosys.service.kontroll;

import java.util.function.Predicate;

import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.RepresentantIUtlandet;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public final class ArbeidsstedKontroller {
    private static final String[] BYER_FRA_SVALBARD = {"Ny-Ålesund", "Svalbard", "Sveagruva", "Hopen", "Bjørnøya", "Spitsbergen", "Longyearbyen"};

    private ArbeidsstedKontroller() {
    }

    public static boolean representantIUtlandetManglerFelter(RepresentantIUtlandet representantIUtlandet) {
        return representantIUtlandet == null || representantIUtlandet.representantNavn == null;
    }

    public static boolean arbeidstedSvalbardOgJanMayen(SedDokument sedDokument) {
        return sedDokument.getArbeidssteder().stream().anyMatch(ARBEIDSSTED_SJ);
    }

    private static Predicate<Arbeidssted> ARBEIDSSTED_SJ = arbeidssted -> StringUtils.equals(arbeidssted.adresse.land, Landkoder.SJ.getKode())
        || containsAnyIgnoreCase(arbeidssted.adresse.by, BYER_FRA_SVALBARD);

    private static boolean containsAnyIgnoreCase(final CharSequence cs, final CharSequence... searchCharSequences) {
        if (StringUtils.isEmpty(cs) || ArrayUtils.isEmpty(searchCharSequences)) {
            return false;
        }
        for (final CharSequence searchCharSequence : searchCharSequences) {
            if (StringUtils.containsIgnoreCase(cs, searchCharSequence)) {
                return true;
            }
        }
        return false;
    }
}
