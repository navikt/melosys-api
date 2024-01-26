package no.nav.melosys.tjenester.gui.dto.statistikk

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema

class StatistikkDto(private val antallUtildelteOppgaverPerBehandlingstema: Map<Behandlingstema, Long>)
