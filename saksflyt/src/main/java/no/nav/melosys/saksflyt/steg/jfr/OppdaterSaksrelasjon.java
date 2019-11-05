package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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
            Long gsakSaksnummer = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
            if (gsakSaksnummer == null) {
                //Vil ikke ligge i data ved prosesstype JFR_NY_BEHANDLING eller JFR_KNYTT
                gsakSaksnummer = prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
            }
            eessiService.lagreSaksrelasjon(gsakSaksnummer, melosysEessiMelding.getRinaSaksnummer(), melosysEessiMelding.getBucType());
        }

        prosessinstans.setSteg(ProsessSteg.JFR_OPPDATER_JOURNALPOST);
    }
}
