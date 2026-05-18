package no.nav.melosys.saksflyt.steg.satsendring

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.saksflyt.steg.fakturering.mapTilFakturaperioder
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenClient
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component

@Component
class BeregnOgSendFaktura(
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenClient: FaktureringskomponentenClient,
    private val pdlService: PersondataService,
    private val behandlingService: BehandlingService
) : StegBehandler {
    override fun inngangsSteg() = ProsessSteg.BEREGN_OG_SEND_FAKTURA

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        val nyTrygdeavgift = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandlingsresultat.hentId(),
            behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            behandlingsresultat.hentInntektsperioder().toList()
        )

        opprettFakturaserieOgLagreReferanse(behandlingsresultat, nyTrygdeavgift)
    }

    private fun opprettFakturaserieOgLagreReferanse(
        behandlingsresultat: Behandlingsresultat,
        nyTrygdeavgift: Set<Trygdeavgiftsperiode>
    ) {
        val fakturaserieDto = mapFakturaserieDto(behandlingsresultat, nyTrygdeavgift)
        val faktureringResponse = faktureringskomponentenClient.lagFakturaserie(fakturaserieDto)
        behandlingsresultat.fakturaserieReferanse = faktureringResponse.fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    private fun mapFakturaserieDto(behandlingsresultat: Behandlingsresultat, trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>): FakturaserieDto {
        val behandling = behandlingService.hentBehandling(behandlingsresultat.hentId())
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }

        return FakturaserieDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = hentOpprinneligFakturaserieReferanse(behandling),
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall = FaktureringIntervall.KVARTAL,
            referanseBruker = "Faktura for årlig satsoppdatering av trygdeavgift",
            perioder = mapTilFakturaperioder(
                    trygdeavgiftsperioder.filter { it.harAvgift() },
                    prefiks = "Faktura for årlig satsoppdatering av trygdeavgift"
                )
        )
    }

    private fun hentOpprinneligFakturaserieReferanse(behandling: Behandling): String? {
        if (behandling.opprinneligBehandling != null) {
            return behandlingsresultatService.hentBehandlingsresultat(behandling.hentOpprinneligBehandling().id).fakturaserieReferanse
        }
        return null
    }

}
