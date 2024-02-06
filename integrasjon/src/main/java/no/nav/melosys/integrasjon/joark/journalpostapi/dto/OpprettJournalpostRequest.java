package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.arkiv.*;

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

    private List<Tilleggsopplysning> tilleggsopplysninger;

    private Sak sak;

    //"Første dokument blir tilknyttet som hoveddokument på journalposten. Øvrige dokumenter tilknyttes som vedlegg. Rekkefølgen på vedlegg beholdes ikke ved uthenting av journalpost."
    private List<Dokument> dokumenter;

    private OverstyrInnsynsregler overstyrInnsynsregler;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate datoMottatt;

    private OpprettJournalpostRequest(JournalpostType journalpostType,
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
                                      List<Dokument> dokumenter,
                                      LocalDate datoMottatt,
                                      OverstyrInnsynsregler overstyrInnsynsregler) {
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
        this.datoMottatt = datoMottatt;
        this.overstyrInnsynsregler = overstyrInnsynsregler;
    }

    public static OpprettJournalpostRequest av(OpprettJournalpost opprettJournalpost) {
        return OpprettJournalpostRequest.builder()
            .avsenderMottaker(avsenderMottaker(
                opprettJournalpost.getKorrespondansepartId(),
                opprettJournalpost.getKorrespondansepartNavn(),
                opprettJournalpost.getKorrespondansepartIdType(),
                opprettJournalpost.getKorrespondansepartLand()))
            .bruker(bruker(opprettJournalpost.getBrukerId(), opprettJournalpost.getBrukerIdType()))
            .tema(opprettJournalpost.getTema())
            .kanal(opprettJournalpost.getMottaksKanal())
            .eksternReferanseId(opprettJournalpost.getEksternReferanseId())
            .sak(new Sak(opprettJournalpost.getSaksnummer()))
            .journalfoerendeEnhet(opprettJournalpost.getJournalførendeEnhet())
            .journalpostType(JournalpostType.av(opprettJournalpost.getJournalposttype()))
            .tittel(opprettJournalpost.getInnhold())
            .dokumenter(dokumenter(opprettJournalpost))
            .tilleggsopplysninger(Collections.emptyList())
            .datoMottatt(opprettJournalpost.getForsendelseMottatt() == null ? null
                : LocalDate.ofInstant(opprettJournalpost.getForsendelseMottatt(), ZoneId.systemDefault()))
            .overstyrInnsynsregler(opprettJournalpost.getOverstyrInnsynsregler())
            .build();
    }

    private static AvsenderMottaker avsenderMottaker(String id, String navn, String idType, String land) {
        return AvsenderMottaker.builder()
            .id(id)
            .idType(AvsenderMottaker.IdType.valueOf(idType))
            .navn(navn)
            .land(land)
            .build();
    }

    private static Bruker bruker(String brukerID, BrukerIdType brukerIdType) {
        return (brukerID != null && brukerIdType != null)
            ? Bruker.builder().id(brukerID).idType(tilBrukerIdType(brukerIdType)).build()
            : null;
    }

    private static Bruker.BrukerIdType tilBrukerIdType(BrukerIdType brukerIdType) {
        return switch (brukerIdType) {
            case FOLKEREGISTERIDENT -> Bruker.BrukerIdType.FNR;
            case AKTØR_ID -> Bruker.BrukerIdType.AKTOERID;
            case ORGNR -> Bruker.BrukerIdType.ORGNR;
        };
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
            .dokumentvarianter(dokument.getDokumentVarianter().stream()
                .map(OpprettJournalpostRequest::dokumentVariant)
                .collect(Collectors.toList()))
            .build();
    }

    private static DokumentVariant dokumentVariant(no.nav.melosys.domain.arkiv.DokumentVariant dokumentVariant) {
        return DokumentVariant.builder()
            .filtype(JournalpostFiltype.valueOf(dokumentVariant.getFiltype().name()))
            .variantformat(dokumentVariant.getVariantFormat().name())
            .fysiskDokument(dokumentVariant.getData())
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

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    public enum JournalpostType {
        INNGAAENDE,
        UTGAAENDE,
        NOTAT;

        public static JournalpostType av(Journalposttype journalposttype) {
            return switch (journalposttype) {
                case INN -> INNGAAENDE;
                case UT -> UTGAAENDE;
                case NOTAT -> NOTAT;
            };
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
        private LocalDate datoMottatt;
        private OverstyrInnsynsregler overstyrInnsynsregler;

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

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder datoMottatt(LocalDate datoMottatt) {
            this.datoMottatt = datoMottatt;
            return this;
        }

        public OpprettJournalpostRequest.OpprettJournalpostRequestBuilder overstyrInnsynsregler(OverstyrInnsynsregler overstyrInnsynsregler) {
            this.overstyrInnsynsregler = overstyrInnsynsregler;
            return this;
        }

        public OpprettJournalpostRequest build() {
            return new OpprettJournalpostRequest(journalpostType, avsenderMottaker, bruker, tema, behandlingstema, tittel,
                kanal, journalfoerendeEnhet, eksternReferanseId, tilleggsopplysninger, sak, dokumenter, datoMottatt, overstyrInnsynsregler);
        }
    }
}
