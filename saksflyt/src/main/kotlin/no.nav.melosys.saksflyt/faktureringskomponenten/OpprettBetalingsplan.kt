package no.nav.melosys.saksflyt.faktureringskomponenten

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Kontaktopplysning
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
import java.math.BigDecimal
import java.util.*

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
        val fagsak = behandlingService.hentBehandling(behandlingsId).fagsak

        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val vedtaksdato = behandlingsresultat.vedtakMetadata.vedtaksdato.toString()
        val fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift
        val kontaktopplysning = hentKontaktopplysning(fagsak, fastsattTrygdeavgift.betalesAv)

        val fakturaseriePeriodeDtoListe = fastsattTrygdeavgift.trygdeavgift.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.verdi,
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${it.hentGjeldendeAvgiftspliktigInntekt()}, Dekning: ${it.hentGjeldendeTrygdedekning()}, Sats: ${it.trygdesats} %"
            )
        }

        val foedselsNr = pdlService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { FunksjonellException("Kunne ikke finne fødselsnummer fra PDL") }

        val intervall = prosessinstans.getData(
            ProsessDataKey.BETALINGSINTERVALL,
            FaktureringsIntervall::class.java,
            FaktureringsIntervall.MANEDLIG
        )

        val fakturaserieDto =
            FakturaserieDto(
                vedtaksId = "${fagsak.saksnummer}-$behandlingsId",
                fodselsnummer = foedselsNr,
                referanseNAV = "Medlemskap og avgift",
                fullmektig = fullmektigDto(fastsattTrygdeavgift.betalesAv, kontaktopplysning),
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = intervall,
                referanseBruker = vedtaksdato,
                perioder = fakturaseriePeriodeDtoListe
            )

        log.info("Oppretter betalingsplan for behandling: $behandlingsId")

        faktureringskomponentenConsumer.lagFakturaSerie(fakturaserieDto)
    }

    private fun hentKontaktopplysning(
        fagsak: Fagsak,
        betalesAv: Aktoer?
    ): Optional<Kontaktopplysning> {
        if (betalesAv == null) return Optional.empty()
        return kontaktopplysningService.hentKontaktopplysning(fagsak.saksnummer, betalesAv.orgnr)
    }

    private fun fullmektigDto(
        betalesAv: Aktoer?,
        kontaktopplysning: Optional<Kontaktopplysning>
    ) = FullmektigDto(
        fodselsnummer = betalesAv?.personIdent,
        organisasjonsnummer = betalesAv?.orgnr,
        kontaktperson = kontaktopplysning.orElse(null)?.kontaktNavn
    )
}
