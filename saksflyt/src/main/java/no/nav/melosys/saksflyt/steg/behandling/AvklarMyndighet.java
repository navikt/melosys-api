package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.AVKLAR_MYNDIGHET;

@Component
public class AvklarMyndighet implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(AvklarMyndighet.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;

    public AvklarMyndighet(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService, UtenlandskMyndighetService utenlandskMyndighetService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return AVKLAR_MYNDIGHET;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID);
        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        boolean innvilgelseEllerAnmodningUnntakSkalSendes = prosessinstans.getType() == ProsessType.ANMODNING_OM_UNNTAK
            || behandlingsresultat.erInnvilgelse();
        boolean søknadSkalVideresendes = behandling.getFagsak().getStatus() == Saksstatuser.VIDERESENDT;

        if (innvilgelseEllerAnmodningUnntakSkalSendes || søknadSkalVideresendes) {
            log.info("Avklarer myndighet for behandling {}", behandlingID);
            utenlandskMyndighetService.avklarUtenlandskMyndighetSomAktørOgLagre(behandling);
        }
    }
}
