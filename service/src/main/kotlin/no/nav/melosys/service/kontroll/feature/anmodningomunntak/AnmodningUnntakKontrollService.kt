package no.nav.melosys.service.kontroll.feature.anmodningomunntak

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.person.Persondata
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.data.AnmodningUnntakKontrollData
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.kontroll.AnmodningUnntakKontrollsett
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.validering.Kontrollfeil
import org.springframework.stereotype.Service


@Service
class AnmodningUnntakKontrollService(
    private val anmodningsperiodeService: AnmodningsperiodeService,
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade,
    private val organisasjonOppslagService: OrganisasjonOppslagService
) {
    fun utførKontroller(behandlingID: Long): Collection<Kontrollfeil> {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)
        val fullmektig = behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_SØKNAD)

        val kontrollData = AnmodningUnntakKontrollData(
            persondata = persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID()),
            mottatteOpplysningerData = behandling.mottatteOpplysninger.mottatteOpplysningerData,
            anmodningsperiode = anmodningsperiodeService.hentFørsteAnmodningsperiode(behandlingID),
            antallArbeidsgivere = avklarteVirksomheterService.hentAntallAvklarteVirksomheter(behandling),
            fullmektig = fullmektig,
            organisasjonDokumentTilFullmektig = hentOrganisasjonFullmektig(fullmektig),
            persondataTilFullmektig = hentPersondataFullmektig(fullmektig),
            medlemskapDokument = behandling.hentMedlemskapDokument(),
        )

        return AnmodningUnntakKontrollsett.hentRegler().mapNotNull { it.apply(kontrollData) }
    }

    private fun hentPersondataFullmektig(fullmektig: Aktoer?): Persondata? =
        if (fullmektig != null && fullmektig.erPerson()) persondataFasade.hentPerson(fullmektig.personIdent) else null

    private fun hentOrganisasjonFullmektig(fullmektig: Aktoer?): OrganisasjonDokument? =
        if (fullmektig != null && fullmektig.erOrganisasjon()) organisasjonOppslagService.hentOrganisasjon(fullmektig.orgnr) else null
}
