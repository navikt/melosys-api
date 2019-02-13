package no.nav.melosys.saksflyt.agent.hs;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.HS_HENLEGG_SAK;
import static no.nav.melosys.domain.ProsessSteg.HS_SEND_BREV;

/**
 * Henlegger Sak
 *
 * Transisjoner:
 * HS_HENLEGG_SAK -> HS_SEND_BREV eller FEILET_MASKINELT hvis feil
 */
@Component
public class HenleggSak extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(HenleggSak.class);

    private final BehandlingRepository behandlingRepo;
    private final FagsakRepository fagsakRepo;

    @Autowired
    public HenleggSak(BehandlingRepository behandlingRepo, FagsakRepository fagsakRepo) {
        this.behandlingRepo = behandlingRepo;
        this.fagsakRepo = fagsakRepo;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return HS_HENLEGG_SAK;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        Behandling behandling = prosessinstans.getBehandling();

        Fagsak fagsak = behandling.getFagsak();
        fagsak.setStatus(Saksstatuser.HENLAGT);
        fagsakRepo.save(fagsak);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandlingRepo.save(behandling);

        log.info("Satt sak til henlagt for prosessinstans {}", prosessinstans.getId());
        prosessinstans.setSteg(HS_SEND_BREV);
    }
}
