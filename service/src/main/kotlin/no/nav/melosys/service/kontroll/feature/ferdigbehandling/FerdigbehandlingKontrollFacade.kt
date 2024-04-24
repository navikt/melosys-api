package no.nav.melosys.service.kontroll.feature.ferdigbehandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.validering.Kontrollfeil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class FerdigbehandlingKontrollFacade(
    private val kontrollerKontrollMedRegisteropplysning: KontrollMedRegisteropplysning,
    private val kontroll: Kontroll
) {
    @Transactional
    fun kontroller(
        behandlingId: Long,
        skalRegisteropplysningerOppdateres: Boolean,
        behandlingsresultattype: Behandlingsresultattyper?,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>?
    ): Collection<Kontrollfeil> {
        return if (skalRegisteropplysningerOppdateres) {
            kontrollerKontrollMedRegisteropplysning.kontroller(behandlingId, behandlingsresultattype, kontrollerSomSkalIgnoreres ?: emptySet())
        } else {
            kontroll.kontroller(behandlingId, behandlingsresultattype, kontrollerSomSkalIgnoreres ?: emptySet())
        }
    }

    @Transactional(readOnly = true)
    fun kontroller(
        behandlingId: Long,
        behandlingsresultattype: Behandlingsresultattyper,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>?
    ): Collection<Kontrollfeil> {
        return kontroll.kontroller(behandlingId, behandlingsresultattype, kontrollerSomSkalIgnoreres ?: emptySet())
    }

    @Transactional
    fun kontrollerVedtakMedRegisteropplysninger(
        behandling: Behandling,
        sakstype: Sakstyper,
        behandlingsresultattype: Behandlingsresultattyper,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>?
    ): Collection<Kontrollfeil> {
        return kontrollerKontrollMedRegisteropplysning.kontrollerVedtak(
            behandling, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres ?: emptySet()
        )
    }
}

