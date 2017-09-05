package no.nav.melosys.integrasjon.kodeverk;

import java.time.LocalDate;

public class Kode {
    
    private String kode;
    private String navn;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    Kode(String kode, String navn, LocalDate gyldigFom, LocalDate gyldigTom) {
        this.kode = kode;
        this.navn = navn;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
    }
    
    public String getKode() {
        return kode;
    }
    
    public String getNavn() {
        return navn;
    }

    public LocalDate getGyldigFom() {
        return gyldigFom;
    }

    public LocalDate getGyldigTom() {
        return gyldigTom;
    }

}
