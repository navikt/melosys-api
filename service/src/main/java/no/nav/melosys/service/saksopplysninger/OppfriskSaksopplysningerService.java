package no.nav.melosys.service.saksopplysninger;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService;
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

    private final AnmodningsperiodeService anmodningsperiodeService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final UfmKontrollService ufmKontrollService;
    private final InngangsvilkaarService inngangsvilkaarService;
    private final RegisteropplysningerService registeropplysningerService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerFactory registeropplysningerFactory;
    private final ÅrsavregningService årsavregningService;
    private final HelseutgiftDekkesPeriodeService helseutgiftDekkesPeriodeService;

    public OppfriskSaksopplysningerService(AnmodningsperiodeService anmodningsperiodeService,
                                           BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           UfmKontrollService ufmKontrollService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           PersondataFasade persondataFasade,
                                           RegisteropplysningerFactory registeropplysningerFactory,
                                           ÅrsavregningService årsavregningService,
                                           HelseutgiftDekkesPeriodeService helseutgiftDekkesPeriodeService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerFactory = registeropplysningerFactory;
        this.årsavregningService = årsavregningService;
        this.helseutgiftDekkesPeriodeService = helseutgiftDekkesPeriodeService;
    }

    @Transactional
    public void oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(long behandlingID, boolean periodeOver5aar) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (behandling.erUtsending() && anmodningsperiodeService.harSendtAnmodningsperiode(behandlingID)) {
            throw new FunksjonellException("Anmodning om unntak er sendt for behandling %s. ".formatted(
                behandlingID) + "Det er ikke lenger mulig å endre mottatteOpplysninger og saksopplysninger");
        }

        log.info("Starter oppdatering av registeropplysninger og tilbakestilling av behandlingsresultat for behandlingID: {} ", behandlingID);
        oppdaterRegisteropplysninger(behandlingID, periodeOver5aar, behandling);

        // Trenger ikke å tømme hvis det er EØS-pensjonist.
        // Alt i behandlingsresultatet er null uansett unntatt avklarte fakta.
        // Vi trenger avklarte fakta fordi den inneholder avhuking av åpen sluttdato.
        if (!behandling.erEøsPensjonist()) {
            behandlingsresultatService.tømBehandlingsresultat(behandlingID);
        }

        if (behandling.erBehandlingAvSed()) {
            ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingID);
        }

        if (inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)) {
            ErPeriode periode = behandling.erÅrsavregning() ?
                hentPeriodeForÅrsavregning(behandlingID) : behandling.finnPeriode().orElse(new Periode());

            inngangsvilkaarService.vurderOgLagreInngangsvilkår(
                behandlingID,
                behandling.hentSøknadsLand(),
                behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.isFlereLandUkjentHvilke(),
                periode
            );
        }
    }

    @Transactional
    public void oppdaterSaksopplysningerForAarsavregning(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        log.info("Starter oppdatering av registeropplysninger for behandlingID: {} ", behandlingID);
        oppdaterRegisteropplysninger(behandlingID, false, behandling);
    }

    private void oppdaterRegisteropplysninger(long behandlingID, boolean periodeOver5aar, Behandling behandling) {
        Optional<String> aktørIdOptional = Optional.ofNullable(behandling.getFagsak().finnBrukersAktørID());
        String brukerID = aktørIdOptional.map(persondataFasade::hentFolkeregisterident).orElse(null);

        //OK om perioden er tom. Ikke alle behandlingstema krever periode.
        ErPeriode periode;
        if (behandling.erÅrsavregning()) {
            periode = hentPeriodeForÅrsavregning(behandlingID);
        } else if (behandling.erEøsPensjonist()) {
            periode = hentPeriodeForEøsPensjonist(behandlingID);
        } else {
            periode = behandling.finnPeriode().orElse(new Periode());
        }

        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(behandlingID)
            .saksopplysningTyper(registeropplysningerFactory.utledSaksopplysningTyper(
                behandling.getFagsak().getType(),
                behandling.getFagsak().getTema(),
                behandling.getTema(),
                behandling.getType()
            ))
            .fnr(brukerID)
            .fom(periode.getFom())
            .tom(periode.getTom())
            .hentOpplysningerFor5aar(periodeOver5aar)
            .build();

        registeropplysningerService.slettRegisterOpplysninger(behandlingID);
        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest);
    }


    private ErPeriode hentPeriodeForEøsPensjonist(Long behandlingID) {
        HelseutgiftDekkesPeriode helseutgiftDekkesPeriode =
            helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(behandlingID);

        if (helseutgiftDekkesPeriode != null) {
            LocalDate fomDato = helseutgiftDekkesPeriode.getFomDato();
            LocalDate tomDato = helseutgiftDekkesPeriode.getTomDato();

            if (fomDato.isAfter(LocalDate.now())) {
                fomDato = LocalDate.now();
            }

            return new Periode(fomDato, tomDato);
        }
        return new Periode();
    }

    private ErPeriode hentPeriodeForÅrsavregning(Long behandlingID) {
        Integer gjeldendeÅrForÅrsavregning = årsavregningService.finnGjeldendeÅrForÅrsavregning(behandlingID);
        int år = gjeldendeÅrForÅrsavregning == null ? LocalDate.now().getYear() : gjeldendeÅrForÅrsavregning;

        return new Periode(LocalDate.of(år, Month.JANUARY, 1), hentTomForÅrsavregning(år));
    }

    private LocalDate hentTomForÅrsavregning(int år) {
        LocalDate now = LocalDate.now();
        if (now.getYear() == år) {
            return now;
        }
        return LocalDate.of(år, Month.DECEMBER, 31);
    }
}
