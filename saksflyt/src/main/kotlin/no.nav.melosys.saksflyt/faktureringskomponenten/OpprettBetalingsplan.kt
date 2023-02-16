package no.nav.melosys.saksflyt.faktureringskomponenten

import mu.KotlinLogging
import no.finn.unleash.Unleash
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Kontaktopplysning
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.saksflyt.ProsessDataKey
import no.nav.melosys.domain.saksflyt.ProsessSteg
import no.nav.melosys.domain.saksflyt.Prosessinstans
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaserieDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FaktureringsIntervall
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FullmektigDto
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class OpprettBetalingsplan(
    @Autowired val behandlingService: BehandlingService,
    @Autowired val behandlingsresultatService: BehandlingsresultatService,
    @Autowired val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    @Autowired val kontaktopplysningService: KontaktopplysningService,
    @Autowired val pdlService: PersondataService,
    @Autowired val unleash: Unleash
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
        val kontaktopplysning =
            kontaktopplysningService.hentKontaktopplysning(fagsak.saksnummer, fastsattTrygdeavgift.betalesAv.orgnr)

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
                fullmektig = fullmektigDto(fastsattTrygdeavgift.betalesAv, kontaktopplysning),
                fakturaGjelder = "Medlemskapsavgift",
                intervall = intervall ?: FaktureringsIntervall.MANEDLIG,
                referanseBruker = vedtaksdato,
                perioder = fakturaseriePeriodeDtoListe
            )

        log.info("Oppretter betalingsplan for behandling: $behandlingsId")

        faktureringskomponentenConsumer.lagFakturaSerie(fakturaserieDto)
    }

    private fun fullmektigDto(
        betalesAv: Aktoer?,
        kontaktopplysning: Optional<Kontaktopplysning>
    ) = FullmektigDto(
        fodselsnummer = betalesAv?.personIdent,
        organisasjonsnummer = betalesAv?.orgnr,
        kontaktperson = kontaktopplysning.orElse(null).kontaktNavn
    )
}
