package no.nav.melosys.service.behandling.jobb

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
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
            log.info { "Behandling $behandlingID er ikke gyldig for automatisk avslutting" }
            return
        }

        if (!toMndHarPassertSidenSaksbehandling(behandling, behandlingsresultat)) {
            log.info { "To måneder har ikke passert for behandling $behandlingID, avslutter ikke" }
            return
        }

        avsluttBehandling(behandling, behandlingsresultat)
    }

    private fun erGyldigForAutomatiskAvslutting(behandling: Behandling): Boolean =
        erFagsakStatusGyldigForAutomatiskAvslutting(behandling) && !finnesNyereRelevantLovvalgBehandling(behandling)

    /**
     * Kun saker med status OPPRETTET skal automatisk avsluttes av jobben.
     * Hvis saksstatus er noe annet enn OPPRETTET, betyr det at saken allerede er håndtert.
     *
     * Dette forhindrer f.eks. at avviste MEDL-perioder blir gjort gyldige igjen.
     */
    private fun erFagsakStatusGyldigForAutomatiskAvslutting(behandling: Behandling): Boolean =
        if (behandling.fagsak.status == Saksstatuser.OPPRETTET) {
            true
        } else {
            log.info { "Avslutter ikke behandling ${behandling.id} med saksstatus ${behandling.fagsak.status} (krever OPPRETTET)" }
            false
        }

    /**
     * Returnerer true hvis det finnes en nyere behandling som er relevant for lovvalg og derfor
     * skal hindre automatisk avslutting av behandlingen som sjekkes.
     *
     * En nyere behandling anses som relevant hvis:
     * - Den er av en relevant type,
     * - Den er inaktiv (status = AVSLUTTET eller MIDLERTIDIG_LOVVALGSBESLUTNING)
     * - Den har resultert i et vedtak eller unntaksregistrering (utfallRegistreringUnntak)
     *
     * Ignorerer f.eks. HENVENDELSE eller andre typer som ikke endrer lovvalgsperioden.
     */
    private fun finnesNyereRelevantLovvalgBehandling(behandling: Behandling): Boolean =
        behandling.fagsak.behandlinger.filter { it.id != behandling.id }
            .filter { it.erInaktiv() }
            .filter { it.registrertDato.isAfter(behandling.registrertDato) }
            .filter { it.type in setOf(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING) }
            .any { kandidat ->
                val resultat = behandlingsresultatService.hentBehandlingsresultat(kandidat.id)
                resultat.harVedtak() || resultat.utfallRegistreringUnntak != null
            }

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
