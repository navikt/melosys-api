package no.nav.melosys.service.dokument.sed.mapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_engelsk_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_engelsk_begrunnelser;

public final class VilkaarsresultatTilBegrunnelseMapper {

    private VilkaarsresultatTilBegrunnelseMapper() {
        throw new IllegalStateException("Utility");
    }

    public static String tilEngelskBegrunnelseString(Vilkaarsresultat vilkaarsresultat) {
        return vilkaarsresultat.getBegrunnelser().stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(VilkaarsresultatTilBegrunnelseMapper::getEngelskKodeverk)
            .filter(Objects::nonNull)
            .map(begrunnelseKode -> tilFritekst(begrunnelseKode, vilkaarsresultat.getBegrunnelseFritekstEessi()))
            .collect(Collectors.joining("\n"));
    }

    private static Kodeverk getEngelskKodeverk(String begrunnelseKode) {
        if (isAnmodningBegrunnelse(begrunnelseKode)) {
            return Anmodning_engelsk_begrunnelser.valueOf(begrunnelseKode);
        } else if (isDirekteTilAnmodningBegrunnelse(begrunnelseKode)) {
            return Direkte_til_anmodning_engelsk_begrunnelser.valueOf(begrunnelseKode);
        }
        return null;
    }

    private static String tilFritekst(Kodeverk begrunnelseKode, String fritekst) {
        final Collection<Kodeverk> særligGrunnKoder = Arrays.asList(
            Anmodning_begrunnelser.SAERLIG_GRUNN,
            Anmodning_engelsk_begrunnelser.SAERLIG_GRUNN,
            Direkte_til_anmodning_begrunnelser.SAERLIG_GRUNN,
            Direkte_til_anmodning_engelsk_begrunnelser.SAERLIG_GRUNN);

        if (særligGrunnKoder.contains(begrunnelseKode)) {
            return fritekst;
        }
        return begrunnelseKode.getBeskrivelse();
    }

    private static boolean isAnmodningBegrunnelse(String kode) {
        return Arrays.stream(Anmodning_begrunnelser.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }

    private static boolean isDirekteTilAnmodningBegrunnelse(String kode) {
        return Arrays.stream(Direkte_til_anmodning_begrunnelser.values())
            .anyMatch(begrunnelse -> begrunnelse.getKode().equals(kode));
    }
}
