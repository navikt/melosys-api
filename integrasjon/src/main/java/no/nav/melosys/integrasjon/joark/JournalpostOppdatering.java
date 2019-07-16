package no.nav.melosys.integrasjon.joark;

import java.util.List;

public final class JournalpostOppdatering {
    private final Long gsakSaksnummer;
    private final String brukerID;
    private final String avsenderID;
    private final String avsenderNavn;
    private final String tittel;
    private final List<String> vedleggTittelListe;
    // Om dokumentkategori skal oppdatteres med standardverdi "IS", Ikke tolkbart skjema
    private final boolean medDokumentkategori;

    public static class Builder {
        private Long gsakSaksnummer;
        private String brukerID;
        private String avsenderID;
        private String avsenderNavn;
        private String tittel;
        private List<String> vedleggTittelListe;
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

        public Builder medVedleggTittelListe(List<String> vedleggTittelListe) {
            this.vedleggTittelListe = vedleggTittelListe;
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
        this.vedleggTittelListe = builder.vedleggTittelListe;
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

    public List<String> getVedleggTittelListe() {
        return vedleggTittelListe;
    }

    public boolean isMedDokumentkategori() {
        return medDokumentkategori;
    }
}
