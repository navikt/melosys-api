package no.nav.melosys.exception.validering;

import java.util.List;

public class KontrollfeilDto {
    private String kode;
    private List<String> felter;

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    public List<String> getFelter() {
        return felter;
    }

    public void setFelter(List<String> felter) {
        this.felter = felter;
    }
}
