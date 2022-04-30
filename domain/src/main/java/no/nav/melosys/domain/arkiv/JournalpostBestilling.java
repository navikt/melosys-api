package no.nav.melosys.domain.arkiv;

import java.util.List;

public final class JournalpostBestilling {
    private final String tittel;
    private final String brevkode;
    private final String dokumentKategori;
    private final String brukerFnr;
    private final String mottakerNavn;
    private final String mottakerId;
    private final OpprettJournalpost.KorrespondansepartIdType mottakerIdType;
    private final String saksnummer;
    private final byte[] pdf;
    private final List<byte[]> vedlegg;

    private JournalpostBestilling(Builder builder) {
        this.tittel = builder.tittel;
        this.brevkode = builder.brevkode;
        this.dokumentKategori = builder.dokumentKategori;
        this.brukerFnr = builder.brukerFnr;
        this.mottakerNavn = builder.mottakerNavn;
        this.mottakerId = builder.mottakerId;
        this.mottakerIdType = builder.mottakerIdType;
        this.saksnummer = builder.saksnummer;
        this.pdf = builder.pdf;
        this.vedlegg = builder.vedlegg;
    }

    public String getTittel() {
        return tittel;
    }

    public String getBrevkode() {
        return brevkode;
    }

    public String getDokumentKategori() {
        return dokumentKategori;
    }

    public String getBrukerFnr() {
        return brukerFnr;
    }

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getMottakerId() {
        return mottakerId;
    }

    public OpprettJournalpost.KorrespondansepartIdType getMottakerIdType() {
        return mottakerIdType;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public byte[] getPdf() {
        return pdf;
    }

    public List<byte[]> getVedlegg() {
        return vedlegg;
    }

    public static class Builder {
        private String tittel;
        private String brevkode;
        private String dokumentKategori;
        private String brukerFnr;
        private String mottakerNavn;
        private String mottakerId;
        private OpprettJournalpost.KorrespondansepartIdType mottakerIdType;
        private String saksnummer;
        private byte[] pdf;
        private List<byte[]> vedlegg;

        public Builder medTittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public Builder medBrevkode(String brevkode) {
            this.brevkode = brevkode;
            return this;
        }

        public Builder medDokumentKategori(String dokumentKategori) {
            this.dokumentKategori = dokumentKategori;
            return this;
        }

        public Builder medBrukerFnr(String brukerFnr) {
            this.brukerFnr = brukerFnr;
            return this;
        }

        public Builder medMottakerNavn(String mottakerNavn) {
            this.mottakerNavn = mottakerNavn;
            return this;
        }

        public Builder medMottakerId(String mottakerId) {
            this.mottakerId = mottakerId;
            return this;
        }

        public Builder medMottakerIdType(OpprettJournalpost.KorrespondansepartIdType mottakerIdType) {
            this.mottakerIdType = mottakerIdType;
            return this;
        }

        public Builder medSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder medPdf(byte[] pdf) {
            this.pdf = pdf;
            return this;
        }

        public Builder medVedlegg(List<byte[]> vedlegg) {
            this.vedlegg = vedlegg;
            return this;
        }

        public JournalpostBestilling build() {
            return new JournalpostBestilling(this);
        }
    }
}
