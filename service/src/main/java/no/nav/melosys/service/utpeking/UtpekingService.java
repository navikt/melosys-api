package no.nav.melosys.service.utpeking;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.melding.UtpekingAvvis;
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.UtpekingsperiodeRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    public void utpekLovvalgsland(Fagsak fagsak, List<String> mottakerinstitusjoner, String ytterligereInformasjonSed) throws MelosysException {
        long behandlingID = fagsak.getAktivBehandling().getId();
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        log.info("Utpeking av annet land for sak: {}, behandling: {}, mottakerinstitusjoner: {}",
            behandling.getFagsak().getSaksnummer(), behandlingID, String.join(", ", mottakerinstitusjoner));

        List<Utpekingsperiode> utpekingsperioder = utpekingsperiodeRepository.findByBehandlingsresultat_Id(behandlingID);
        validerUtpekingsperioder(utpekingsperioder);

        mottakerinstitusjoner = eessiService.validerOgAvklarMottakerInstitusjonerForBuc(
            mottakerinstitusjoner,
            landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID),
            BucType.LA_BUC_02
        );

        prosessinstansService.opprettProsessinstansUtpekAnnetLand(
            behandling, utpekingsperioder.get(0).getLovvalgsland(), mottakerinstitusjoner, ytterligereInformasjonSed
        );
        oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.getFagsak().getSaksnummer());
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void avvisUtpeking(long behandlingID, UtpekingAvvis utpekingAvvis) throws FunksjonellException {
        validerAvslagUtpeking(utpekingAvvis);

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_UTL);
        behandlingService.lagre(behandling);
        if (behandling.erUtpekingAvAnnetLand()) {
            behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandlingID, Utfallregistreringunntak.IKKE_GODKJENT);
        }

        prosessinstansService.opprettProsessinstansAvvisUtpeking(behandling, utpekingAvvis);
    }

    private void validerUtpekingsperioder(List<Utpekingsperiode> utpekingsperioder) throws MelosysException {
        if (CollectionUtils.isEmpty(utpekingsperioder)) {
            throw new FunksjonellException("Du må velge en utpekingsperiode for å kunne utpeke et annet land");
        }
        if (utpekingsperioder.size() != 1) {
            throw new FunksjonellException("Flere utpekingsperioder er ikke støttet ved utpeking av et annet land");
        }
    }

    private void validerAvslagUtpeking(UtpekingAvvis utpekingAvvis) throws FunksjonellException {
        if (StringUtils.isEmpty(utpekingAvvis.getBegrunnelse())) {
            throw new FunksjonellException("Du må oppgi en begrunnelse for å kunne avslå en utpeking");
        }
        if (utpekingAvvis.isEtterspørInformasjon() == null) {
            throw new FunksjonellException("Du må oppgi om forespørsel om mer informasjon vil bli sendt");
        }
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppdaterSendtUtland(Utpekingsperiode utpekingsperiode) throws FunksjonellException, TekniskException {

        if (utpekingsperiode.getId() == null) {
            throw new TekniskException("Forsøk på å oppdatere en ikke-persistert utpekingsperiode");
        } else if (utpekingsperiode.getSendtUtland() != null) {
            throw new FunksjonellException("Utpekingsperiode " + utpekingsperiode.getId() + " er allerede markert som sendtUtland");
        }

        utpekingsperiode.setSendtUtland(LocalDate.now());
        utpekingsperiodeRepository.save(utpekingsperiode);
    }
}
