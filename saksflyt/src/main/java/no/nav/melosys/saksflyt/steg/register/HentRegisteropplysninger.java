package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_REGISTEROPPLYSNINGER;
import static no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory.utledSaksopplysningTyper;

@Component
public class HentRegisteropplysninger implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentRegisteropplysninger.class);

    private final RegisteropplysningerService registeropplysningerService;
    private final BehandlingService behandlingService;
    private final PersondataFasade persondataFasade;

    @Autowired
    public HentRegisteropplysninger(RegisteropplysningerService registeropplysningerService, BehandlingService behandlingService, PersondataFasade persondataFasade) {
        this.registeropplysningerService = registeropplysningerService;
        this.behandlingService = behandlingService;
        this.persondataFasade = persondataFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HENT_REGISTEROPPLYSNINGER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());
        String brukerId = persondataFasade.hentIdentForAktørId(behandling.getFagsak().hentBruker().getAktørId());


        var registeropplysningerRequestBuilder = RegisteropplysningerRequest.builder()
            .behandlingID(prosessinstans.getBehandling().getId())
            .fnr(brukerId);

        if (behandling.getFagsak().getType() == Sakstyper.FTRL) {
            registeropplysningerRequestBuilder
                .saksopplysningTyper(RegisteropplysningerRequest.SaksopplysningTyper.builder().personopplysninger().build());
        } else {
            registeropplysningerRequestBuilder
                .saksopplysningTyper(utledSaksopplysningTyper(prosessinstans.getBehandling().getTema()));
        }

        behandling.finnPeriode().ifPresent(periode -> {
            registeropplysningerRequestBuilder.fom(periode.getFom());
            registeropplysningerRequestBuilder.tom(periode.getTom());
        });

        registeropplysningerService.hentOgLagreOpplysninger(registeropplysningerRequestBuilder.build());
        log.info("Hentet registeropplysninger for behandling {}", behandling.getId());
    }
}
