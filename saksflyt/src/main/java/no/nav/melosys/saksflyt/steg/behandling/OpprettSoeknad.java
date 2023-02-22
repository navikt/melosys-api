package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_SØKNAD;

@Component
public class OpprettSoeknad implements StegBehandler {
    private final MottatteOpplysningerService mottatteOpplysningerService;

    public OpprettSoeknad(MottatteOpplysningerService mottatteOpplysningerService) {
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(prosessinstans);
    }
}
