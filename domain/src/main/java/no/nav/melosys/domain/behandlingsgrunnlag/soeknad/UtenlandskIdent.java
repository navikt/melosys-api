package no.nav.melosys.domain.behandlingsgrunnlag.soeknad;

import java.util.Objects;

public class UtenlandskIdent {
    public String ident;
    public String landkode;

    public UtenlandskIdent() {
    }

    public UtenlandskIdent(String ident, String landkode) {
        this.ident = ident;
        this.landkode = landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtenlandskIdent that = (UtenlandskIdent) o;
        return ident.equals(that.ident) &&
            landkode.equals(that.landkode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ident, landkode);
    }
}
