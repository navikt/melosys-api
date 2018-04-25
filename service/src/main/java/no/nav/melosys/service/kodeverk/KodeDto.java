package no.nav.melosys.service.kodeverk;

public class KodeDto {
    private String kode;
    private String term;

    public KodeDto(String kode, String term) {
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
