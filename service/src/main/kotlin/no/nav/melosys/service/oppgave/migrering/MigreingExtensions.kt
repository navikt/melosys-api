package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.oppgave.OppgaveGosysMapping

internal fun SakOgBehandlingDTO.tilSak(): Migrering.Sak =
    Migrering.Sak(sakstype, sakstema, behandlingstype, behandlingstema)

internal fun OppgaveGosysMapping.all(
    check: (
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
    ) -> OppgaveGosysMapping.Oppgave? = { sakstype, sakstema, btype, btema ->
        finnOppgave(
            sakstype,
            sakstema,
            btema,
            btype
        )
    }
): List<Migrering.TableRowSingle> {
    var i = 0
    return sequence {
        Sakstyper.values().forEach { sakstype: Sakstyper ->
            Sakstemaer.values().forEach { sakstemae: Sakstemaer ->
                Behandlingstyper.values().forEach { behandlingstype ->
                    Behandlingstema.values().forEach { behandlingstema ->
                        try {
                            val oppgave = check(sakstype, sakstemae, behandlingstype, behandlingstema)
                            if (oppgave != null) yield(
                                Migrering.TableRowSingle(
                                    sakstype,
                                    sakstemae,
                                    behandlingstype,
                                    behandlingstema,
                                    oppgave
                                )
                            )
                        } catch (_: IllegalStateException) {
                            i++
                        }
                    }
                }
            }
        }
    }.toList().apply { println("don't match:$i") }
}

