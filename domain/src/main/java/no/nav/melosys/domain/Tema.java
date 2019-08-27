package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum Tema implements Kodeverk {
    MED("MED"), // Medlemskap
    TRY("TRY"), // Trygdeavgift
    UFM("UFM"); // Unntak fra medlemskap

    private final String kode;

    Tema(String kode) {
        this.kode = kode;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return null;
    }
}
