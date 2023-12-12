package no.nav.melosys.exception.validering;

import java.util.List;

import exception.KontrolldataFeilType;

public class KontrollfeilDto {
    private String kode;
    private List<String> felter;
    private KontrolldataFeilType type;

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

    public KontrolldataFeilType getType() {
        return type;
    }

    public void setType(KontrolldataFeilType type) {
        this.type = type;
    }
}
