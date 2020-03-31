package no.nav.melosys.service.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.PrioritetType;

public final class OppgaveFactory {

    private OppgaveFactory() {}

    static Oppgavetyper hentOppgavetype(Behandlingstyper behandlingstype) {
        return hentOppgaveParametere(behandlingstype).oppgavetype;
    }

    public static Oppgave.Builder lagBehandlingsOppgaveForType(Behandlingstyper behandlingstype) {
        OppgaveParametere parametere = hentOppgaveParametere(behandlingstype);

        return new Oppgave.Builder()
            .setBehandlingstype(behandlingstype)
            .setTema(parametere.tema)
            .setOppgavetype(parametere.oppgavetype)
            .setFristFerdigstillelse(parametere.fristFerdigstillelse)
            .setBehandlingstema(parametere.behandlingstema)
            .setPrioritet(PrioritetType.NORM);
    }

    private static OppgaveParametere hentOppgaveParametere(Behandlingstyper behandlingstype) {
        switch (behandlingstype) {
            case SOEKNAD:
            case SOEKNAD_IKKE_YRKESAKTIV:
            case SOEKNAD_ARBEID_FLERE_LAND:
            case SOEKNAD_ARBEID_NORGE_BOSATT_ANNET_LAND:
            case NY_VURDERING:
                return new OppgaveParametere(Tema.MED, Oppgavetyper.BEH_SAK_MK, fristDager(30), Behandlingstema.EU_EOS);
            case ENDRET_PERIODE:
                return new OppgaveParametere(Tema.MED, Oppgavetyper.VUR, fristDager(1), Behandlingstema.EU_EOS);
            case REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING:
            case REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE:
                return new OppgaveParametere(Tema.UFM, Oppgavetyper.BEH_SED, fristUker(2), Behandlingstema.EU_EOS);
            case BESLUTNING_LOVVALG_NORGE:
                return new OppgaveParametere(Tema.MED, Oppgavetyper.BEH_SED, fristUker(4), Behandlingstema.EU_EOS);
            case BESLUTNING_LOVVALG_ANNET_LAND:
                return new OppgaveParametere(Tema.UFM, Oppgavetyper.BEH_SED, fristUker(4), Behandlingstema.EU_EOS);
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
            case ØVRIGE_SED:
                return new OppgaveParametere(Tema.UFM, Oppgavetyper.BEH_SED, fristUker(8), Behandlingstema.EU_EOS);
            case VURDER_TRYGDETID:
                return new OppgaveParametere(Tema.MED, Oppgavetyper.BEH_SED, fristUker(8), Behandlingstema.EU_EOS);
            case KLAGE:
            case ANKE:
            default:
                throw new IllegalArgumentException("Melosys støtter ikke mapping for behandlingstype  " + behandlingstype);
        }
    }

    private static LocalDate fristUker(int uker) {
        return LocalDate.now().plusWeeks(uker);
    }

    private static LocalDate fristDager(int dager) {
        return LocalDate.now().plusDays(dager);
    }

    public static class OppgaveParametere {
        final Tema tema;
        final Oppgavetyper oppgavetype;
        final LocalDate fristFerdigstillelse;
        final Behandlingstema behandlingstema;

        OppgaveParametere(Tema tema, Oppgavetyper oppgavetype, LocalDate fristFerdigstillelse, Behandlingstema behandlingstema) {
            this.tema = tema;
            this.oppgavetype = oppgavetype;
            this.fristFerdigstillelse = fristFerdigstillelse;
            this.behandlingstema = behandlingstema;
        }
    }
}
