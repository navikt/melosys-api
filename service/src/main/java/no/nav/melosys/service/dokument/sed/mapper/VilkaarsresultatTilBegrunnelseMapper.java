package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;
import java.util.List;
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

    public static String tilBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {
        List<String> begrunnelser = getVilkaarBegrunnelseKoder(vilkaarsresultat)
            .map(VilkaarsresultatTilBegrunnelseMapper::getBeskrivelse)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        setFritekst(begrunnelser, vilkaarsresultat.getBegrunnelseFritekst());

        return String.join("\n", begrunnelser);
    }

    public static String tilEngelskBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {
        List<String> begrunnelser = getVilkaarBegrunnelseKoder(vilkaarsresultat)
            .map(VilkaarsresultatTilBegrunnelseMapper::getEngelskBeskrivelse)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        setFritekst(begrunnelser, vilkaarsresultat.getBegrunnelseFritekst());

        return String.join("\n", begrunnelser);
    }

    private static Stream<String> getVilkaarBegrunnelseKoder(Vilkaarsresultat vilkaarsresultat) {
        return vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode);
    }

    private static String getBeskrivelse(String kode) {
        if (isArt16_1_Anmodning_Begrunnelse(kode)) {
            return Art16_1_Anmodning_Begrunnelser.valueOf(kode).getBeskrivelse();
        }
        return null;
    }

    private static String getEngelskBeskrivelse(String kode) {
        if (isArt16_1_Anmodning_Begrunnelse(kode)) {
            return Art16_1_Anmodning_Begrunnelser_Engelsk.valueOf(kode).getBeskrivelse();
        }
        return null;
    }

    private static void setFritekst(List<String> begrunnelser, String fritekst) {
        begrunnelser.replaceAll(begrunnelse -> {
            if (begrunnelse.equals(Art16_1_Anmodning_Begrunnelser.SAERLIG_GRUNN.getBeskrivelse())
                || begrunnelse.equals(Art16_1_Anmodning_Begrunnelser_Engelsk.SAERLIG_GRUNN.getBeskrivelse())) {
                return fritekst;
            }

            return begrunnelse;
        });
    }

    private static boolean isArt16_1_Anmodning_Begrunnelse(String kode) {
        return Arrays.stream(Art16_1_Anmodning_Begrunnelser.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }
}
