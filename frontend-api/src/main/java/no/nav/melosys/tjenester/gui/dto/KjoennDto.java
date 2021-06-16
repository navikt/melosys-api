package no.nav.melosys.tjenester.gui.dto;

public final class KjoennDto {
    private final String kode;
    private final String term;

    public KjoennDto(String kode, String term) {
        this.kode = kode;
        this.term = term;
    }

    public String getKode() {
        return kode;
    }

    public String getTerm() {
        return term;
    }
}
