package no.nav.melosys.saksflyt.faktureringskomponenten

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Kontaktopplysning
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
import no.nav.melosys.service.ftrl.FaktureringsKomponentenHjelper
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
        val fastsattTrygdeavgift = behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift

        if (!fastsattTrygdeavgift.skalBetalesTilNav()) {
            return
        }

        val trygdeavgiftsperioderMedAvgift = fastsattTrygdeavgift.trygdeavgiftsperioder.filter { it.harAvgift() }

        if (trygdeavgiftsperioderMedAvgift.isEmpty()) {
            return
        }

        val vedtaksdato = behandlingsresultat.vedtakMetadata.vedtaksdato
        val fullmektig = fagsak.finnRepresentant(Representerer.BRUKER).orElse(null)
        val kontaktopplysning = hentKontaktopplysning(fagsak, fullmektig)

        val fakturaseriePeriodeDtoListe = trygdeavgiftsperioderMedAvgift.map {
            FakturaseriePeriodeDto(
                it.trygdeavgiftsbeløpMd.verdi,
                it.periodeFra,
                it.periodeTil,
                "Inntekt: ${it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi}, Dekning: ${it.grunnlagMedlemskapsperiode.trygdedekning.beskrivelse}, Sats: ${it.trygdesats} %"
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
                vedtaksId = FaktureringsKomponentenHjelper.konverterTilVedtaksId(fagsak.saksnummer, behandlingsId),
                fodselsnummer = foedselsNr,
                referanseNAV = "Medlemskap og avgift",
                fullmektig = fullmektigDto(fullmektig, kontaktopplysning),
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = intervall,
                referanseBruker = "Vedtak om medlemskap datert " +
                    DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(vedtaksdato),
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
        fullmektig: Aktoer?,
        kontaktopplysning: Kontaktopplysning?
    ) = FullmektigDto(
        fodselsnummer = fullmektig?.personIdent,
        organisasjonsnummer = fullmektig?.orgnr,
        kontaktperson = kontaktopplysning?.kontaktNavn
    )
}
