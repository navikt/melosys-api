package no.nav.melosys.service.kontroll.regler;

import java.util.function.Predicate;
import java.util.regex.Pattern;

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
        arbeidssted -> StringUtils.equals(arbeidssted.adresse.land, Landkoder.SJ.getKode())
            || StringUtils.equals(arbeidssted.adresse.land, Landkoder.NO.getKode()) && BYER_FRA_SVALBARD_PATTERN.matcher(arbeidssted.adresse.by).find();


    public static boolean representantIUtlandetMangler(RepresentantIUtlandet representantIUtlandet) {
        return representantIUtlandet == null || representantIUtlandet.representantNavn == null;
    }

    public static boolean erArbeidsstedFraSvalbardOgJanMayen(SedDokument sedDokument) {
        return sedDokument.getArbeidssteder().stream().anyMatch(ARBEIDSSTED_SVALBARD_JAN_MAIEN);
    }
}
