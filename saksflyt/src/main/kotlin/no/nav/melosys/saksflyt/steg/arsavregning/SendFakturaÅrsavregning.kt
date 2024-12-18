package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.Innbetalingstype
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningKonstanter.MINIMUM_BELØP_FAKTURERING
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class SendFakturaÅrsavregning(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val pdlService: PersondataService,
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_FAKTURA_AARSAVREGNING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandlingsId = prosessinstans.behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)


        if (tilFaktureringBelopErStørreEllerLikMinimumBeløp(behandlingsresultat)) {
            val fakturaDto = mapFakturaserieDto(behandlingsresultat)
            val responseDto = faktureringskomponentenConsumer.lagFaktura(fakturaDto, saksbehandlerIdent)
            behandlingsresultat.fakturaserieReferanse = responseDto.fakturaserieReferanse
            behandlingsresultatService.lagre(behandlingsresultat)
            log.info("Oppretter årsavregningfaktura for behandling: $behandlingsId")
        } else {
            log.info("Belop til fakturering er mindre enn ${MINIMUM_BELØP_FAKTURERING.beløp} kr for behandling: $behandlingsId, faktura sendes ikke")
        }
    }

    private fun tilFaktureringBelopErStørreEllerLikMinimumBeløp(behandlingsresultat: Behandlingsresultat): Boolean {
        return behandlingsresultat.årsavregning.tilFaktureringBeloep.abs() >= MINIMUM_BELØP_FAKTURERING.beløp
    }

    private fun mapFakturaserieDto(behandlingsresultat: Behandlingsresultat): FakturaDto {
        val behandling = behandlingService.hentBehandling(behandlingsresultat.id)
        val årsavregning = behandlingsresultat.årsavregning
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato = FORMATTER.format(behandlingsresultat.vedtakMetadata.vedtaksdato)
        val startDato = behandlingsresultat.trygdeavgiftsperioder.minBy { trygdeavgiftsperiode -> trygdeavgiftsperiode.periodeFra }.periodeFra
        val sluttDato = behandlingsresultat.trygdeavgiftsperioder.maxBy { trygdeavgiftsperiode -> trygdeavgiftsperiode.periodeTil }.periodeTil
        val harTidligereÅrsavregning = årsavregning.tidligereBehandlingsresultat.behandling.erÅrsavregning()

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
