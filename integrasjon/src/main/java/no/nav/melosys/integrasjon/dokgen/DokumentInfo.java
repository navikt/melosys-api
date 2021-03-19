package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.joark.DokumentKategoriKode;

public class DokumentInfo {
    private final String dokgenMalnavn;
    private final DokumentKategoriKode dokumentKategori;
    private final String journalføringsTittel;

    public DokumentInfo(String dokgenMalnavn, DokumentKategoriKode dokumentKategori, String journalføringsTittel) {
        this.dokgenMalnavn = dokgenMalnavn;
        this.dokumentKategori = dokumentKategori;
        this.journalføringsTittel = journalføringsTittel;
    }

    public String getDokgenMalnavn() {
        return dokgenMalnavn;
    }

    public String getDokumentKategoriKode() {
        return dokumentKategori.getKode();
    }

    public String getJournalføringsTittel() {
        return journalføringsTittel;
    }
}
