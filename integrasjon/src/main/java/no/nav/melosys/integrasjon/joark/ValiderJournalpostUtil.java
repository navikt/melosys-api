package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import org.apache.commons.lang3.StringUtils;

final class ValiderJournalpostUtil {

    private ValiderJournalpostUtil() {
        throw new IllegalStateException("Utility");
    }

    // Manuell validering før vi prøver å opprette journalpost.
    // Ref: https://confluence.adeo.no/display/BOA/ferdigstillJournalpost og https://confluence.adeo.no/display/BOA/opprettJournalpost
    static boolean validerJournalpostForEndeligJfr(OpprettJournalpostRequest opprettJournalpostRequest) {
        return validerSak(opprettJournalpostRequest.getSak())
            && validerJournalpost(opprettJournalpostRequest)
            && validerBruker(opprettJournalpostRequest.getBruker())
            && validerDokument(opprettJournalpostRequest.getDokumenter());
    }

    private static boolean validerSak(Sak sak) {
        return StringUtils.isNotEmpty(sak.getArkivsaksnummer())
            && StringUtils.isNotEmpty(sak.getArkivsaksystem());
    }

    private static boolean validerJournalpost(OpprettJournalpostRequest request) {
        return request.getJournalpostType() != null
            && StringUtils.isNotEmpty(request.getTittel())
            && validerAvsenderMottaker(request)
            && validerKanal(request);
    }

    private static boolean validerAvsenderMottaker(OpprettJournalpostRequest request) {
        AvsenderMottaker avsenderMottaker = request.getAvsenderMottaker();
        if (request.getJournalpostType() == OpprettJournalpostRequest.JournalpostType.NOTAT) {
            return validerBetingetAvsenderMottakerId(avsenderMottaker);
        }

        return validerBetingetAvsenderMottakerId(avsenderMottaker)
            && StringUtils.isNotEmpty(avsenderMottaker.getNavn());
    }

    private static boolean validerBetingetAvsenderMottakerId(AvsenderMottaker avsenderMottaker) {
        if (StringUtils.isNotEmpty(avsenderMottaker.getId())) {
            return avsenderMottaker.getIdType() != null;
        }
        return true;
    }

    private static boolean validerKanal(OpprettJournalpostRequest request) {
        if (request.getJournalpostType() == OpprettJournalpostRequest.JournalpostType.INNGAAENDE) {
            return StringUtils.isNotEmpty(request.getKanal());
        }
        return true;
    }

    private static boolean validerBruker(Bruker bruker) {
        return StringUtils.isNotEmpty(bruker.getId())
            && (bruker.getIdType() != null);
    }

    private static boolean validerDokument(List<Dokument> dokumenter) {
        return dokumenter.stream().allMatch(dokument ->
            StringUtils.isNotEmpty(dokument.getTittel())
                && StringUtils.isNotEmpty(dokument.getDokumentKategori()));
    }
}
