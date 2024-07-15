package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.arkiv.BrukerIdType;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.DokumentDto;

public final class JournalpostDto {
    private final Instant mottattDato;
    private final String brukerID;
    private final String virksomhetOrgnr;
    private final String avsenderID;
    private final String avsenderNavn;
    private final Avsendertyper avsenderType;
    private final boolean erHovedpartAvsender;
    private final DokumentDto hoveddokument;
    private final List<DokumentDto> vedlegg;
    private BehandlingsInformasjon behandlingsInformasjon;
    private final Boolean mottaksKanalErEessi;
    private final Boolean mottaksKanalErElektronisk;

    private JournalpostDto(Instant mottattDato, String brukerID,
                           String virksomhetOrgnr, String avsenderID, String avsenderNavn,
                           Avsendertyper avsenderType, boolean erHovedpartAvsender,
                           DokumentDto hoveddokument, List<DokumentDto> vedlegg, Boolean mottaksKanalErEessi, Boolean mottaksKanalErElektronisk) {
        this.mottattDato = mottattDato;
        this.brukerID = brukerID;
        this.virksomhetOrgnr = virksomhetOrgnr;
        this.avsenderID = avsenderID;
        this.avsenderNavn = avsenderNavn;
        this.avsenderType = avsenderType;
        this.erHovedpartAvsender = erHovedpartAvsender;
        this.hoveddokument = hoveddokument;
        this.vedlegg = vedlegg;
        this.mottaksKanalErEessi = mottaksKanalErEessi;
        this.mottaksKanalErElektronisk = mottaksKanalErElektronisk;
    }

    public static JournalpostDto av(Journalpost journalpost, String hovedpartIdent) {
        var hovedpartErBruker = journalpost.getBrukerIdType() != BrukerIdType.ORGNR;
        String avsenderID = journalpost.getAvsenderId();
        DokumentDto hoveddokument = new DokumentDto(
            journalpost.getHoveddokument().getDokumentId(),
            journalpost.getHoveddokument().getTittel(),
            journalpost.getHoveddokument().hentLogiskeVedleggTitler());
        List<DokumentDto> vedlegg = journalpost.getVedleggListe().stream()
            .map(v -> new DokumentDto(v.getDokumentId(), v.getTittel(), v.hentLogiskeVedleggTitler()))
            .toList();

        return new JournalpostDto(
            journalpost.getForsendelseMottatt(),
            hovedpartErBruker ? hovedpartIdent : null,
            !hovedpartErBruker ? hovedpartIdent : null,
            avsenderID,
            journalpost.getAvsenderNavn(),
            journalpost.getAvsenderType(),
            avsenderID != null && avsenderID.equalsIgnoreCase(hovedpartIdent),
            hoveddokument,
            vedlegg,
            journalpost.mottaksKanalErEessi(),
            journalpost.mottaksKanalErElektronisk()
        );
    }

    public Instant getMottattDato() {
        return mottattDato;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public String getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    public boolean isErHovedpartAvsender() {
        return erHovedpartAvsender;
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

    public List<DokumentDto> getVedlegg() {
        return vedlegg;
    }

    public BehandlingsInformasjon getBehandlingsInformasjon() {
        return behandlingsInformasjon;
    }

    public void setBehandlingsInformasjon(BehandlingsInformasjon behandlingsInformasjon) {
        this.behandlingsInformasjon = behandlingsInformasjon;
    }

    public Boolean getMottaksKanalErEessi() {
        return mottaksKanalErEessi;
    }

    public Boolean getMottaksKanalErElektronisk() {
        return mottaksKanalErElektronisk;
    }
}
