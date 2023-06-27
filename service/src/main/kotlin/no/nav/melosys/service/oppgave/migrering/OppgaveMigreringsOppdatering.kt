package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import no.nav.melosys.service.oppgave.OppgaveBehandlingstype

data class OppgaveMigreringsOppdatering(
    val oppgaveBehandlingstema: OppgaveBehandlingstema?,
    val oppgaveBehandlingstype: OppgaveBehandlingstype?,
    val tema: Tema?,
    val oppgaveType: Oppgavetyper?,
    val beskrivelse: String?,
    val mappingError: String? = null,
    var oppgaveOppdateringError: String? = null,
    var sedListe: MutableList<String> = mutableListOf()
) {
    constructor(mappingError: String?) : this(
        null,
        null,
        null,
        null,
        null,
        mappingError = mappingError
    )

    fun harFeil(): Boolean {
        if (mappingError != null) return true
        return beskrivelseInneholderErrorMessage()
    }

    internal fun beskrivelseInneholderErrorMessage() = beskrivelse?.contains("feilet for") == true
    internal fun sedTypeFraBehandlingErNull() = beskrivelse?.contains("SedType fra behandling er null når beskrivelsefelt er SED") == true
    internal fun fantIkkeOppgaveMapping() = mappingError?.contains("Fant ikke oppgave mapping for") == true
}
