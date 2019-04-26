package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Art16_1_Anmodning_Begrunnelser;
import no.nav.melosys.domain.kodeverk.Art16_1_Anmodning_Begrunnelser_Engelsk;

public final class VilkaarsresultatTilBegrunnelseMapper {

    private VilkaarsresultatTilBegrunnelseMapper() {
        throw new IllegalStateException("Utility");
    }

    public static String tilEngelskBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {
        return getVilkaarBegrunnelseKoder(vilkaarsresultat)
            .map(VilkaarsresultatTilBegrunnelseMapper::getEngelskBeskrivelse)
            .filter(Objects::nonNull)
            .map(begrunnelse -> tilFritekst(begrunnelse, vilkaarsresultat.getBegrunnelseFritekst()))
            .collect(Collectors.joining("\n"));
    }

    private static Stream<String> getVilkaarBegrunnelseKoder(Vilkaarsresultat vilkaarsresultat) {
        return vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode);
    }

    private static String getEngelskBeskrivelse(String kode) {
        if (isArt16_1_Anmodning_Begrunnelse(kode)) {
            return Art16_1_Anmodning_Begrunnelser_Engelsk.valueOf(kode).getBeskrivelse();
        }
        return null;
    }

    private static String tilFritekst(String begrunnelse, String fritekst) {
        if (Art16_1_Anmodning_Begrunnelser.SAERLIG_GRUNN.getBeskrivelse().equals(begrunnelse)
            || Art16_1_Anmodning_Begrunnelser_Engelsk.SAERLIG_GRUNN.getBeskrivelse().equals(begrunnelse)) {
            return fritekst;
        }

        return begrunnelse;
    }

    private static boolean isArt16_1_Anmodning_Begrunnelse(String kode) {
        return Arrays.stream(Art16_1_Anmodning_Begrunnelser.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }
}
