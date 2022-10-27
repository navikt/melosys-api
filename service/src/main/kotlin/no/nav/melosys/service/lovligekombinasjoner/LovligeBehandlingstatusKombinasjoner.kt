package no.nav.melosys.service.lovligekombinasjoner

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus

object LovligeBehandlingstatusKombinasjoner {
    @JvmField
    val ALLE_MULIGE_BEHANDLINGSTATUSER = mutableSetOf(
        Behandlingsstatus.AVVENT_DOK_PART,
        Behandlingsstatus.AVVENT_DOK_UTL,
        Behandlingsstatus.UNDER_BEHANDLING,
        Behandlingsstatus.AVVENT_FAGLIG_AVKLARING
    )
}
