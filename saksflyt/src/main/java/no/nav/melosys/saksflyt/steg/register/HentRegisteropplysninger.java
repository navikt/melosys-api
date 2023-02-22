package no.nav.melosys.saksflyt.steg.register;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_REGISTEROPPLYSNINGER;
import static no.nav.melosys.featuretoggle.ToggleName.IKKEYRKESAKTIV_FLYT;
import static no.nav.melosys.featuretoggle.ToggleName.REGISTRERING_UNNTAK_MEDLEMSKAP;
import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory.utledSaksopplysningTyper;

@Component
public class HentRegisteropplysninger implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentRegisteropplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;
    private final Unleash unleash;

    public HentRegisteropplysninger(RegisteropplysningerService registeropplysningerService,
                                    BehandlingService behandlingService,
                                    PersondataFasade persondataFasade,
                                    Unleash unleash) {
        this.registeropplysningerService = registeropplysningerService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
        this.unleash = unleash;
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

        var folketrygdenToggleEnabled = unleash.isEnabled("melosys.folketrygden.mvp");
        var ikkeYrkesaktivToggleEnabled =  unleash.isEnabled(IKKEYRKESAKTIV_FLYT);
        var registreringUnntakMedlemskapToggleEnabled =  unleash.isEnabled(REGISTRERING_UNNTAK_MEDLEMSKAP);

        var aktørId = behandling.getFagsak().finnBrukersAktørID().orElseThrow(
            () -> new FunksjonellException("Kan ikke hente registreopplysninger når bruker ikke har aktørID")
        );

            var registeropplysningerRequestBuilder = RegisteropplysningerRequest.builder()
                .behandlingID(prosessinstans.getBehandling().getId())
                .fnr(persondataFasade.hentFolkeregisterident(aktørId))
                .saksopplysningTyper(utledSaksopplysningTyper(
                    behandling.getFagsak().getType(),
                    behandling.getFagsak().getTema(),
                    behandling.getTema(),
                    behandling.getType(),
                    folketrygdenToggleEnabled,
                    ikkeYrkesaktivToggleEnabled,
                    registreringUnntakMedlemskapToggleEnabled));

        behandling.finnPeriode().ifPresent(periode -> {
            registeropplysningerRequestBuilder.fom(periode.getFom());
            registeropplysningerRequestBuilder.tom(periode.getTom());
        });

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequestBuilder.build());
        log.info("Hentet registeropplysninger for behandling {}", behandling.getId());
    }
}
