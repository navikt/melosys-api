package stub;

import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public class JoarkStub implements JoarkFasade {
    @Override
    public void ferdigstillJournalføring(String journalpostId) {

    }

    @Override
    public byte[] hentDokument(String journalPostID, String dokumentID) {
        return new byte[0];
    }

    @Override
    public Journalpost hentJournalpost(String journalpostID) {
        Journalpost journalpost = new Journalpost(journalpostID);
        journalpost.setErFerdigstilt(false);
        return journalpost;
    }

    @Override
    public List<Journalpost> hentJournalposterTilknyttetSak(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest) {
        return null;
    }

    @Override
    public String opprettJournalpost(OpprettJournalpost opprettJournalpost, boolean forsøkEndeligJfr) {
        return null;
    }

    @Override
    public void oppdaterJournalpost(String journalpostID, JournalpostOppdatering journalpostOppdatering, boolean forsøkFerdigstill) {

    }

    @Override
    public void validerDokumenterTilhørerSakOgHarTilgang(HentJournalposterTilknyttetSakRequest hentJournalposterTilknyttetSakRequest, Collection<DokumentReferanse> dokumentReferanser) {

    }

    @Override
    public LocalDate hentMottaksDatoForJournalpost(String journalpostID) {
        return null;
    }
}
