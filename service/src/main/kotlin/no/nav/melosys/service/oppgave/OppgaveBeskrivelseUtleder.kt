package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

private val log = KotlinLogging.logger { }

class OppgaveBeskrivelseUtleder internal constructor(
    private val oppgaveGosysMapping: OppgaveGosysMapping = OppgaveGosysMapping()
) {

    fun utledBeskrivelse(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        gjelderÅr: String? = null,
        hentSedDokument: (logHvisMangler: Boolean) -> SedDokument?
    ): String {
        val beskrivelsefelt =
            oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).beskrivelsefelt

        return when (beskrivelsefelt) {
            OppgaveGosysMapping.Beskrivelsefelt.TOMT -> ""
            OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT -> hentSedDokument(false)?.sedType?.name ?: ""
            OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR -> beskrivelsefelt.beskrivelse
            OppgaveGosysMapping.Beskrivelsefelt.SED -> utledBeskrivelse(behandlingstype, hentSedDokument)
            OppgaveGosysMapping.Beskrivelsefelt.BEHANDLINGSTEMA -> behandlingstema.beskrivelse
            OppgaveGosysMapping.Beskrivelsefelt.GJELDER_ÅR -> gjelderÅr
                ?: "".also {
                    log.warn(
                        "Mangler gjelderÅr for beskrivelse på årsavregningsoppgave " +
                            "(sakstype:$sakstype, sakstema:$sakstema, behandlingstema:$behandlingstema, behandlingstype:$behandlingstype)"
                    )
                }
        }
    }

    private fun utledBeskrivelse(
        behandlingstype: Behandlingstyper,
        hentSedDokument: (logHvisMangler: Boolean) -> SedDokument?
    ): String {
        if (behandlingstype in listOf(Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE)) {
            // Vi har valgt å løse dette ved å legge inn ekstra mapping i OppgaveGosysMapping
            // Se https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
            log.warn("Det skal ikke være mulig å ha NY_VURDERING eller KLAGE med Beskrivelsefelt.SED, sjekk OppgaveGosysMapping")
            return ""
        }
        return hentSedDokument(true)?.sedType?.name ?: ""
    }
}
