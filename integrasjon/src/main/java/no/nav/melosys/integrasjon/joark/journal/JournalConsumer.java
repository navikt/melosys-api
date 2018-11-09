package no.nav.melosys.integrasjon.joark.journal;


import no.nav.tjeneste.virksomhet.journal.v3.*;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.*;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentURLResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;

public interface JournalConsumer {
    HentDokumentResponse hentDokument(HentDokumentRequest request) throws HentDokumentDokumentIkkeFunnet, HentDokumentSikkerhetsbegrensning, HentDokumentJournalpostIkkeFunnet;

    HentDokumentURLResponse hentDokumentURL(HentDokumentURLRequest request) throws HentDokumentURLDokumentIkkeFunnet, HentDokumentURLSikkerhetsbegrensning;

    HentKjerneJournalpostListeResponse hentKjerneJournalpostListe(HentKjerneJournalpostListeRequest request) throws HentKjerneJournalpostListeSikkerhetsbegrensning, HentKjerneJournalpostListeUgyldigInput;
}
