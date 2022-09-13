package no.nav.melosys.service.lovligekombinasjoner;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.lovligekombinasjoner.LovligeBehandlingsKombinasjoner.BEHANDLINGSTYPER_FOR_VIRKSOMHET;

@Service
public class LovligeKombinasjonerService {
    private final BehandlingService behandlingService;

    public LovligeKombinasjonerService(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    public Set<Sakstyper> hentMuligeSakstyper() {
        return LovligeSaksKombinasjoner.ALLE_MULIGE_SAKSTYPER;
    }

    public Set<Sakstemaer> hentMuligeSakstemaer(Aktoersroller hovedpart, Sakstyper sakstype) {
        return switch (hovedpart) {
            case BRUKER -> LovligeSaksKombinasjoner.muligeSaksKombinasjonerBruker.get(sakstype).stream()
                .map(SakstemaBehandlingsKombinasjon::sakstema)
                .collect(Collectors.toSet());
            case VIRKSOMHET -> LovligeSaksKombinasjoner.ALLE_MULIGE_SAKSTEMAER;
            default -> Collections.emptySet();
        };
    }

    public Set<Behandlingstema> hentMuligeBehandlingstemaer(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        @Nullable Behandlingstema sistBehandlingstema
    ) {
        switch (hovedpart) {
            case BRUKER:
                Set<Behandlingstema> behandlingstemaer = LovligeSaksKombinasjoner.muligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .filter(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.sakstema() == sakstema)
                    .flatMap(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.behandlingstemaBehandlingstyperKombinasjoner().stream())
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().stream())
                    .collect(Collectors.toSet());

                if (sistBehandlingstema != null && Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    BESLUTNING_LOVVALG_NORGE,
                    BESLUTNING_LOVVALG_ANNET_LAND,
                    ANMODNING_OM_UNNTAK_HOVEDREGEL).contains(sistBehandlingstema)) {
                    return Set.of(sistBehandlingstema);
                }
                return behandlingstemaer;
            case VIRKSOMHET:
            default:
                return Collections.emptySet();
        }
    }

    public Set<Behandlingstyper> hentMuligeBehandlingstyper(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        @Nullable Behandlingstema behandlingstema,
        @Nullable Long sisteBehandlingsID
    ) {
        Behandlingstema sistBehandlingstema = null;
        Behandlingstyper sistBehandlingstype = null;
        Saksstatuser sistSaksstatus = null;

        if (sisteBehandlingsID != null) {
            Behandling sisteBehandling = behandlingService.hentBehandling(sisteBehandlingsID);
            sistBehandlingstema = sisteBehandling.getTema();
            sistBehandlingstype = sisteBehandling.getType();
            sistSaksstatus = sisteBehandling.getFagsak().getStatus();
        }

        switch (hovedpart) {
            case BRUKER:
                Set<Behandlingstyper> behandlingstyper = LovligeSaksKombinasjoner.muligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .filter(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.sakstema() == sakstema)
                    .flatMap(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.behandlingstemaBehandlingstyperKombinasjoner().stream())
                    .filter(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().contains(behandlingstema))
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTyper().stream())
                    .collect(Collectors.toSet());

                if (sistBehandlingstema != null && Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    BESLUTNING_LOVVALG_NORGE,
                    BESLUTNING_LOVVALG_ANNET_LAND,
                    ANMODNING_OM_UNNTAK_HOVEDREGEL).contains(sistBehandlingstema)) {
                    behandlingstyper = Set.of(NY_VURDERING, KLAGE, HENVENDELSE);
                }

                if (sistBehandlingstype == FØRSTEGANG &&
                    sistBehandlingstema != null && Set.of(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG).contains(sistBehandlingstema)
                ) {
                    behandlingstyper.add(ENDRET_PERIODE);
                }

                if (sistSaksstatus != null && Set.of(HENLAGT, HENLAGT_BORTFALT, AVSLUTTET).contains(sistSaksstatus)) {
                    return Set.of(HENVENDELSE);
                }

                return behandlingstyper;
            case VIRKSOMHET:
                return BEHANDLINGSTYPER_FOR_VIRKSOMHET;
            default:
                return Collections.emptySet();
        }
    }

    public void validerOmNyttTemaKanEndresTil(Behandling behandling, Behandlingstema tema) {
        var muligeBehandlingstema = behandlingstemaSomKanEndresTil(behandling);
        if (!muligeBehandlingstema.contains(tema)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til tema %s. Gyldige temaer for behandling %s er %s",
                tema, behandling.getId(), muligeBehandlingstema));
        }
    }

    public Set<Behandlingstema> behandlingstemaSomKanEndresTil(Behandling behandling) {
        if (behandling == null || behandling.kanIkkeEndres()) {
            return Collections.emptySet();
        }

        if (behandling.getFagsak() != null && behandling.getFagsak().getType() != null && behandling.getFagsak().getTema() != null) {
            Sakstyper sakstype = behandling.getFagsak().getType();
            Aktoersroller hovedpart = behandling.getFagsak().getHovedpartRolle();
            Sakstemaer sakstema = behandling.getFagsak().getTema();

            return hentMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, null);
        }
        return Collections.emptySet();
    }

    public void validerOmNyTypeKanEndresTil(Behandling behandling, Behandlingstyper type) {
        var muligeBehandlingstyper = behandlinstyperSomKanEndresTil(behandling);
        if (!muligeBehandlingstyper.contains(type)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til type %s. Gyldige typer for behandling %s er %s",
                type, behandling.getId(), muligeBehandlingstyper));
        }
    }

    public Set<Behandlingstyper> behandlinstyperSomKanEndresTil(Behandling behandling) {
        if (behandling == null || behandling.kanIkkeEndres()) {
            return Collections.emptySet();
        }
        if (behandling.getFagsak() != null && behandling.getFagsak().getType() != null && behandling.getFagsak().getTema() != null) {
            Sakstyper sakstype = behandling.getFagsak().getType();
            Sakstemaer sakstema = behandling.getFagsak().getTema();
            Aktoersroller hovedpart = behandling.getFagsak().getHovedpartRolle();
            Behandlingstema behandlingstema = behandling.getTema();

            return hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, null);
        }
        return Collections.emptySet();
    }
}
