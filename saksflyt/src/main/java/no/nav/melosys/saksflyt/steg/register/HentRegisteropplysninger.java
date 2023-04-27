package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_REGISTEROPPLYSNINGER;

@Component
public class HentRegisteropplysninger implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentRegisteropplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final RegisteropplysningerFactory registeropplysningerFactory;

    public HentRegisteropplysninger(RegisteropplysningerService registeropplysningerService,
                                    BehandlingService behandlingService,
                                    PersondataFasade persondataFasade,
                                    RegisteropplysningerFactory registeropplysningerFactory) {
        this.registeropplysningerService = registeropplysningerService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.registeropplysningerFactory = registeropplysningerFactory;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HENT_REGISTEROPPLYSNINGER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        boolean erForVirksomhet = behandling.getFagsak().getHovedpartRolle() == Aktoersroller.VIRKSOMHET;

        if (!behandling.getFagsak().erSakstypeEøs() || erForVirksomhet) {
            log.debug("Hopper over steg {} fordi sak {} har sakstype {} og behandlingstema {}", HENT_REGISTEROPPLYSNINGER.getKode(), behandling.getFagsak().getSaksnummer(), behandling.getFagsak().getType(), behandling.getTema());
            return;
        }

        var aktørId = behandling.getFagsak().finnBrukersAktørID().orElseThrow(
            () -> new FunksjonellException("Kan ikke hente registreopplysninger når bruker ikke har aktørID")
        );

        var registeropplysningerRequestBuilder = RegisteropplysningerRequest.builder()
            .behandlingID(prosessinstans.getBehandling().getId())
            .fnr(persondataFasade.hentFolkeregisterident(aktørId))
            .saksopplysningTyper(registeropplysningerFactory.utledSaksopplysningTyper(
                behandling.getFagsak().getType(),
                behandling.getFagsak().getTema(),
                behandling.getTema(),
                behandling.getType()
            ));

        behandling.finnPeriode().ifPresent(periode -> {
            registeropplysningerRequestBuilder.fom(periode.getFom());
            registeropplysningerRequestBuilder.tom(periode.getTom());
        });

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequestBuilder.build());
        log.info("Hentet registeropplysninger for behandling {}", behandling.getId());
    }
}
