package no.nav.melosys.service;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory.utledSaksopplysningTyper;

@Service
public class OppfriskSaksopplysningerService {
    private static final Logger log = LoggerFactory.getLogger(OppfriskSaksopplysningerService.class);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final KontrollresultatService kontrollresultatService;
    private final InngangsvilkaarService inngangsvilkaarService;
    private final RegisteropplysningerService registeropplysningerService;
    private final PersondataFasade persondataFasade;

    public OppfriskSaksopplysningerService(BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           KontrollresultatService kontrollresultatService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           PersondataFasade persondataFasade) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.kontrollresultatService = kontrollresultatService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.persondataFasade = persondataFasade;
    }

    @Transactional
    public void oppfriskSaksopplysning(long behandlingID, boolean medFamilierelasjoner) {
        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        String aktørID = behandling.getFagsak().hentBruker().getAktørId();
        String brukerID = persondataFasade.hentFolkeregisterIdent(aktørID);

        //OK om perioden er tom. Ikke alle behandlingstema krever periode.
        //Implisitt at perioden eksisterer om behandling kan resultere i vedtak
        ErPeriode periode = behandling.finnPeriode().orElse(new Periode());
        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();

        RegisteropplysningerRequest registeropplysningerRequest = RegisteropplysningerRequest.builder()
            .behandlingID(behandlingID)
            .saksopplysningTyper(utledSaksopplysningTyper(behandling.getTema()))
            .fnr(brukerID)
            .fom(fom)
            .tom(tom)
            .informasjonsbehov(medFamilierelasjoner
                ? Informasjonsbehov.MED_FAMILIERELASJONER
                : Informasjonsbehov.STANDARD)
            .build();

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequest);
        behandlingsresultatService.tømBehandlingsresultat(behandlingID);

        if (behandling.erBehandlingAvSed()) {
            kontrollresultatService.utførKontrollerOgRegistrerFeil(behandlingID);
        }

        if (behandling.kanResultereIVedtak()
            && behandling.getFagsak().getType() == Sakstyper.EU_EOS
            && !inngangsvilkaarService.oppfyllervurderingEF_883_2004(behandlingID)) {
            inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, behandling.finnSøknadsLand(), periode);
        }
    }
}
