package no.nav.melosys.integrasjon.joark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JournalpostOppdatering {
    private final Long gsakSaksnummer;
    private final String brukerID;
    private final String avsenderID;
    private final String avsenderNavn;
    private final String tittel;
    private final Map<String, String> fysiskeVedlegg;
    private final List<String> logiskeVedleggTitler;
    // Om dokumentkategori skal oppdatteres med standardverdi "IS", Ikke tolkbart skjema
    private final boolean medDokumentkategori;

    public static class Builder {
        private Long gsakSaksnummer;
        private String brukerID;
        private String avsenderID;
        private String avsenderNavn;
        private String tittel;
        private Map<String, String> fysiskeVedlegg = new HashMap<>();
        private List<String> logiskeVedleggTitler = new ArrayList<>();
        private boolean medDokumentkategori;

        public Builder medGsakSaksnummer(Long gsakSaksnummer) {
            this.gsakSaksnummer = gsakSaksnummer;
            return this;
        }

        public Builder medBrukerID(String brukerID) {
            this.brukerID = brukerID;
            return this;
        }

        public Builder medAvsenderID(String avsenderID) {
            this.avsenderID = avsenderID;
            return this;
        }

        public Builder medAvsenderNavn(String avsenderNavn) {
            this.avsenderNavn = avsenderNavn;
            return this;
        }

        public Builder medTittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public Builder medFysiskeVedlegg(Map<String, String> fysiskeVedlegg) {
            this.fysiskeVedlegg = fysiskeVedlegg;
            return this;
        }

        public Builder medLogiskeVedleggTitler(List<String> logiskeVedleggTitler) {
            this.logiskeVedleggTitler = logiskeVedleggTitler;
            return this;
        }

        public Builder medDokumentkategori(boolean medDokumentkategori) {
            this.medDokumentkategori = medDokumentkategori;
            return this;
        }

        public JournalpostOppdatering build() {
            return new JournalpostOppdatering(this);
        }
    }

    private JournalpostOppdatering(Builder builder) {
        this.gsakSaksnummer = builder.gsakSaksnummer;
        this.brukerID = builder.brukerID;
        this.avsenderID = builder.avsenderID;
        this.avsenderNavn = builder.avsenderNavn;
        this.tittel = builder.tittel;
        this.fysiskeVedlegg = builder.fysiskeVedlegg;
        this.logiskeVedleggTitler = builder.logiskeVedleggTitler;
        this.medDokumentkategori = builder.medDokumentkategori;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public String getTittel() {
        return tittel;
    }

    Map<String, String> getFysiskeVedlegg() {
        return fysiskeVedlegg;
    }

    List<String> getLogiskeVedleggTitler() {
        return logiskeVedleggTitler;
    }

    public boolean isMedDokumentkategori() {
        return medDokumentkategori;
    }
}
