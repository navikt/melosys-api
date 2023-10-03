package no.nav.melosys.domain.dokument.inntekt.tillegsinfo;

import javax.validation.constraints.NotNull;

public class Tilleggsinformasjon {

    @NotNull
    public String kategori;

    public TilleggsinformasjonDetaljer tilleggsinformasjonDetaljer;
}
