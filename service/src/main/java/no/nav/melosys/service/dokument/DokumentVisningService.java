package no.nav.melosys.service.dokument;

import java.util.List;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.FagsakRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DokumentVisningService {
    private static final String FINNES_IKKE = " finnes ikke.";

    private final FagsakRepository fagsakRepository;
    private final JoarkFasade joarkFasade;

    @Autowired
    public DokumentVisningService(FagsakRepository fagsakRepository, JoarkFasade joarkFasade) {
        this.fagsakRepository = fagsakRepository;
        this.joarkFasade = joarkFasade;
    }

    /**
     * Henter et dokument fra Joark
     */
    public byte[] hentDokument(String journalpostID, String dokumentID) throws IkkeFunnetException, SikkerhetsbegrensningException {
        return joarkFasade.hentDokument(journalpostID, dokumentID);
    }

    /**
     * Henter dokumenter knyttet til en sak med et gitt saksnummer
     */
    public List<Journalpost> hentDokumenter(String saksnummer) throws IkkeFunnetException, IntegrasjonException, SikkerhetsbegrensningException {
        Fagsak fagsak = fagsakRepository.findBySaksnummer(saksnummer);
        if (fagsak == null) {
            throw new IkkeFunnetException("Fagsak med saksnummer " + saksnummer + FINNES_IKKE);
        }

        return joarkFasade.hentKjerneJournalpostListe(fagsak.getGsakSaksnummer());
    }
}
