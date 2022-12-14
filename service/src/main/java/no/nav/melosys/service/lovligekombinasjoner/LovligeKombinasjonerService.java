package no.nav.melosys.service.lovligekombinasjoner;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

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
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.stereotype.Service;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.HENLAGT;
import static no.nav.melosys.domain.kodeverk.Saksstatuser.HENLAGT_BORTFALT;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.AVSLUTTET;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;

@Service
public class LovligeKombinasjonerService {
    private final FagsakService fagsakService;
    private final BehandlingService behandlingService;
    private final BehandlingsresultatService behandlingsresultatService;

    public LovligeKombinasjonerService(FagsakService fagsakService, BehandlingService behandlingService, BehandlingsresultatService behandlingsresultatService) {
        this.fagsakService = fagsakService;
        this.behandlingService = behandlingService;
        this.behandlingsresultatService = behandlingsresultatService;
    }

    /**
     * Henter mulige sakstyper
     *
     * @param saksnummer Saksnummer til eksisterende fagsak. (default = null)
     *                   Brukt for å sjekke om eksisterende fagsak kan endre sakstype.
     */
    public Set<Sakstyper> hentMuligeSakstyper(@Nullable String saksnummer) {
        return saksnummer == null || fagsakService.hentFagsak(saksnummer).kanEndreTypeOgTema()
            ? LovligeSakskombinasjoner.ALLE_MULIGE_SAKSTYPER
            : Collections.emptySet();
    }

    /**
     * Henter mulige sakstemaer
     *
     * @param hovedpart  Hovedpart knyttet til fagsaken. (default = null)
     *                   Sender vi ikke inn hovedpart returnerer vi mulige sakstemaer for alle støttede hovedparter.
     * @param sakstype   Allerede valgt sakstype.
     * @param saksnummer Saksnummer til eksisterende fagsak. (default = null)
     *                   Brukt for å sjekke om eksisterende fagsak kan endre sakstema.
     */
    public Set<Sakstemaer> hentMuligeSakstemaer(@Nullable Aktoersroller hovedpart, Sakstyper sakstype, @Nullable String saksnummer) {
        if (hovedpart == null) {
            return combineSets(
                hentMuligeSakstemaer(Aktoersroller.BRUKER, sakstype, saksnummer),
                hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, sakstype, saksnummer)
            );
        }

        if (saksnummer != null && !fagsakService.hentFagsak(saksnummer).kanEndreTypeOgTema()) {
            return Collections.emptySet();
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

    /**
     * Henter mulige behandlingstemaer
     *
     * @param hovedpart           Hovedpart knyttet til fagsaken. (default = null)
     *                            Hvis null returnerer vi mulige behandlingstemaer for alle støttede hovedparter samt
     *                            mulige behandlingstemaer for SED. (MELOSYS-5223)
     * @param sakstype            Allerede valgt sakstype.
     * @param sakstema            Allerede valgt sakstema.
     * @param sistBehandlingstema Behandlingstema til forrige behandling i fagsaken. (default = null)
     *                            Brukt ved knytting til eksisterende sak.
     */
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

    private Set<Behandlingstema> hentMuligeBehandlingstemaerSED(Sakstyper sakstype, Sakstemaer sakstema) {
        if (sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.UNNTAK) {
            return Set.of(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND);
        }
        if (sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG) {
            return Set.of(BESLUTNING_LOVVALG_NORGE);
        }
        return Collections.emptySet();
    }

    public Set<Behandlingstyper> hentMuligeBehandlingstyper(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        @Nullable Behandlingstema behandlingstema,
        @Nullable Long sisteBehandlingsID
    ) {
        Behandling sisteBehandling = sisteBehandlingsID != null ? behandlingService.hentBehandling(sisteBehandlingsID) : null;
        Behandlingsresultat sisteBehandlingsresultat = sisteBehandlingsID != null ? behandlingsresultatService.hentBehandlingsresultatMedAnmodningsperioder(sisteBehandlingsID) : null;
        return hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, sisteBehandling, sisteBehandlingsresultat);
    }

    /**
     * Henter mulige behandlingstemaer
     *
     * @param hovedpart                Hovedpart knyttet til fagsaken.
     * @param sakstype                 Allerede valgt sakstype.
     * @param sakstema                 Allerede valgt sakstema.
     * @param behandlingstema          Allerede valgt behandlingstema.
     * @param sisteBehandling          Forrige behandling i fagsaken. (default = null)
     *                                 Brukt ved knytting til eksisterende sak.
     * @param sisteBehandlingsresultat Forrige behandlingsresultat i fagsaken. (default = null)
     *                                 Brukt ved knytting til eksisterende sak.
     */
    public Set<Behandlingstyper> hentMuligeBehandlingstyper(
        Aktoersroller hovedpart,
        Sakstyper sakstype,
        Sakstemaer sakstema,
        Behandlingstema behandlingstema,
        @Nullable Behandling sisteBehandling,
        @Nullable Behandlingsresultat sisteBehandlingsresultat
    ) {
        Behandlingstema sistBehandlingstema = null;
        Saksstatuser sistSaksstatus = null;

        if (sisteBehandling != null) {
            if (sisteBehandling.erAktiv()) {
                return sisteBehandlingsresultat != null && sisteBehandlingsresultat.erArtikkel16MedSendtAnmodningOmUnntak()
                    ? Set.of(NY_VURDERING) : Set.of();
            }
            sistBehandlingstema = sisteBehandling.getTema();
            sistSaksstatus = sisteBehandling.getFagsak().getStatus();
        }

        switch (hovedpart) {
            case BRUKER:
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

                if (sisteBehandling != null && sisteBehandling.erInaktiv()) {
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

    public Set<Behandlingsstatus> hentMuligeBehandlingStatuser() {
        return LovligeBehandlingstatus.ALLE_MULIGE_BEHANDLINGSTATUSER;
    }

    public void validerNyStatusMulig(Behandling behandling, Behandlingsstatus status) {
        if (!hentMuligeBehandlingStatuser().contains(status)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til status %s. Gyldige statuser for behandling %s er %s",
                status, behandling.getId(), hentMuligeBehandlingStatuser()));
        }
    }

    public void validerOpprettelseOgEndring(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        validerSakstema(hovedpart, sakstype, sakstema, null);
        validerBehandlingstema(hovedpart, sakstype, sakstema, behandlingstema, null);
        validerBehandlingstype(hovedpart, sakstype, sakstema, behandlingstema, behandlingstype, null, null);
    }

    public void validerBehandlingstemaOgBehandlingstypeForAndregangsbehandling(Fagsak fagsak, Behandling sistBehandling, Behandlingsresultat sistBehandlingsresultat, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        validerBehandlingstema(fagsak.getHovedpartRolle(), fagsak.getType(), fagsak.getTema(), behandlingstema, sistBehandling.getTema());
        validerBehandlingstype(fagsak.getHovedpartRolle(), fagsak.getType(), fagsak.getTema(), behandlingstema, behandlingstype, sistBehandling, sistBehandlingsresultat);
    }

    private void validerSakstema(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, String saksnummer) {
        if (!hentMuligeSakstemaer(hovedpart, sakstype, saksnummer).contains(sakstema)) {
            throw new FunksjonellException(sakstema + " er ikke et lovlig sakstema med de andre valgte verdiene");
        }
    }

    private void validerBehandlingstema(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstema sistBehandlingstema) {
        if (!hentMuligeBehandlingstemaer(hovedpart, sakstype, sakstema, sistBehandlingstema).contains(behandlingstema)) {
            throw new FunksjonellException(behandlingstema + " er ikke et lovlig behandlingstema med de andre valgte verdiene");
        }
    }

    private void validerBehandlingstype(Aktoersroller hovedpart, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype, Behandling sistBehandling, Behandlingsresultat sistBehandlingsresultat) {
        if (!hentMuligeBehandlingstyper(hovedpart, sakstype, sakstema, behandlingstema, sistBehandling, sistBehandlingsresultat).contains(behandlingstype)) {
            throw new FunksjonellException(behandlingstype + " er ikke en lovlig behandlingstype med de andre valgte verdiene");
        }
    }

    @SafeVarargs
    private static <T> Set<T> combineSets(Set<T>... t) {
        return Stream.of(t)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }
}
