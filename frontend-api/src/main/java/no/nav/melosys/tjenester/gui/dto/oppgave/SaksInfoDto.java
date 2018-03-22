package no.nav.melosys.tjenester.gui.dto.oppgave;

public class SaksInfoDto {
    private String kode ;
    private String term;

    public SaksInfoDto(String kode, String term) {
        this.kode = kode;
        this.term = term;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }
}
