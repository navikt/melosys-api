package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.tjenester.gui.dto.dokument.DokumentDto;

public final class JournalpostDto {
    private Instant mottattDato;
    private String brukerID;
    private String avsenderID;
    private String avsenderNavn;
    private boolean erBrukerAvsender;
    private DokumentDto hoveddokument;
    private List<DokumentDto> vedlegg = new ArrayList<>();
    private SedBehandling sedBehandling;

    private JournalpostDto(Instant mottattDato, String brukerID, String avsenderID, String avsenderNavn, boolean erBrukerAvsender) {
        this.mottattDato = mottattDato;
        this.brukerID = brukerID;
        this.avsenderID = avsenderID;
        this.avsenderNavn = avsenderNavn;
        this.erBrukerAvsender = erBrukerAvsender;
    }

    public static JournalpostDto av(Journalpost journalpost) {
        Instant mottatDato = journalpost.getForsendelseMottatt();
        String brukerID = journalpost.getBrukerId();
        String avsenderID = journalpost.getAvsenderId();
        String avsenderNavn = journalpost.getAvsenderNavn();
        boolean erBrukerAvsender = brukerID != null && brukerID.equalsIgnoreCase(avsenderID);
        JournalpostDto dto = new JournalpostDto(mottatDato, brukerID, avsenderID, avsenderNavn, erBrukerAvsender);
        DokumentDto dokumentDto = new DokumentDto(journalpost.getHoveddokument().getDokumentId(), journalpost.getHoveddokument().getTittel());
        dto.setHoveddokument(dokumentDto);
        dto.setVedlegg(journalpost.getVedleggListe().stream().map(v -> new DokumentDto(v.getDokumentId(), v.getTittel())).collect(Collectors.toList()));
        return dto;
    }

    public Instant getMottattDato() {
        return mottattDato;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public boolean isErBrukerAvsender() {
        return erBrukerAvsender;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public DokumentDto getHoveddokument() {
        return hoveddokument;
    }

    private void setHoveddokument(DokumentDto hoveddokument) {
        this.hoveddokument = hoveddokument;
    }

    public List<DokumentDto> getVedlegg() {
        return vedlegg;
    }

    private void setVedlegg(List<DokumentDto> vedlegg) {
        this.vedlegg = vedlegg;
    }

    public SedBehandling getSedBehandling() {
        return sedBehandling;
    }

    public void setSedBehandling(SedBehandling sedBehandling) {
        this.sedBehandling = sedBehandling;
    }
}
