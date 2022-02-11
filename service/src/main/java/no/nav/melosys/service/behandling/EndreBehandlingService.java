package no.nav.melosys.service.behandling;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ENDRET_PERIODE;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;

@Service
public class EndreBehandlingService {

    private final Set<Behandlingsstatus> MULIGE_STATUSER = Set.of(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING);
    private final Set<Behandlingstema> TEMAER_SOM_KAN_AVSLUTTES = Set.of(ØVRIGE_SED_MED, ØVRIGE_SED_UFM, TRYGDETID, IKKE_YRKESAKTIV);

    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public EndreBehandlingService(BehandlingService behandlingService,
                                  BehandlingsresultatService behandlingsresultatService,
                                  OppgaveService oppgaveService,
                                  BehandlingsgrunnlagService behandlingsgrunnlagService,
                                  ApplicationEventPublisher applicationEventPublisher) {
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public void endreBehandling(long behandlingID, Sakstyper ignoredSakstype, Behandlingstyper type, Behandlingstema tema, Behandlingsstatus status, LocalDate behandlingsfrist) {
        // TODO: Endre sakstype (MELOSYS-4899 for EØS <-> trygdeavtale)
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        if (!behandling.erAktiv()) {
            throw new FunksjonellException("Behandlingen må være aktiv for å kunne endres");
        }

        if (status != null) {
            endreStatus(behandlingID, status);
        }

        boolean behandlingErEndret = false;
        if (kanEndreType(behandling, type)) {
            behandling.setType(type);
            behandlingErEndret = true;
        }
        if (kanEndreFrist(behandling, behandlingsfrist)) {
            behandling.setBehandlingsfrist(behandlingsfrist);
            behandlingErEndret = true;
        }
        if (kanEndreTema(behandling, tema)) {
            behandling.setTema(tema);
            behandlingErEndret = true;
        }
        if (behandlingErEndret) {
            behandlingService.lagre(behandling);
            tilbakestillBehandlingsgrunnlag(behandling);

            applicationEventPublisher.publishEvent(new BehandlingEndretAvSaksbehandlerEvent(behandlingID, behandling));
        }
    }

    public void endreStatus(long behandlingID, Behandlingsstatus status) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        if (!hentMuligeStatuser(behandling).contains(status)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til status %s. Gyldige statuser for behandling %s er %s", status, behandlingID, hentMuligeStatuser(behandling)));
        }
        behandlingService.oppdaterStatus(behandling, status);
    }

    public Collection<Behandlingsstatus> hentMuligeStatuser(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        return hentMuligeStatuser(behandling);
    }

    private Collection<Behandlingsstatus> hentMuligeStatuser(Behandling behandling) {
        if (behandling.erInaktiv()) return Collections.emptyList();

        Set<Behandlingsstatus> muligeStatuser = new HashSet<>(MULIGE_STATUSER);

        if (TEMAER_SOM_KAN_AVSLUTTES.contains(behandling.getTema())) {
            muligeStatuser.add(Behandlingsstatus.AVSLUTTET);
        }

        return muligeStatuser;
    }

    public Collection<Behandlingstyper> hentMuligeTyper(long behandlingID) {
        var behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);

        if (behandling.erInaktiv()) return Collections.emptyList();

        return Set.of(behandling.getType(), ENDRET_PERIODE, NY_VURDERING);
    }

    public List<Behandlingstema> hentMuligeBehandlingstema(long behandlingID) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        return hentMuligeBehandlingstema(behandling);
    }

    private List<Behandlingstema> hentMuligeBehandlingstema(Behandling behandling) {
        boolean kanOppdatereBehandlingstema = kanOppdatereBehandlingstema(behandling);
        if (kanOppdatereBehandlingstema && erGyldigBehandlingAvSøknad(behandling.getTema())) {
            return BEHANDLINGSTEMA_SØKNAD;
        } else if (kanOppdatereBehandlingstema && erBehandlingAvSedForespørsler(behandling.getTema())) {
            return BEHANDLINGSTEMA_SED_FORESPØRSEL;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @deprecated Erstattes av endreBestilling
     */
    @Deprecated
    @Transactional
    public void endreBehandlingstemaTilBehandling(long behandlingID, Behandlingstema nyttTema) {
        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        if (hentMuligeBehandlingstema(behandling).contains(nyttTema)) {
            behandling.setTema(nyttTema);
            behandlingService.lagre(behandling);
            behandlingsresultatService.tømBehandlingsresultat(behandlingID);
            oppdaterOppgave(behandling);
            if (nyttTema != ARBEID_FLERE_LAND) {
                oppdaterBehandlingsgrunnlag(behandling.getBehandlingsgrunnlag());
            }
        } else {
            throw new FunksjonellException("Ikke mulig å endre behandlingstema");
        }
    }

    private void oppdaterOppgave(Behandling behandling) {
        Oppgave oppgave = oppgaveService.finnÅpenOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())
            .orElseThrow(() -> new FunksjonellException("Finner ikke tilhørende oppgave"));

        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType()).build();

        oppgaveService.oppdaterOppgave(oppgave.getOppgaveId(),
            OppgaveOppdatering.builder()
                .behandlingstema(behandlingsOppgaveForType.getBehandlingstema())
                .behandlingstype(behandlingsOppgaveForType.getBehandlingstype())
                .tema(behandlingsOppgaveForType.getTema())
                .build());
    }

    private boolean kanEndreType(Behandling behandling, Behandlingstyper type) {
        return type != null && type != behandling.getType();
    }

    private boolean kanEndreTema(Behandling behandling, Behandlingstema tema) {
        return tema != null
            && tema != behandling.getTema()
            && behandlingsresultatService.hentBehandlingsresultat(behandling.getId()).erIkkeArtikkel16MedSendtAnmodningOmUnntak()
            && hentMuligeBehandlingstema(behandling).contains(tema);
    }

    private boolean kanEndreFrist(Behandling behandling, LocalDate behandlingsfrist) {
        return behandlingsfrist != null && !behandlingsfrist.equals(behandling.getBehandlingsfrist());
    }

    private boolean kanOppdatereBehandlingstema(Behandling behandling) {
        return behandling.erAktiv() && behandlingsresultatService.hentBehandlingsresultat(
            behandling.getId()).erIkkeArtikkel16MedSendtAnmodningOmUnntak();
    }

    private void tilbakestillBehandlingsgrunnlag(Behandling behandling) {
        behandlingsresultatService.tømBehandlingsresultat(behandling.getId());
        if (behandling.getTema() != ARBEID_FLERE_LAND) {
            behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand = false;
            behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandling.getBehandlingsgrunnlag());
        }
    }

    private void oppdaterBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        behandlingsgrunnlag.getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand = false;
        behandlingsgrunnlagService.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);
    }
}
