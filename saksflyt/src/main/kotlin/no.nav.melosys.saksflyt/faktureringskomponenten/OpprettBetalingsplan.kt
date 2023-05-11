package no.nav.melosys.saksflyt.faktureringskomponenten

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Representerer
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.ProsessSteg
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.*
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OpprettBetalingsplan(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val kontaktopplysningService: KontaktopplysningService,
    private val pdlService: PersondataService,
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_BETALINGSPLAN
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled("melosys.folketrygden.mvp")) {
            return
        }

        val behandlingsId = prosessinstans.behandling.id
        val behandling = behandlingService.hentBehandling(behandlingsId)
        val fagsak = behandling.fagsak
        val aktoerer = fagsak.aktører.filter { it.rolle == Aktoersroller.BRUKER }

        if (aktoerer.size > 1) {
            throw FunksjonellException("Kunne ikke opprette betalingsplan, det finnes ${aktoerer.size} aktører med rolle BRUKER")
        } else if (aktoerer.isEmpty()) {
            throw FunksjonellException("Kunne ikke opprette betalingsplan, det finnes ${aktoerer.size} aktører")
        }

        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val vedtaksdato = behandlingsresultat.vedtakMetadata.vedtaksdato.toString()
        val medlemskapsperioder = behandlingsresultat.medlemAvFolketrygden.medlemskapsperioder
        val fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift
        val avgiftspliktigUtenlandskInntektMnd = fastsattTrygdeavgift.avgiftspliktigUtenlandskInntektMnd ?: 0
        val avgiftspliktigNorskInntektMnd = fastsattTrygdeavgift.avgiftspliktigNorskInntektMnd ?: 0
        val inntektBelopMnd = avgiftspliktigUtenlandskInntektMnd + avgiftspliktigNorskInntektMnd
        val fullmektig = fagsak.finnRepresentant(Representerer.BRUKER).orElse(null)
        val kontaktopplysning = hentKontaktopplysning(fagsak, fullmektig)

        val alleTrygdeavgiftIMedlemskap = medlemskapsperioder.flatMap {
            it.trygdeavgift.map { trygdeavgift -> trygdeavgift }
        }
        val fakturaseriePeriodeDtoListe = alleTrygdeavgiftIMedlemskap.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd,
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${inntektBelopMnd}, Dekning: ${it.medlemskapsperiode.dekning}, Sats: ${it.trygdesats} %"
            )
        }

        val foedselsNr = pdlService.finnFolkeregisterident(aktoerer.first().aktørId)
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }

        val intervall = prosessinstans.getData(
            ProsessDataKey.BETALINGSINTERVALL,
            FaktureringsIntervall::class.java
        )

        val fakturaserieDto =
            FakturaserieDto(
                vedtaksId = "${fagsak.saksnummer}-$behandlingsId",
                fodselsnummer = foedselsNr,
                referanseNAV = "Medlemskap og avgift",
                fullmektig = fullmektigDto(fagsak, kontaktopplysning),
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = intervall ?: FaktureringsIntervall.MANEDLIG,
                referanseBruker = vedtaksdato,
                perioder = fakturaseriePeriodeDtoListe
            )

        log.info("Oppretter betalingsplan for behandling: $behandlingsId")

        faktureringskomponentenConsumer.lagFakturaSerie(fakturaserieDto)
    }

    private fun hentKontaktopplysning(
        fagsak: Fagsak,
        betalesAv: Aktoer?
    ): Kontaktopplysning? {
        if (betalesAv == null) return null
        return kontaktopplysningService.hentKontaktopplysning(fagsak.saksnummer, betalesAv.orgnr).orElse(null)
    }

    private fun fullmektigDto(
        fagsak: Fagsak,
        kontaktopplysning: Kontaktopplysning?
    ): FullmektigDto {
        val fullmektig = fagsak.finnRepresentant(Representerer.BRUKER).orElse(null)
        return FullmektigDto(
            fodselsnummer = fullmektig?.personIdent,
            organisasjonsnummer = fullmektig?.orgnr,
            kontaktperson = kontaktopplysning?.kontaktNavn
        )
    }
}
