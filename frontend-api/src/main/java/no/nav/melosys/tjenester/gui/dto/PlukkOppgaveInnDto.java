package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

public class PlukkOppgaveInnDto {

    public List<String> fagområdeKodeListe;
    private String underkategori;
    private List<String> oppgavetypeListe;

    public List<String> getFagområdeKodeListe() {
        return fagområdeKodeListe;
    }

    public void setFagområdeKodeListe(List<String> fagområdeKodeListe) {
        this.fagområdeKodeListe = fagområdeKodeListe;
    }

    public String getUnderkategori() {
        return underkategori;
    }

    public void setUnderkategori(String underkategori) {
        this.underkategori = underkategori;
    }

    public List<String> getOppgavetypeListe() {
        return oppgavetypeListe;
    }

    public void setOppgavetypeListe(List<String> oppgavetypeListe) {
        this.oppgavetypeListe = oppgavetypeListe;
    }
}
