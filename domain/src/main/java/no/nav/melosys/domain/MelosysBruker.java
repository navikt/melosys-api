package no.nav.melosys.domain;

import java.util.Collection;

public class MelosysBruker {

    private final String ident;
    private final String navn;
    private final Collection<String> grupper;

    public MelosysBruker(String ident, String navn, Collection<String> grupper) {
        this.ident = ident;
        this.navn = navn;
        this.grupper = grupper;
    }

    public String getIdent() {
        return ident;
    }

    public String getNavn() {
        return navn;
    }

    public Collection<String> getGrupper() {
        return grupper;
    }
}
