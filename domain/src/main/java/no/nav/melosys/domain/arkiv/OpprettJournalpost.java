package no.nav.melosys.domain.arkiv;

import java.util.List;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.eessi.SedType;

import static no.nav.melosys.domain.arkiv.FysiskDokument.*;

public class OpprettJournalpost extends Journalpost {
    private static final String SENTRAL_UTSKRIFT = "S";
    private static final String MEDLEMSKAP_OG_AVGIFT = "4530";
    private static final String NAV_NO = "NAV_NO";

    public enum KorrespondansepartIdType {
        UTENLANDSK_ORGANISASJON("UTL_ORG"),
        ORGNR("ORGNR"),
        FNR("FNR");

        private final String kode;

        public String getKode() {
            return kode;
        }

        KorrespondansepartIdType(String tittel) {
            this.kode = tittel;
        }
    }

    private String eksternReferanseId;
    private String journalførendeEnhet;
    private String korrespondansepartIdType;
    private String korrespondansepartLand;
    private FysiskDokument hoveddokument;
    private List<FysiskDokument> vedlegg;

    public OpprettJournalpost() {
        super(null);
    }

    public static OpprettJournalpost lagJournalpostForSendingAvSedSomBrev(
        String saksnummer, String brukerFnr, SedType sedType, byte[] sedPdf, String institusjonID,
        String institusjonNavn, String institusjonLand, List<FysiskDokument> vedlegg, Tema tema, String eksternReferanseIdForJournalpost) {

        var opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagFysiskDokumentSed(sedType, sedPdf));
        opprettJournalpost.setVedlegg(vedlegg);
        opprettJournalpost.setSaksnummer(saksnummer);
        opprettJournalpost.setMottaksKanal(SENTRAL_UTSKRIFT);
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(tema.getKode());

        opprettJournalpost.setKorrespondansepartId(institusjonID);
        opprettJournalpost.setKorrespondansepartNavn(institusjonNavn);
        opprettJournalpost.setKorrespondansepartLand(institusjonLand);
        opprettJournalpost.setKorrespondansepartIdType(KorrespondansepartIdType.UTENLANDSK_ORGANISASJON);
        opprettJournalpost.setBrukerId(brukerFnr);
        opprettJournalpost.setBrukerIdType(BrukerIdType.FOLKEREGISTERIDENT);
        opprettJournalpost.setEksternReferanseId(eksternReferanseIdForJournalpost);

        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());

        return opprettJournalpost;
    }

    public static OpprettJournalpost lagJournalpostForBrev(JournalpostBestilling bestilling, Tema tema) {
        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagFysiskDokument(bestilling));
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(tema.getKode());
        opprettJournalpost.setSaksnummer(bestilling.getSaksnummer());
        opprettJournalpost.setBrukerId(bestilling.getHovedpartId());
        opprettJournalpost.setEksternReferanseId(bestilling.getEksternReferanseId());
        opprettJournalpost.setBrukerIdType(bestilling.getHovedpartIdType());
        opprettJournalpost.setKorrespondansepartId(bestilling.getMottakerId());
        opprettJournalpost.setKorrespondansepartNavn(bestilling.getMottakerNavn());
        opprettJournalpost.setKorrespondansepartIdType(bestilling.getMottakerIdType());
        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());
        opprettJournalpost.setVedlegg(FysiskDokument.lagFysiskDokumentListeFraVedlegg(bestilling, bestilling.getVedlegg()));

        return opprettJournalpost;
    }

    public String getEksternReferanseId() {
        return eksternReferanseId;
    }

    public void setEksternReferanseId(String eksternReferanseId) {
        this.eksternReferanseId = eksternReferanseId;
    }

    public String getJournalførendeEnhet() {
        return journalførendeEnhet;
    }

    public void setJournalførendeEnhet(String journalførendeEnhet) {
        this.journalførendeEnhet = journalførendeEnhet;
    }

    public String getKorrespondansepartIdType() {
        return korrespondansepartIdType;
    }

    public void setKorrespondansepartIdType(KorrespondansepartIdType korrespondansepartIdType) {
        this.korrespondansepartIdType = korrespondansepartIdType.getKode();
    }

    public String getKorrespondansepartLand() {
        return korrespondansepartLand;
    }

    public void setKorrespondansepartLand(String korrespondansepartLand) {
        this.korrespondansepartLand = korrespondansepartLand;
    }

    @Override
    public FysiskDokument getHoveddokument() {
        return hoveddokument;
    }

    public void setHoveddokument(FysiskDokument hoveddokument) {
        this.hoveddokument = hoveddokument;
    }

    public List<FysiskDokument> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(List<FysiskDokument> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
