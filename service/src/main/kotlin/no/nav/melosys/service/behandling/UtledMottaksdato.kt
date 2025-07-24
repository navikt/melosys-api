package no.nav.melosys.service.behandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.integrasjon.joark.JoarkFasade
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class UtledMottaksdato(val joarkFasade: JoarkFasade) {

    fun getMottaksdato(behandling: Behandling): LocalDate? {
        if (behandling.behandlingsårsak != null) {
            return behandling.behandlingsårsak!!.mottaksdato
        }
        if (behandling.mottatteOpplysninger != null && behandling.mottatteOpplysninger!!.mottaksdato != null) {
            return behandling.mottatteOpplysninger!!.mottaksdato
        }
        val journalpost = finnJournalpost(behandling.initierendeJournalpostId)
        return tilLocalDate(journalpost?.forsendelseMottatt ?: behandling.registrertDato)
    }

    fun getMottaksdato(behandling: Behandling, journalpost: Journalpost?): LocalDate? {
        if (behandling.behandlingsårsak != null) {
            return behandling.behandlingsårsak!!.mottaksdato
        }
        if (behandling.mottatteOpplysninger != null && behandling.mottatteOpplysninger!!.mottaksdato != null) {
            return behandling.mottatteOpplysninger!!.mottaksdato
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
