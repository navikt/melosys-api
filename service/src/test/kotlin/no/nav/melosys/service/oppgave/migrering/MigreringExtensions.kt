package no.nav.melosys.service.oppgave.migrering

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.oppgave.OppgaveGosysMapping

internal fun MigreringsSak.tilSak(): Migrering.Sak =
    Migrering.Sak(sak.sakstype, sak.sakstema, sak.behandlingstype, sak.behandlingstema, ny.beskrivelse)

internal fun OppgaveGosysMapping.all(
    check: (
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstype: Behandlingstyper,
        behandlingstema: Behandlingstema,
    ) -> OppgaveGosysMapping.Oppgave? = { sakstype, sakstema, btype, btema ->
        finnOppgave(sakstype, sakstema, btema, btype)
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

internal fun OppgaveGosysMapping.allGrouped(action: (name: String, list: List<Migrering.TableRowSingle>) -> Unit) =
    mapOf(
        "finnOppgaveFraTabell" to all { sakstype, sakstema, btype, btema ->
            finnOppgaveFraTabell(sakstype, sakstema, btema, btype)
        },
        "finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet" to all { sakstype, sakstema, btype, btema ->
            when {
                finnOppgaveFraTabell(sakstype, sakstema, btema, btype) != null -> null
                else -> finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet(sakstype, sakstema, btema, btype)
            }
        },
        "finnOppgaveVedBehandlingstypeHenvendelse" to all { sakstype, sakstema, btype, btema ->
            when {
                finnOppgaveFraTabell(sakstype, sakstema, btema, btype) != null -> null
                finnOppgaveVedBehandlingstypeHenvendelseOgVirksomhet(sakstype, sakstema, btema, btype) != null -> null
                else -> finnOppgaveVedBehandlingstypeHenvendelse(sakstype, sakstema, btema, btype)
            }
        }
    ).forEach { action(it.key, it.value) }
