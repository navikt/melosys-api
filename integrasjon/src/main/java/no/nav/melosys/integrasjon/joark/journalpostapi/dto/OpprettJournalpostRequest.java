package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;

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

    public static OpprettJournalpostRequest av(OpprettJournalpost opprettJournalpost) {
        return OpprettJournalpostRequest.builder()
            .avsenderMottaker(avsenderMottaker(
                opprettJournalpost.getKorrespondansepartId(),
                opprettJournalpost.getKorrespondansepartNavn(),
                opprettJournalpost.getKorrespondansepartIdType()))
            .bruker(bruker(opprettJournalpost.getBrukerId()))
            .tema(opprettJournalpost.getTema())
            .kanal(opprettJournalpost.getMottaksKanal())
            .sak(arkivsak(opprettJournalpost.getArkivSakId()))
            .journalfoerendeEnhet(opprettJournalpost.getJournalførendeEnhet())
            .journalpostType(JournalpostType.av(opprettJournalpost.getJournalposttype()))
            .tittel(opprettJournalpost.getInnhold())
            .dokumenter(dokumenter(opprettJournalpost))
            .tilleggsopplysninger(Collections.emptyList())
            .build();
    }

    private static AvsenderMottaker avsenderMottaker(String id, String navn, String idType) {
        return AvsenderMottaker.builder()
            .id(id)
            .idType(AvsenderMottaker.IdType.valueOf(idType))
            .navn(navn)
            .build();
    }

    private static Bruker bruker(String fnr) {
        return Bruker.builder()
            .id(fnr).idType(Bruker.BrukerIdType.FNR)
            .build();
    }

    private static Sak arkivsak(String gsakSaksnummer) {
        return Sak.builder().arkivsaksnummer(gsakSaksnummer).build();
    }

    private static List<Dokument> dokumenter(OpprettJournalpost opprettJournalpost) {
        List<Dokument> dokumentliste = new ArrayList<>();
        dokumentliste.add(dokument(opprettJournalpost.getHoveddokument()));

        if (opprettJournalpost.getVedlegg() != null) {
            dokumentliste.addAll(opprettJournalpost.getVedlegg().stream()
                .map(OpprettJournalpostRequest::dokument)
                .collect(Collectors.toList()));
        }

        return dokumentliste;
    }

    private static Dokument dokument(FysiskDokument dokument) {
        return Dokument.builder()
            .tittel(dokument.getTittel())
            .brevkode(dokument.getBrevkode())
            .dokumentKategori(dokument.getDokumentKategori())
            .dokumentvarianter(Collections.singletonList(
                DokumentVariant.builder()
                    .filtype(JournalpostFiltype.valueOf(dokument.getFiltype().name()))
                    .variantformat(dokument.getVariantFormat())
                    .fysiskDokument(dokument.getData())
                    .build()))
            .build();
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
        NOTAT;

        public static JournalpostType av(Journalposttype journalposttype) {
            switch (journalposttype) {
                case INN:
                    return INNGAAENDE;
                case UT:
                    return UTGAAENDE;
                case NOTAT:
                    return NOTAT;
            }
            throw new IllegalArgumentException("Finner ikke journalposttype " + journalposttype);
        }
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
