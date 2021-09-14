package no.nav.melosys.service.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;

public final class OppgaveFactory {

    private static final long FRIST_FERDIGSTILLELSE_JFR_OPPG = 7;

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

    static OppgaveParametere hentOppgaveParametere(Behandlingstema behandlingstema) {

        OppgaveParametere oppgaveParametere;

        switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER:
            case UTSENDT_SELVSTENDIG:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0034", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            case ARBEID_FLERE_LAND:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0242", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            //case ARBEID_NORGE_BOSATT_ANNET_LAND: FIXME: behandlingstema ikke i bruk i Melosys
            //    oppgaveParametere = new OppgaveParametere(behandlingstema, behandlingstype, Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
            //    break;
            case ARBEID_ETT_LAND_ØVRIG:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0243", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            case IKKE_YRKESAKTIV:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0238", Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0111", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(2));
                break;
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0235", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(2));
                break;
            case BESLUTNING_LOVVALG_NORGE:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0112", Tema.MED, Oppgavetyper.BEH_SED, fristUker(4));
                break;
            case BESLUTNING_LOVVALG_ANNET_LAND:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0113", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(4));
                break;
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0110", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(8));
                break;
            case ØVRIGE_SED_UFM:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0254", Tema.UFM, Oppgavetyper.BEH_SED, fristUker(8));
                break;
            case ØVRIGE_SED_MED:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0254", Tema.MED, Oppgavetyper.BEH_SED, fristUker(8));
                break;
            case TRYGDETID:
                oppgaveParametere = new OppgaveParametere("ab0424", "ae0236", Tema.MED, Oppgavetyper.BEH_SED, fristUker(8));
                break;
            case ARBEID_I_UTLANDET:
                oppgaveParametere = new OppgaveParametere("ab0388", null, Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            case TRYGDEAVTALE_UK:
                // https://confluence.adeo.no/display/TEESSI/Storbritannia#Storbritannia-Journalf%C3%B8ring
                oppgaveParametere = new OppgaveParametere("ab0387", null, Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            default:
                throw new IllegalArgumentException("Melosys støtter ikke mapping for behandlingstema  " + behandlingstema);
        }


        return oppgaveParametere;
    }

    private static LocalDate fristUker(int uker) {
        return LocalDate.now().plusWeeks(uker);
    }

    private static LocalDate fristDager(int dager) {
        return LocalDate.now().plusDays(dager);
    }

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
