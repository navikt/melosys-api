package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.util.List;

public class Dokument {

    private String tittel;
    private String brevkode;
    private String dokumentKategori;
    private List<DokumentVariant> dokumentvarianter;

    public Dokument(String tittel, String sedType, String dokumentKategori, List<DokumentVariant> dokumentvarianter) {
        this.tittel = tittel;
        this.brevkode = sedType;
        this.dokumentKategori = dokumentKategori;
        this.dokumentvarianter = dokumentvarianter;
    }

    public Dokument() {
    }

    public static DokumentBuilder builder() {
        return new DokumentBuilder();
    }

    public String getTittel() {
        return this.tittel;
    }

    public String getBrevkode() {
        return this.brevkode;
    }

    public String getDokumentKategori() {
        return this.dokumentKategori;
    }

    public List<DokumentVariant> getDokumentvarianter() {
        return this.dokumentvarianter;
    }

    public static class DokumentBuilder {
        private String tittel;
        private String brevkode;
        private String dokumentKategori;
        private List<DokumentVariant> dokumentvarianter;

        DokumentBuilder() {
        }

        public Dokument.DokumentBuilder tittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public Dokument.DokumentBuilder brevkode(String brevkode) {
            this.brevkode = brevkode;
            return this;
        }

        public Dokument.DokumentBuilder dokumentKategori(String dokumentKategori) {
            this.dokumentKategori = dokumentKategori;
            return this;
        }

        public Dokument.DokumentBuilder dokumentvarianter(List<DokumentVariant> dokumentvarianter) {
            this.dokumentvarianter = dokumentvarianter;
            return this;
        }

        public Dokument build() {
            return new Dokument(tittel, brevkode, dokumentKategori, dokumentvarianter);
        }
    }
}
