package no.nav.melosys.service.kontroll.feature.ufm

import io.micrometer.core.instrument.Metrics
import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Kontrollresultat
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser
import no.nav.melosys.metrics.MetrikkerNavn
import no.nav.melosys.repository.KontrollresultatRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.kontroll.feature.ufm.data.UfmKontrollData
import no.nav.melosys.service.kontroll.feature.ufm.kontroll.UfmKontrollsett
import no.nav.melosys.service.kontroll.regler.PeriodeRegler
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

private val log = KotlinLogging.logger { }

@Service
class UfmKontrollService(
    private val kontrollresultatRepository: KontrollresultatRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val mottatteOpplysningerService: MottatteOpplysningerService,
    private val behandlingService: BehandlingService,
    private val persondataFasade: PersondataFasade,
) {

    @PostConstruct
    private fun registerKontrollBegrunnelser() {
        Kontroll_begrunnelser.values().forEach { begrunnelse ->
            Metrics.counter(
                MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF,
                MetrikkerNavn.TAG_BEGRUNNELSE,
                begrunnelse.kode
            )
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
        val personhistorikkDokumenter = saksopplysninger
            .filter { it.dokument is PersonhistorikkDokument }
            .map { it.dokument as PersonhistorikkDokument }

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

    private fun utførKontroller(
        kontrollData: UfmKontrollData,
        sedType: SedType
    ): List<Kontroll_begrunnelser> = UfmKontrollsett.hentRegelsettForSedType(sedType)
        .mapNotNull { it(kontrollData) }
        .onEach { registrerMetrikk(it) }

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
            }.toList()

        kontrollresultatRepository.saveAll(kontrollresultater)
    }

    private fun lagKontrollresultat(behandlingsresultat: Behandlingsresultat, kontrollBegrunnelse: Kontroll_begrunnelser): Kontrollresultat {
        val kontrollresultat = Kontrollresultat()
        kontrollresultat.begrunnelse = kontrollBegrunnelse
        kontrollresultat.behandlingsresultat = behandlingsresultat

        return kontrollresultat
    }

    private fun registrerMetrikk(unntakPeriodeBegrunnelse: Kontroll_begrunnelser) {
        Metrics.counter(
            MetrikkerNavn.UNNTAKSPERIODE_KONTROLL_TREFF,
            MetrikkerNavn.TAG_BEGRUNNELSE, unntakPeriodeBegrunnelse.kode
        ).increment()
    }

    private fun harFeilIPeriode(sedDokument: SedDokument): Boolean = PeriodeRegler.feilIPeriode(
        sedDokument.lovvalgsperiode.fom,
        sedDokument.lovvalgsperiode.tom
    )
}
