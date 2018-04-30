package no.nav.melosys.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public class Journalpost {
    private final String journalpostId;
    private String arkivSakId;
    private String arkivSakSystem;
    private String avsenderId;
    private String brukerId;
    private LocalDateTime forsendelseMottatt;
    private String hoveddokumentId;
    private String hoveddokumentTittel;

    public Journalpost(String journalpostId) {
        this.journalpostId = journalpostId;
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

    public Optional<String> getArkivSakSystem() {
        return Optional.ofNullable(arkivSakSystem);
    }

    public void setArkivSakSystem(String arkivSakSystem) {
        this.arkivSakSystem = arkivSakSystem;
    }

    public String getAvsenderId() {
        return avsenderId;
    }

    public void setAvsenderId(String avsenderId) {
        this.avsenderId = avsenderId;
    }

    public LocalDateTime getForsendelseMottatt() {
        return forsendelseMottatt;
    }

    public void setForsendelseMottatt(LocalDateTime forsendelseMottatt) {
        this.forsendelseMottatt = forsendelseMottatt;
    }

    public String getBrukerId() {
        return brukerId;
    }

    public void setBrukerId(String brukerId) {
        this.brukerId = brukerId;
    }

    public String getHoveddokumentId() {
        return hoveddokumentId;
    }

    public void setHoveddokumentId(String hoveddokumentId) {
        this.hoveddokumentId = hoveddokumentId;
    }

    public String getHoveddokumentTittel() {
        return hoveddokumentTittel;
    }

    public void setHoveddokumentTittel(String hoveddokumentTittel) {
        this.hoveddokumentTittel = hoveddokumentTittel;
    }
}
