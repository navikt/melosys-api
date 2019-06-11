package no.nav.melosys.saksflyt.agent;

import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktSendSed extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AbstraktSendSed.class);

    protected final BehandlingRepository behandlingRepository;
    private final EessiService eessiService;
    private final BehandlingsresultatService behandlingsresultatService;

    protected AbstraktSendSed(BehandlingRepository behandlingRepository, EessiService eessiService, BehandlingsresultatService behandlingsresultatService) {
        this.behandlingRepository = behandlingRepository;
        this.eessiService = eessiService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(prosessinstans.getBehandling().getId());
        if (behandling == null) {
            throw new TekniskException(String.format("Finner ikke behandlingen %s.", prosessinstans.getBehandling().getId()));
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.getId());
        if (skalSendeSed(behandlingsresultat)) {
            log.info("Starter sending av SED for behandling {}", behandling.getId());
            eessiService.opprettOgSendSed(behandling, behandlingsresultat);
        }
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    protected abstract boolean skalSendeSed(Behandlingsresultat behandlingsresultat);
}
