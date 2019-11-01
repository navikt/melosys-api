package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_engelsk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12_engelsk;

public final class VilkaarsresultatTilBegrunnelseMapper {

    private VilkaarsresultatTilBegrunnelseMapper() {
        throw new IllegalStateException("Utility");
    }

    public static String tilEngelskBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {
        return vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(VilkaarsresultatTilBegrunnelseMapper::getEngelskKodeverk)
            .filter(Objects::nonNull)
            .map(begrunnelseKode -> tilFritekst(begrunnelseKode, vilkaarsresultat.getBegrunnelseFritekst()))
            .collect(Collectors.joining("\n"));
    }

    private static Kodeverk getEngelskKodeverk(String begrunnelseKode) {
        if (isArt16_1_Anmodning_Begrunnelse(begrunnelseKode)) {
            return Art16_1_anmodning_engelsk.valueOf(begrunnelseKode);
        } else if (isArt16_1_Anmodning_UtenArt12_Begrunnelse(begrunnelseKode)) {
            return Art16_1_anmodning_uten_art12_engelsk.valueOf(begrunnelseKode);
        }
        return null;
    }

    private static String tilFritekst(Kodeverk begrunnelseKode, String fritekst) {
        final Collection<Kodeverk> særligGrunnKoder = Arrays.asList(
            Art16_1_anmodning.SAERLIG_GRUNN,
            Art16_1_anmodning_engelsk.SAERLIG_GRUNN,
            Art16_1_anmodning_uten_art12.SAERLIG_GRUNN,
            Art16_1_anmodning_uten_art12_engelsk.SAERLIG_GRUNN);

        if (særligGrunnKoder.contains(begrunnelseKode)) {
            return fritekst;
        }
        return begrunnelseKode.getBeskrivelse();
    }

    private static boolean isArt16_1_Anmodning_Begrunnelse(String kode) {
        return Arrays.stream(Art16_1_anmodning.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }

    private static boolean isArt16_1_Anmodning_UtenArt12_Begrunnelse(String kode) {
        return Arrays.stream(Art16_1_anmodning_uten_art12.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }
}