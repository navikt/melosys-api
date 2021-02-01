package no.nav.melosys.domain.behandlingsgrunnlag.data;

public class LuftfartBase {
    public String hjemmebaseNavn;
    public String hjemmebaseLand;
    public String typeFlyvninger;

    public LuftfartBase() {
    }

    public LuftfartBase(String hjemmebaseNavn, String hjemmebaseLand, String typeFlyvninger) {
        this.hjemmebaseNavn = hjemmebaseNavn;
        this.hjemmebaseLand = hjemmebaseLand;
        this.typeFlyvninger = typeFlyvninger;
    }
}
