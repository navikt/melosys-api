package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse
import no.nav.melosys.integrasjon.hendelser.RapportType
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger { }

@Component
class SendPoppHendelseÅrsavregning(
    private val behandlingsresultatService: BehandlingsresultatService,
    private val persondataService: PersondataService,
    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer,
    private val årsavregningService: ÅrsavregningService,
    private val unleash: Unleash
) : StegBehandler {

    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.SEND_POPP_HENDELSE_AARSAVREGNING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        if (!unleash.isEnabled(ToggleName.MELOSYS_SEND_POPP_HENDELSE)) {
            log.debug("POPP-hendelse feature toggle er av")
            return
        }

        val behandlingId = prosessinstans.hentBehandling.id
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingId)
        val behandling = prosessinstans.hentBehandling
        val fagsak = behandling.fagsak

        // Validations: Only send for FTRL scope, non-tax-liable users
        if (!skalSendePoppHendelse(behandlingsresultat, fagsak)) {
            log.info("Sender ikke POPP-hendelse for behandling $behandlingId - validering feilet")
            return
        }

        val årsavregning = behandlingsresultat.årsavregning
            ?: throw IllegalStateException("Årsavregning mangler for behandling $behandlingId")

        // Get user's folkeregisterident
        val folkeregisterident = persondataService.finnFolkeregisterident(fagsak.hentBrukersAktørID())
            .orElseThrow { IllegalStateException("Kunne ikke finne folkeregisterident for behandling $behandlingId") }

        // Determine report type
        val rapportType = bestemRapportType(årsavregning, fagsak.saksnummer)

        // Get PGI (pension-earning income)
        val pgi = hentPgi(årsavregning, rapportType)

        // Create and send event
        val hendelse = PensjonsopptjeningHendelse(
            fnr = folkeregisterident,
            pgi = pgi,
            inntektsAr = årsavregning.aar,
            fastsattTidspunkt = behandlingsresultat.vedtakMetadata?.vedtaksdato?.let {
                LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            } ?: LocalDateTime.now(),
            rapportType = rapportType,
            vedtakId = behandlingId.toString()
        )

        try {
            kafkaMelosysHendelseProducer.produserBestillingsmelding(MelosysHendelse(hendelse))
            log.info("Sendt POPP-hendelse for behandling $behandlingId, rapportType: $rapportType, pgi: $pgi, inntektsÅr: ${årsavregning.aar}")
        } catch (e: Exception) {
            log.error("Feil ved sending av POPP-hendelse for behandling $behandlingId", e)
            // Don't fail the decision, just log the error
        }
    }

    private fun skalSendePoppHendelse(behandlingsresultat: Behandlingsresultat, fagsak: Fagsak): Boolean {
        // Only FTRL scope for now
        if (fagsak.type != Sakstyper.FTRL) {
            log.debug("Sender ikke POPP-hendelse: Sakstype er ikke FTRL")
            return false
        }

        // Only if yearly settlement exists
        if (behandlingsresultat.årsavregning == null) {
            log.debug("Sender ikke POPP-hendelse: Ingen årsavregning")
            return false
        }

        // TODO: Add check for "ikke skattepliktig til Norge" (not tax liable to Norway)
        // This requires access to tax liability information from the treatment
        // For now, we'll send for all FTRL yearly settlements

        return true
    }

    private fun bestemRapportType(årsavregning: Årsavregning, saksnummer: String): RapportType {
        // Check if there's a previous yearly settlement for the same year
        val tidligereÅrsavregninger = årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, årsavregning.aar, null)
            .filter { it.id != årsavregning.id }

        return when {
            // If PGI is 0, user hasn't paid - should be removed
            hentPgi(årsavregning, RapportType.FORSTE_GANG) == 0L -> RapportType.AVGANG
            // If there are previous yearly settlements for this year with different amounts
            tidligereÅrsavregninger.isNotEmpty() -> RapportType.ENDRING
            // First time for this year
            else -> RapportType.FORSTE_GANG
        }
    }

    private fun hentPgi(årsavregning: Årsavregning, rapportType: RapportType): Long {
        // For AVGANG (user hasn't paid), PGI should be 0
        if (rapportType == RapportType.AVGANG) {
            return 0L
        }

        // Use manual amount if set, otherwise use calculated amount
        val beløp = årsavregning.manueltAvgiftBeloep ?: årsavregning.beregnetAvgiftBelop
            ?: throw IllegalStateException("Både manuelt og beregnet avgiftsbeløp mangler for årsavregning ${årsavregning.id}")

        // Convert BigDecimal to Long (NOK amount)
        return beløp.toLong()
    }
}
