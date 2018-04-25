package no.nav.melosys.domain;

public enum  VedleggTittel implements Kodeverk {

    // FIXME venter på definisjon
    TODO_1("TITTEL_1", "Vedleggstittel 1"),
    TODO_2("TITTEL_2", "Vedleggstittel 2"),
    ANNET("ANNET", "Annet...");

    private String kode;
    private String beskrivelse;

    private VedleggTittel(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }
}
