package no.nav.melosys.tjenester.gui.dto.dokument;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.arkiv.LogiskeVedlegg;
import no.nav.melosys.domain.kodeverk.Mottaksretning;

public class JournalpostInfoDto {
    public final String journalpostID;
    public final Instant mottattDato;
    public final Instant journalforingDato;
    public final Mottaksretning mottaksretning;
    public final String avsenderEllerMottaker;
    public final DokumentDto hoveddokument;
    public final List<DokumentDto> vedlegg;

    public JournalpostInfoDto(String journalpostID,
                              Instant mottattDato,
                              Instant journalforingDato,
                              Mottaksretning mottaksretning,
                              String avsenderEllerMottaker,
                              DokumentDto hoveddokument,
                              List<DokumentDto> vedlegg) {
        this.journalpostID = journalpostID;
        this.mottattDato = mottattDato;
        this.journalforingDato = journalforingDato;
        this.mottaksretning = mottaksretning;
        this.avsenderEllerMottaker = avsenderEllerMottaker;
        this.hoveddokument = hoveddokument;
        this.vedlegg = vedlegg;
    }

    public static JournalpostInfoDto av(Journalpost journalpost) {
        return new JournalpostInfoDto(journalpost.getJournalpostId(),
            journalpost.getForsendelseMottatt(),
            journalpost.getForsendelseJournalfoert(),
            av(journalpost.getJournalposttype()),
            journalpost.getKorrespondansepartNavn(),
            new DokumentDto(journalpost.getHoveddokument().getDokumentId(), journalpost.getHoveddokument().getTittel()),
            lagVedlegg(journalpost.getVedleggListe(), journalpost.getHoveddokument().getLogiskeVedlegg()));
    }

    private static List<DokumentDto> lagVedlegg(List<ArkivDokument> vedlegg, List<LogiskeVedlegg> interneVedlegg) {
        List<DokumentDto> vedleggListe = new ArrayList<>();
        vedlegg.forEach(v -> vedleggListe.add(new DokumentDto(v.getDokumentId(), v.getTittel())));
        interneVedlegg.forEach(v -> vedleggListe.add(new DokumentDto(v.getTittel())));
        return vedleggListe;
    }

    public Instant hentGjeldendeTidspunkt() {
        return this.mottattDato != null ? this.mottattDato : this.journalforingDato;
    }

    public static Mottaksretning av(Journalposttype journalposttype) {
        Mottaksretning retning;
        switch (journalposttype) {
            case INN: retning = Mottaksretning.INN;
                break;
            case UT: retning = Mottaksretning.UT;
                break;
            case NOTAT: retning = Mottaksretning.NOTAT;
                break;
            default:
                throw new IllegalArgumentException("Journalposttype " + journalposttype.getKode() + " støttes ikke.");
        }
        return retning;
    }
}
