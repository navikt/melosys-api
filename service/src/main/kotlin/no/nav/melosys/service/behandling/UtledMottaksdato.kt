package no.nav.melosys.service.behandling

import no.finn.unleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.integrasjon.joark.JoarkFasade
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class UtledMottaksdato(val joarkFasade: JoarkFasade, val unleash: Unleash) {

    fun getMottaksdato(behandling: Behandling): LocalDate? {
        if (unleash.isEnabled("melosys.ny_opprett_sak")) {
            if (behandling.behandlingsårsak != null) {
                return behandling.behandlingsårsak.mottaksdato
            }
            val journalpost = finnJournalpost(behandling.initierendeJournalpostId)
            return tilLocalDate(journalpost?.forsendelseMottatt ?: behandling.registrertDato)
        }
        return behandling.mottatteOpplysninger?.mottaksdato ?: tilLocalDate(behandling.registrertDato)
    }

    fun getMottaksdato(behandling: Behandling, journalpost: Journalpost?): LocalDate? {
        if (behandling.behandlingsårsak != null) {
            return behandling.behandlingsårsak.mottaksdato
        }
        return tilLocalDate(journalpost?.forsendelseMottatt ?: behandling.registrertDato)
    }

    private fun finnJournalpost(initierendeJournalpostId: String?): Journalpost? {
        if (initierendeJournalpostId == null) return null
        return joarkFasade.hentJournalpost(initierendeJournalpostId)
    }

    private fun tilLocalDate(instant: Instant?): LocalDate? {
        return if (instant == null) null else LocalDate.ofInstant(instant, ZoneId.systemDefault())
    }
}
