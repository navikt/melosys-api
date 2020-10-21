package no.nav.melosys.saksflyt.steg.sed;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.saksopplysninger.OpprettSedDokumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettSedDokument implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettSedDokument.class);

    private final OpprettSedDokumentService opprettSedDokumentService;

    @Autowired
    public OpprettSedDokument(OpprettSedDokumentService opprettSedDokumentService) {
        this.opprettSedDokumentService = opprettSedDokumentService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_SEDDOKUMENT;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        opprettSedDokumentService.opprettSedSaksopplysning(
            prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class), prosessinstans.getBehandling()
        );
        log.info("Opprettet SedDokument for behandling {}", prosessinstans.getBehandling().getId());
    }
}
