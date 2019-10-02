package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.util.ArrayList;
import java.util.List;

public class OpprettJournalpostRequest {

    private JournalpostType journalpostType;
    private AvsenderMottaker avsenderMottaker;
    private Bruker bruker;
    private String tema;
    private String behandlingstema;
    private String tittel;
    private String kanal;
    //"Ved automatisk journalføring uten mennesker involvert skal enhet settes til \"9999\"."
    private String journalfoerendeEnhet;
    private String eksternReferanseId;

    private List<Tilleggsopplysning> tilleggsopplysninger = new ArrayList<>();

    private Sak sak;

    //"Første dokument blir tilknyttet som hoveddokument på journalposten. Øvrige dokumenter tilknyttes som vedlegg. Rekkefølgen på vedlegg beholdes ikke ved uthenting av journalpost."
    private List<Dokument> dokumenter;

    public OpprettJournalpostRequest(JournalpostType journalpostType,
                                     AvsenderMottaker avsenderMottaker,
                                     Bruker bruker,
                                     String tema,
                                     String behandlingstema,
                                     String tittel,
                                     String kanal,
                                     String journalfoerendeEnhet,
                                     String eksternReferanseId,
                                     List<Tilleggsopplysning> tilleggsopplysninger,
                                     Sak sak,
                                     List<Dokument> dokumenter) {
        this.journalpostType = journalpostType;
        this.avsenderMottaker = avsenderMottaker;
        this.bruker = bruker;
        this.tema = tema;
        this.behandlingstema = behandlingstema;
        this.tittel = tittel;
        this.kanal = kanal;
        this.journalfoerendeEnhet = journalfoerendeEnhet;
        this.eksternReferanseId = eksternReferanseId;
        this.tilleggsopplysninger = tilleggsopplysninger;
        this.sak = sak;
        this.dokumenter = dokumenter;
    }

    public OpprettJournalpostRequest() {
    }

    public static OpprettJournalpostRequestBuilder builder() {
        return new OpprettJournalpostRequestBuilder();
    }

    public OpprettJournalpostRequest.JournalpostType getJournalpostType() {
        return this.journalpostType;
    }

    public AvsenderMottaker getAvsenderMottaker() {
        return this.avsenderMottaker;
    }

    public Bruker getBruker() {
        return this.bruker;
    }

    public String getTema() {
        return this.tema;
    }

    public String getBehandlingstema() {
        return this.behandlingstema;
    }

    public String getTittel() {
        return this.tittel;
    }

    public String getKanal() {
        return this.kanal;
    }

    public String getJournalfoerendeEnhet() {
        return this.journalfoerendeEnhet;
    }

    public String getEksternReferanseId() {
        return this.eksternReferanseId;
    }

    public List<Tilleggsopplysning> getTilleggsopplysninger() {
        return this.tilleggsopplysninger;
    }

    public Sak getSak() {
        return this.sak;
    }

    public List<Dokument> getDokumenter() {
        return this.dokumenter;
    }

    public enum JournalpostType {
        INNGAAENDE,
        UTGAAENDE,
        NOTAT
    }

    public static class OpprettJournalpostRequestBuilder {
        private JournalpostType journalpostType;
        private AvsenderMottaker avsenderMottaker;
        private Bruker bruker;
        private String tema;
        private String behandlingstema;
        private String tittel;
        private String kanal;
        private String journalfoerendeEnhet;
        private String eksternReferanseId;
        private List<Tilleggsopplysning> tilleggsopplysninger;
        private Sak sak;
        private List<Dokument> dokumenter;

        OpprettJournalpostRequestBuilder() {
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder journalpostType(JournalpostType journalpostType) {
            this.journalpostType = journalpostType;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder avsenderMottaker(AvsenderMottaker avsenderMottaker) {
            this.avsenderMottaker = avsenderMottaker;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder bruker(Bruker bruker) {
            this.bruker = bruker;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder tema(String tema) {
            this.tema = tema;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder behandlingstema(String behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder tittel(String tittel) {
            this.tittel = tittel;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder kanal(String kanal) {
            this.kanal = kanal;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder journalfoerendeEnhet(String journalfoerendeEnhet) {
            this.journalfoerendeEnhet = journalfoerendeEnhet;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder eksternReferanseId(String eksternReferanseId) {
            this.eksternReferanseId = eksternReferanseId;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder tilleggsopplysninger(List<Tilleggsopplysning> tilleggsopplysninger) {
            this.tilleggsopplysninger = tilleggsopplysninger;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder sak(Sak sak) {
            this.sak = sak;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder dokumenter(List<Dokument> dokumenter) {
            this.dokumenter = dokumenter;
            return this;
        }

        public OpprettJournalpostRequest build() {
            return new OpprettJournalpostRequest(journalpostType, avsenderMottaker, bruker, tema, behandlingstema, tittel, kanal, journalfoerendeEnhet, eksternReferanseId, tilleggsopplysninger, sak, dokumenter);
        }
    }
}
