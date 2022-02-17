package no.nav.melosys.service.behandling;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.Behandling.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ENDRET_PERIODE;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;

public class MuligeManuelleBehandlingsendringer {

    private static final Set<Behandlingsstatus> MULIGE_STATUSER = Set.of(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING);
    private static final Set<Behandlingstema> TEMAER_SOM_KAN_AVSLUTTES = Set.of(ØVRIGE_SED_MED, ØVRIGE_SED_UFM, TRYGDETID, IKKE_YRKESAKTIV);
    private static final Set<Behandlingstema> TEMAER_SOM_KAN_ENDRE_TYPE = Set.of(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG);

    public static Set<Behandlingsstatus> hentMuligeStatuser(Behandling behandling) {
        if (behandling.erInaktiv()) return Collections.emptySet();

        Set<Behandlingsstatus> muligeStatuser = new HashSet<>(MULIGE_STATUSER);

        if (TEMAER_SOM_KAN_AVSLUTTES.contains(behandling.getTema())) {
            muligeStatuser.add(Behandlingsstatus.AVSLUTTET);
        }

        return muligeStatuser;
    }

    public static Set<Behandlingstyper> hentMuligeTyper(Behandling behandling) {
        if (behandling.erInaktiv() || !TEMAER_SOM_KAN_ENDRE_TYPE.contains(behandling.getTema())) {
            return Collections.emptySet();
        }

        return switch (behandling.getType()) {
            case ENDRET_PERIODE -> Collections.singleton(NY_VURDERING);
            case NY_VURDERING -> Collections.singleton(ENDRET_PERIODE);
            default -> Collections.emptySet();
        };
    }

    public static Set<Behandlingstema> hentMuligeBehandlingstema(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        boolean kanOppdatereBehandlingstema = kanOppdatereBehandlingstema(behandling, behandlingsresultat);
        if (kanOppdatereBehandlingstema && erGyldigBehandlingAvSøknad(behandling.getTema())) {
            return BEHANDLINGSTEMA_SØKNAD;
        } else if (kanOppdatereBehandlingstema && erBehandlingAvSedForespørsler(behandling.getTema())) {
            return BEHANDLINGSTEMA_SED_FORESPØRSEL;
        } else {
            return Collections.emptySet();
        }
    }

    private static boolean kanOppdatereBehandlingstema(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        return behandling.erAktiv() && behandlingsresultat.erIkkeArtikkel16MedSendtAnmodningOmUnntak();
    }

    public static void validerNyStatusMulig(Behandling behandling, Behandlingsstatus status) {
        if (!hentMuligeStatuser(behandling).contains(status)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til status %s. Gyldige statuser for behandling %s er %s",
                status, behandling.getId(), hentMuligeStatuser(behandling)));
        }
    }

    public static void validerNyTypeMulig(Behandling behandling, Behandlingstyper type) {
        if (!hentMuligeTyper(behandling).contains(type)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til type %s. Gyldige typer for behandling %s er %s",
                type, behandling.getId(), hentMuligeTyper(behandling)));
        }
    }

    public static void validerNyttTemaMulig(Behandling behandling, Behandlingsresultat behandlingsresultat, Behandlingstema tema) {
        if (!hentMuligeBehandlingstema(behandling, behandlingsresultat).contains(tema)) {
            throw new FunksjonellException(String.format("Behandlingen kan ikke endres til tema %s. Gyldige temaer for behandling %s er %s",
                tema, behandling.getId(), hentMuligeBehandlingstema(behandling, behandlingsresultat)));
        }
    }
}
