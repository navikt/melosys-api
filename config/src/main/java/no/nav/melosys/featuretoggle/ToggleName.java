package no.nav.melosys.featuretoggle;

public enum ToggleName {
    BEHANDLINGSTYPE_KLAGE("melosys.behandlingstype.klage");

    ToggleName(String name) {
        this.name = name;
    }

    public final String name;
}
