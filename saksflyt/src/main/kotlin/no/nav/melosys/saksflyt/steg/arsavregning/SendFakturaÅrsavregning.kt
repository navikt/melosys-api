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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

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
        val behandlingsId = prosessinstans.behandlingOrFail().id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsId)
        val saksbehandlerIdent = prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)!!


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
        val startDato = finnStartDato(behandlingsresultat)
        val sluttDato = finnSluttDato(behandlingsresultat)
        val startDatoFormatert = FORMATTER.format(startDato)
        val sluttDatoFormatert = FORMATTER.format(sluttDato)
        val harTidligereÅrsavregning = årsavregning.tidligereBehandlingsresultat?.behandling?.erÅrsavregning() ?: false
        val tidligereFakturertSum = Objects.requireNonNullElse(årsavregning.tidligereFakturertBeloep, BigDecimal.ZERO).add(
            Objects
                .requireNonNullElse(årsavregning.trygdeavgiftFraAvgiftssystemet, BigDecimal.ZERO)
        )

        return FakturaDto(
            fodselsnummer = foedselsNr,
            fakturaserieReferanse = if (harTidligereÅrsavregning) årsavregning.tidligereBehandlingsresultat.fakturaserieReferanse else null,
            referanseNAV = "Medlemskap og avgift",
            fullmektig = FullmektigDto(fullmektig),
            fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING,
            referanseBruker = "Årsavregning datert $vedtaksdato",
            belop = årsavregning.tilFaktureringBeloep,
            startDato = startDato,
            sluttDato = sluttDato,
            beskrivelse = if (årsavregning.manueltAvgiftBeloep == null) {
                "Medlemskapsperiode ${startDatoFormatert} - $sluttDatoFormatert, endelig beregnet trygdeavgift ${årsavregning.beregnetAvgiftBelop} - forskuddsvis" +
                    " fakturert trygdeavgift $tidligereFakturertSum"
            } else "Årsavregning ${årsavregning.aar}" // TODO: Endre denne når fag har kommet fram til bedre begrep. Kanskje lage egen felt for "fakturalinjeBeskrivelse" i FakturaDto?
        )
    }

    /**
     * Startdato hentes fra trygdeavgiftsperiodene i behandlingsresultatet på nåværende behandling.
     * Hvis denne ikke har trygdeavgiftsperioder så kommer dette av at man han brukt manuel avgift og da
     * benyttes tidligere trygdeavgiftsperioder. Ved ingen grunnlag så finnes det ikke trygdeavgiftsperioder i det hele
     * tatt og da brukes 1. januar i året for årsavregningen.
     */
    private fun finnStartDato(behandlingsresultat: Behandlingsresultat): LocalDate {
        val perioder = behandlingsresultat.trygdeavgiftsperioder

        val tidligerePerioder = if (perioder.isNullOrEmpty()) {
            behandlingsresultat.årsavregning?.tidligereBehandlingsresultat?.trygdeavgiftsperioder
        } else null

        return perioder?.takeIf { it.isNotEmpty() }?.minOfOrNull { it.periodeFra }
            ?: tidligerePerioder?.minOfOrNull { it.periodeFra }
            ?: LocalDate.of(behandlingsresultat.årsavregning.aar, 1, 1)
    }

    private fun finnSluttDato(behandlingsresultat: Behandlingsresultat): LocalDate {
        val perioder = behandlingsresultat.trygdeavgiftsperioder

        val tidligerePerioder = if (perioder.isNullOrEmpty()) {
            behandlingsresultat.årsavregning?.tidligereBehandlingsresultat?.trygdeavgiftsperioder
        } else null

        return perioder?.takeIf { it.isNotEmpty() }?.minOfOrNull { it.periodeTil }
            ?: tidligerePerioder?.minOfOrNull { it.periodeTil }
            ?: LocalDate.of(behandlingsresultat.årsavregning.aar, 12, 31)
    }

    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
    }
}
