package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.hendelser.KafkaPensjonsopptjeningHendelseProducer
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse.*
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
    private val kafkaPensjonsopptjeningHendelseProducer: KafkaPensjonsopptjeningHendelseProducer,
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

        // Send kun for FTRL-saker, brukere som ikke er skattepliktige til Norge
        if (!skalSendePoppHendelse(behandlingsresultat, fagsak)) {
            return
        }

        val årsavregning = behandlingsresultat.årsavregning
            ?: error("Årsavregning mangler for behandling $behandlingId")

        val folkeregisterident = persondataService.hentFolkeregisterident(fagsak.hentBrukersAktørID())

        val endringstype = bestemEndringstype(årsavregning, fagsak.saksnummer)

        val pgi = hentPgi(årsavregning, endringstype)

        val hendelse = PensjonsopptjeningHendelse(
            fnr = folkeregisterident,
            pgi = pgi,
            inntektsAr = årsavregning.aar,
            fastsattTidspunkt = behandlingsresultat.vedtakMetadata?.vedtaksdato?.let {
                LocalDateTime.ofInstant(it, ZoneId.systemDefault())
            } ?: LocalDateTime.now(),
            endringstype = endringstype,
            melosysBehandlingID = behandlingId.toString()
        )

        try {
            kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(hendelse)
            log.info("Sendt POPP-hendelse for behandling $behandlingId, endringstype: $endringstype, pgi: $pgi, inntektsÅr: ${årsavregning.aar}")
        } catch (e: Exception) {
            log.error("Feil ved sending av POPP-hendelse for behandling $behandlingId", e)
        }
    }

    private fun skalSendePoppHendelse(behandlingsresultat: Behandlingsresultat, fagsak: Fagsak): Boolean {
        if (fagsak.type != Sakstyper.FTRL) {
            log.info("Sender ikke POPP-hendelse: Sakstype er ikke FTRL")
            return false
        }

        // Kun hvis årsavregning eksisterer
        if (behandlingsresultat.årsavregning == null) {
            log.info("Sender ikke POPP-hendelse: Ingen årsavregning for behandlingsresultat: ${behandlingsresultat.id}")
            return false
        }

        // Kun for brukere som ikke er skattepliktige til Norge
        val skatteplikttype = behandlingsresultat.utledSkatteplikttype()

        if (skatteplikttype != Skatteplikttype.IKKE_SKATTEPLIKTIG) {
            log.info("Sender ikke POPP-hendelse: Bruker er skattepliktig til Norge (skatteplikttype=$skatteplikttype, behandling=${behandlingsresultat.id})")
            return false
        }

        return true
    }

    private fun bestemEndringstype(årsavregning: Årsavregning, saksnummer: String): Endringstype {
        // Sjekk om det finnes en tidligere årsavregning for samme år
        val tidligereÅrsavregninger = årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, årsavregning.aar, null)
            .filter { it.id != årsavregning.id }

        return when {
            // Hvis PGI er 0, har bruker ikke betalt - skal fjernes
            hentPgi(årsavregning, Endringstype.NY_INNTEKT) == 0L -> Endringstype.FJERNING
            // Hvis det finnes tidligere årsavregninger for dette året med forskjellige beløp
            tidligereÅrsavregninger.isNotEmpty() -> Endringstype.OPPDATERING
            // Første gang for dette året
            else -> Endringstype.NY_INNTEKT
        }
    }

    private fun hentPgi(årsavregning: Årsavregning, endringstype: Endringstype): Long {
        // For FJERNING (bruker har ikke betalt), skal PGI være 0
        if (endringstype == Endringstype.FJERNING) {
            return 0L
        }

        // Bruk manuelt beløp hvis satt, ellers bruk beregnet beløp
        val beløp = årsavregning.manueltAvgiftBeloep
            ?: årsavregning.beregnetAvgiftBelop
            ?: error("Både manuelt og beregnet avgiftsbeløp mangler for årsavregning ${årsavregning.id}")

        // Konverter BigDecimal til Long (NOK-beløp)
        return beløp.toLong()
    }
}
