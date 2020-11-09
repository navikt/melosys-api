package no.nav.melosys.domain.arkiv;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.IkkeFunnetException;

/**
 * Journalpostopplysninger fra Joark. Transient for Melosys.
 */
public class Journalpost {
    private final String journalpostId;
    private boolean erFerdigstilt;
    private String arkivSakId;
    private String avsenderId;
    private String avsenderNavn;
    private Avsendertyper avsenderType;
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

    public Optional<ArkivDokument> finnArkivDokument(String dokumentID) {
        if (hoveddokument.getDokumentId().equals(dokumentID)) {
            return Optional.of(hoveddokument);
        }

        return vedleggListe.stream()
            .filter(arkivDokument -> arkivDokument.getDokumentId().equals(dokumentID))
            .findFirst();
    }

    public ArkivDokument hentArkivDokument(String dokumentID) throws IkkeFunnetException {
        return finnArkivDokument(dokumentID).orElseThrow(() ->
            new IkkeFunnetException(String.format("Finner ikke dokument %s i journalpost %s", dokumentID, journalpostId)));
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setErFerdigstilt(boolean erFerdigstilt) {
        this.erFerdigstilt = erFerdigstilt;
    }

    public boolean isErFerdigstilt() {
        return erFerdigstilt;
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

    public Avsendertyper getAvsenderType() {
        return avsenderType;
    }

    public void setAvsenderType(Avsendertyper avsenderType) {
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
