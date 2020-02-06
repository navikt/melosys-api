package no.nav.melosys.service.utpeking;

import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class UtpekingService {

    private static final Logger log = LoggerFactory.getLogger(UtpekingService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final EessiService eessiService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final UtpekingsperiodeRepository utpekingsperiodeRepository;
    private final LandvelgerService landvelgerService;

    public UtpekingService(BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService,
                           EessiService eessiService, OppgaveService oppgaveService,
                           ProsessinstansService prosessinstansService,
                           UtpekingsperiodeRepository utpekingsperiodeRepository, LandvelgerService landvelgerService) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.eessiService = eessiService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.utpekingsperiodeRepository = utpekingsperiodeRepository;
        this.landvelgerService = landvelgerService;
    }

    public Collection<Utpekingsperiode> hentUtpekingsperioder(long behandlingID) {
        return utpekingsperiodeRepository.findByBehandlingsresultat_Id(behandlingID);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public Collection<Utpekingsperiode> lagreUtpekingsperioder(long behandlingID, Collection<Utpekingsperiode> utpekingsperioder) throws FunksjonellException {
        List<Utpekingsperiode> eksisterende = utpekingsperiodeRepository.findByBehandlingsresultat_Id(behandlingID);

        for (Utpekingsperiode utpekingsperiode : eksisterende) {
            if (utpekingsperiode.getSendtUtland() != null) {
                throw new FunksjonellException("Kan ikke oppdatere utpekingsperiode etter at A003 er sendt!");
            }
        }

        Behandlingsresultat behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        utpekingsperiodeRepository.deleteByBehandlingsresultat(behandlingsresultat);
        utpekingsperiodeRepository.flush();
        utpekingsperioder.forEach(a -> a.setBehandlingsresultat(behandlingsresultat));
        return utpekingsperiodeRepository.saveAll(utpekingsperioder);
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void utpekLovvalgsland(Fagsak fagsak, List<String> mottakerinstitusjoner) throws MelosysException {
        long behandlingID = fagsak.getAktivBehandling().getId();
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        behandling.setStatus(Behandlingsstatus.UTPEKING_SENDT);
        log.info("Utpeking av annet land for sak: {}, behandling: {}, mottakerinstitusjoner: {}",
            behandling.getFagsak().getSaksnummer(), behandlingID, String.join(", ", mottakerinstitusjoner));

        List<Utpekingsperiode> utpekingsperioder = utpekingsperiodeRepository.findByBehandlingsresultat_Id(behandlingID);
        validerUtpekingsperioder(utpekingsperioder);

        mottakerinstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            mottakerinstitusjoner,
            landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID),
            BucType.LA_BUC_02
        );

        prosessinstansService.opprettProsessinstansUtpekAnnetLand(behandling, utpekingsperioder.get(0).getLovvalgsland(), mottakerinstitusjoner);
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    void validerUtpekingsperioder(List<Utpekingsperiode> utpekingsperioder) throws MelosysException {
        if (CollectionUtils.isEmpty(utpekingsperioder)) {
            throw new FunksjonellException("Du må velge en utpekingsperiode for å kunne utpeke et annet land");
        }
        if (utpekingsperioder.size() != 1) {
            throw new FunksjonellException("Flere utpekingsperioder er ikke støttet ved utpeking av et annet land");
        }
    }
}
