package no.nav.melosys.saksflyt.steg.fakturering

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OpprettFakturaserie(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val pdlService: PersondataService,
    private val trygdeavgiftService: TrygdeavgiftService,
) : StegBehandler {

    override fun inngangsSteg() = ProsessSteg.OPPRETT_FAKTURASERIE

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.behandling
        val behandlingID = behandling.id
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if (behandlingsresultat.erOpphørt() || andregangsvurderingHarFjernetTrygdeavgift(behandling, behandlingsresultat)) {
            val opprinneligFakturaserieReferanse =
                behandlingsresultatService.hentBehandlingsresultat(behandling.opprinneligBehandling.id).fakturaserieReferanse
            log.info("Kansellerer fakturaserie for behandling: $behandlingID med fakturaseriereferanse: $opprinneligFakturaserieReferanse")
            kansellerFakturaserieOgLagreReferanse(behandlingsresultat, opprinneligFakturaserieReferanse, saksbehandlerIdent)
        } else if (skalOppretteFakturaserie(behandlingsresultat)) {
            log.info("Oppretter fakturaserie for behandling: $behandlingID")
            opprettFakturaserieOgLagreReferanse(behandlingsresultat, mapFakturaserieDto(behandlingsresultat, prosessinstans), saksbehandlerIdent)
        }
    }

    private fun andregangsvurderingHarFjernetTrygdeavgift(behandling: Behandling, behandlingsresultat: Behandlingsresultat): Boolean =
        behandling.erAndregangsbehandling()
            && harOpprinneligBehandlingFakturerbarTrygdeavgift(behandling)
            && !trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat)

    private fun kansellerFakturaserieOgLagreReferanse(
        behandlingsresultat: Behandlingsresultat,
        opprinneligFakturaserieReferanse: String,
        saksbehandlerIdent: String
    ) {
        val fakturaserieResponse = faktureringskomponentenConsumer.kansellerFakturaserie(opprinneligFakturaserieReferanse, saksbehandlerIdent)
        behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    private fun skalOppretteFakturaserie(behandlingsresultat: Behandlingsresultat): Boolean =
        trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat)
            && skalFaktureres(behandlingsresultat.behandling)

    private fun opprettFakturaserieOgLagreReferanse(
        behandlingsresultat: Behandlingsresultat,
        fakturaserieDto: FakturaserieDto,
        saksbehandlerIdent: String
    ) {
        val fakturaserieResponse = faktureringskomponentenConsumer.lagFakturaserie(fakturaserieDto, saksbehandlerIdent)
        behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
    }

    private fun mapFakturaserieDto(behandlingsresultat: Behandlingsresultat, prosessinstans: Prosessinstans): FakturaserieDto {
        val behandling = behandlingService.hentBehandling(behandlingsresultat.id)
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(behandlingsresultat.vedtakMetadata.vedtaksdato)

        return FakturaserieDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = hentSisteFakturaserieReferanse(behandling),
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall = hentBetalingsIntervall(prosessinstans),
            referanseBruker = "Vedtak om medlemskap datert $vedtaksdato",
            perioder = mapFakturaseriePeriodeDto(behandlingsresultat.trygdeavgiftsperioder.filter { it.harAvgift() })
        )
    }

    private fun skalFaktureres(behandling: Behandling): Boolean =
        !behandling.erPensjonist() || behandling.fagsak.betalingsvalg == Betalingstype.FAKTURA

    private fun harOpprinneligBehandlingFakturerbarTrygdeavgift(behandling: Behandling): Boolean =
        behandling.opprinneligBehandling?.let {
            trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultatService.hentBehandlingsresultat(it.id))
        } ?: false

    private fun hentBetalingsIntervall(prosessinstans: Prosessinstans): FaktureringIntervall =
        prosessinstans.getData(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall::class.java, FaktureringIntervall.KVARTAL)

    private fun hentSisteFakturaserieReferanse(behandling: Behandling): String? = behandling.fagsak.behandlinger
        .asSequence()
        .filter { it.erInaktiv() && !it.erÅrsavregning() && it.id != behandling.id }
        .sortedByDescending { it.registrertDato }
        .mapNotNull {
            behandlingsresultatService.hentBehandlingsresultat(it.id).fakturaserieReferanse
        }
        .firstOrNull()

    private fun mapFakturaseriePeriodeDto(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): List<FakturaseriePeriodeDto> {
        return trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.verdi,
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${it.grunnlagInntekstperiode!!.avgiftspliktigMndInntekt.verdi}, " +
                    "Dekning: ${mapDekning(it)}, " +
                    "Sats: ${it.trygdesats} %"
            )
        }
    }

    private fun mapDekning(trygdeavgiftsperiode: Trygdeavgiftsperiode): String {
        if (trygdeavgiftsperiode.grunnlagInntekstperiode!!.type === Inntektskildetype.PENSJON_UFØRETRYGD ||
            trygdeavgiftsperiode.grunnlagInntekstperiode!!.type === Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
        ) {
            return DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL
        }

        return trygdeavgiftsperiode.grunnlagMedlemskapsperiodeNotNull.trygdedekning.beskrivelse
    }

    companion object {
        const val DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL = "Helsedel"
    }
}
