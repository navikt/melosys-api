package no.nav.melosys.saksflyt.steg.fakturering

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Betalingstype
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
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
import java.time.LocalDate
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
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg() = ProsessSteg.OPPRETT_FAKTURASERIE

    override fun utfør(prosessinstans: Prosessinstans) {
        val behandling = prosessinstans.hentBehandling
        val behandlingID = behandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val skalOppretteFakturaSerie = unleash.isEnabled(ToggleName.MELOSYS_EØS_FAKTURERING_AV_TRYGDEAVGIFT) && behandling.fagsak.erLovvalg() && behandlingsresultat.trygdeavgiftsperioder.isNotEmpty()

        if (!skalOppretteFakturaSerie) {
            return
        }

        val saksbehandlerIdent = prosessinstans.hentData(ProsessDataKey.SAKSBEHANDLER)

        if (behandlingsresultat.erOpphørt() || andregangsvurderingHarFjernetTrygdeavgift(behandling, behandlingsresultat)) {
            val opprinneligFakturaserieReferanse =
                behandlingsresultatService.hentBehandlingsresultat(behandling.hentOpprinneligBehandling().id).hentFakturaserieReferanse()
            log.info("Kansellerer fakturaserie for behandling: $behandlingID med fakturaseriereferanse: $opprinneligFakturaserieReferanse")
            kansellerFakturaserieOgLagreReferanse(behandlingsresultat, opprinneligFakturaserieReferanse, saksbehandlerIdent)
        } else if (skalOppretteFakturaserie(behandlingsresultat) || skalAvregneInneværendeOgFremtidigePerioderTilNull(behandlingsresultat)) {
            log.info("Oppretter fakturaserie for behandling: $behandlingID")
            opprettFakturaserieOgLagreReferanse(behandlingsresultat, mapFakturaserieDto(behandlingsresultat, prosessinstans), saksbehandlerIdent)
        } else {
            log.info("Ingen fakturaserie opprettet for behandling: $behandlingID")
        }
    }


    private fun andregangsvurderingHarFjernetTrygdeavgift(behandling: Behandling, behandlingsresultat: Behandlingsresultat): Boolean =
        behandling.erAndregangsbehandling()
            && harOpprinneligBehandlingFakturerbarTrygdeavgift(behandling)
            && behandling.fagsak.behandlinger.none { it.type == Behandlingstyper.ÅRSAVREGNING }
            && alleTrygdeavgiftsperioderPåSakenErInneværendeEllerFremtidige(behandling)
            && !trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultat)

    private fun alleTrygdeavgiftsperioderPåSakenErInneværendeEllerFremtidige(behandling: Behandling): Boolean {
        if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            return behandling.fagsak.behandlinger
                .filter { it.id != behandling.id }
                .all { it.let { behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder.all { it.fom.year >= LocalDate.now().year } } }
        }
        return true
    }


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
            && skalFaktureres(behandlingsresultat)

    /**
     * Dette er et spesialtilfelle hvor førstegangsbehandlingen har fakturert, men ny vurdering setter medlemskapsperiodene
     * til kun tidligere år. Vi trenger da å avregne i faktureringskomponenten med tidligere fakturaserieref og tom periode.
     */
    private fun skalAvregneInneværendeOgFremtidigePerioderTilNull(behandlingsresultat: Behandlingsresultat): Boolean {
        if (!unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            return false
        }

        return erNyVurderingUtenPerioderMedTidligereFakturering(behandlingsresultat)
            && opprinneligBehandlingHarInneværendeEllerFremtidigeAvgiftsperioder(behandlingsresultat)
    }

    private fun erNyVurderingUtenPerioderMedTidligereFakturering(behandlingsresultat: Behandlingsresultat): Boolean {
        val behandling = behandlingsresultat.hentBehandling()
        return behandling.erNyVurdering()
            && behandlingsresultat.trygdeavgiftsperioder.isEmpty()
            && hentSisteFakturaserieReferanse(behandling) != null
    }

    private fun opprinneligBehandlingHarInneværendeEllerFremtidigeAvgiftsperioder(behandlingsresultat: Behandlingsresultat): Boolean {
        val opprinneligBehandling = behandlingsresultat.behandling?.opprinneligBehandling ?: return false
        val opprinneligBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id)

        return opprinneligBehandlingsresultat.trygdeavgiftsperioder.isNotEmpty() && opprinneligBehandlingsresultat.trygdeavgiftsperioder.any { it.periodeTil.year >= LocalDate.now().year }
    }


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
        val behandling = behandlingService.hentBehandling(behandlingsresultat.hentId())
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(behandlingsresultat.hentVedtakMetadata().vedtaksdato)
        val erEøsPensjonist = behandling.erEøsPensjonist()
        val erLovvalg = fagsak.erLovvalg()

        return FakturaserieDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = hentSisteFakturaserieReferanse(behandling),
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall = hentBetalingsIntervall(prosessinstans),
            referanseBruker = if (erEøsPensjonist) "Informasjon om trygdeavgift datert $vedtaksdato" else "Vedtak om medlemskap datert $vedtaksdato",
            perioder = if (erEøsPensjonist || erLovvalg)
                mapFakturaseriePeriodeDtoUtenDekning(behandlingsresultat.trygdeavgiftsperioder.filter { it.harAvgift() })
            else
                mapFakturaseriePeriodeDto(behandlingsresultat.trygdeavgiftsperioder.filter { it.harAvgift() })
        )
    }

    private fun skalFaktureres(behandlingsresultat: Behandlingsresultat): Boolean =
        !behandlingsresultat.hentBehandling().erPensjonist() ||
            behandlingsresultat.hentBehandling().fagsak.betalingsvalg == Betalingstype.FAKTURA


    private fun harOpprinneligBehandlingFakturerbarTrygdeavgift(behandling: Behandling): Boolean =
        behandling.opprinneligBehandling?.let {
            trygdeavgiftService.harFakturerbarTrygdeavgift(behandlingsresultatService.hentBehandlingsresultat(it.id))
        } ?: false

    private fun hentBetalingsIntervall(prosessinstans: Prosessinstans): FaktureringIntervall =
        prosessinstans.finnData<FaktureringIntervall>(ProsessDataKey.BETALINGSINTERVALL, FaktureringIntervall.KVARTAL)

    private fun hentSisteFakturaserieReferanse(behandling: Behandling): String? =
        behandling.fagsak.behandlinger
            .asSequence()
            .filter { it.erInaktiv() && !it.erÅrsavregning() && it.id != behandling.id }
            .map {
                behandlingsresultatService.hentBehandlingsresultat(it.id)
            }
            .sortedByDescending { it.vedtakMetadata?.vedtaksdato }
            .map { it.fakturaserieReferanse }
            .firstOrNull()

    private fun mapFakturaseriePeriodeDto(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): List<FakturaseriePeriodeDto> {
        return trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.hentVerdi(),
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${it.hentGrunnlagInntekstperiode().avgiftspliktigMndInntekt.verdi}, " +
                    "Dekning: ${mapDekning(it)}, " +
                    "Sats: ${it.trygdesats} %"
            )
        }
    }

    private fun mapFakturaseriePeriodeDtoUtenDekning(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): List<FakturaseriePeriodeDto> {
        return trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.hentVerdi(),
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${it.hentGrunnlagInntekstperiode().avgiftspliktigMndInntekt.verdi}, " +
                    "Sats: ${it.trygdesats} %"
            )
        }
    }

    private fun mapDekning(trygdeavgiftsperiode: Trygdeavgiftsperiode): String {
        if (trygdeavgiftsperiode.hentGrunnlagInntekstperiode().type === Inntektskildetype.PENSJON_UFØRETRYGD ||
            trygdeavgiftsperiode.hentGrunnlagInntekstperiode().type === Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
        ) {
            return DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL
        }

        return trygdeavgiftsperiode.hentGrunnlagMedlemskapsperiode().hentTrygdedekning().beskrivelse
    }

    companion object {
        const val DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL = "Helsedel"
    }
}
