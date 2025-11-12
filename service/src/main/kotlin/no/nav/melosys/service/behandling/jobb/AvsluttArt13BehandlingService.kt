package no.nav.melosys.service.behandling.jobb

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.kontroll.regler.PeriodeRegler.datoEldreEnn2Mnd
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class AvsluttArt13BehandlingService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val medlPeriodeService: MedlPeriodeService,
    private val lovvalgsperiodeService: LovvalgsperiodeService
) {

    @Transactional
    fun avsluttBehandlingHvisToMndPassert(behandlingID: Long) {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingID)
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        if (!erGyldigForAutomatiskAvslutting(behandling)) {
            log.info { "Behandling $behandlingID har ugyldig saksstatus ${behandling.fagsak.status}, avslutter ikke" }
            return
        }

        if (!toMndHarPassertSidenSaksbehandling(behandling, behandlingsresultat)) {
            log.info { "To måneder har ikke passert for behandling $behandlingID, avslutter ikke" }
            return
        }

        avsluttBehandling(behandling, behandlingsresultat)
    }

    /**
     * Validerer at saken er gyldig for automatisk avslutting.
     * Dette forhindrer f.eks. at avviste MEDL-perioder blir gjort gyldige igjen.
     *
     * Kun saker med status OPPRETTET skal automatisk avsluttes av cron-jobben.
     * Hvis saksstatus er noe annet enn OPPRETTET, betyr det at saken allerede er håndtert.
     */
    private fun erGyldigForAutomatiskAvslutting(behandling: Behandling): Boolean =
        behandling.fagsak.status == Saksstatuser.OPPRETTET

    private fun avsluttBehandling(behandling: Behandling, behandlingsresultat: Behandlingsresultat) {
        log.info { "To måneder har passert siden saksbehandling for behandling ${behandling.id}. Avslutter behandlingen" }
        val lovvalgsperiode = hentLovvalgsperiode(behandlingsresultat)

        validerLovvalgsperiodeKanAvsluttes(behandling.id, lovvalgsperiode)

        fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, behandling, Saksstatuser.LOVVALG_AVKLART)
        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode)

        log.info { "Behandling ${behandling.id} avsluttet og satt til endelig i Medl." }
    }

    private fun validerLovvalgsperiodeKanAvsluttes(behandlingID: Long, lovvalgsperiode: Lovvalgsperiode) {
        if (!lovvalgsperiode.erArtikkel13()) {
            throw FunksjonellException(
                "Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse ${lovvalgsperiode.bestemmelse}"
            )
        }

        if (lovvalgsperiode.medlPeriodeID == null) {
            throw FunksjonellException(
                "Behandling $behandlingID har en lovvalgsperiode som ikke er registrert i Medl. " +
                        "Kan ikke avslutte art13 behandling automatisk"
            )
        }
    }

    private fun toMndHarPassertSidenSaksbehandling(behandling: Behandling, behandlingsresultat: Behandlingsresultat): Boolean {
        log.debug { "Sjekker om 2 måneder har passert siden saksbehandling for behandling ${behandling.id}" }

        val erUtpekingUtenVedtak = erUtpekingUtenVedtak(behandlingsresultat)
        val skalHaVedtak = behandling.kanResultereIVedtak() && !erUtpekingUtenVedtak

        log.debug {
            "Behandling ${behandling.id}: kanResultereIVedtak=${behandling.kanResultereIVedtak()}, " +
            "erUtpekingUtenVedtak=$erUtpekingUtenVedtak"
        }

        if (skalHaVedtak) {
            require(behandlingsresultat.harVedtak()) {
                "Behandling ${behandling.id} skal ha vedtak men mangler vedtak. Status kan ikke settes til AVSLUTTET"
            }

            return datoEldreEnn2Mnd(behandlingsresultat.vedtakMetadata!!.vedtaksdato)
        }

        return datoEldreEnn2Mnd(behandlingsresultat.endretDato)
    }

    private fun hentLovvalgsperiode(behandlingsresultat: Behandlingsresultat): Lovvalgsperiode =
        if (erUtpekingUtenVedtak(behandlingsresultat)) {
            opprettLovvalgsperiode(behandlingsresultat.id!!, behandlingsresultat.hentValidertUtpekingsperiode())
        } else {
            behandlingsresultat.hentLovvalgsperiode()
        }

    private fun erUtpekingUtenVedtak(behandlingsresultat: Behandlingsresultat): Boolean =
        behandlingsresultat.utpekingsperioder.isNotEmpty() && !behandlingsresultat.harVedtak()

    private fun opprettLovvalgsperiode(behandlingID: Long, utpekingsperiode: Utpekingsperiode): Lovvalgsperiode =
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, setOf(Lovvalgsperiode.av(utpekingsperiode)))
            .single()
}
