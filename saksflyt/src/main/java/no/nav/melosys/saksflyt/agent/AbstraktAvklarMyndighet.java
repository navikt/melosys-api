package no.nav.melosys.saksflyt.agent;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator;
import no.nav.melosys.service.aktoer.AvklarMyndighetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktAvklarMyndighet extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AbstraktAvklarMyndighet.class);

    private final BehandlingRepository behandlingRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final AvklarMyndighetService avklarMyndighetService;

    public AbstraktAvklarMyndighet(BehandlingRepository behandlingRepository,
                                   BehandlingsresultatRepository behandlingsresultatRepository,
                                   AvklarMyndighetService avklarMyndighetService) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.avklarMyndighetService = avklarMyndighetService;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {

        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);

        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.findWithSaksbehandlingById(behandlingID)
            .orElseThrow(() -> new TekniskException("Behandlingsresultat " + behandlingID + " finnes ikke."));

        boolean innvilgelseEllerAnmodningUnntakSkalSendes = prosessinstans.getType() == ProsessType.ANMODNING_OM_UNNTAK ||
            SendBrevValidator.innvilgelsesbrevSkalSendes(behandlingsresultat.getType(), behandlingsresultat.getLovvalgsperioder().iterator().next());

        if (innvilgelseEllerAnmodningUnntakSkalSendes) {

            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            String saksnummer = fagsak.getSaksnummer();
            Aktoer myndighetPart = fagsak.hentAktørMedRolleType(Aktoersroller.MYNDIGHET);
            if (myndighetPart == null) {
                avklarMyndighetService.avklarMyndighetOgLagre(behandling);
                log.info("Avklart myndighet for sak {}.", saksnummer);
            } else {
                log.debug("Sak {} har allerede en myndighet med kode {}", saksnummer, myndighetPart.getInstitusjonId());
            }
        }
    }
}
