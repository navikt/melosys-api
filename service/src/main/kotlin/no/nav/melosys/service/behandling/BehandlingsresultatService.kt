package no.nav.melosys.service.behandling

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import kotlin.jvm.optionals.getOrNull


private val log = KotlinLogging.logger { }

@Service
class BehandlingsresultatService(
    private val behandlingsresultatRepository: BehandlingsresultatRepository
) {
    @Transactional
    fun tømBehandlingsresultat(behandlingID: Long) {
        log.info("Fjerner avklarte fakta, lovvalgsperioder, medlemAvFolketrygden og vilkårsresultater fra behandlingsresultat med behandlingID: $behandlingID")

        behandlingsresultatRepository.findById(behandlingID)
            .orElseThrowIkkeFunnetException(behandlingID).apply {
                avklartefakta.clear()
                lovvalgsperioder.clear()
                medlemskapsperioder.clear()
                trygdeavgiftsperioder.clear()
                vilkaarsresultater.clear()
                utfallRegistreringUnntak = null
                begrunnelseFritekst = null
                innledningFritekst = null
                nyVurderingBakgrunn = null
                trygdeavgiftFritekst = null
            }.also {
                behandlingsresultatRepository.save(it)
            }
    }

    @Transactional
    fun tømMedlemskapsperioder(behandlingID: Long) {
        hentBehandlingsresultat(behandlingID).medlemskapsperioder.clear()
    }

    fun hentBehandlingsresultat(behandlingsid: Long): Behandlingsresultat =
        behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrowIkkeFunnetException(behandlingsid)

    fun finnAlleBehandlingsresultatMedFakturaserieReferanse(fakturaserieReferanse: String): List<Behandlingsresultat> =
        behandlingsresultatRepository.findAllByFakturaserieReferanse(fakturaserieReferanse)

    fun hentBehandlingsresultatMedAnmodningsperioder(behandlingsid: Long): Behandlingsresultat =
        behandlingsresultatRepository.findWithAnmodningsperioderById(behandlingsid)
            .orElseThrowIkkeFunnetException(behandlingsid)

    fun finnResultatMedMedlemskapOgLovvalg(behandlingsid: Long): Behandlingsresultat? =
        behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(behandlingsid).orElse(null)

    fun hentResultatMedMedlemskapOgLovvalg(behandlingsid: Long): Behandlingsresultat =
        behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(behandlingsid)
            .orElseThrowIkkeFunnetException(behandlingsid)

    fun hentBehandlingsresultatMedKontrollresultat(behandlingsid: Long): Behandlingsresultat =
        behandlingsresultatRepository.findWithKontrollresultaterById(behandlingsid)
            .orElseThrowIkkeFunnetException(behandlingsid)

    fun hentBehandlingsresultatMedAvklartefakta(behandlingsid: Long): Behandlingsresultat =
        behandlingsresultatRepository.findWithAvklartefaktaById(behandlingsid)
            .orElseThrowIkkeFunnetException(behandlingsid)

    fun lagre(resultat: Behandlingsresultat): Behandlingsresultat = behandlingsresultatRepository.save(resultat)

    fun lagreOgFlush(resultat: Behandlingsresultat): Behandlingsresultat = behandlingsresultatRepository.saveAndFlush(resultat)

    fun lagreNyttBehandlingsresultat(behandling: Behandling) {
        Behandlingsresultat().apply {
            setBehandling(behandling)
            type = Behandlingsresultattyper.IKKE_FASTSATT
            behandlingsmåte = Behandlingsmaate.MANUELT
        }.also {
            behandlingsresultatRepository.save(it)
        }
    }

    fun oppdaterBehandlingsresultattype(id: Long, behandlingsresultattype: Behandlingsresultattyper) {
        behandlingsresultatRepository.findById(id).getOrNull()?.let { behandlingsresultat ->
            log.info("Setter behandlingsresultattype på $id  til $behandlingsresultattype")
            behandlingsresultat.type = behandlingsresultattype
            behandlingsresultatRepository.save(behandlingsresultat)
        }
    }

    fun oppdaterBehandlingsMaate(id: Long, behandlingsmaate: Behandlingsmaate) {
        hentBehandlingsresultat(id).apply {
            behandlingsmåte = behandlingsmaate
        }.also {
            behandlingsresultatRepository.save(it)
        }
    }

    fun settUtfallRegistreringUnntakOgType(behandlingID: Long, utfallRegistreringUnntak: Utfallregistreringunntak) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        if (behandlingsresultat.utfallRegistreringUnntak != null) {
            throw FunksjonellException("Utfall for registrering av unntak er allerede satt for behandlingsresultat $behandlingID")
        }
        behandlingsresultat.type = finnKorrektBehandlingsResultat(utfallRegistreringUnntak)
        oppdaterUtfallRegistreringUnntak(behandlingID, utfallRegistreringUnntak)
    }

    fun oppdaterUtfallRegistreringUnntak(behandlingID: Long, utfallUtpeking: Utfallregistreringunntak): Behandlingsresultat {
        hentBehandlingsresultatMedKontrollresultat(behandlingID).apply {
            utfallRegistreringUnntak = utfallUtpeking
        }.also {
            return behandlingsresultatRepository.save(it)
        }
    }

    fun oppdaterUtfallUtpeking(behandlingID: Long, utfallUtpeking: Utfallregistreringunntak) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        if (behandlingsresultat.utfallUtpeking != null) {
            throw FunksjonellException("Utfall for utpeking er allerede satt for behandlingsresultat $behandlingID")
        }
        behandlingsresultat.utfallUtpeking = utfallUtpeking
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun oppdaterBegrunnelser(behandlingID: Long, begrunnelser: Set<BehandlingsresultatBegrunnelse>, begrunnelseFritekst: String) {
        hentBehandlingsresultat(behandlingID).let { behandlingsresultat ->
            begrunnelser.forEach { it.behandlingsresultat = behandlingsresultat }
            behandlingsresultat.behandlingsresultatBegrunnelser.addAll(begrunnelser)
            behandlingsresultat.begrunnelseFritekst = begrunnelseFritekst
            behandlingsresultatRepository.save(behandlingsresultat)
        }
    }

    fun oppdaterFritekster(
        behandlingID: Long,
        begrunnelseFritekst: String,
        innledningFritekst: String,
        trygdeavgiftFritekst: String?
    ): Behandlingsresultat = hentBehandlingsresultat(behandlingID).let { behandlingsresultat ->
        behandlingsresultat.begrunnelseFritekst = begrunnelseFritekst
        behandlingsresultat.innledningFritekst = innledningFritekst
        behandlingsresultat.trygdeavgiftFritekst = trygdeavgiftFritekst
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun oppdaterNyVurderingBakgrunn(behandlingID: Long, nyVurderingBakgrunn: String): Behandlingsresultat =
        hentBehandlingsresultat(behandlingID).let { behandlingsresultat ->
            behandlingsresultat.nyVurderingBakgrunn = nyVurderingBakgrunn
            behandlingsresultatRepository.save(behandlingsresultat)
        }

    private fun finnKorrektBehandlingsResultat(utfallregistreringunntak: Utfallregistreringunntak): Behandlingsresultattyper =
        when (utfallregistreringunntak) {
            Utfallregistreringunntak.GODKJENT, Utfallregistreringunntak.DELVIS_GODKJENT -> Behandlingsresultattyper.REGISTRERT_UNNTAK
            Utfallregistreringunntak.IKKE_GODKJENT -> Behandlingsresultattyper.FERDIGBEHANDLET
            else -> Behandlingsresultattyper.IKKE_FASTSATT
        }


    private fun <T> Optional<T>.orElseThrowIkkeFunnetException(behandlingsid: Long): T =
        this.orElseThrow { IkkeFunnetException("Kan ikke finne behandlingsresultat for behandling: $behandlingsid") }
}
