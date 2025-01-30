package no.nav.melosys.saksflyt.steg.satsendring

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.Innbetalingstype
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflyt.steg.arsavregning.SendFakturaÅrsavregning
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class BeregnOgSendFaktura(
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val pdlService: PersondataService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.BEREGN_OG_SEND_FAKTURA
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandlingsresultat.id,
            behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            behandlingsresultat.hentInntektsperioder().toList()
        )

        faktureringskomponentenConsumer.lagFaktura()
    }

    private fun mapFakturaserieDto(behandling:Behandling, behandlingsresultat: Behandlingsresultat): FakturaDto {
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato = FORMATTER.format(behandlingsresultat.vedtakMetadata.vedtaksdato)
        val startDato = behandlingsresultat.trygdeavgiftsperioder.minBy { trygdeavgiftsperiode -> trygdeavgiftsperiode.periodeFra }.periodeFra
        val sluttDato = behandlingsresultat.trygdeavgiftsperioder.maxBy { trygdeavgiftsperiode -> trygdeavgiftsperiode.periodeTil }.periodeTil

        return FakturaDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = if (harTidligereÅrsavregning) årsavregning.tidligereBehandlingsresultat.fakturaserieReferanse else null,
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            referanseBruker = "Årsavregning datert $vedtaksdato",
            belop = årsavregning.tilFaktureringBeloep,
            startDato = startDato,
            sluttDato = sluttDato,
            beskrivelse = "Medlemskapsperiode $startDato - $sluttDato, endelig beregnet trygdeavgift ${årsavregning.nyttTotalbeloep} - forskuddsvis fakturert trygdeavgift ${årsavregning.tidligereFakturertBeloep}"
        )
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
    }

}
