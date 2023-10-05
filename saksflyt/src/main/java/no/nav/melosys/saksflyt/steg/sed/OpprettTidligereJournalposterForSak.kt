package no.nav.melosys.saksflyt.steg.sed

import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.ProsessSteg
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.service.dokument.sed.EessiService
import org.springframework.stereotype.Component
import java.util.*


@Component
class OpprettTidligereJournalposterForSak(private val joarkFasade: JoarkFasade,
                                          private val eessiService: EessiService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_TIDLIGERE_JOURNALPOSTER_FOR_SAK
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        finnEessiMelding(prosessinstans).ifPresent { melosysEessiMelding: MelosysEessiMelding ->
            eessiService.opprettJournalpostForTidligereSed(
                melosysEessiMelding.rinaSaksnummer
            )
        }
    }

    private fun finnEessiMelding(prosessinstans: Prosessinstans): Optional<MelosysEessiMelding> {
        val eessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding::class.java)
        if (eessiMelding != null) {
            return Optional.of(eessiMelding)
        }
        val journalpostID = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID)
        return if (joarkFasade.hentJournalpost(journalpostID).mottaksKanalErEessi()) {
            Optional.of(eessiService.hentSedTilknyttetJournalpost(journalpostID))
        } else Optional.empty()
    }
}

