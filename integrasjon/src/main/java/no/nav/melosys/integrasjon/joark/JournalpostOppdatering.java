package no.nav.melosys.integrasjon.joark;

import java.util.*;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import org.springframework.util.CollectionUtils;

public final class JournalpostOppdatering {
    private final Long arkivSakID;
    private final String hovedDokumentID;
    private final String brukerID;
    private final String avsenderID;
    private final String avsenderNavn;
    private final Avsendertyper avsenderType;
    private final String tittel;
    private final Map<String, String> fysiskeVedlegg;
    private final List<String> logiskeVedleggTitler;
    // Om dokumentkategori skal oppdatteres med standardverdi "IS", Ikke tolkbart skjema
    private final boolean medDokumentkategori;

    public static class Builder {
        private Long arkivSakID;
        private String hovedDokumentID;
        private String brukerID;
        private String avsenderID;
        private String avsenderNavn;
        private Avsendertyper avsenderType;
        private String tittel;
        private Map<String, String> fysiskeVedlegg = new HashMap<>();
        private List<String> logiskeVedleggTitler = new ArrayList<>();
        private boolean medDokumentkategori;

        public Builder medArkivSakID(Long arkivSakID) {
            this.arkivSakID = arkivSakID;
            return this;
        }

        public Builder medHovedDokumentID(String hovedDokumentID) {
            this.hovedDokumentID = hovedDokumentID;
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

        public Builder medAvsenderType(Avsendertyper avsenderType) {
            this.avsenderType = avsenderType;
            return this;
        }

        public Builder medTittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public Builder medFysiskeVedlegg(Map<String, String> fysiskeVedlegg) {
            if (!CollectionUtils.isEmpty(fysiskeVedlegg)) {
                this.fysiskeVedlegg = fysiskeVedlegg;
            }
            return this;
        }

        public Builder medLogiskeVedleggTitler(List<String> logiskeVedleggTitler) {
            if (!CollectionUtils.isEmpty(logiskeVedleggTitler)) {
                this.logiskeVedleggTitler = logiskeVedleggTitler;
            }
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
        this.arkivSakID = builder.arkivSakID;
        this.hovedDokumentID = builder.hovedDokumentID;
        this.brukerID = builder.brukerID;
        this.avsenderID = builder.avsenderID;
        this.avsenderNavn = builder.avsenderNavn;
        this.avsenderType = builder.avsenderType;
        this.tittel = builder.tittel;
        this.fysiskeVedlegg = builder.fysiskeVedlegg;
        this.logiskeVedleggTitler = builder.logiskeVedleggTitler;
        this.medDokumentkategori = builder.medDokumentkategori;
    }

    public Long getArkivSakID() {
        return arkivSakID;
    }

    public String getHovedDokumentID() {
        return hovedDokumentID;
    }

    public String getBrukerID() {
        return brukerID;
    }

    String getAvsenderID() {
        return avsenderID;
    }

    String getAvsenderNavn() {
        return avsenderNavn;
    }

    Avsendertyper getAvsenderType() {
        return avsenderType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JournalpostOppdatering)) return false;
        JournalpostOppdatering that = (JournalpostOppdatering) o;
        return isMedDokumentkategori() == that.isMedDokumentkategori() &&
            Objects.equals(getArkivSakID(), that.getArkivSakID()) &&
            Objects.equals(getHovedDokumentID(), that.getHovedDokumentID()) &&
            Objects.equals(getBrukerID(), that.getBrukerID()) &&
            Objects.equals(getAvsenderID(), that.getAvsenderID()) &&
            Objects.equals(getAvsenderNavn(), that.getAvsenderNavn()) &&
            getAvsenderType() == that.getAvsenderType() &&
            Objects.equals(getTittel(), that.getTittel()) &&
            Objects.equals(getFysiskeVedlegg(), that.getFysiskeVedlegg()) &&
            Objects.equals(getLogiskeVedleggTitler(), that.getLogiskeVedleggTitler());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getArkivSakID(), getHovedDokumentID(), getBrukerID(), getAvsenderID(), getAvsenderNavn(),
            getAvsenderType(), getTittel(), getFysiskeVedlegg(), getLogiskeVedleggTitler(), isMedDokumentkategori());
    }
}
