package no.nav.melosys.service.kontroll.feature.ufm

import io.micrometer.core.instrument.Metrics
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Kontrollresultat
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.metrics.MetrikkerNavn
import no.nav.melosys.repository.KontrollresultatRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.kontroll.feature.ufm.kontroll.UfmKontrollsett
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData
import no.nav.melosys.service.kontroll.regler.PeriodeRegler
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.function.Function


@Service
@Primary
class UfmKontrollService(
    private val kontrollresultatRepository: KontrollresultatRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UfmKontrollService::class.java)

        init {
            Arrays.stream(Kontroll_begrunnelser.values())
                .forEach { b: Kontroll_begrunnelser ->
                    Metrics.counter(
                        MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF,
                        MetrikkerNavn.TAG_BEGRUNNELSE,
                        b.kode
                    )
                }
        }
    }

    @Transactional
    fun utførKontrollerOgRegistrerFeil(behandlingId: Long) {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        val registrerteTreff = utførKontroller(behandling)

        log.info("Treff ved validering av periode for behandling {}: {}", behandlingId, registrerteTreff)
        lagreKontrollresultater(behandlingId, registrerteTreff)
    }

    fun utførKontroller(behandling: Behandling): List<Kontroll_begrunnelser> {
        val sedDokument = behandling.hentSedDokument()

        if (harFeilIPeriode(sedDokument)) {
            return listOf(Kontroll_begrunnelser.FEIL_I_PERIODEN)
        }
        val sedType = sedDokument.sedType
        val saksopplysninger = behandling.saksopplysninger
        val personhistorikkDokumenter =
            saksopplysninger.stream()
                .filter { a: Saksopplysning -> a.dokument is PersonhistorikkDokument }
                .map { a: Saksopplysning -> a.dokument as PersonhistorikkDokument }
                .toList()
        val ufmKontrollData = lagUfmKontrollData(behandling, personhistorikkDokumenter, sedDokument)

        return utførKontroller(ufmKontrollData, sedType)
    }

    private fun lagUfmKontrollData(
        behandling: Behandling,
        personhistorikkDokumenter: List<PersonhistorikkDokument?>,
        sedDokument: SedDokument
    ): UfmKontrollData {
        val persondata = persondataFasade.hentPerson(behandling.fagsak.hentBrukersAktørID())
        val persondataMedHistorikk = persondataFasade.hentPersonMedHistorikk(behandling.fagsak.hentBrukersAktørID())
        val medlemskapDokument = behandling.hentMedlemskapDokument()
        val inntektDokument = behandling.hentInntektDokument()
        val utbetalingDokument = behandling.finnUtbetalingDokument().orElse(null)
        val optionalMottatteOpplysningerData = mottatteOpplysningerService.finnMottatteOpplysningerData(behandling.id)
        return UfmKontrollData(
            sedDokument,
            persondata,
            medlemskapDokument,
            inntektDokument,
            utbetalingDokument,
            optionalMottatteOpplysningerData,
            personhistorikkDokumenter,
            Optional.of(persondataMedHistorikk)
        )
    }

    private fun utførKontroller(kontrollData: UfmKontrollData, sedType: SedType): List<Kontroll_begrunnelser> {
        return UfmKontrollsett.hentRegelsettForSedType(sedType).stream()
            .map { f: Function<UfmKontrollData?, Kontroll_begrunnelser> ->
                f.apply(
                    kontrollData
                )
            }
            .filter { obj: Kontroll_begrunnelser? -> Objects.nonNull(obj) }
            .peek { unntak_periode_begrunnelse: Kontroll_begrunnelser ->
                this.registrerMetrikk(
                    unntak_periode_begrunnelse
                )
            } //NOSONAR
            .toList()
    }

    private fun lagreKontrollresultater(behandlingID: Long, kontrollBegrunnelser: List<Kontroll_begrunnelser>) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        kontrollresultatRepository.deleteByBehandlingsresultat(behandlingsresultat)
        kontrollresultatRepository.flush()

        val kontrollresultater = kontrollBegrunnelser.stream()
            .map { kontrollBegrunnelse: Kontroll_begrunnelser ->
                lagKontrollresultat(
                    behandlingsresultat,
                    kontrollBegrunnelse
                )
            }
            .toList()

        kontrollresultatRepository.saveAll(kontrollresultater)
    }

    private fun lagKontrollresultat(behandlingsresultat: Behandlingsresultat, kontrollBegrunnelse: Kontroll_begrunnelser): Kontrollresultat {
        val kontrollresultat = Kontrollresultat()
        kontrollresultat.begrunnelse = kontrollBegrunnelse
        kontrollresultat.behandlingsresultat = behandlingsresultat

        return kontrollresultat
    }

    private fun registrerMetrikk(unntak_periode_begrunnelse: Kontroll_begrunnelser) {
        Metrics.counter(MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF, MetrikkerNavn.TAG_BEGRUNNELSE, unntak_periode_begrunnelse.kode).increment()
    }

    private fun harFeilIPeriode(sedDokument: SedDokument): Boolean {
        return PeriodeRegler.feilIPeriode(
            sedDokument.lovvalgsperiode.fom,
            sedDokument.lovvalgsperiode.tom
        )
    }
}
