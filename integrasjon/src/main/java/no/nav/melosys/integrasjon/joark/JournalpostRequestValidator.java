package no.nav.melosys.integrasjon.joark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.*;
import org.apache.commons.lang3.StringUtils;

final class JournalpostRequestValidator {
    private JournalpostRequestValidator() {
        throw new IllegalStateException("Utility");
    }

    // Manuell validering før vi prøver å opprette journalpost.
    // Ref: https://confluence.adeo.no/display/BOA/ferdigstillJournalpost
    // og https://confluence.adeo.no/display/BOA/opprettJournalpost
    static void validerJournalpostForEndeligJfr(OpprettJournalpostRequest opprettJournalpostRequest)
        throws FunksjonellException {
        Collection<String> feil = new ArrayList<>();
        feil.addAll(validerSak(opprettJournalpostRequest.getSak()));
        feil.addAll(validerJournalpost(opprettJournalpostRequest));
        feil.addAll(validerBruker(opprettJournalpostRequest.getBruker()));
        feil.addAll(validerDokument(opprettJournalpostRequest.getDokumenter()));
        if (!feil.isEmpty()) {
            throw new FunksjonellException("Journalpost kan ikke ferdigstilles:" + System.lineSeparator()
                + String.join(System.lineSeparator(), feil));
        }
    }

    private static Collection<String> validerSak(Sak sak) {
        Collection<String> feil = new ArrayList<>();
        if (StringUtils.isEmpty(sak.getFagsakId())) {
            feil.add("Saksnummer mangler");
        }
        if (StringUtils.isEmpty(sak.getArkivsaksystem())) {
            feil.add("Arkivsaksystem mangler");
        }
        return feil;
    }

    private static Collection<String> validerJournalpost(OpprettJournalpostRequest request) {
        Collection<String> feil = new ArrayList<>();
        if (request.getJournalpostType() == null) {
            feil.add("JournalpostType mangler");
        }
        if (StringUtils.isEmpty(request.getTittel())) {
            feil.add("Tittel mangler");
        }
        feil.addAll(validerAvsenderMottaker(request));
        feil.addAll(validerKanal(request));
        return feil;
    }

    private static Collection<String> validerAvsenderMottaker(OpprettJournalpostRequest request) {
        AvsenderMottaker avsenderMottaker = request.getAvsenderMottaker();
        Collection<String> feil = new ArrayList<>(validerBetingetAvsenderMottakerId(avsenderMottaker));
        if (request.getJournalpostType() != OpprettJournalpostRequest.JournalpostType.NOTAT
            && StringUtils.isEmpty(avsenderMottaker.getNavn())) {
            feil.add("AvsenderMottaker mangler navn");
        }
        return feil;
    }

    private static Collection<String> validerBetingetAvsenderMottakerId(AvsenderMottaker avsenderMottaker) {
        if (StringUtils.isNotEmpty(avsenderMottaker.getId())
            && avsenderMottaker.getIdType() == null) {
            return List.of("AvsenderMottaker mangler idType");
        }
        return Collections.emptyList();
    }

    private static Collection<String> validerKanal(OpprettJournalpostRequest request) {
        if (request.getJournalpostType() == OpprettJournalpostRequest.JournalpostType.INNGAAENDE
            && StringUtils.isEmpty(request.getKanal())) {
            return List.of("Kanal mangler for inngående journalpost");
        }
        return Collections.emptyList();
    }

    private static Collection<String> validerBruker(Bruker bruker) {
        Collection<String> feil = new ArrayList<>();
        if (StringUtils.isEmpty(bruker.getId())) {
            feil.add("Bruker.Id mangler");
        }
        if (bruker.getIdType() == null) {
            feil.add("Bruker.IdType mangler");
        }
        return feil;
    }

    private static Collection<String> validerDokument(List<Dokument> dokumenter) {
        return dokumenterHarMangler(dokumenter) ? List.of("Dokument mangler Tittel eller DokumentKategori")
            : Collections.emptyList();
    }

    private static boolean dokumenterHarMangler(List<Dokument> dokumenter) {
        return dokumenter.stream().anyMatch(dokument ->
            StringUtils.isEmpty(dokument.getTittel()) || StringUtils.isEmpty(dokument.getDokumentKategori()));
    }
}
