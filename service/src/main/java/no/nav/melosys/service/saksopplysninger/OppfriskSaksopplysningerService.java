package no.nav.melosys.service.saksopplysninger;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
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
    private final SaksbehandlingRegler saksbehandlingRegler;

    public OppfriskSaksopplysningerService(AnmodningsperiodeService anmodningsperiodeService,
                                           BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           UfmKontrollService ufmKontrollService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           PersondataFasade persondataFasade,
                                           RegisteropplysningerFactory registeropplysningerFactory,
                                           SaksbehandlingRegler saksbehandlingRegler) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerFactory = registeropplysningerFactory;
        this.saksbehandlingRegler = saksbehandlingRegler;
    }

    @Transactional
    public void oppfriskSaksopplysning(long behandlingID, boolean periodeOver5aar) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (behandling.erUtsending() && anmodningsperiodeService.harSendtAnmodningsperiode(behandlingID)) {
            throw new FunksjonellException("Anmodning om unntak er sendt for behandling %s. ".formatted(
                behandlingID) + "Det er ikke lenger mulig å endre mottatteOpplysninger og saksopplysninger");
        }

        Optional<String> aktørIdOptional = behandling.getFagsak().finnBrukersAktørID();
        String brukerID = aktørIdOptional.map(persondataFasade::hentFolkeregisterident).orElse(null);

        //OK om perioden er tom. Ikke alle behandlingstema krever periode.
        ErPeriode periode = behandling.finnPeriode().orElse(new Periode());

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

        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);
        registeropplysningerService.slettRegisterOpplysninger(behandlingID);
        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest);
        behandlingsresultatService.tømBehandlingsresultat(behandlingID);

        if (behandling.erBehandlingAvSed()) {
            ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingID);
        }

        if (behandling.getFagsak().erSakstypeEøs()
            && behandling.harPeriodeOgLand()
            && !saksbehandlingRegler.harIngenFlyt(behandling)
            && behandling.kanResultereIVedtak()
            && !inngangsvilkaarService.oppfyllervurderingEF_883_2004(behandlingID)) {
            inngangsvilkaarService.vurderOgLagreInngangsvilkår(
                behandlingID,
                behandling.hentSøknadsLand(),
                behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.erUkjenteEllerAlleEosLand,
                periode
            );
        }
    }
}
