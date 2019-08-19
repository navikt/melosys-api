package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_engelsk;

public final class VilkaarsresultatTilBegrunnelseMapper {

    private VilkaarsresultatTilBegrunnelseMapper() {
        throw new IllegalStateException("Utility");
    }

    public static String tilEngelskBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {
        return vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(VilkaarsresultatTilBegrunnelseMapper::getEngelskBeskrivelse)
            .filter(Objects::nonNull)
            .map(begrunnelse -> tilFritekst(begrunnelse, vilkaarsresultat.getBegrunnelseFritekst()))
            .collect(Collectors.joining("\n"));
    }

    private static String getEngelskBeskrivelse(String kode) {
        if (isArt16_1_Anmodning_Begrunnelse(kode)) {
            return Art16_1_anmodning_engelsk.valueOf(kode).getBeskrivelse();
        }
        return null;
    }

    private static String tilFritekst(String begrunnelse, String fritekst) {
        if (Art16_1_anmodning.SAERLIG_GRUNN.getBeskrivelse().equals(begrunnelse)
            || Art16_1_anmodning_engelsk.SAERLIG_GRUNN.getBeskrivelse().equals(begrunnelse)) {
            return fritekst;
        }

        return begrunnelse;
    }

    private static boolean isArt16_1_Anmodning_Begrunnelse(String kode) {
        return Arrays.stream(Art16_1_anmodning.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }
}
