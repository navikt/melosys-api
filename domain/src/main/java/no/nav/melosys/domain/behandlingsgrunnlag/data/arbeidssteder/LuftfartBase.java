package no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder;

import no.nav.melosys.domain.kodeverk.Flyvningstyper;

public class LuftfartBase {
    public String hjemmebaseNavn;
    public String hjemmebaseLand;
    public Flyvningstyper typeFlyvninger;

    public LuftfartBase() {
    }

    public LuftfartBase(String hjemmebaseNavn, String hjemmebaseLand, Flyvningstyper flyvningstype) {
        this.hjemmebaseNavn = hjemmebaseNavn;
        this.hjemmebaseLand = hjemmebaseLand;
        this.typeFlyvninger = flyvningstype;
    }
}
