package no.nav.melosys.service.saksopplysninger;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OppfriskSaksopplysningerService {
    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysningerService.class);

    private final ConcurrentHashMap<Long, Lock> locks = new ConcurrentHashMap<>();

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UfmKontrollService ufmKontrollService;
    private final InngangsvilkaarService inngangsvilkaarService;
    private final RegisteropplysningerService registeropplysningerService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerFactory registeropplysningerFactory;
    private final ÅrsavregningService årsavregningService;

    public OppfriskSaksopplysningerService(AnmodningsperiodeService anmodningsperiodeService,
                                           BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           UfmKontrollService ufmKontrollService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           PersondataFasade persondataFasade,
                                           RegisteropplysningerFactory registeropplysningerFactory,
                                           ÅrsavregningService årsavregningService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerFactory = registeropplysningerFactory;
        this.årsavregningService = årsavregningService;
    }

    @Transactional
    public void oppfriskSaksopplysning(long behandlingID, boolean periodeOver5år) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (behandling.erUtsending() && anmodningsperiodeService.harSendtAnmodningsperiode(behandlingID)) {
            throw new FunksjonellException("Anmodning om unntak er sendt for behandling %s. ".formatted(
                behandlingID) + "Det er ikke lenger mulig å endre mottatteOpplysninger og saksopplysninger");
        }

        String aktørId = behandling.getFagsak().finnBrukersAktørID();
        String brukerID = aktørId != null ? persondataFasade.hentFolkeregisterident(aktørId) : null;

        //OK om perioden er tom. Ikke alle behandlingstema krever periode.
        ErPeriode periode = behandling.erÅrsavregning() ?
            hentPeriodeForÅrsavregning(behandlingID) : behandling.finnPeriode().orElse(new Periode());

        RegisteropplysningerRequest nyRegisteropplysningerRequest = lagRegisteropplysningerRequest(behandling, periodeOver5år, brukerID, periode);

        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);
        executeWithLock(behandlingID, () -> tilbakestillBehandling(behandlingID, nyRegisteropplysningerRequest));

        if (behandling.erBehandlingAvSed()) {
            ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingID);
        }

        if (inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)) {
            inngangsvilkaarService.vurderOgLagreInngangsvilkår(
                behandlingID,
                behandling.hentSøknadsLand(),
                behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.isFlereLandUkjentHvilke(),
                periode
            );
        }
    }

    private RegisteropplysningerRequest lagRegisteropplysningerRequest(Behandling behandling, boolean periodeOver5år, String brukerID, ErPeriode periode) {
        return RegisteropplysningerRequest.builder()
            .behandlingID(behandling.getId())
            .saksopplysningTyper(registeropplysningerFactory.utledSaksopplysningTyper(
                behandling.getFagsak().getType(),
                behandling.getFagsak().getTema(),
                behandling.getTema(),
                behandling.getType()
            ))
            .fnr(brukerID)
            .fom(periode.getFom())
            .tom(periode.getTom())
            .hentOpplysningerFor5aar(periodeOver5år)
            .build();
    }

    private void executeWithLock(long behandlingID, Runnable action) {
        Lock lock = locks.computeIfAbsent(behandlingID, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new IllegalStateException("En annen tråd er allerede i gang med  behandlingID: " + behandlingID);
        }
        try {
            action.run();
        } finally {
            lock.unlock();
            locks.remove(behandlingID, lock);
        }
    }

    private void tilbakestillBehandling(long behandlingID, RegisteropplysningerRequest registeropplysningerRequest) {
        registeropplysningerService.slettRegisterOpplysninger(behandlingID);
        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest);
        behandlingsresultatService.tømBehandlingsresultat(behandlingID);
    }

    private ErPeriode hentPeriodeForÅrsavregning(Long behandlingID) {
        int år = Optional.ofNullable(årsavregningService.finnGjeldendeÅrForÅrsavregning(behandlingID))
            .orElse(LocalDate.now().getYear());

        return new Periode(LocalDate.of(år, Month.JANUARY, 1), hentTomForÅrsavregning(år));
    }

    private LocalDate hentTomForÅrsavregning(int år) {
        return LocalDate.now().getYear() == år ? LocalDate.now() : LocalDate.of(år, Month.DECEMBER, 31);
    }
}
