package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class TrygdeavgiftsberegningService
    (
    private val behandlingService: BehandlingService,
    private val eregFasade: EregFasade,
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(
        behandlingsresultatID: Long,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Set<Trygdeavgiftsperiode> {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)

        if (medlemAvFolketrygden.fastsattTrygdeavgift == null) {
            medlemAvFolketrygden.fastsattTrygdeavgift = nyFastsattTrygdeavgift(medlemAvFolketrygden)
        } else {
            medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.clear()
        }

        medlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden)

        TrygdeavgiftValideringService.validerTrygdeavgiftberegningRequest(oppdaterTrygdeavgiftsgrunnlagRequest, medlemAvFolketrygden);

        val innvilgedeMedlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }

        val medlemskapsperiodeDtos = mapTilMedlemskapsperiodeDtos(innvilgedeMedlemskapsperioder)
        val skatteforholdsperioderDtos = mapTilSkatteforholdsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest)
        val inntektsperioderDtos = mapInntektsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest)

        val trygdeavgiftsberegningRequest = TrygdeavgiftsberegningRequest(
            medlemskapsperiodeDtos,
            skatteforholdsperioderDtos,
            inntektsperioderDtos,
            hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID, innvilgedeMedlemskapsperioder)
        )

        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)

        val skalBetalesTilNAV =
            trygdeavgiftMottakerService.skalBetalesTilNav(medlemAvFolketrygden.fastsattTrygdeavgift, oppdaterTrygdeavgiftsgrunnlagRequest)
        if (!skalBetalesTilNAV && !erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift)) {
            throw IllegalStateException("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")
        }

        val nyeTrygdeavgiftsperioder = lagNyeTrygdeavgiftsperioder(
            medlemAvFolketrygden,
            skatteforholdsperioderDtos,
            inntektsperioderDtos,
            beregnetTrygdeavgift
        )

        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.addAll(nyeTrygdeavgiftsperioder)

        medlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden)
        return nyeTrygdeavgiftsperioder;
    }

    private fun mapTilMedlemskapsperiodeDtos(medlemskapsperioder: List<Medlemskapsperiode>) =
        medlemskapsperioder.map {
            MedlemskapsperiodeDto(
                it.idToUUID(),
                DatoPeriodeDto(it.fom, it.tom),
                AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekning(it.trygdedekning),
                it.medlemskapstype
            )
        }.toSet()


    private fun mapTilSkatteforholdsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest) =
        oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.map {
            SkatteforholdsperiodeDto(
                it.toUUID(),
                DatoPeriodeDto(it.fomDato, it.tomDato ?: LocalDate.MAX), //TODO: Håndter null
                it.skatteplikttype
            )
        }.toSet()


    private fun mapInntektsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest) =
        oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.map {
            InntektsperiodeDto(
                it.toUUID(),
                DatoPeriodeDto(it.fomDato, it.tomDato ?: LocalDate.MAX), //TODO: Håndter null
                it.type,
                it.arbeidsgiversavgiftBetales,
                PengerDto(it.avgiftspliktigInntektMnd ?: 0.toBigDecimal())
            )
        }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun nyFastsattTrygdeavgift(medlemAvFolketrygden: MedlemAvFolketrygden): FastsattTrygdeavgift =
        FastsattTrygdeavgift().apply {
            this.trygdeavgiftstype = Trygdeavgift_typer.FORELØPIG
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

    private fun lagNyeTrygdeavgiftsperioder(
        medlemAvFolketrygden: MedlemAvFolketrygden,
        skatteForholdTilNorgeDtos: Set<SkatteforholdsperiodeDto>,
        inntektsperiodeDtos: List<InntektsperiodeDto>,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>,
    ): Set<Trygdeavgiftsperiode> {

        val skatteforholdTilNorge = skatteForholdTilNorgeDtos.map {
            Pair(
                it.id,
                SkatteforholdTilNorge().apply {
                    this.fomDato = it.periode.fom
                    this.tomDato = it.periode.tom
                    this.skatteplikttype = it.skatteforhold
                })
        }

        val inntektsperioder = inntektsperiodeDtos.map {
            Pair(
                it.id,
                Inntektsperiode().apply {
                    this.fomDato = it.periode.fom
                    this.tomDato = it.periode.tom
                    this.type = it.inntektskilde
                    this.isArbeidsgiversavgiftBetalesTilSkatt = it.arbeidsgiverBetalerAvgift == true
                    this.avgiftspliktigInntektMnd = it.månedsbeløp?.tilPenger()
                })
        }

        return beregnetTrygdeavgift.map {
            lagTrygdeavgiftsperiode(
                medlemAvFolketrygden,
                skatteforholdTilNorge,
                inntektsperioder,
                it
            )
        }.toSet()
    }

    private fun lagTrygdeavgiftsperiode(
        medlemAvFolketrygden: MedlemAvFolketrygden,
        skatteforholdTilNorge: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioder: List<Pair<UUID, Inntektsperiode>>,
        trygdeavgiftsberegningResponse: TrygdeavgiftsberegningResponse,
    ): Trygdeavgiftsperiode {
        val beregnetPeriode = trygdeavgiftsberegningResponse.beregnetPeriode
        val beregningsgrunnlag = trygdeavgiftsberegningResponse.grunnlag

        return Trygdeavgiftsperiode().apply {
            this.periodeFra = beregnetPeriode.periode.fom
            this.periodeTil = beregnetPeriode.periode.tom
            this.trygdesats = beregnetPeriode.sats
            this.trygdeavgiftsbeløpMd = beregnetPeriode.månedsavgift.tilPenger()
            this.fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
            this.grunnlagMedlemskapsperiode = medlemAvFolketrygden.medlemskapsperioder
                .find {
                    UUID.nameUUIDFromBytes(it.id.toString().toByteArray()) == beregningsgrunnlag.medlemskapsperiodeId
                }
            this.grunnlagSkatteforholdTilNorge = skatteforholdTilNorge.find {
                it.first == beregningsgrunnlag.skatteforholdsperiodeId
            }?.second
            this.grunnlagInntekstperiode = inntektsperioder.find {
                it.first == beregningsgrunnlag.inntektsperiodeId
            }?.second
        }
    }

    private fun erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>): Boolean {
        return beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.valueOf(0)) == 0 }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map { it.fastsattTrygdeavgift?.trygdeavgiftsperioder }
            .orElse(null)
            ?: emptySet()
    }

    @Transactional(readOnly = true)
    fun finnFakturamottakerNavn(behandlingID: Long): String {
        val fagsak = behandlingService.hentBehandling(behandlingID).fagsak
        fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            .let {
                if (it == null)
                    return persondataService.hentSammensattNavn(fagsak.hentBrukersAktørID())
                if (it.erPerson())
                    return persondataService.hentSammensattNavn(it.personIdent)
                return eregFasade.hentOrganisasjonNavn(it.orgnr)
            }
    }

    companion object {
        fun SkatteforholdTilNorgeRequest.toUUID(): UUID {
            return UUID.nameUUIDFromBytes("${this.fomDato}${this.tomDato}${this.skatteplikttype}".toByteArray())
        }

        fun InntektskildeRequest.toUUID(): UUID {
            return UUID.nameUUIDFromBytes("${this.fomDato}${this.tomDato}${this.type}${this.arbeidsgiversavgiftBetales}${this.avgiftspliktigInntektMnd}".toByteArray())
        }

        fun Medlemskapsperiode.idToUUID(): UUID {
            return UUID.nameUUIDFromBytes(this.id.toString().toByteArray())
        }
    }
}
