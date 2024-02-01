package no.nav.melosys.tjenester.gui.dto.kontroller

import no.nav.melosys.domain.kodeverk.Vedtakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper

@JvmRecord
data class FerdigbehandlingKontrollerDto(
    @JvmField val behandlingID: Long,
    @JvmField val vedtakstype: Vedtakstyper?,
    @JvmField val behandlingsresultattype: Behandlingsresultattyper?,
    @JvmField val kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>?,
    @JvmField val skalRegisteropplysningerOppdateres: Boolean?
)
