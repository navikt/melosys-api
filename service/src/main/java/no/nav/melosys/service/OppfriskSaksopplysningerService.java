package no.nav.melosys.service;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.sak.FagsakService;
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
    private final FagsakService fagsakService;
    private final KontrollresultatService kontrollresultatService;
    private final InngangsvilkaarService inngangsvilkaarService;
    private final RegisteropplysningerService registeropplysningerService;
    private final TpsFasade tpsFasade;

    public OppfriskSaksopplysningerService(BehandlingService behandlingService,
                                           BehandlingsresultatService behandlingsresultatService,
                                           FagsakService fagsakService,
                                           KontrollresultatService kontrollresultatService,
                                           InngangsvilkaarService inngangsvilkaarService,
                                           RegisteropplysningerService registeropplysningerService,
                                           TpsFasade tpsFasade) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.fagsakService = fagsakService;
        this.kontrollresultatService = kontrollresultatService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.registeropplysningerService = registeropplysningerService;
        this.tpsFasade = tpsFasade;
    }

    @Transactional(rollbackFor = MelosysException.class)
    public void oppfriskSaksopplysning(long behandlingID, boolean medFamilierelasjoner) throws MelosysException {
        log.info("Starter oppfrisking av behandlingID: {} ", behandlingID);

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        String aktørID = behandling.getFagsak().hentBruker().getAktørId();
        String brukerID = tpsFasade.hentIdentForAktørId(aktørID);

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

        Fagsak fagsak = behandling.getFagsak();
        if (behandling.kanResultereIVedtak() && fagsak.getType() == Sakstyper.UKJENT) {
            boolean kvalifisererForEF_883_2004 = inngangsvilkaarService.vurderOgLagreInngangsvilkår(behandlingID, behandling.finnSøknadsLand(), periode);
            fagsakService.oppdaterType(fagsak, kvalifisererForEF_883_2004);
        }
    }
}
