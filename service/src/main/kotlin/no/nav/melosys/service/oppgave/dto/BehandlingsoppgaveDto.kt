package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.oppgave.PrioritetType
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import java.time.LocalDate

class BehandlingsoppgaveDto(
    override val aktivTil: LocalDate?,
    override val ansvarligID: String?,
    override val oppgaveID: String,
    override val prioritet: PrioritetType,
    override val navn: String,
    override val hovedpartIdent: String,
    override val versjon: Int,
    val behandling: BehandlingDto,
    val saksnummer: String,
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer,
    val land: SoeknadslandDto?,
    val periode: PeriodeDto?,
    val oppgaveBeskrivelse: String?,
    val sisteNotat: String?,
) : OppgaveDto
