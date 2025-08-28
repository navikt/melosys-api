package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.service.persondata.PersondataFasade
import org.springframework.stereotype.Component

@Component
class OppgaveSoekFilter(
    private val oppgaveFasade: OppgaveFasade,
    private val joarkFasade: JoarkFasade,
    private val persondataFasade: PersondataFasade
) {
    fun finnBehandlingsoppgaverMedPersonIdent(personIdent: String): List<Oppgave> {
        val aktørId = persondataFasade.hentAktørIdForIdent(personIdent)
            ?: throw IkkeFunnetException("Finner ikke aktørId for ident $personIdent")

        return filtrerMedJournalpostOgMottaksdato(
            oppgaveFasade.finnOppgaverMedAktørId(
                aktørId,
                BEHANDLINGSOPPGAVE_TYPER
            )
        )
    }

    fun finnBehandlingsoppgaverMedOrgnr(orgnr: String): List<Oppgave> {
        return filtrerMedJournalpostOgMottaksdato(oppgaveFasade.finnOppgaverMedOrgnr(orgnr, BEHANDLINGSOPPGAVE_TYPER))
    }

    private fun filtrerMedJournalpostOgMottaksdato(oppgaveListe: List<Oppgave>): List<Oppgave> {
        return oppgaveListe
            .filter { it.journalpostId != null }
            .filter { joarkFasade.hentMottaksDatoForJournalpost(it.journalpostId) != null }
    }

    companion object {
        private val BEHANDLINGSOPPGAVE_TYPER = arrayOf(
            Oppgavetyper.BEH_SAK_MK.kode,
            Oppgavetyper.BEH_SAK.kode,
            Oppgavetyper.BEH_ARSAVREG.kode,
            Oppgavetyper.BEH_SED.kode,
            Oppgavetyper.VUR.kode,
            Oppgavetyper.VURD_HENV.kode,
            Oppgavetyper.VURD_MAN_INNB.kode
        )
    }
}
