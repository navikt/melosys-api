package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import java.time.Instant

data class FagsakOppsummeringDto(
    val saksnummer: String,
    val navn: String,
    val sakstema: Sakstemaer,
    val sakstype: Sakstyper,
    val saksstatus: Saksstatuser,
    val land: SoeknadslandDto,
    val periode: PeriodeDto,
    val opprettetDato: Instant,
    val behandlingOversikter: List<BehandlingOversiktDto>,
    val hovedpartRolle: Aktoersroller
)
