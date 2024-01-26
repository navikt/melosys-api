package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import java.time.Instant

class BehandlingOversiktDto {
    @JvmField
    var behandlingID: Long? = null
    @JvmField
    var behandlingsstatus: Behandlingsstatus? = null
    @JvmField
    var behandlingstype: Behandlingstyper? = null
    @JvmField
    var behandlingstema: Behandlingstema? = null
    @JvmField
    var lovvalgsperiode: PeriodeDto? = null
    @JvmField
    var medlemskapsperiode: PeriodeDto? = null
    @JvmField
    var land: SoeknadslandDto? = null
    @JvmField
    var soknadsperiode: PeriodeDto? = null
    @JvmField
    var opprettetDato: Instant? = null
    @JvmField
    var behandlingsresultattype: Behandlingsresultattyper? = null
    @JvmField
    var svarFrist: Instant? = null
}
