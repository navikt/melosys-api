package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakStatus;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brukes til å opprette en sak i GSAK og i databasen etter en søknad er mottat
 */
@Service
public class MottakService {

    private static final Logger log = LoggerFactory.getLogger(MottakService.class);

    private GsakFasade gsakFasade;

    private TpsFasade tpsFasade;

    private FagsakRepository fagsakRepo;

    private BehandlingRepository behandlingRepo;

    @Autowired
    public MottakService(GsakFasade gsakFasade, TpsFasade tpsFasade, FagsakRepository fagsakRepo, BehandlingRepository behandlingRepo) {
        this.gsakFasade = gsakFasade;
        this.tpsFasade = tpsFasade;
        this.fagsakRepo = fagsakRepo;
        this.behandlingRepo = behandlingRepo;
    }

    // TODO En søknad skal erstatte fnr her
    @Transactional
    public Behandling opprettSak(String fnr) {

        // Oppretter en sak i DB slik at id kan brukes i GSAK
        Fagsak fagsak = new Fagsak();
        fagsakRepo.save(fagsak);

        // Oppretter en behandling knyttet til saken
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setType(BehandlingType.SØKNAD);
        behandling.setStatus(BehandlingStatus.OPPRETTET);
        behandlingRepo.save(behandling); // TODO Vilkårene/behandlingsgrunnlaget må lagres også

        return behandling;
    }

    @Transactional
    public Behandling klargjoer(Behandling behandling) {

        // FIXME TPS oppslag. Har vi aktørId eller fnr her?
        String fnr = null;

        Fagsak fagsak = behandling.getFagsak();

        // Beriker brukeren med informasjon fra TPS
        // TODO bruker eksisterer ikke lenger
        // fagsak.setBruker(bruker);

        // Oppretter en sak i GSAK
        // TODO Francois koble til eksisterende sak
        String saksNummer = gsakFasade.opprettSak(fagsak.getId(), fnr);

        // Oppdaterer fagsak med saksnummer fra GSAK
        fagsak.setSaksnummer(Long.parseLong(saksNummer));
        fagsak.setStatus(FagsakStatus.OPPRETTET); // FIXME FA riktig status
        fagsakRepo.save(fagsak);

        return behandling;
    }

}
