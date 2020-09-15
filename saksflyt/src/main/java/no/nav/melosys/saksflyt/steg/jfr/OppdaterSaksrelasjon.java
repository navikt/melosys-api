package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSaksrelasjon implements StegBehandler {

    private final JoarkFasade joarkFasade;
    private final EessiService eessiService;

    public OppdaterSaksrelasjon(JoarkFasade joarkFasade, @Qualifier("system") EessiService eessiService) {
        this.joarkFasade = joarkFasade;
        this.eessiService = eessiService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_OPPDATER_SAKSRELASJON;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        Journalpost journalpost = joarkFasade.hentJournalpost(journalpostID);
        if (journalpost.mottaksKanalErEessi()) {
            Long arkivsakID = prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
            MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpostID);
            eessiService.lagreSaksrelasjon(arkivsakID, melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getBucType());
        }
    }
}
