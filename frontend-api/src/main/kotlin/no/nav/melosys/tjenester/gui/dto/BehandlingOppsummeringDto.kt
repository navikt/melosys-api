package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.Instant
import java.time.LocalDate

class BehandlingOppsummeringDto {
    @JvmField
    var behandlingsstatus: Behandlingsstatus? = null
    @JvmField
    var behandlingstype: Behandlingstyper? = null
    @JvmField
    var behandlingstema: Behandlingstema? = null
    @JvmField
    var registrertDato: Instant? = null
    @JvmField
    var endretDato: Instant? = null
    @JvmField
    var endretAvNavn: String? = null
    @JvmField
    var sisteOpplysningerHentetDato: Instant? = null
    @JvmField
    var svarFrist: Instant? = null
    @JvmField
    var behandlingsfrist: LocalDate? = null
    @JvmField
    var behandlingsresultattype: Behandlingsresultattyper? = null
}
