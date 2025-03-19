package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import java.time.Instant

data class BehandlingOversiktDto(
    var behandlingID: Long? = null,
    var behandlingsstatus: Behandlingsstatus? = null,
    var behandlingstype: Behandlingstyper? = null,
    var behandlingstema: Behandlingstema? = null,
    var lovvalgsperiode: PeriodeDto? = null,
    var medlemskapsperiode: PeriodeDto? = null,
    var land: SoeknadslandDto? = null,
    var soknadsperiode: PeriodeDto? = null,
    var opprettetDato: Instant? = null,
    var behandlingsresultattype: Behandlingsresultattyper? = null,
    var svarFrist: Instant? = null
)
