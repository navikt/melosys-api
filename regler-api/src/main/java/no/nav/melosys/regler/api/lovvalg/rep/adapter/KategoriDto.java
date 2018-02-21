package no.nav.melosys.regler.api.lovvalg.rep.adapter;

import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;

public class KategoriDto {
    public Alvorlighetsgrad alvorlighetsgrad;
    public String melding;

    // Brukes av JAXB
    public KategoriDto() {}

    public KategoriDto(Alvorlighetsgrad alvorlighetsgrad, String melding) {
        this.alvorlighetsgrad = alvorlighetsgrad;
        this.melding = melding;
    }
}
