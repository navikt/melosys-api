package no.nav.melosys.service.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;

public final class OppgaveFactory {

    private static final long FRIST_FERDIGSTILLELSE_JFR_OPPG = 7;
    private static final String EU_EOS = "ab0424";

    private OppgaveFactory() {
    }

    public static Oppgave.Builder lagJournalføringsoppgave(String journalpostID) {
        return new Oppgave.Builder()
            .setOppgavetype(Oppgavetyper.JFR)
            .setTema(Tema.MED)
            .setPrioritet(PrioritetType.NORM)
            .setJournalpostId(journalpostID)
            .setFristFerdigstillelse(LocalDate.now().plusDays(FRIST_FERDIGSTILLELSE_JFR_OPPG));
    }

    /**
     * @deprecated Fjernes med toggle melosys.oppgave.oppretting
     */
    @Deprecated
    public static Oppgave.Builder lagBehandlingsOppgaveForType(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        final OppgaveParametere parametere = hentOppgaveParametere(behandlingstema);

        if (behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            parametere.fristFerdigstillelse = fristDager(1);
            parametere.oppgavetype = Oppgavetyper.VUR;
        }

        return new Oppgave.Builder()
            .setBehandlingstype(parametere.behandlingstype)
            .setBehandlingstema(parametere.behandlingstema)
            .setTema(parametere.tema)
            .setOppgavetype(parametere.oppgavetype)
            .setFristFerdigstillelse(parametere.fristFerdigstillelse)
            .setPrioritet(PrioritetType.NORM)
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS);
    }

    public static Oppgave.Builder lagBehandlingsoppgave(Sakstemaer sakstema, Sakstyper sakstype, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        // Dokumentasjon for regler: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
        return new Oppgave.Builder()
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS)
            .setPrioritet(PrioritetType.NORM)
            .setBehandlingstema(utledBehandlingstema(behandlingstema, sakstype))
            .setTema(utledTema(sakstema))
            .setOppgavetype(utledOppgavetype(sakstype, behandlingstema, behandlingstype))
            .setBeskrivelse(utledBeskrivelse(behandlingstema, sakstema, sakstype))
            .setFristFerdigstillelse(Behandling.utledFristForBehandlingstema(behandlingstema));
    }

    public static Oppgave.Builder lagBehandlingsoppgave(Fagsak fagsak, Behandling behandling) {
        return lagBehandlingsoppgave(fagsak.getTema(), fagsak.getType(), behandling.getTema(), behandling.getType());
    }

    public static Oppgave.Builder lagBehandlingsoppgave(Behandling behandling) {
        return lagBehandlingsoppgave(behandling.getFagsak(), behandling);
    }

    /**
     * @deprecated Fjernes med toggle melosys.oppgave.oppretting
     */
    @Deprecated
    static OppgaveParametere hentOppgaveParametere(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG ->
                new OppgaveParametere(EU_EOS, "ae0034", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case ARBEID_FLERE_LAND ->
                new OppgaveParametere(EU_EOS, "ae0242", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case ARBEID_ETT_LAND_ØVRIG, ARBEID_TJENESTEPERSON_ELLER_FLY ->
                new OppgaveParametere(EU_EOS, "ae0243", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case IKKE_YRKESAKTIV ->
                new OppgaveParametere(EU_EOS, "ae0238", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING ->
                new OppgaveParametere(EU_EOS, "ae0111", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(2));
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE ->
                new OppgaveParametere(EU_EOS, "ae0235", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(2));
            case BESLUTNING_LOVVALG_NORGE ->
                new OppgaveParametere(EU_EOS, "ae0112", Tema.MED, Oppgavetyper.BEH_SED, fristUker(4));
            case BESLUTNING_LOVVALG_ANNET_LAND ->
                new OppgaveParametere(EU_EOS, "ae0113", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(4));
            case ANMODNING_OM_UNNTAK_HOVEDREGEL ->
                new OppgaveParametere(EU_EOS, "ae0110", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(8));
            case ØVRIGE_SED_UFM ->
                new OppgaveParametere(EU_EOS, "ae0254", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(8));
            case ØVRIGE_SED_MED ->
                new OppgaveParametere(EU_EOS, "ae0254", Tema.MED, Oppgavetyper.BEH_SED, fristUker(8));
            case TRYGDETID -> new OppgaveParametere(EU_EOS, "ae0236", Tema.MED, Oppgavetyper.BEH_SED, fristUker(8));
            case ARBEID_I_UTLANDET ->
                new OppgaveParametere("ab0388", null, Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case ARBEID_KUN_NORGE, YRKESAKTIV ->
                new OppgaveParametere("ab0387", null, Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            default -> throw new IllegalArgumentException(
                "Melosys støtter ikke mapping for behandlingstema  " + behandlingstema);
        };
    }

    public static String utledBehandlingstema(Behandlingstema behandlingstema, Sakstyper sakstype) {
        return switch (behandlingstema) {
            case PENSJONIST -> "ab0355";
            case YRKESAKTIV -> "ab0462";
            case ANMODNING_OM_UNNTAK_HOVEDREGEL -> "ab0460";
            case REGISTRERING_UNNTAK -> "ab0461";
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND ->
                "ab0461";
            default -> switch (sakstype) {
                case EU_EOS -> "ab0424";
                case FTRL -> "ab0388";
                case TRYGDEAVTALE -> "ab0387";
            };
        };
    }

    private static Tema utledTema(Sakstemaer sakstema) {
        return switch (sakstema) {
            case MEDLEMSKAP_LOVVALG -> Tema.MED;
            case TRYGDEAVGIFT -> Tema.TRY;
            case UNNTAK -> Tema.UFM;
        };
    }

    private static Oppgavetyper utledOppgavetype(Sakstyper sakstype, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            return Oppgavetyper.VURD_HENV;
        }

        if (sakstype == Sakstyper.EU_EOS) {
            return switch (behandlingstema) {
                case BESLUTNING_LOVVALG_NORGE, ANMODNING_OM_UNNTAK_HOVEDREGEL, REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND ->
                    Oppgavetyper.BEH_SED;
                default -> Oppgavetyper.BEH_SAK_MK;
            };
        }

        return Oppgavetyper.BEH_SAK_MK;
    }

    private static String utledBeskrivelse(Behandlingstema behandlingstema, Sakstemaer sakstema, Sakstyper sakstype) {
        return switch (behandlingstema) {
            case PENSJONIST -> sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG ? sakstype.getBeskrivelse() : "";
            case YRKESAKTIV -> "";
            case ANMODNING_OM_UNNTAK_HOVEDREGEL -> sakstype == Sakstyper.EU_EOS ? "SEDA001" : "";
            case BESLUTNING_LOVVALG_ANNET_LAND -> "SEDA003";
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING -> "SEDA009";
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE -> "SEDA010";
            case FORESPØRSEL_TRYGDEMYNDIGHET -> switch (sakstype) {
                case EU_EOS -> "SEDA005";
                case TRYGDEAVTALE -> "SEDA008";
                case FTRL -> "";
            };
            default -> behandlingstema.getBeskrivelse();
        };
    }

    private static LocalDate fristUker(int uker) {
        return LocalDate.now().plusWeeks(uker);
    }

    private static LocalDate fristDager(int dager) {
        return LocalDate.now().plusDays(dager);
    }

    /**
     * @deprecated Fjernes med toggle melosys.oppgave.oppretting
     */
    @Deprecated
    static class OppgaveParametere {
        final String behandlingstema;
        final String behandlingstype;
        final Tema tema;
        Oppgavetyper oppgavetype;
        LocalDate fristFerdigstillelse;

        OppgaveParametere(String behandlingstema, String behandlingstype, Tema tema, Oppgavetyper oppgavetype, LocalDate fristFerdigstillelse) {
            this.behandlingstema = behandlingstema;
            this.behandlingstype = behandlingstype;
            this.tema = tema;
            this.oppgavetype = oppgavetype;
            this.fristFerdigstillelse = fristFerdigstillelse;
        }
    }
}
