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

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker
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

    public static Oppgave.Builder lagBehandlingsoppgave(Behandling behandling, LocalDate mottaksdato) {
        // Dokumentasjon for regler: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
        Sakstyper sakstype = behandling.getFagsak().getType();
        Sakstemaer sakstema = behandling.getFagsak().getTema();
        Behandlingstema behandlingstema = behandling.getTema();
        Behandlingstyper behandlingstype = behandling.getType();

        var oppgaveBehandlingstema = utledBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype);
        return new Oppgave.Builder()
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS)
            .setPrioritet(PrioritetType.NORM)
            .setBehandlingstema(oppgaveBehandlingstema.getKode())
            .setTema(utledTema(sakstema))
            .setOppgavetype(utledOppgavetype(sakstype, behandlingstema, behandlingstype))
            .setBeskrivelse(utledBeskrivelse(oppgaveBehandlingstema, sakstype, sakstema, behandlingstema, behandlingstype))
            .setFristFerdigstillelse(utledBehandlingsfrist(behandling, mottaksdato));
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker
     */
    @Deprecated
    static OppgaveParametere hentOppgaveParametere(Behandlingstema behandlingstema) {
        return switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG ->
                new OppgaveParametere(EU_EOS, "ae0034", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case ARBEID_FLERE_LAND ->
                new OppgaveParametere(EU_EOS, "ae0242", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            case ARBEID_TJENESTEPERSON_ELLER_FLY ->
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

    static OppgaveBehandlingstema utledBehandlingstema(Sakstyper sakstype, Sakstemaer sakstema,
                                                       Behandlingstema behandlingstema,
                                                       Behandlingstyper behandlingstype) {
        if (skalBrukeMelosysBehandlingstemaForBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype)) {
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

    private static boolean skalBrukeMelosysBehandlingstemaForBehandlingstema(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
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
        if (erAnmodningOmUnntak(tema) || erRegistreringAvUnntak(tema) || erBehandlingAvSedForespørsler(tema)) {
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

    private static LocalDate fristUker(int uker) {
        return LocalDate.now().plusWeeks(uker);
    }

    private static LocalDate fristDager(int dager) {
        return LocalDate.now().plusDays(dager);
    }

    /**
     * @deprecated Fjernes med toggle melosys.behandle_alle_saker
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
