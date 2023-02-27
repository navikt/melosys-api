package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_MOTTATTEOPPLYSNINGER;

@Component
public class OpprettMottatteOpplysninger implements StegBehandler {
    private final MottatteOpplysningerService mottatteOpplysningerService;

    public OpprettMottatteOpplysninger(MottatteOpplysningerService mottatteOpplysningerService) {
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_MOTTATTEOPPLYSNINGER;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        mottatteOpplysningerService.opprettSøknadEllerAnmodningEllerAttest(prosessinstans);
    }
}
