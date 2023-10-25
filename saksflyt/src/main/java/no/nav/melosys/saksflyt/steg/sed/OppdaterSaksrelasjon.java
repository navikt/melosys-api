package no.nav.melosys.saksflyt.steg.sed;

import java.util.Optional;

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSaksrelasjon implements StegBehandler {

    private final JoarkFasade joarkFasade;
    private final EessiService eessiService;
    private final FagsakService fagsakService;

    public OppdaterSaksrelasjon(JoarkFasade joarkFasade, EessiService eessiService, FagsakService fagsakService) {
        this.joarkFasade = joarkFasade;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPDATER_SAKSRELASJON;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        finnEessiMelding(prosessinstans).ifPresent(melosysEessiMelding -> eessiService.lagreSaksrelasjon(
                hentArkivsakID(prosessinstans),
                melosysEessiMelding.getRinaSaksnummer(),
                melosysEessiMelding.getBucType()
            ));
    }

    private Optional<MelosysEessiMelding> finnEessiMelding(Prosessinstans prosessinstans) {
        MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (eessiMelding != null) {
            return Optional.of(eessiMelding);
        }

        String journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        if (joarkFasade.hentJournalpost(journalpostID).mottaksKanalErEessi()) {
            return Optional.of(eessiService.hentSedTilknyttetJournalpost(journalpostID));
        }

        return Optional.empty();
    }

    private long hentArkivsakID(Prosessinstans prosessinstans) {
        if (prosessinstans.getBehandling() != null) {
            return prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
        }

        Long arkivsakID = prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class);
        if (arkivsakID == null) {
            arkivsakID = fagsakService.hentFagsak(prosessinstans.getData(ProsessDataKey.SAKSNUMMER)).getGsakSaksnummer();
        }

        return arkivsakID;
    }
}
