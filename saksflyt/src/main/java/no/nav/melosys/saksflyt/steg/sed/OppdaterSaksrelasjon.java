package no.nav.melosys.saksflyt.steg.sed;

import java.util.Optional;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IkkeInngaaendeJournalpostException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OppdaterSaksrelasjon implements StegBehandler {

    private final JoarkFasade joarkFasade;
    private final EessiService eessiService;
    private final FagsakService fagsakService;

    public OppdaterSaksrelasjon(JoarkFasade joarkFasade, @Qualifier("system") EessiService eessiService, FagsakService fagsakService) {
        this.joarkFasade = joarkFasade;
        this.eessiService = eessiService;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPDATER_SAKSRELASJON;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        Optional<MelosysEessiMelding> eessiMelding = finnEessiMelding(prosessinstans);
        if (eessiMelding.isPresent()) {
            eessiService.lagreSaksrelasjon(
                hentArkivsakID(prosessinstans),
                eessiMelding.get().getRinaSaksnummer(),
                eessiMelding.get().getBucType()
            );
        }
    }

    private Optional<MelosysEessiMelding> finnEessiMelding(Prosessinstans prosessinstans) throws MelosysException {
        MelosysEessiMelding eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);

        if (eessiMelding != null) {
            return Optional.of(eessiMelding);
        }

        String journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        Journalpost journalpost;
        try {
            journalpost = joarkFasade.hentJournalpost(journalpostID);
        } catch (IkkeInngaaendeJournalpostException e) {
            return Optional.empty();
        }
        if (journalpost.mottaksKanalErEessi()) {
            return Optional.of(eessiService.hentSedTilknyttetJournalpost(journalpostID));
        }

        return Optional.empty();
    }

    private long hentArkivsakID(Prosessinstans prosessinstans) throws IkkeFunnetException {
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
