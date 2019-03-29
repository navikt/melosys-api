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
import no.nav.melosys.service.dokument.sed.SedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktSendSed extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AbstraktSendSed.class);

    private final BehandlingRepository behandlingRepository;
    private final SedService sedService;
    private final BehandlingsresultatService behandlingsresultatService;

    protected AbstraktSendSed(BehandlingRepository behandlingRepository, SedService sedService, BehandlingsresultatService behandlingsresultatService) {
        this.behandlingRepository = behandlingRepository;
        this.sedService = sedService;
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
            sedService.opprettOgSendSed(behandling, behandlingsresultat);
        }
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    protected abstract boolean skalSendeSed(Behandlingsresultat behandlingsresultat);
}
