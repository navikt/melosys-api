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
    private String avsenderNavn;
    private AvsenderType avsenderType;
    private String brukerId;
    private String korrespondansepartNavn;
    private String korrespondansepartId;
    private Instant forsendelseJournalfoert;
    private Instant forsendelseMottatt;
    private ArkivDokument hoveddokument;
    private String innhold;
    private Journalposttype journalposttype;
    private List<ArkivDokument> vedleggListe;
    private String mottaksKanal;
    private String tema;

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

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public void setAvsenderNavn(String avsenderNavn) {
        this.avsenderNavn = avsenderNavn;
    }

    public AvsenderType getAvsenderType() {
        return avsenderType;
    }

    public void setAvsenderType(AvsenderType avsenderType) {
        this.avsenderType = avsenderType;
    }

    public String getBrukerId() {
        return brukerId;
    }

    public void setBrukerId(String brukerId) {
        this.brukerId = brukerId;
    }

    public String getKorrespondansepartNavn() {
        return korrespondansepartNavn;
    }

    public void setKorrespondansepartNavn(String korrespondansepartNavn) {
        this.korrespondansepartNavn = korrespondansepartNavn;
    }

    public String getKorrespondansepartId() {
        return korrespondansepartId;
    }

    public void setKorrespondansepartId(String korrespondansepartId) {
        this.korrespondansepartId = korrespondansepartId;
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

    public String getMottaksKanal() {
        return mottaksKanal;
    }

    public void setMottaksKanal(String mottaksKanal) {
        this.mottaksKanal = mottaksKanal;
    }

    public boolean mottaksKanalErEessi() {
        return "EESSI".equals(mottaksKanal);
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }
}
