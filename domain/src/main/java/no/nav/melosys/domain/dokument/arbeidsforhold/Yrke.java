package no.nav.melosys.domain.dokument.arbeidsforhold;

public class Yrke {

    private String kode;
    private String term;

    // Brukes av JAXB
    public Yrke() {}

    public Yrke(String yrkeKode) {
        this.kode = yrkeKode;
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
