package no.nav.melosys.saksflyt.steg.behandling;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.altinn.AltinnSoeknadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.MOTTATT_SOKNAD_ID;

@Component
public class OpprettFagsakOgBehandlingFraAltinnSøknad implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettFagsakOgBehandlingFraAltinnSøknad.class);

    private final AltinnSoeknadService altinnSoeknadService;

    public OpprettFagsakOgBehandlingFraAltinnSøknad(AltinnSoeknadService altinnSoeknadService) {
        this.altinnSoeknadService = altinnSoeknadService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_FRA_ALTINN_SØKNAD;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {

        final String søknadID = prosessinstans.getData(MOTTATT_SOKNAD_ID);
        log.info("Oppretter fagsak og behandling for Altinn-søknad med referanse {}", søknadID);

        Behandling behandling = altinnSoeknadService.opprettFagsakOgBehandlingFraAltinnSøknad(søknadID);
        prosessinstans.setBehandling(behandling);
    }
}
