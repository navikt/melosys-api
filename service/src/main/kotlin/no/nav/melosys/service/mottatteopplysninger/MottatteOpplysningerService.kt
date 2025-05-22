package no.nav.melosys.service.mottatteopplysninger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.mottatteopplysninger.*
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.MottatteOpplysningerRepository
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.UtledMottaksdato
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.slf4j.MarkerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger { }

@Service
class MottatteOpplysningerService(
    private val mottatteOpplysningerRepository: MottatteOpplysningerRepository,
    private val behandlingService: BehandlingService,
    private val utledMottaksdato: UtledMottaksdato,
    private val saksbehandlingRegler: SaksbehandlingRegler
) {
    private val teamLogsMarker = MarkerFactory.getMarker("TEAM_LOGS")

    @Transactional(readOnly = true)
    fun hentMottatteOpplysninger(behandlingID: Long): MottatteOpplysninger =
        finnMottatteOpplysninger(behandlingID).orElseThrow { IkkeFunnetException("Finner ikke mottatteOpplysninger for behandling $behandlingID") }

    @Transactional
    fun hentEllerOpprettMottatteOpplysninger(
        behandlingID: Long,
        behandlingKanRedigeresAvSaksbehandler: Boolean
    ): MottatteOpplysninger? =
        finnMottatteOpplysninger(behandlingID).orElseGet {
            val behandling = behandlingService.hentBehandling(behandlingID)
            if (saksbehandlingRegler.harIngenFlyt(behandling) || !behandlingKanRedigeresAvSaksbehandler) {
                throw IkkeFunnetException("Finner ikke mottatteOpplysninger for behandling $behandlingID")
            } else {
                opprettSøknadEllerAnmodningEllerAttest(behandling, Periode(), Soeknadsland())
            }
        }

    @Transactional(readOnly = true)
    fun finnMottatteOpplysningerData(behandlingID: Long): Optional<MottatteOpplysningerData> {
        val mottatteOpplysninger = finnMottatteOpplysninger(behandlingID).orElse(null)
        return Optional.ofNullable(mottatteOpplysninger?.mottatteOpplysningerData)
    }

    fun opprettSedGrunnlag(behandlingID: Long, sedGrunnlag: SedGrunnlag): MottatteOpplysninger =
        opprettMottatteOpplysninger(
            behandlingID = behandlingID,
            mottatteOpplysningerData = sedGrunnlag,
            type = Mottatteopplysningertyper.SED,
            versjon = VERSJON_SED_GRUNNLAG
        )

    fun opprettSøknadEllerAnmodningEllerAttest(prosessinstans: Prosessinstans): MottatteOpplysninger? =
        opprettSøknadEllerAnmodningEllerAttest(
            prosessinstans.behandling,
            prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode::class.java, Periode()),
            prosessinstans.getData(ProsessDataKey.SØKNADSLAND, Soeknadsland::class.java, Soeknadsland())
        )


    fun opprettSøknadEllerAnmodningEllerAttest(
        behandling: Behandling,
        periode: Periode?,
        soeknadsland: Soeknadsland?
    ): MottatteOpplysninger? =
        if (saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling))
            opprettAnmodningEllerAttest(behandling, periode, soeknadsland)
        else opprettSøknad(behandling, periode, soeknadsland)


    fun opprettSøknadUtsendteArbeidstakereEøs(
        behandlingID: Long, orginalData: String?, soeknad: Soeknad, eksternReferanseID: String?
    ): MottatteOpplysninger = opprettMottatteOpplysninger(
        behandlingID,
        orginalData,
        soeknad,
        Mottatteopplysningertyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS,
        VERSJON_SOEKNAD_GRUNNLAG,
        eksternReferanseID
    )

    private fun opprettAnmodningEllerAttest(
        behandling: Behandling, periode: Periode?, soeknadsland: Soeknadsland?
    ): MottatteOpplysninger {
        val mottatteOpplysningerData = AnmodningEllerAttest().apply {
            this.periode = periode
            this.soeknadsland = soeknadsland
        }

        val mottatteOpplysninger = opprettMottatteOpplysninger(
            behandlingID = behandling.id,
            mottatteOpplysningerData = mottatteOpplysningerData,
            type = Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST,
            versjon = VERSJON_ANMODNING_ATTEST_GRUNNLAG
        )

        log.info("Opprettet anmodning/attest for behandling {}.", behandling.id)
        return mottatteOpplysninger
    }

    @Transactional
    fun opprettMottatteopplysningerForAarsavregning(behandlingID: Long) {
        opprettMottatteOpplysninger(
            behandlingID = behandlingID,
            mottatteOpplysningerData = SøknadNorgeEllerUtenforEØS(),
            type = Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS,
            versjon = VERSJON_SOEKNAD_GRUNNLAG
        )
    }

    private fun opprettSøknad(
        behandling: Behandling, periode: Periode?, soeknadsland: Soeknadsland?
    ): MottatteOpplysninger? {
        val behandlingID = behandling.id
        if (saksbehandlingRegler.harIngenFlyt(behandling)) {
            log.info { "Søknad trengs ikke og opprettes ikke for behandling $behandlingID med tema ${behandling.tema}" }
            return null
        }

        val type: Mottatteopplysningertyper?
        val data: MottatteOpplysningerData?

        if (saksbehandlingRegler.harIkkeYrkesaktivFlyt(behandling)) {
            type = Mottatteopplysningertyper.SØKNAD_IKKE_YRKESAKTIV
            data = SøknadIkkeYrkesaktiv()
        } else {
            when (behandling.fagsak.type) {
                Sakstyper.EU_EOS -> {
                    type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
                    data = Soeknad()
                }
                Sakstyper.FTRL, Sakstyper.TRYGDEAVTALE -> {
                    type = Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS
                    data = SøknadNorgeEllerUtenforEØS()
                }
                else -> throw FunksjonellException("Klarer ikke opprette søknad for behandling $behandlingID")
            }
        }

        data.apply {
            this.periode = periode
            this.soeknadsland = soeknadsland
        }

        val mottatteOpplysninger = opprettMottatteOpplysninger(
            behandlingID = behandlingID,
            mottatteOpplysningerData = data,
            type = type,
            versjon = VERSJON_SOEKNAD_GRUNNLAG
        )

        log.info("Opprettet søknad for behandling {}.", behandlingID)
        return mottatteOpplysninger
    }

    private fun opprettMottatteOpplysninger(
        behandlingID: Long,
        originalData: String? = null,
        mottatteOpplysningerData: MottatteOpplysningerData,
        type: Mottatteopplysningertyper,
        versjon: String,
        eksternReferanseID: String? = null
    ): MottatteOpplysninger {
        if (eksternReferanseID != null && harMottattSøknadMedEksternReferanseID(eksternReferanseID)) {
            val mapper = ObjectMapper().registerKotlinModule()
            val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mottatteOpplysningerData)
            log.info(teamLogsMarker, "Feil i mottatteopplysninger for mottatteOpplysningerData: $json") //TODO fjern etter debugging

            throw FunksjonellException("Det finnes allerede mottatteOpplysninger med eksterReferanseID $eksternReferanseID")
        }

        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID).apply {
            if (mottatteOpplysninger != null) {
                throw FunksjonellException("Finnes allerede mottatteOpplysninger for behandling $id")
            }
        }

        val nå = Instant.now()
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.behandling = behandling
            this.registrertDato = nå
            this.endretDato = nå
            this.type = type
            this.versjon = versjon
            this.mottaksdato = utledMottaksdato.getMottaksdato(behandling)
            this.originalData = originalData
            this.eksternReferanseID = eksternReferanseID
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }

        behandling.mottatteOpplysninger = mottatteOpplysninger
        return mottatteOpplysningerRepository.save(mottatteOpplysninger)
    }

    @Transactional
    fun oppdaterMottatteOpplysninger(
        behandlingID: Long, mottatteOpplysningerDataJson: JsonNode
    ): MottatteOpplysninger? {
        val mottatteOpplysninger = hentMottatteOpplysninger(behandlingID)
        mottatteOpplysninger.jsonData = mottatteOpplysningerDataJson.toPrettyString()
        return mottatteOpplysningerRepository.saveAndFlush(mottatteOpplysninger)
    }

    @Transactional
    fun oppdaterMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger) {
        MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger)
        mottatteOpplysningerRepository.saveAndFlush(mottatteOpplysninger)
    }

    @Transactional
    fun oppdaterMottatteOpplysningerPeriodeOgLand(
        behandlingID: Long, periode: Periode?, soeknadsland: Soeknadsland?
    ) {
        val mottatteOpplysninger = hentMottatteOpplysninger(behandlingID).apply {
            mottatteOpplysningerData.periode = periode
            mottatteOpplysningerData.soeknadsland = soeknadsland
        }
        MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger)
        mottatteOpplysningerRepository.saveAndFlush(mottatteOpplysninger)
    }

    @Transactional
    fun slettOpplysninger(behandlingID: Long) {
        val behandling = behandlingService.hentBehandling(behandlingID)
        behandling.mottatteOpplysninger = null
        mottatteOpplysningerRepository.deleteByBehandling_Id(behandlingID)
    }

    fun finnMottatteOpplysninger(behandlingID: Long): Optional<MottatteOpplysninger> =
        mottatteOpplysningerRepository.findByBehandling_Id(behandlingID)

    fun harMottattSøknadMedEksternReferanseID(eksternReferanseID: String): Boolean =
        mottatteOpplysningerRepository.findByEksternReferanseID(eksternReferanseID).isNotEmpty()

    companion object {
        private const val VERSJON_SED_GRUNNLAG = "1"
        private const val VERSJON_SOEKNAD_GRUNNLAG = "1.2"
        private const val VERSJON_ANMODNING_ATTEST_GRUNNLAG = "1"
    }
}
