package no.nav.melosys.service.sak;

import no.finn.unleash.Unleash;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Behandling.erBehandlingAvSedForespørsler;
import static no.nav.melosys.domain.Behandling.erBehandlingAvSøknad;
import static no.nav.melosys.domain.Fagsak.erSakstypeEøs;
import static no.nav.melosys.service.sak.SakstypeBehandlingstemaKobling.erGyldigBehandlingstemaForSakstype;

@Service
public class OpprettNySakFraOppgave {
    private final JoarkFasade joarkFasade;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final Unleash unleash;

    public OpprettNySakFraOppgave(JoarkFasade joarkFasade, OppgaveService oppgaveService, @Lazy ProsessinstansService prosessinstansService,
                                  Unleash unleash) {
        this.joarkFasade = joarkFasade;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.unleash = unleash;
    }

    @Transactional
    public void bestillNySakOgBehandling(OpprettSakDto opprettSakDto) {
        validerOpprettSakDto(opprettSakDto);
        final Oppgave oppgave = validerOppgave(opprettSakDto.getOppgaveID());
        validerJournalpost(joarkFasade.hentJournalpost(oppgave.getJournalpostId()));
        switch (opprettSakDto.getSakstype()) {
            case EU_EOS -> prosessinstansService.opprettProsessinstansNySakEØS(
                oppgave.getJournalpostId(),
                opprettSakDto,
                erBehandlingAvSøknad(opprettSakDto.getBehandlingstema()) ? Behandlingstyper.SOEKNAD : Behandlingstyper.SED
            );
            case FTRL, TRYGDEAVTALE -> prosessinstansService.opprettProsessinstansNySakFTRLTrygdeavtale(
                oppgave.getJournalpostId(),
                opprettSakDto
            );
        }
    }

    void validerOpprettSakDto(OpprettSakDto opprettSakDto) {
        final var sakstype = opprettSakDto.getSakstype();
        final var behandlingstema = opprettSakDto.getBehandlingstema();

        validerBehandlingstema(behandlingstema, sakstype);

        if (erBehandlingAvSøknad(behandlingstema) && erSakstypeEøs(sakstype)) {
            validerSøknadData(opprettSakDto.getSoknadDto());
        }
    }

    void validerBehandlingstema(Behandlingstema behandlingstema, Sakstyper sakstype) {
        if (behandlingstema == null) {
            throw new FunksjonellException("Behandlingstema mangler for å opprette ny sak");
        } else if (!erBehandlingAvSøknad(behandlingstema) && !erBehandlingAvSedForespørsler(behandlingstema)) {
            throw new FunksjonellException("Kan ikke opprette ny sak med behandlingstema " + behandlingstema);
        } else if (!erGyldigBehandlingstemaForSakstype(sakstype, behandlingstema)) {
            throw new FunksjonellException("Behandlingstema " + behandlingstema + " er ikke gyldig for sakstype " + sakstype);
        } else if (behandlingstema == Behandlingstema.ARBEID_I_UTLANDET && !unleash.isEnabled("melosys.folketrygden.mvp")) {
            throw new FunksjonellException("Kan ikke opprette ny sak med behandlingstema " + behandlingstema);
        }
    }

    private void validerSøknadData(SøknadDto soknadDto) {
        boolean feilet = false;
        StringBuilder feilmeldingBuilder = new StringBuilder();
        if (soknadDto == null) {
            throw new FunksjonellException("SoknadDto må ikke være null for å opprette en søknadbehandling.");
        }
        PeriodeDto periodeDto = soknadDto.getPeriode();
        if (periodeDto.getFom() == null) {
            feilet = true;
            feilmeldingBuilder.append("søknadsperiodes fra og med dato, ");
        }
        if (!soknadDto.getLand().erGyldig()) {
            feilet = true;
            feilmeldingBuilder.append("land, ");
        }
        if (feilet) {
            throw new FunksjonellException(feilmeldingBuilder.append("mangler for å opprette en søknadbehandling.").toString());
        }
        if (periodeDto.getTom() != null && periodeDto.getFom().isAfter(periodeDto.getTom())) {
            throw new FunksjonellException("Fra og med dato kan ikke være etter til og med dato.");
        }
    }

    private Oppgave validerOppgave(String oppgaveID) {
        if (StringUtils.isEmpty(oppgaveID)) {
            throw new FunksjonellException("OppgaveID mangler.");
        }
        final Oppgave oppgave = oppgaveService.hentOppgaveMedOppgaveID(oppgaveID);
        if (!nySakKanOpprettesFraOppgavetype(oppgave.getOppgavetype())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes på bakgrunn av oppgave med type: " + oppgave.getOppgavetype().getBeskrivelse());
        }
        if (StringUtils.isEmpty(oppgave.getJournalpostId())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes fordi oppgave " + oppgaveID + " mangler journalpost.");
        }
        return oppgave;
    }

    private static boolean nySakKanOpprettesFraOppgavetype(Oppgavetyper oppgavetype) {
        return oppgavetype == Oppgavetyper.BEH_SAK_MK
            || oppgavetype == Oppgavetyper.BEH_SAK
            || oppgavetype == Oppgavetyper.BEH_SED;
    }

    private void validerJournalpost(Journalpost journalpost) {
        if (journalpost.getJournalposttype() == Journalposttype.UT) {
            throw new FunksjonellException("Ny sak kan ikke opprettes fra utgående journalposter siden brev refererer til mottaksdato.");
        }
    }
}
