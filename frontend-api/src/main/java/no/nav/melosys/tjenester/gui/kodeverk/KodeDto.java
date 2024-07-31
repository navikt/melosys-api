package no.nav.melosys.tjenester.gui.kodeverk;

import java.util.List;

import no.nav.melosys.integrasjon.kodeverk.Kode;

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

    public static List<KodeDto> tilKodeDto(List<Kode> kodeliste) {
        return kodeliste.stream().map(kode -> new KodeDto(kode.kode, kode.navn)).toList();
    }
}
