package no.nav.melosys.service.kontroll.feature.ferdigbehandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService
import no.nav.melosys.service.validering.Kontrollfeil
import org.springframework.stereotype.Component


@Component
class KontrollMedRegisteropplysning(
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade,
    private val registeropplysningerService: RegisteropplysningerService,
    private val kontroll: Kontroll
) {
    internal fun kontroller(
        behandlingId: Long,
        behandlingsresultattype: Behandlingsresultattyper?,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
    ): Collection<Kontrollfeil> {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        val sakstype = behandling.fagsak.type

        return kontrollerVedtak(behandling, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres)
    }

    internal fun kontrollerVedtak(
        behandling: Behandling,
        sakstype: Sakstyper,
        behandlingsresultattype: Behandlingsresultattyper?,
        kontrollerSomSkalIgnoreres: Set<Kontroll_begrunnelser>
    ): Collection<Kontrollfeil> {
        hentNyeRegisteropplysninger(behandling)

        if(behandling.erEøsPensjonist())
            return kontroll.kontrollerBrev(behandling)

        // Pass Behandling object instead of ID to prevent entity reload and race condition
        return kontroll.kontrollerVedtak(behandling, sakstype, behandlingsresultattype, kontrollerSomSkalIgnoreres)
    }

    private fun hentNyeRegisteropplysninger(behandling: Behandling) {
        val søknadsperiode = behandling.mottatteOpplysninger!!.mottatteOpplysningerData.periode
        val fnr = persondataFasade.hentFolkeregisterident(behandling.fagsak.hentBrukersAktørID())

        registeropplysningerService.hentOgLagreOpplysninger(
            RegisteropplysningerRequest.builder()
                .behandlingID(behandling.id)
                .fnr(fnr)
                .fom(søknadsperiode.getFom())
                .tom(søknadsperiode.getTom())
                .saksopplysningTyper(
                    RegisteropplysningerRequest.SaksopplysningTyper.builder().medlemskapsopplysninger().build()
                )
                .build()
        )
    }
}
