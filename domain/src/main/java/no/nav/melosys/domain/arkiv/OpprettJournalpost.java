package no.nav.melosys.domain.arkiv;

import no.nav.melosys.domain.eessi.SedType;

import java.util.List;

import static no.nav.melosys.domain.arkiv.FysiskDokument.lagFysiskDokumentSed;

public class OpprettJournalpost extends Journalpost {

    private static final String SENTRAL_UTSKRIFT = "S";
    private static final String MEDLEMSKAP_OG_AVGIFT = "4530";
    private static final String UNNTAK_FRA_MEDLEMSKAP = "UFM";
    private static final String UTENLANDSK_ORGANISASJON = "UTL_ORG";

    private String journalførendeEnhet;
    private String korrespondansepartIdType;
    private String korrespondansepartLand;
    private FysiskDokument hoveddokument;
    private List<FysiskDokument> vedlegg;

    public OpprettJournalpost() {
        super(null);
    }

    public static OpprettJournalpost lagJournalpostForSendingAvSedSomBrev(
        Long gsakSaksnummer, String brukerFnr, SedType sedType, byte[] sedPdf,
        String institusjonID, String institusjonNavn, String institusjonLand, List<FysiskDokument> vedlegg) {

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagFysiskDokumentSed(sedType, sedPdf));
        opprettJournalpost.setVedlegg(vedlegg);
        opprettJournalpost.setArkivSakId(gsakSaksnummer.toString());
        opprettJournalpost.setMottaksKanal(SENTRAL_UTSKRIFT);
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(UNNTAK_FRA_MEDLEMSKAP);

        opprettJournalpost.setKorrespondansepartId(institusjonID);
        opprettJournalpost.setKorrespondansepartNavn(institusjonNavn);
        opprettJournalpost.setKorrespondansepartLand(institusjonLand);
        opprettJournalpost.setKorrespondansepartIdType(UTENLANDSK_ORGANISASJON);
        opprettJournalpost.setBrukerId(brukerFnr);

        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());

        return opprettJournalpost;
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

    public void setKorrespondansepartIdType(String korrespondansepartIdType) {
        this.korrespondansepartIdType = korrespondansepartIdType;
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
