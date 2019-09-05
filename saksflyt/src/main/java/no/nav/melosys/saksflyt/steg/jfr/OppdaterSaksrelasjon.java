package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSaksrelasjon extends AbstraktStegBehandler {

    private final JoarkFasade joarkFasade;
    private final EessiService eessiService;

    public OppdaterSaksrelasjon(JoarkFasade joarkFasade, EessiService eessiService) {
        this.joarkFasade = joarkFasade;
        this.eessiService = eessiService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_OPPDATER_SAKSRELASJON;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        Journalpost journalpost = joarkFasade.hentJournalpost(journalpostID);
        if (journalpost.mottaksKanalErEessi()) {
            MelosysEessiMelding melosysEessiMelding = eessiService.hentSedTilknyttetJournalpost(journalpostID);
            Long gsakSaksnummer = prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
            eessiService.lagreSaksrelasjon(gsakSaksnummer, melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getBucType());
        }

        prosessinstans.setSteg(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }
}
