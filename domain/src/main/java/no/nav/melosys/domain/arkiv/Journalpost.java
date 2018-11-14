package no.nav.melosys.domain.arkiv;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Journalpostopplysninger fra Joark. Transient for Melosys.
 */
public class Journalpost {
    private final String journalpostId;
    private String arkivSakId;
    private String avsenderId;
    private String brukerId;
    private Instant forsendelseJournalfoert;
    private Instant forsendelseMottatt;
    private ArkivDokument hoveddokument;
    private String innhold;
    private Journalposttype journalposttype;
    private List<ArkivDokument> vedleggListe;

    public Journalpost(String journalpostId) {
        this.journalpostId = journalpostId;
        this.vedleggListe = new ArrayList<>();
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getArkivSakId() {
        return arkivSakId;
    }

    public void setArkivSakId(String arkivSakId) {
        this.arkivSakId = arkivSakId;
    }

    public String getAvsenderId() {
        return avsenderId;
    }

    public void setAvsenderId(String avsenderId) {
        this.avsenderId = avsenderId;
    }

    public Instant getForsendelseJournalfoert() {
        return forsendelseJournalfoert;
    }

    public void setForsendelseJournalfoert(Instant forsendelseJournalfoert) {
        this.forsendelseJournalfoert = forsendelseJournalfoert;
    }

    public Instant getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public void setForsendelseMottatt(Instant forsendelseMottatt) {
        this.forsendelseMottatt = forsendelseMottatt;
    }

    public String getBrukerId() {
        return brukerId;
    }

    public void setBrukerId(String brukerId) {
        this.brukerId = brukerId;
    }

    public ArkivDokument getHoveddokument() {
        return hoveddokument;
    }

    public void setHoveddokument(ArkivDokument hoveddokument) {
        this.hoveddokument = hoveddokument;
    }

    public String getInnhold() {
        return innhold;
    }

    public void setInnhold(String innhold) {
        this.innhold = innhold;
    }

    public Journalposttype getJournalposttype() {
        return journalposttype;
    }

    public void setJournalposttype(Journalposttype journalposttype) {
        this.journalposttype = journalposttype;
    }

    public List<ArkivDokument> getVedleggListe() {
        return vedleggListe;
    }

    public void setVedleggListe(List<ArkivDokument> vedleggListe) {
        this.vedleggListe = vedleggListe;
    }
}
