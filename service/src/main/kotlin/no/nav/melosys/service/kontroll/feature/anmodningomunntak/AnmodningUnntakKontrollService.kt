package no.nav.melosys.service.kontroll.feature.anmodningomunntak

import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.data.AnmodningUnntakKontrollData
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.kontroll.AnmodningUnntakKontrollsett
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.validering.Kontrollfeil
import org.springframework.stereotype.Service


@Service
class AnmodningUnntakKontrollService(
    private val anmodningsperiodeService: AnmodningsperiodeService,
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade
) {
    fun utførKontroller(behandlingID: Long): Collection<Kontrollfeil> {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)

        val kontrollData = AnmodningUnntakKontrollData(
            persondata = persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID()),
            mottatteOpplysningerData = behandling.mottatteOpplysninger.mottatteOpplysningerData,
            anmodningsperiode = anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID),
            antallArbeidsgivere = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling)
        )

        return AnmodningUnntakKontrollsett.hentRegler().mapNotNull { it.apply(kontrollData) }
    }
}
