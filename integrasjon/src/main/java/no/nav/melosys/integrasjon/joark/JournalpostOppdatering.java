package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import org.springframework.util.CollectionUtils;

public final class JournalpostOppdatering {
    private final String saksnummer;
    private final String hovedDokumentID;
    private final String brukerID;
    private final String avsenderID;
    private final String avsenderNavn;
    private final Avsendertyper avsenderType;
    private final String avsenderLand;
    private final String tittel;
    private final Map<String, String> fysiskeVedlegg;
    private final List<String> logiskeVedleggTitler;
    private final LocalDate mottattDato;
    private final String tema;

    public static class Builder {
        private String saksnummer;
        private String hovedDokumentID;
        private String brukerID;
        private String avsenderID;
        private String avsenderNavn;
        private String avsenderLand;
        private Avsendertyper avsenderType;
        private String tittel;
        private LocalDate mottattDato;
        private Map<String, String> fysiskeVedlegg = new HashMap<>();
        private List<String> logiskeVedleggTitler = new ArrayList<>();
        private String tema;

        public Builder medSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
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

        public Builder medAvsenderLand(String avsenderLand) {
            this.avsenderLand = avsenderLand;
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

        public Builder medMottattDato(LocalDate mottattDato) {
            this.mottattDato = mottattDato;
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

        public Builder medTema(String tema) {
            this.tema = tema;
            return this;
        }

        public JournalpostOppdatering build() {
            return new JournalpostOppdatering(this);
        }
    }

    private JournalpostOppdatering(Builder builder) {
        this.saksnummer = builder.saksnummer;
        this.hovedDokumentID = builder.hovedDokumentID;
        this.brukerID = builder.brukerID;
        this.avsenderID = builder.avsenderID;
        this.avsenderNavn = builder.avsenderNavn;
        this.avsenderType = builder.avsenderType;
        this.tittel = builder.tittel;
        this.fysiskeVedlegg = builder.fysiskeVedlegg;
        this.logiskeVedleggTitler = builder.logiskeVedleggTitler;
        this.mottattDato = builder.mottattDato;
        this.avsenderLand = builder.avsenderLand;
        this.tema = builder.tema;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getHovedDokumentID() {
        return hovedDokumentID;
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

    public Avsendertyper getAvsenderType() {
        return avsenderType;
    }


    public String getAvsenderLand() {
        return avsenderLand;
    }


    public String getTittel() {
        return tittel;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public Map<String, String> getFysiskeVedlegg() {
        return fysiskeVedlegg;
    }

    boolean harFysiskeVedlegg() {
        return !CollectionUtils.isEmpty(fysiskeVedlegg);
    }


    public List<String> getLogiskeVedleggTitler() {
        return logiskeVedleggTitler;
    }

    boolean harLogiskeVedlegg() {
        return !CollectionUtils.isEmpty(logiskeVedleggTitler);
    }

    public String getTema() {
        return tema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JournalpostOppdatering that)) return false;
        return Objects.equals(getSaksnummer(), that.getSaksnummer()) &&
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
        return Objects.hash(getSaksnummer(), getHovedDokumentID(), getBrukerID(), getAvsenderID(), getAvsenderNavn(),
            getAvsenderType(), getTittel(), getFysiskeVedlegg(), getLogiskeVedleggTitler());
    }
}
