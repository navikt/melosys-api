package no.nav.melosys.service.oppgave;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;

import static no.nav.melosys.domain.Behandling.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.TRYGDETID;

public final class OppgaveFactory {

    private static final long FRIST_FERDIGSTILLELSE_JFR_OPPG = 7;
    private static final String EU_EOS = "ab0424";

    private OppgaveFactory() {
    }

    static Oppgave.Builder lagJournalføringsoppgave(String journalpostID) {
        return new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.JFR)
            .setTema(Tema.MED)
            .setPrioritet(PrioritetType.NORM)
            .setJournalpostId(journalpostID)
            .setFristFerdigstillelse(LocalDate.now().plusDays(FRIST_FERDIGSTILLELSE_JFR_OPPG));
    }

    public static Oppgave.Builder lagBehandlingsoppgave(Behandling behandling, LocalDate mottaksdato) {
        // Dokumentasjon for regler: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
        Sakstyper sakstype = behandling.getFagsak().getType();
        Sakstemaer sakstema = behandling.getFagsak().getTema();
        Behandlingstema behandlingstema = behandling.getTema();
        Behandlingstyper behandlingstype = behandling.getType();

        var oppgaveBehandlingstema = utledOppgaveBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype);
        var oppgaveBehandlingstype = utledOppgaveBehandlingstype(sakstype, sakstema, behandlingstema);
        return new Oppgave.Builder()
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS)
            .setPrioritet(PrioritetType.NORM)
            .setBehandlingstema(oppgaveBehandlingstema.getKode())
            .setBehandlingstype(oppgaveBehandlingstype == null ? null : oppgaveBehandlingstype.getKode())
            .setTema(utledTema(sakstema))
            .setOppgavetype(utledOppgavetype(sakstype, behandlingstema, behandlingstype))
            .setBeskrivelse(utledBeskrivelse(oppgaveBehandlingstema, sakstype, sakstema, behandlingstema, behandlingstype))
            .setFristFerdigstillelse(utledBehandlingsfrist(behandling, mottaksdato));
    }

    static OppgaveBehandlingstema utledOppgaveBehandlingstema(Sakstyper sakstype, Sakstemaer sakstema,
                                                              Behandlingstema behandlingstema,
                                                              Behandlingstyper behandlingstype) {
        if (skalBrukeMelosysBehandlingstemaForOppgaveBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype)) {
            return switch (behandlingstema) {
                case PENSJONIST -> OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET;
                case YRKESAKTIV -> OppgaveBehandlingstema.YRKESAKTIV;
                case REGISTRERING_UNNTAK, REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND ->
                    OppgaveBehandlingstema.REGISTRERING_UNNTAK;
                case ANMODNING_OM_UNNTAK_HOVEDREGEL -> OppgaveBehandlingstema.ANMODNING_UNNTAK;
                default ->
                    throw new FunksjonellException("Mangler mapping av behandlingstema %s".formatted(behandlingstema));
            };
        }

        return switch (sakstype) {
            case EU_EOS -> OppgaveBehandlingstema.EU_EOS_LAND;
            case TRYGDEAVTALE -> OppgaveBehandlingstema.AVTALELAND;
            case FTRL -> OppgaveBehandlingstema.UTENFOR_AVTALELAND;
        };
    }

    private static boolean skalBrukeMelosysBehandlingstemaForOppgaveBehandlingstema(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        if (behandlingstype == null) return false;

        return switch (behandlingstema) {
            case PENSJONIST -> switch (sakstema) {
                case MEDLEMSKAP_LOVVALG ->
                    List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE).contains(behandlingstype);
                case TRYGDEAVGIFT ->
                    List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE, Behandlingstyper.HENVENDELSE).contains(behandlingstype);
                case UNNTAK -> false;
            };
            case YRKESAKTIV ->
                sakstema == Sakstemaer.TRYGDEAVGIFT && List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE, Behandlingstyper.HENVENDELSE).contains(behandlingstype);
            case ANMODNING_OM_UNNTAK_HOVEDREGEL -> switch (sakstype) {
                case EU_EOS ->
                    sakstema == Sakstemaer.UNNTAK && List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING).contains(behandlingstype);
                case TRYGDEAVTALE ->
                    sakstema == Sakstemaer.UNNTAK && List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.HENVENDELSE).contains(behandlingstype);
                default -> false;
            };
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND ->
                sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.UNNTAK && List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING).contains(behandlingstype);
            case REGISTRERING_UNNTAK ->
                sakstype == Sakstyper.TRYGDEAVTALE && sakstema == Sakstemaer.UNNTAK && List.of(Behandlingstyper.FØRSTEGANG, Behandlingstyper.NY_VURDERING, Behandlingstyper.KLAGE).contains(behandlingstype);
            default -> false;
        };
    }

    static OppgaveBehandlingstype utledOppgaveBehandlingstype(Sakstyper sakstype, Sakstemaer sakstema,
                                                              Behandlingstema behandlingstema) {
        if (sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG && behandlingstema == Behandlingstema.BESLUTNING_LOVVALG_NORGE) {
            return OppgaveBehandlingstype.EOS_LOVVALG_NORGE;
        }
        return null;
    }

    public static Tema utledTema(Sakstemaer sakstema) {
        return switch (sakstema) {
            case MEDLEMSKAP_LOVVALG -> Tema.MED;
            case TRYGDEAVGIFT -> Tema.TRY;
            case UNNTAK -> Tema.UFM;
        };
    }

    private static Oppgavetyper utledOppgavetype(Sakstyper sakstype, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        if (sakstype == Sakstyper.EU_EOS) {
            return oppgavetypeEøs(behandlingstema, behandlingstype);
        }
        if (sakstype == Sakstyper.TRYGDEAVTALE) {
            return oppgavetypeTrygdeavtale(behandlingstema, behandlingstype);
        }

        if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            return Oppgavetyper.VURD_HENV;
        }
        return Oppgavetyper.BEH_SAK_MK;
    }

    private static Oppgavetyper oppgavetypeEøs(Behandlingstema tema, Behandlingstyper behandlingstype) {
        if (erAnmodningOmUnntak(tema) || erRegistreringAvUnntak(tema) || List.of(FORESPØRSEL_TRYGDEMYNDIGHET, TRYGDETID).contains(tema)) {
            return Oppgavetyper.BEH_SED;
        }
        if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            return Oppgavetyper.VURD_HENV;
        }
        return Oppgavetyper.BEH_SAK_MK;
    }

    private static Oppgavetyper oppgavetypeTrygdeavtale(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        if (behandlingstema == FORESPØRSEL_TRYGDEMYNDIGHET) {
            return Oppgavetyper.BEH_SAK_MK;
        }
        if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            return Oppgavetyper.VURD_HENV;
        }
        return Oppgavetyper.BEH_SAK_MK;
    }

    private static String utledBeskrivelse(OppgaveBehandlingstema oppgaveBehandlingstema, Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        return switch (oppgaveBehandlingstema) {
            case PENSJONIST_ELLER_UFORETRYGDET -> switch (sakstema) {
                case MEDLEMSKAP_LOVVALG -> sakstype.getBeskrivelse();
                case TRYGDEAVGIFT -> "";
                case UNNTAK -> behandlingstema.getBeskrivelse();
            };
            case YRKESAKTIV -> "";
            case ANMODNING_UNNTAK -> switch (sakstype) {
                case EU_EOS -> "SEDA001";
                case TRYGDEAVTALE -> "";
                case FTRL -> behandlingstema.getBeskrivelse();
            };
            case REGISTRERING_UNNTAK -> switch (behandlingstema) {
                case BESLUTNING_LOVVALG_ANNET_LAND -> "SEDA003";
                case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING -> "SEDA009";
                case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE -> "SEDA010";
                case REGISTRERING_UNNTAK -> "";
                default -> behandlingstema.getBeskrivelse();
            };
            case EU_EOS_LAND -> sedEllerDefaultBeskrivelse(sakstype, behandlingstema, behandlingstype, "SEDA005");
            case AVTALELAND -> sedEllerDefaultBeskrivelse(sakstype, behandlingstema, behandlingstype, "SEDA008");
            default -> behandlingstema.getBeskrivelse();
        };
    }

    private static String sedEllerDefaultBeskrivelse(Sakstyper sakstype, Behandlingstema behandlingstema, Behandlingstyper behandlingstype, String sed) {
        return sakstype == Sakstyper.EU_EOS && behandlingstype == Behandlingstyper.HENVENDELSE && behandlingstema == Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET ? sed : behandlingstema.getBeskrivelse();
    }
}
