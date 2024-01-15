package no.nav.melosys.saksflyt.steg.fakturering

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger { }

@Component
class OpprettFakturaserie(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val pdlService: PersondataService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_FAKTURASERIE
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(ToggleName.FOLKETRYGDEN_MVP)) {
            return
        }

        //TODO NY_VURDERING

        val behandlingsId = prosessinstans.behandling.id
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)

        if (behandlingsresultat.type === Behandlingsresultattyper.OPPHØRT) {
            log.info("Kansellerer fakturaserie for behandling: $behandlingsId med fakturaseriereferanse: ${behandlingsresultat.fakturaserieReferanse}")
            kansellerFakturaserieOgLagreReferanse(behandlingsresultat, saksbehandlerIdent)
        } else if (skalOppretteFakturaserie(behandlingsresultat)) {
            log.info("Oppretter fakturaserie for behandling: $behandlingsId")
            opprettFakturaserieOgLagreReferanse(behandlingsresultat, mapFakturaserieDto(behandlingsresultat, prosessinstans), saksbehandlerIdent)
        } else if (prosessinstans.behandling.type === Behandlingstyper.NY_VURDERING
            && !trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag)) {
            val fakturaserie = faktureringskomponentenConsumer.getFakturaSerie(behandlingsresultat.fakturaserieReferanse)
            val fom = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.first().periodeFra
            val tom = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.last().periodeTil
            //Find the lowest startdato and sluttdato in perioder
        }
    }

    private fun kansellerFakturaserieOgLagreReferanse(behandlingsresultat: Behandlingsresultat, saksbehandlerIdent: String) {
        val fakturaserieResponse = faktureringskomponentenConsumer.kansellerFakturaserie(behandlingsresultat.fakturaserieReferanse, saksbehandlerIdent)
        behandlingsresultat.fakturaserieReferanse = fakturaserieResponse.fakturaserieReferanse
        behandlingsresultatService.lagre(behandlingsresultat)
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

    private fun skalOppretteFakturaserie(behandlingsresultat: Behandlingsresultat): Boolean {
        return trygdeavgiftsperioderMedAvgift(behandlingsresultat).isNotEmpty()
            && trygdeavgiftMottakerService.skalBetalesTilNav(behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag)
    }

    private fun trygdeavgiftsperioderMedAvgift(behandlingsresultat: Behandlingsresultat): List<Trygdeavgiftsperiode> {
        return behandlingsresultat.medlemAvFolketrygden?.fastsattTrygdeavgift?.trygdeavgiftsperioder?.filter { it.harAvgift() } ?: emptyList()
    }

    private fun mapFakturaserieDto(behandlingsresultat: Behandlingsresultat, prosessinstans: Prosessinstans): FakturaserieDto {
        val behandling = behandlingService.hentBehandling(behandlingsresultat.id)
        val fagsak = behandling.fagsak
        val fullmektig = fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT).orElse(null)
        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }
        val vedtaksdato =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(behandlingsresultat.vedtakMetadata.vedtaksdato)

        return FakturaserieDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = hentOpprinneligFakturaserieReferanse(behandling),
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall = hentBetalingsIntervall(prosessinstans),
            referanseBruker = "Vedtak om medlemskap datert $vedtaksdato",
            perioder = mapFakturaseriePeriodeDto(trygdeavgiftsperioderMedAvgift(behandlingsresultat))
        )
    }

    private fun hentBetalingsIntervall(prosessinstans: Prosessinstans): FaktureringsIntervall {
        return prosessinstans.getData(ProsessDataKey.BETALINGSINTERVALL, FaktureringsIntervall::class.java, FaktureringsIntervall.KVARTAL)
    }

    private fun hentOpprinneligFakturaserieReferanse(behandling: Behandling): String? {
        if (behandling.opprinneligBehandling != null) {
            return behandlingsresultatService.hentBehandlingsresultat(behandling.opprinneligBehandling.id).fakturaserieReferanse
        }
        return null
    }

    private fun mapFakturaseriePeriodeDto(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): List<FakturaseriePeriodeDto> {
        return trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.verdi,
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi}, " +
                    "Dekning: ${it.grunnlagMedlemskapsperiode.trygdedekning.beskrivelse}, " +
                    "Sats: ${it.trygdesats} %"
            )
        }
    }
}
