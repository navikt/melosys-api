package no.nav.melosys.saksflyt.faktureringskomponenten

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName.FOLKETRYGDEN_MVP
import no.nav.melosys.featuretoggle.ToggleName.REFAKTORERING_ORDINÆR_TRYGDEAVGIFT
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OpprettBetalingsplan(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val pdlService: PersondataService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_BETALINGSPLAN
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(FOLKETRYGDEN_MVP)) {
            return
        }

        val behandlingsId = prosessinstans.behandling.id
        val fagsak = behandlingService.hentBehandling(behandlingsId).fagsak
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift

        if (
            if (unleash.isEnabled(REFAKTORERING_ORDINÆR_TRYGDEAVGIFT)) !trygdeavgiftMottakerService.skalBetalesTilNav(fastsattTrygdeavgift.trygdeavgiftsgrunnlag)
            else !fastsattTrygdeavgift.skalBetalesTilNav()
            ) { return }

        val trygdeavgiftsperioderMedAvgift = fastsattTrygdeavgift.trygdeavgiftsperioder.filter { it.harAvgift() }

        if (trygdeavgiftsperioderMedAvgift.isEmpty()) {
            return
        }

        val vedtaksdato = behandlingsresultat.vedtakMetadata.vedtaksdato
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT).orElse(null)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val intervall = hentBetalingsIntervall(prosessinstans, FaktureringsIntervall.KVARTAL)

        val forrigeFakturaserieReferanse = hentForrigeFakturaserieReferanse(fagsak)

        val fakturaserieDto = opprettFakturaserieDto(
            foedselsNr,
            forrigeFakturaserieReferanse,
            fullmektig,
            vedtaksdato,
            trygdeavgiftsperioderMedAvgift,
            intervall
        )

        log.info("Oppretter betalingsplan for behandling: $behandlingsId")

        oppdaterFakturaserieReferanseOgLagreReferanse(behandlingsresultat, fakturaserieDto)
    }

    private fun hentBetalingsIntervall(
        prosessinstans: Prosessinstans,
        intervall: FaktureringsIntervall
    ): FaktureringsIntervall {
        return prosessinstans.getData(
            ProsessDataKey.BETALINGSINTERVALL,
            FaktureringsIntervall::class.java,
            intervall
        )
    }

    private fun hentForrigeFakturaserieReferanse(fagsak: Fagsak): String? {
        if (fagsak.behandlinger.isNotEmpty()) {
            val forrigeAktivBehandling = fagsak.hentSistOppdatertBehandling()
            return behandlingsresultatService.hentBehandlingsresultat(forrigeAktivBehandling.id).fakturaserieReferanse
        }
        return null
    }

    private fun opprettFakturaserieDto(
        foedselsNr: String,
        forrigeFakturaserieReferanse: String?,
        fullmektig: Aktoer?,
        vedtaksdato: Instant,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>,
        intervall: FaktureringsIntervall
    ): FakturaserieDto {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        val fakturaseriePeriodeDtoListe = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.verdi,
                it.periodeFra,
                it.periodeTil,
                "Periode: ${it.periodeFra.format(FORMATTER)} - ${it.periodeTil.format(FORMATTER)}, " +
                    "Inntekt: ${it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi}, " +
                    "Dekning: ${it.grunnlagMedlemskapsperiode.trygdedekning.beskrivelse}, " +
                    "Sats: ${it.trygdesats} %"
            )
        }

        return FakturaserieDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = forrigeFakturaserieReferanse,
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall = intervall,
            referanseBruker = "Vedtak om medlemskap datert " +
                DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(vedtaksdato),
            perioder = fakturaseriePeriodeDtoListe
        )
    }

    private fun oppdaterFakturaserieReferanseOgLagreReferanse(
        behandlingsresultat: Behandlingsresultat,
        fakturaserieDto: FakturaserieDto
    ) {
        val fakturaserieResponse = faktureringskomponentenConsumer.lagFakturaSerie(fakturaserieDto)
        behandlingsresultat.apply {
            fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
        }
        behandlingsresultatService.lagre(behandlingsresultat)
    }
}
