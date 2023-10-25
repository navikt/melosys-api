package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.saksopplysninger.OpprettSedDokumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokument implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private final OpprettSedDokumentService opprettSedDokumentService;

    public OpprettSedDokument(OpprettSedDokumentService opprettSedDokumentService) {
        this.opprettSedDokumentService = opprettSedDokumentService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_SEDDOKUMENT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        opprettSedDokumentService.opprettSedSaksopplysning(
            prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class), prosessinstans.getBehandling()
        );
        log.info("Opprettet SedDokument for behandling {}", prosessinstans.getBehandling().getId());
    }
}
