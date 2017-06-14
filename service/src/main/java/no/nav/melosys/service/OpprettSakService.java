package no.nav.melosys.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.Bruker;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;

@Service
public class OpprettSakService {

    private static final Logger log = LoggerFactory.getLogger(OpprettSakService.class);

    private GsakFasade gsakFasade;

    private TpsFasade tpsFasade;

    private FagsakRepository fagsakRepo;

    @Autowired
    public OpprettSakService(GsakFasade gsakFasade, TpsFasade tpsFasade, FagsakRepository fagsakRepo) {
        this.gsakFasade = gsakFasade;
        this.tpsFasade = tpsFasade;
        this.fagsakRepo = fagsakRepo;
    }

    public Fagsak opprettSak(String fnr) {
        Optional<Long> aktørId = tpsFasade.hentAktørIdForIdent(fnr);
        if (!aktørId.isPresent()) {
            throw new IllegalArgumentException("Finner ikke aktørID");
        }

        Bruker bruker = new Bruker();
        bruker.setAktørId(aktørId.get());
        bruker.setFnr(fnr);
        bruker = tpsFasade.hentKjerneinformasjon(bruker);

        // Oppretter en sak i DB
        Fagsak fagsak = new Fagsak();
        fagsak.setBruker(bruker);
        fagsakRepo.save(fagsak);

        // Oppretter en sak i GSAK
        String saksNummer = gsakFasade.opprettSak(fagsak.getId(), fnr);

        // Oppdaterer fagsak med saksnummer fra GSAK og endre status
        fagsak.setSaksnummer(Long.parseLong(saksNummer));
        fagsak.setStatus(FagsakStatus.UBEH); // FIXME FA riktig status
        fagsakRepo.save(fagsak);

        return fagsak;
    }

}
