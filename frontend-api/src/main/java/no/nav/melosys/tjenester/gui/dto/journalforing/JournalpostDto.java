package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.tjenester.gui.dto.dokument.DokumentDto;

public final class JournalpostDto {
    private Instant mottattDato;
    private String brukerID;
    private String avsenderID;
    private String avsenderNavn;
    private Avsendertyper avsenderType;
    private boolean erBrukerAvsender;
    private DokumentDto hoveddokument;
    private List<DokumentDto> vedlegg = new ArrayList<>();
    private BehandlingsInformasjon behandlingsInformasjon;

    private JournalpostDto(Instant mottattDato, String brukerID,
                           String avsenderID, String avsenderNavn,
                           Avsendertyper avsenderType, boolean erBrukerAvsender) {
        this.mottattDato = mottattDato;
        this.brukerID = brukerID;
        this.avsenderID = avsenderID;
        this.avsenderNavn = avsenderNavn;
        this.avsenderType = avsenderType;
        this.erBrukerAvsender = erBrukerAvsender;
    }

    public static JournalpostDto av(Journalpost journalpost) {
        Instant mottattDato = journalpost.getForsendelseMottatt();
        String brukerID = journalpost.getBrukerId();
        String avsenderID = journalpost.getAvsenderId();
        String avsenderNavn = journalpost.getAvsenderNavn();
        Avsendertyper avsenderType = journalpost.getAvsenderType();
        boolean erBrukerAvsender = brukerID != null && brukerID.equalsIgnoreCase(avsenderID);
        JournalpostDto dto = new JournalpostDto(mottattDato, brukerID, avsenderID, avsenderNavn, avsenderType, erBrukerAvsender);
        DokumentDto dokumentDto = new DokumentDto(journalpost.getHoveddokument().getDokumentId(),
            journalpost.getHoveddokument().getTittel(), journalpost.getHoveddokument().hentLogiskeVedleggTitler());
        dto.setHoveddokument(dokumentDto);
        dto.setVedlegg(journalpost.getVedleggListe().stream()
            .map(v -> new DokumentDto(v.getDokumentId(), v.getTittel(), v.hentLogiskeVedleggTitler()))
            .collect(Collectors.toList()));

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

    public Avsendertyper getAvsenderType() {
        return avsenderType;
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

    public BehandlingsInformasjon getBehandlingsInformasjon() {
        return behandlingsInformasjon;
    }

    public void setBehandlingsInformasjon(BehandlingsInformasjon behandlingsInformasjon) {
        this.behandlingsInformasjon = behandlingsInformasjon;
    }
}
