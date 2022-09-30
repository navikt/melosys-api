package no.nav.melosys.service.saksopplysninger;

import java.time.LocalDate;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory.utledSaksopplysningTyper;

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
    private final Unleash unleash;

    public OppfriskSaksopplysningerService(AnmodningsperiodeService anmodningsperiodeService,
                                           BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           UfmKontrollService ufmKontrollService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           PersondataFasade persondataFasade,
                                           Unleash unleash) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.ufmKontrollService = ufmKontrollService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
    }

    @Transactional
    public void oppfriskSaksopplysning(long behandlingID, boolean medFamilierelasjoner) {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);

        if (behandling.erUtsending() && anmodningsperiodeService.harSendtAnmodningsperiode(behandlingID)) {
            throw new FunksjonellException("Anmodning om unntak er sendt for behandling %s. ".formatted(
                behandlingID) + "Det er ikke lenger mulig å endre behandlingsgrunnlag og saksopplysninger");
        }

        String aktørID = behandling.getFagsak().hentBrukersAktørID();
        String brukerID = persondataFasade.hentFolkeregisterident(aktørID);

        //OK om perioden er tom. Ikke alle behandlingstema krever periode.
        //Implisitt at perioden eksisterer om behandling kan resultere i vedtak
        ErPeriode periode = behandling.finnPeriode().orElse(new Periode());
        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();

        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(behandlingID)
            .saksopplysningTyper(utledSaksopplysningTyper(
                behandling.getFagsak().getType(),
                behandling.getTema(),
                behandling.getType(),
                unleash.isEnabled("melosys.behandle_alle_saker")))
            .fnr(brukerID)
            .fom(fom)
            .tom(tom)
            .informasjonsbehov(medFamilierelasjoner
                ? Informasjonsbehov.MED_FAMILIERELASJONER
                : Informasjonsbehov.STANDARD)
            .build();

        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);
        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest);
        behandlingsresultatService.tømBehandlingsresultat(behandlingID);

        if (behandling.erBehandlingAvSed()) {
            ufmKontrollService.utførKontrollerOgRegistrerFeil(behandlingID);
        }

        if (behandling.kanResultereIVedtak()
            && behandling.getFagsak().getType() == Sakstyper.EU_EOS
            && !inngangsvilkaarService.oppfyllervurderingEF_883_2004(behandlingID)) {
            inngangsvilkaarService.vurderOgLagreInngangsvilkår(
                behandlingID,
                behandling.finnSøknadsLand(),
                behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand,
                periode
            );
        }
    }
}
