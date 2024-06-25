package no.nav.melosys.service.behandling

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatBegrunnelse
import no.nav.melosys.domain.kodeverk.Utfallregistreringunntak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer

@Service
class BehandlingsresultatService(
    private val behandlingsresultatRepository: BehandlingsresultatRepository,
    private val vilkaarsresultatService: VilkaarsresultatService
) {
    @Transactional
    fun tømBehandlingsresultat(behandlingID: Long) {
        val behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow { IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingID) }

        log.info(
            "Fjerner avklarte fakta, lovvalgsperioder, medlemAvFolketrygden og vilkårsresultater fra behandlingsresultat med behandlingID: {} ",
            behandlingID
        )
        behandlingsresultat.avklartefakta.clear()
        behandlingsresultat.lovvalgsperioder.clear()
        behandlingsresultat.medlemskapsperioder.clear()
        fjernMedlemAvFolketrygdenHvisDenFinnes(behandlingsresultat)
        behandlingsresultat.utfallRegistreringUnntak = null
        behandlingsresultat.begrunnelseFritekst = null
        behandlingsresultat.innledningFritekst = null
        behandlingsresultat.nyVurderingBakgrunn = null
        behandlingsresultat.trygdeavgiftFritekst = null
        vilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(behandlingID)
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun hentBehandlingsresultat(behandlingsid: Long): Behandlingsresultat {
        return behandlingsresultatRepository.findById(behandlingsid)
            .orElseThrow { IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid) }
    }

    fun finnAlleBehandlingsresultatMedFakturaserieReferanse(fakturaserieReferanse: String?): List<Behandlingsresultat> {
        return behandlingsresultatRepository.findAllByFakturaserieReferanse(fakturaserieReferanse!!)
    }

    fun hentBehandlingsresultatMedAnmodningsperioder(behandlingsid: Long): Behandlingsresultat {
        return behandlingsresultatRepository.findWithAnmodningsperioderById(behandlingsid)
            .orElseThrow { IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid) }
    }

    fun finnBehandlingsresultatMedLovvalgsperioder(behandlingsid: Long): Behandlingsresultat {
        return behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(behandlingsid).orElse(null)
    }

    fun hentBehandlingsresultatMedLovvalgsperioder(behandlingsid: Long): Behandlingsresultat {
        return behandlingsresultatRepository.findWithLovvalgOgMedlemskapsperioderById(behandlingsid)
            .orElseThrow { IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid) }
    }

    fun hentBehandlingsresultatMedKontrollresultat(behandlingsid: Long): Behandlingsresultat {
        return behandlingsresultatRepository.findWithKontrollresultaterById(behandlingsid)
            .orElseThrow { IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid) }
    }

    fun hentBehandlingsresultatMedAvklartefakta(behandlingsid: Long): Behandlingsresultat {
        return behandlingsresultatRepository.findWithAvklartefaktaById(behandlingsid)
            .orElseThrow { IkkeFunnetException(KAN_IKKE_FINNE_BEHANDLINGSRESULTAT + behandlingsid) }
    }

    fun lagre(resultat: Behandlingsresultat): Behandlingsresultat {
        return behandlingsresultatRepository.save(resultat)
    }

    fun lagreOgFlush(resultat: Behandlingsresultat): Behandlingsresultat {
        return behandlingsresultatRepository.saveAndFlush(resultat)
    }

    fun lagreNyttBehandlingsresultat(behandling: Behandling?) {
        val nyttBehandlingsresultat = Behandlingsresultat()
        nyttBehandlingsresultat.behandling = behandling
        nyttBehandlingsresultat.type = Behandlingsresultattyper.IKKE_FASTSATT
        nyttBehandlingsresultat.behandlingsmåte = Behandlingsmaate.MANUELT
        behandlingsresultatRepository.save(nyttBehandlingsresultat)
    }

    fun oppdaterBehandlingsresultattype(id: Long, behandlingsresultattype: Behandlingsresultattyper?) {
        val optionalBehandlingsresultat = behandlingsresultatRepository.findById(id)
        if (optionalBehandlingsresultat.isPresent) {
            val behandlingsresultat = optionalBehandlingsresultat.get()
            log.info("Setter behandlingsresultattype på {} til {}", id, behandlingsresultattype)
            behandlingsresultat.type = behandlingsresultattype
            behandlingsresultatRepository.save(behandlingsresultat)
        }
    }

    fun oppdaterBehandlingsMaate(id: Long, behandlingsmaate: Behandlingsmaate?) {
        val behandlingsresultat = hentBehandlingsresultat(id)

        behandlingsresultat.behandlingsmåte = behandlingsmaate
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun settUtfallRegistreringUnntakOgType(behandlingID: Long, utfallRegistreringUnntak: Utfallregistreringunntak) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        if (behandlingsresultat.utfallRegistreringUnntak != null) {
            throw FunksjonellException("Utfall for registrering av unntak er allerede satt for behandlingsresultat $behandlingID")
        }

        behandlingsresultat.type = finnKorrektBehandlingsResultat(utfallRegistreringUnntak)
        oppdaterUtfallRegistreringUnntak(behandlingID, utfallRegistreringUnntak)
    }

    fun oppdaterUtfallRegistreringUnntak(
        behandlingID: Long,
        utfallUtpeking: Utfallregistreringunntak?
    ): Behandlingsresultat {
        val behandlingsresultat = hentBehandlingsresultatMedKontrollresultat(behandlingID)
        behandlingsresultat.utfallRegistreringUnntak = utfallUtpeking
        return behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun oppdaterUtfallUtpeking(behandlingID: Long, utfallUtpeking: Utfallregistreringunntak?) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        if (behandlingsresultat.utfallUtpeking != null) {
            throw FunksjonellException("Utfall for utpeking er allerede satt for behandlingsresultat $behandlingID")
        }

        behandlingsresultat.utfallUtpeking = utfallUtpeking
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun oppdaterBegrunnelser(
        behandlingID: Long,
        begrunnelser: Set<BehandlingsresultatBegrunnelse>,
        begrunnelseFritekst: String?
    ) {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        begrunnelser.forEach(Consumer { b: BehandlingsresultatBegrunnelse ->
            b.behandlingsresultat = behandlingsresultat
        })
        behandlingsresultat.behandlingsresultatBegrunnelser.addAll(begrunnelser)
        behandlingsresultat.begrunnelseFritekst = begrunnelseFritekst
        behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun oppdaterFritekster(
        behandlingID: Long,
        begrunnelseFritekst: String?,
        innledningFritekst: String?,
        trygdeavgiftFritekst: String?
    ): Behandlingsresultat {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)
        behandlingsresultat.begrunnelseFritekst = begrunnelseFritekst
        behandlingsresultat.innledningFritekst = innledningFritekst
        behandlingsresultat.trygdeavgiftFritekst = trygdeavgiftFritekst
        return behandlingsresultatRepository.save(behandlingsresultat)
    }

    fun oppdaterNyVurderingBakgrunn(behandlingID: Long, nyVurderingBakgrunn: String?): Behandlingsresultat {
        val behandlingsresultat = hentBehandlingsresultat(behandlingID)

        behandlingsresultat.nyVurderingBakgrunn = nyVurderingBakgrunn

        return behandlingsresultatRepository.save(behandlingsresultat)
    }

    private fun fjernMedlemAvFolketrygdenHvisDenFinnes(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftsperioder.clear()
        behandlingsresultatRepository.saveAndFlush(behandlingsresultat)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BehandlingsresultatService::class.java)

        //TODO: Ha generisk toppklasse?
        const val KAN_IKKE_FINNE_BEHANDLINGSRESULTAT: String = "Kan ikke finne behandlingsresultat for behandling: "

        private fun finnKorrektBehandlingsResultat(utfallregistreringunntak: Utfallregistreringunntak): Behandlingsresultattyper {
            if (utfallregistreringunntak == Utfallregistreringunntak.GODKJENT || utfallregistreringunntak == Utfallregistreringunntak.DELVIS_GODKJENT) {
                return (Behandlingsresultattyper.REGISTRERT_UNNTAK)
            } else if (utfallregistreringunntak == Utfallregistreringunntak.IKKE_GODKJENT) {
                return (Behandlingsresultattyper.FERDIGBEHANDLET)
            }
            return Behandlingsresultattyper.IKKE_FASTSATT
        }
    }
}
