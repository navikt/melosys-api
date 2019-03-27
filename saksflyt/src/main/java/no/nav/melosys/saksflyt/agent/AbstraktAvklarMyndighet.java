package no.nav.melosys.saksflyt.agent;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.saksflyt.agent.iv.validering.SendBrevValidator;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstraktAvklarMyndighet extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(AbstraktAvklarMyndighet.class);

    private final BehandlingRepository behandlingRepository;

    private final BehandlingsresultatRepository behandlingsresultatRepository;

    private final FagsakService fagsakService;

    private final LandvelgerService landvelgerService;

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;

    public AbstraktAvklarMyndighet(BehandlingRepository behandlingRepository,
                           BehandlingsresultatRepository behandlingsresultatRepository,
                           FagsakService fagsakService,
                           LandvelgerService landvelgerService,
                           UtenlandskMyndighetRepository utenlandskMyndighetRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.fagsakService = fagsakService;
        this.landvelgerService = landvelgerService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
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
                Landkoder landkode = landvelgerService.hentTrygdemyndighetsland(behandling);
                UtenlandskMyndighet myndighet = utenlandskMyndighetRepository.findByLandkode(landkode);
                String institusjonsID = landkode.getKode() + ":" + myndighet.institusjonskode;
                fagsakService.leggTilAktør(saksnummer, Aktoersroller.MYNDIGHET, institusjonsID);
                log.info("Avklart landkode {} og myndighet {} for sak {}.", landkode, myndighet.institusjonskode, saksnummer);
            } else {
                log.debug("Sak {} har allerede en myndighet med kode {}", saksnummer, myndighetPart.getInstitusjonId());
            }
        }
    }
}
