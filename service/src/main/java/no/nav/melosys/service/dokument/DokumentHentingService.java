package no.nav.melosys.service.dokument;

import java.util.List;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.integrasjon.joark.HentJournalposterTilknyttetSakRequest;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;

@Service
public class DokumentHentingService {

    private final FagsakService fagsakService;
    private final JoarkFasade joarkFasade;

    public DokumentHentingService(FagsakService fagsakService, JoarkFasade joarkFasade) {
        this.fagsakService = fagsakService;
        this.joarkFasade = joarkFasade;
    }

    /**
     * Henter et dokument fra Joark
     */
    public byte[] hentDokument(String journalpostID, String dokumentID) {
        return joarkFasade.hentDokument(journalpostID, dokumentID);
    }

    /**
     * Henter dokumenter knyttet til en sak med et gitt saksnummer
     */
    public List<Journalpost> hentDokumenter(String saksnummer) {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        return joarkFasade.hentJournalposterTilknyttetSak(new HentJournalposterTilknyttetSakRequest(fagsak.getGsakSaksnummer(), saksnummer));
    }
}
