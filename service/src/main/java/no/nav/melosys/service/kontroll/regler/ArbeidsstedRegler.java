package no.nav.melosys.service.kontroll.regler;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import no.nav.melosys.domain.eessi.melding.Arbeidsland;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.RepresentantIUtlandet;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.melding.Arbeidssted;
import no.nav.melosys.domain.kodeverk.Landkoder;
import org.apache.commons.lang3.StringUtils;

public final class ArbeidsstedRegler {

    private ArbeidsstedRegler() {
    }

    private static final String BYER_FRA_SVALBARD_REGEX =
        "(Ny-Ålesund)|(Ny-Aalesund)|(Ny-Alesund)|(Ny Ålesund)|(Ny Aalesund)|(Ny Alesund)|(Svalbard)|(Sveagruva)|" +
            "(Bjørnøya)|(Bjoernoya)|(Bjornoya)|(Spitsbergen)|(Longyearbyen)|(\\bHopen\\b)";

    private static final Pattern BYER_FRA_SVALBARD_PATTERN = Pattern.compile(BYER_FRA_SVALBARD_REGEX,
                                                                             Pattern.CASE_INSENSITIVE | Pattern.CANON_EQ | Pattern.UNICODE_CASE);

    private static final Predicate<Arbeidssted> ARBEIDSSTED_SVALBARD_JAN_MAIEN =
        arbeidssted -> StringUtils.equals(arbeidssted.getAdresse().getLand(), Landkoder.SJ.getKode())
            || StringUtils.equals(arbeidssted.getAdresse().getLand(), Landkoder.NO.getKode()) && BYER_FRA_SVALBARD_PATTERN.matcher(arbeidssted.getAdresse().getBy()).find();

    private static final Predicate<Arbeidsland> ARBEIDSLAND_ARBEIDSSTED_SVALBARD_JAN_MAIEN =
        arbeidsland -> StringUtils.equals(arbeidsland.getLand(), Landkoder.SJ.getKode())
            || StringUtils.equals(arbeidsland.getLand(), Landkoder.NO.getKode()) && arbeidsland.getArbeidssted().stream().anyMatch(arbeidssted ->
            BYER_FRA_SVALBARD_PATTERN.matcher(arbeidssted.getAdresse().getBy()).find());

    public static boolean representantIUtlandetMangler(RepresentantIUtlandet representantIUtlandet) {
        return representantIUtlandet == null || representantIUtlandet.getRepresentantNavn() == null;
    }

    public static boolean erArbeidsstedFraSvalbardOgJanMayen(SedDokument sedDokument) {
        return sedDokument.getArbeidssteder().stream().anyMatch(ARBEIDSSTED_SVALBARD_JAN_MAIEN);
    }


    public static boolean erArbeidsstedFraSvalbardOgJanMayen4_3(SedDokument sedDokument) {
        return sedDokument.getArbeidsland().stream().anyMatch(ARBEIDSLAND_ARBEIDSSTED_SVALBARD_JAN_MAIEN);
    }
}
