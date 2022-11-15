package no.nav.melosys.service.lovligekombinasjoner;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.HENLAGT;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.HENLAGT_BORTFALT;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.AVSLUTTET;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

@Service
public class LovligeKombinasjonerService {

    public Set<Sakstyper> hentMuligeSakstyper() {
        return LovligeSakskombinasjoner.ALLE_MULIGE_SAKSTYPER;
    }

    public Set<Sakstemaer> hentMuligeSakstemaer(@Nullable Aktoersroller hovedpart, Sakstyper sakstype) {
        if (hovedpart == null) {
            return combineSets(
                hentMuligeSakstemaer(Aktoersroller.BRUKER, sakstype),
                hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, sakstype)
            );
        }

        return switch (hovedpart) {
            case BRUKER -> LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker.get(sakstype).stream()
                .map(SakstemaBehandlingsKombinasjon::sakstema)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            case VIRKSOMHET -> LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet.get(sakstype).stream()
                .map(SakstemaBehandlingsKombinasjon::sakstema)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            default -> Collections.emptySet();
        };
    }

    public Set<Behandlingstema> hentMuligeBehandlingstemaer(
        @Nullable Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        @Nullable Behandlingstema sistBehandlingstema
    ) {
        if (hovedpart == null) {
            return combineSets(
                hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, sakstype, sakstema, sistBehandlingstema),
                hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, sakstype, sakstema, sistBehandlingstema),
                hentMuligeBehandlingstemaerSED(sakstype, sakstema)
            );
        }

        switch (hovedpart) {
            case BRUKER:
                Set<Behandlingstema> behandlingstemaer = LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .filter(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.sakstema() == sakstema)
                    .flatMap(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.behandlingstemaBehandlingstyperKombinasjoner().stream())
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

                if (sistBehandlingstema != null && Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    BESLUTNING_LOVVALG_NORGE,
                    BESLUTNING_LOVVALG_ANNET_LAND,
                    ANMODNING_OM_UNNTAK_HOVEDREGEL).contains(sistBehandlingstema)) {
                    return Set.of(sistBehandlingstema);
                }
                return behandlingstemaer;
            case VIRKSOMHET:
                return LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet.get(sakstype).stream()
                    .filter(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.sakstema() == sakstema)
                    .flatMap(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.behandlingstemaBehandlingstyperKombinasjoner().stream())
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            default:
                return Collections.emptySet();
        }
    }

    public Set<Behandlingsstatus> hentMuligeBehandlingStatuser() {
        return LovligeBehandlingstatus.ALLE_MULIGE_BEHANDLINGSTATUSER;
    }

    public void validerNyStatusMulig(Behandling behandling, Behandlingsstatus status) {
        if (!hentMuligeBehandlingStatuser().contains(status)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til status %s. Gyldige statuser for behandling %s er %s",
                status, behandling.getId(), hentMuligeBehandlingStatuser()));
        }
    }

    public void validerBehandlingstemaOgBehandlingstypeForOpprettelse(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        validerBehandlingstema(hovedpart, sakstype, sakstema, behandlingstema, null);
        validerBehandlingstype(hovedpart, sakstype, sakstema, behandlingstema, behandlingstype, null, null);
    }

    public void validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(Fagsak fagsak, Behandling sistBehandling, Behandlingsresultat sistBehandlingsresultat, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        validerBehandlingstema(fagsak.getHovedpartRolle(), fagsak.getType(), fagsak.getTema(), behandlingstema, sistBehandling.getTema());
        validerBehandlingstype(fagsak.getHovedpartRolle(), fagsak.getType(), fagsak.getTema(), behandlingstema, behandlingstype, sistBehandling, sistBehandlingsresultat);
    }

    private void validerBehandlingstype(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype, Behandling sistBehandling, Behandlingsresultat sistBehandlingsresultat) {
        if (!hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, sistBehandling, sistBehandlingsresultat).contains(behandlingstype)) {
            throw new FunksjonellException(behandlingstype + " er ikke en lovlig behandlingstype med de andre valgte verdiene");
        }
    }

    private void validerBehandlingstema(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstema sistBehandlingstema) {
        if (!hentMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema).contains(behandlingstema)) {
            throw new FunksjonellException(behandlingstema + " er ikke et lovlig behandlingstema med de andre valgte verdiene");
        }
    }

    public Set<Behandlingstyper> hentMuligeBehandlingstyper(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        @Nullable Behandlingstema behandlingstema,
        @Nullable Behandling sisteBehandling,
        @Nullable Behandlingsresultat sisteBehandlingsresultat
    ) {
        Behandlingstema sistBehandlingstema = null;
        Saksstatuser sistSaksstatus = null;

        if (sisteBehandling != null) {
            sistBehandlingstema = sisteBehandling.getTema();
            sistSaksstatus = sisteBehandling.getFagsak().getStatus();
        }

        switch (hovedpart) {
            case BRUKER:
                if (sisteBehandling != null && sisteBehandling.erAktiv() &&
                    sisteBehandlingsresultat != null && sisteBehandlingsresultat.erArtikkel16MedSendtAnmodningOmUnntak()) {
                    return Set.of(NY_VURDERING);
                }

                Set<Behandlingstyper> behandlingstyper = LovligeSakskombinasjoner.muligeSaksKombinasjonerBruker.get(sakstype).stream()
                    .filter(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.sakstema() == sakstema)
                    .flatMap(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.behandlingstemaBehandlingstyperKombinasjoner().stream())
                    .filter(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().contains(behandlingstema))
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTyper().stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

                if (sistBehandlingstema != null && Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING,
                    REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE,
                    BESLUTNING_LOVVALG_NORGE,
                    BESLUTNING_LOVVALG_ANNET_LAND,
                    ANMODNING_OM_UNNTAK_HOVEDREGEL).contains(sistBehandlingstema)) {
                    behandlingstyper = new LinkedHashSet<>(List.of(NY_VURDERING, KLAGE, HENVENDELSE));
                }

                if (sisteBehandling != null && sisteBehandling.erAvsluttet()) {
                    behandlingstyper.remove(FØRSTEGANG);
                }

                if (sistSaksstatus != null && Set.of(HENLAGT, HENLAGT_BORTFALT, AVSLUTTET).contains(sistSaksstatus)) {
                    behandlingstyper = Set.of(HENVENDELSE);
                }

                return behandlingstyper;
            case VIRKSOMHET:
                return LovligeSakskombinasjoner.muligeSaksKombinasjonerVirksomhet.get(sakstype).stream()
                    .filter(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.sakstema() == sakstema)
                    .flatMap(sakstemaBehandlingsKombinasjon -> sakstemaBehandlingsKombinasjon.behandlingstemaBehandlingstyperKombinasjoner().stream())
                    .filter(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTemaer().contains(behandlingstema))
                    .flatMap(behandlingsKombinasjon -> behandlingsKombinasjon.behandlingsTyper().stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
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

            return hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, null, null);
        }
        return Collections.emptySet();
    }

    private Set<Behandlingstema> hentMuligeBehandlingstemaerSED(Sakstyper sakstype, Sakstemaer sakstema) {
        if (sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.UNNTAK) {
            return Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND);
        }
        if (sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG) {
            return Set.of(BESLUTNING_LOVVALG_NORGE);
        }
        return Collections.emptySet();
    }

    @SafeVarargs
    private static <T> Set<T> combineSets(Set<T>... t) {
        return Stream.of(t)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }
}
