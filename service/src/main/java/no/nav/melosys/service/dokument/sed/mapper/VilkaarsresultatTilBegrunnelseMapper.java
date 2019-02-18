package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Art16_1_Avslag__Begrunnelser;

public class VilkaarsresultatTilBegrunnelseMapper {

    public static String mapVilkaarsresultatTilBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {

        Set<String> begrunnelser = vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(kode -> Art16_1_Avslag__Begrunnelser.valueOf(kode).getBeskrivelse())
            .collect(Collectors.toSet());

        if (begrunnelser.contains(Art16_1_Avslag__Begrunnelser.SAERLIG_AVSLAGSGRUNN.getBeskrivelse())) {
            return vilkaarsresultat.getBegrunnelseFritekst();
        }

        return String.join("\n", begrunnelser);
    }
}
