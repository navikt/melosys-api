package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Art12_1_Begrunnelser;
import no.nav.melosys.domain.kodeverk.Art16_1_Anmodning_Begrunnelser;

public final class VilkaarsresultatTilBegrunnelseMapper {

    private VilkaarsresultatTilBegrunnelseMapper() {
        throw new IllegalStateException("Utility");
    }

    public static String mapVilkaarsresultatTilBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {

        List<String> begrunnelser = vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(VilkaarsresultatTilBegrunnelseMapper::getBeskrivelse)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // Særlig grunn blir byttet ut med verdi i fritekst-felt
        begrunnelser.replaceAll(begrunnelse -> {
            if (begrunnelse.equals(Art16_1_Anmodning_Begrunnelser.SAERLIG_GRUNN.getBeskrivelse())) {
                return vilkaarsresultat.getBegrunnelseFritekst();
            }

            return begrunnelse;
        });

        return String.join("\n", begrunnelser);
    }

    private static String getBeskrivelse(String kode) {
        if (isArt12_1_Begrunnelse(kode)) {
            return Art12_1_Begrunnelser.valueOf(kode).getBeskrivelse();
        } else if (isArt16_1_Anmodning_Begrunnelse(kode)) {
            return Art16_1_Anmodning_Begrunnelser.valueOf(kode).getBeskrivelse();
        } else {
            return null;
        }
    }

    private static boolean isArt12_1_Begrunnelse(String kode) {
        return Arrays.stream(Art12_1_Begrunnelser.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }

    private static boolean isArt16_1_Anmodning_Begrunnelse(String kode) {
        return Arrays.stream(Art16_1_Anmodning_Begrunnelser.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }
}
