package no.nav.melosys.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Bruker;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;

/**
 * Brukes til å opprette en sak i GSAK og i databasen etter en søknad er mottat
 */
@Component
public class OpprettSakService {

    private static final Logger log = LoggerFactory.getLogger(OpprettSakService.class);

    private GsakFasade gsakFasade;

    private TpsFasade tpsFasade;

    private FagsakRepository fagsakRepo;

    private BehandlingRepository behandlingRepo;

    @Autowired
    public OpprettSakService(GsakFasade gsakFasade, TpsFasade tpsFasade, FagsakRepository fagsakRepo, BehandlingRepository behandlingRepo) {
        this.gsakFasade = gsakFasade;
        this.tpsFasade = tpsFasade;
        this.fagsakRepo = fagsakRepo;
        this.behandlingRepo = behandlingRepo;
    }

    // TODO En søknad skal erstatte fnr her
    @Transactional
    public Fagsak opprettSak(String fnr) {
        Optional<Long> aktørId = tpsFasade.hentAktørIdForIdent(fnr);
        if (!aktørId.isPresent()) {
            throw new IllegalArgumentException("Finner ikke aktørID for fnr: " + fnr);
        }

        Bruker bruker = new Bruker();
        bruker.setAktørId(aktørId.get());
        bruker.setFnr(fnr);
        bruker = tpsFasade.hentKjerneinformasjon(bruker);

        // Oppretter en sak i DB slik at id kan brukes i GSAK
        Fagsak fagsak = new Fagsak();
        fagsak.setBruker(bruker);
        fagsakRepo.save(fagsak);

        // Oppretter en behandling knyttet til saken
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setType(BehandlingType.FØRSTEGANGSSØKNAD);
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandlingRepo.save(behandling); // TODO Vilkårene/behandlingsgrunnlaget må lagres også

        // Oppretter en sak i GSAK
        String saksNummer = gsakFasade.opprettSak(fagsak.getId(), fnr);

        // Oppdaterer fagsak med saksnummer fra GSAK og endre status
        fagsak.setSaksnummer(Long.parseLong(saksNummer));
        fagsak.setStatus(FagsakStatus.UBEH); // FIXME FA riktig status
        fagsakRepo.save(fagsak);

        return fagsak;
    }

}
