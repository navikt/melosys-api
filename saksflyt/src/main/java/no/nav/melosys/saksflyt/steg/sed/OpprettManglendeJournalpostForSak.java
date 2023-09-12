package no.nav.melosys.saksflyt.steg.sed;

import java.util.Optional;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.springframework.stereotype.Component;

@Component
public class OpprettManglendeJournalpostForSak implements StegBehandler {

    private final JoarkFasade joarkFasade;
    private final EessiService eessiService;
    private final Unleash unleash;

    public OpprettManglendeJournalpostForSak(JoarkFasade joarkFasade, EessiService eessiService, Unleash unleash) {
        this.joarkFasade = joarkFasade;
        this.eessiService = eessiService;
        this.unleash = unleash;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_TIDLIGERE_JOURNALPOSTER_FOR_SAK;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        if (unleash.isEnabled(ToggleName.IKKE_JOURNALFOER_UTEN_PID)) {
            finnEessiMelding(prosessinstans).ifPresent(melosysEessiMelding ->
                eessiService.opprettJournalpostForTidligereSed(melosysEessiMelding.getRinaSaksnummer()));
        }
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
}
