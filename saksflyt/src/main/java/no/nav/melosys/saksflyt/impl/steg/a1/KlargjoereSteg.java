package no.nav.melosys.saksflyt.impl.steg.a1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.saksflyt.api.Steg;
import no.nav.melosys.saksflyt.impl.Utils;
import no.nav.melosys.service.MottakService;

/**
 * Steget sørger for å * hente opp personinfo * Koble til ny eller eksisterende sak * Ferdigstille journalføring
 * 
 * Transisjoner: NY → KLARGJORT: Hvis det lar seg gjøre å klargjøre maskinelt NY → FEILET: Hvis ingen treff i TPS etc.
 * 
 * Status blir uforandret dersom TPS ikke svarer
 * 
 */
@Component
public class KlargjoereSteg implements Steg {

    private static final Logger log = LoggerFactory.getLogger(KlargjoereSteg.class);

    @Autowired
    private Binge binge;

    @Autowired
    private BehandlingRepository behandlingRepo;

    @Autowired
    private MottakService mottakService;

    @Override
    public void finnBehandlingOgUtfoerSteg() {
        Behandling behandling = binge.fjernFørsteBehandling(Utils.medStatus(BehandlingStatus.OPPRETTET));
        if (behandling == null) {
            // Ingenting å gjøre
            return;
        }

        // Hent personopplysninger fra TPS
        // Sett personopplysninger på sak

        Fagsak sak = behandling.getFagsak();

        // TODO Journalføring i JOARK

        // Endre status på behandlingen
        behandling.setStatus(BehandlingStatus.KLARGJORT);
        behandlingRepo.save(behandling);
        binge.leggTil(behandling);
    }

}
