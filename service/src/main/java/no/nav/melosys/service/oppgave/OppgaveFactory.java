package no.nav.melosys.service.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;

public final class OppgaveFactory {

    private OppgaveFactory() {}

    static Oppgavetyper hentOppgavetype(Behandlingstema behandlingstema) {
        return hentOppgaveParametere(behandlingstema).oppgavetype;
    }

    public static Oppgave.Builder lagBehandlingsOppgaveForType(Behandlingstema behandlingstema, Behandlingstyper behandlingstype) {
        OppgaveParametere parametere = hentOppgaveParametere(behandlingstema);

        if (behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            parametere.fristFerdigstillelse = fristDager(1);
            parametere.oppgavetype = Oppgavetyper.VUR;
        }

        return new Oppgave.Builder()
            .setBehandlingstype(behandlingstype)
            .setBehandlingstema(behandlingstema)
            .setTema(parametere.tema)
            .setOppgavetype(parametere.oppgavetype)
            .setFristFerdigstillelse(parametere.fristFerdigstillelse)
            .setPrioritet(PrioritetType.NORM);
    }

    private static OppgaveParametere hentOppgaveParametere(Behandlingstema behandlingstema) {

        OppgaveParametere oppgaveParametere;

        switch (behandlingstema) {
            case UTSENDT_ARBEIDSTAKER:
            case UTSENDT_SELVSTENDIG:
            case ARBEID_FLERE_LAND:
            case ARBEID_NORGE_BOSATT_ANNET_LAND:
                oppgaveParametere = new OppgaveParametere(Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30));
                break;
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING:
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE:
                oppgaveParametere = new OppgaveParametere(Tema.UFM, Oppgavetyper.BEH_SED, fristUker(2));
                break;
            case BESLUTNING_LOVVALG_NORGE:
                oppgaveParametere = new OppgaveParametere(Tema.MED, Oppgavetyper.BEH_SED, fristUker(4));
                break;
            case BESLUTNING_LOVVALG_ANNET_LAND:
                oppgaveParametere = new OppgaveParametere(Tema.UFM, Oppgavetyper.BEH_SED, fristUker(4));
                break;
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
            case ØVRIGE_SED:
                oppgaveParametere =  new OppgaveParametere(Tema.UFM, Oppgavetyper.BEH_SED, fristUker(8));
                break;
            case TRYGDETID:
                oppgaveParametere = new OppgaveParametere(Tema.MED, Oppgavetyper.BEH_SED, fristUker(8));
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

    public static class OppgaveParametere {
        final Tema tema;
        Oppgavetyper oppgavetype;
        LocalDate fristFerdigstillelse;

        OppgaveParametere(Tema tema, Oppgavetyper oppgavetype, LocalDate fristFerdigstillelse) {
            this.tema = tema;
            this.oppgavetype = oppgavetype;
            this.fristFerdigstillelse = fristFerdigstillelse;
        }
    }
}
