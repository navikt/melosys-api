package no.nav.melosys.domain.arkiv;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.MoreCollectors;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;

import static no.nav.melosys.domain.arkiv.FysiskDokument.*;

public class OpprettJournalpost extends Journalpost {
    private static final String SENTRAL_UTSKRIFT = "S";
    private static final String MEDLEMSKAP_OG_AVGIFT = "4530";
    private static final String UNNTAK_FRA_MEDLEMSKAP = "UFM";
    private static final String MEDLEMSKAP = "MED";
    private static final String ALTINN = "ALTINN";
    private static final String UTENLANDSK_ORGANISASJON = "UTL_ORG";
    private static final String ORGNR = "ORGNR";
    private static final String FNR = "FNR";

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
        Long arkivsakID, String brukerFnr, SedType sedType, byte[] sedPdf,
        String institusjonID, String institusjonNavn, String institusjonLand, List<FysiskDokument> vedlegg) {

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagFysiskDokumentSed(sedType, sedPdf));
        opprettJournalpost.setVedlegg(vedlegg);
        opprettJournalpost.setArkivSakId(arkivsakID.toString());
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

    public static OpprettJournalpost lagJournalpostForMottakAltinnSøknad(Fagsak fagsak,
                                                                         Collection<AltinnDokument> dokumenter,
                                                                         String brukerID,
                                                                         String avsenderNavn)
        throws FunksjonellException, TekniskException {
        AltinnDokument hovedDokument = dokumenter.stream().filter(AltinnDokument::erSøknad)
            .collect(MoreCollectors.onlyElement());
        dokumenter.remove(hovedDokument);

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        final var behandlingsgrunnlag = fagsak.hentSistAktiveBehandling().getBehandlingsgrunnlag();
        opprettJournalpost.setHoveddokument(lagFysiskHovedDokumentAltinn(hovedDokument, behandlingsgrunnlag));
        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());
        opprettJournalpost.setVedlegg(dokumenter.stream().map(FysiskDokument::lagFysiskDokumentAltinn).collect(Collectors.toList()));
        opprettJournalpost.setArkivSakId(fagsak.getGsakSaksnummer().toString());
        opprettJournalpost.setMottaksKanal(ALTINN);
        opprettJournalpost.setEksternReferanseId(hovedDokument.getSoknadID());
        opprettJournalpost.setJournalposttype(Journalposttype.INN);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(MEDLEMSKAP);
        opprettJournalpost.setBrukerId(brukerID);
        opprettJournalpost.setForsendelseMottatt(hovedDokument.getInnsendtTidspunkt());

        fagsak.hentRepresentant(Representerer.BRUKER).ifPresentOrElse(
            r -> {
                opprettJournalpost.setKorrespondansepartId(r.getOrgnr());
                opprettJournalpost.setKorrespondansepartNavn(avsenderNavn);
                opprettJournalpost.setKorrespondansepartIdType(ORGNR);
            },
            () -> {
                opprettJournalpost.setKorrespondansepartId(brukerID);
                opprettJournalpost.setKorrespondansepartNavn(avsenderNavn);
                opprettJournalpost.setKorrespondansepartIdType(FNR);
            }
        );

        return opprettJournalpost;
    }

    public static OpprettJournalpost lagJournalpostForBrev(JournalpostBestilling bestilling) {
        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(lagHoveddokument(bestilling));
        opprettJournalpost.setJournalposttype(Journalposttype.UT);
        opprettJournalpost.setJournalførendeEnhet(MEDLEMSKAP_OG_AVGIFT);
        opprettJournalpost.setTema(MEDLEMSKAP);
        opprettJournalpost.setArkivSakId(bestilling.getArkivSakId());
        opprettJournalpost.setBrukerId(bestilling.getBrukerFnr());
        opprettJournalpost.setKorrespondansepartId(bestilling.getMottakerId());
        opprettJournalpost.setKorrespondansepartNavn(bestilling.getMottakerNavn());
        opprettJournalpost.setKorrespondansepartIdType(bestilling.erMottakerOrg() ? ORGNR : FNR);
        opprettJournalpost.setInnhold(opprettJournalpost.getHoveddokument().getTittel());

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
