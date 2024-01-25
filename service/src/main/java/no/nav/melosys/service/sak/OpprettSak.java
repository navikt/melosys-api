package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.UtledBehandlingsaarsak;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.Fagsak.erSakstypeEøs;

@Service
public class OpprettSak {
    private final JournalfoeringService journalfoeringService;
    private final OppgaveService oppgaveService;
    private final ProsessinstansService prosessinstansService;
    private final SaksbehandlingRegler saksbehandlingRegler;

    private final LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;

    public OpprettSak(JournalfoeringService journalfoeringService, OppgaveService oppgaveService,
                      @Lazy ProsessinstansService prosessinstansService,
                      SaksbehandlingRegler saksbehandlingRegler,
                      LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService) {
        this.journalfoeringService = journalfoeringService;
        this.oppgaveService = oppgaveService;
        this.prosessinstansService = prosessinstansService;
        this.saksbehandlingRegler = saksbehandlingRegler;
        this.lovligeKombinasjonerSaksbehandlingService = lovligeKombinasjonerSaksbehandlingService;
    }

    @Transactional
    public void opprettNySakOgBehandlingFraOppgave(OpprettSakDto opprettSakDto) {
        if (StringUtils.isEmpty(opprettSakDto.getOppgaveID())) {
            throw new FunksjonellException("OppgaveID mangler.");
        }
        validerOpprettSakDto(opprettSakDto);

        final Oppgave oppgave = oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID());
        validerOppgave(oppgave);

        Journalpost journalpost = journalfoeringService.hentJournalpost(oppgave.getJournalpostId());
        validerJournalpost(journalpost);

        opprettSakDto.setBehandlingsaarsakType(getBehandlingsaarsakType(journalpost, opprettSakDto));
        opprettSakDto.setMottaksdato(LocalDate.ofInstant(journalpost.getForsendelseMottatt(), ZoneId.systemDefault()));

        Sakstyper sakstype = opprettSakDto.getSakstype();
        if (sakstype == Sakstyper.EU_EOS) {
            prosessinstansService.opprettProsessinstansNySakEØS(
                oppgave.getJournalpostId(),
                opprettSakDto.tilOpprettSakRequest()
            );
        } else if (sakstype == Sakstyper.FTRL || sakstype == Sakstyper.TRYGDEAVTALE) {
            prosessinstansService.opprettProsessinstansNySakFTRLTrygdeavtale(
                oppgave.getJournalpostId(),
                opprettSakDto.tilOpprettSakRequest()
            );
        } else {
            throw new FunksjonellException("Sakstype %s støttes ikke".formatted(sakstype));
        }
    }

    private static Behandlingsaarsaktyper getBehandlingsaarsakType(Journalpost journalpost, OpprettSakDto opprettSakDto) {
        Sakstemaer sakstema = opprettSakDto.getSakstema();
        Behandlingstema behandlingstema = opprettSakDto.getBehandlingstema();
        Behandlingstyper behandlingstype = opprettSakDto.getBehandlingstype();
        return UtledBehandlingsaarsak.utledÅrsaktype(journalpost, sakstema, behandlingstema, behandlingstype);
    }

    @Transactional
    public void opprettNySakOgBehandling(OpprettSakDto opprettSakDto) {
        if (opprettSakDto.getMottaksdato() == null) {
            throw new FunksjonellException("Mottaksdato er påkrevd for å opprette sak uten oppgave/journalpost");
        }
        if (opprettSakDto.getBehandlingsaarsakType() == null) {
            throw new FunksjonellException("Årsak er påkrevd for å opprette behandling");
        }
        if (StringUtils.isNotEmpty(opprettSakDto.getBehandlingsaarsakFritekst()) && opprettSakDto.getBehandlingsaarsakType() != Behandlingsaarsaktyper.FRITEKST) {
            throw new FunksjonellException("Kan ikke lagre fritekst som årsak når årsakstype er " + opprettSakDto.getBehandlingsaarsakType());
        }

        validerOpprettSakDto(opprettSakDto);
        prosessinstansService.opprettNySakOgBehandling(opprettSakDto.tilOpprettSakRequest());
    }

    void validerOpprettSakDto(OpprettSakDto opprettSakDto) {
        var hovedpart = opprettSakDto.getHovedpart();
        var sakstype = opprettSakDto.getSakstype();
        var sakstema = opprettSakDto.getSakstema();
        var behandlingstema = opprettSakDto.getBehandlingstema();
        var behandlingstype = opprettSakDto.getBehandlingstype();

        lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(
            hovedpart, sakstype, sakstema, behandlingstema, behandlingstype);

        if (erSakstypeEøs(sakstype)
            && !saksbehandlingRegler.harIngenFlyt(sakstype, sakstema, behandlingstype, behandlingstema)
            && !saksbehandlingRegler.harIkkeYrkesaktivFlyt(sakstype, behandlingstema)
            && !saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(sakstype, sakstema, behandlingstema)) {
            validerSøknadData(opprettSakDto.getSoknadDto());
        }
    }

    private void validerSøknadData(SøknadDto soknadDto) {
        boolean feilet = false;
        StringBuilder feilmeldingBuilder = new StringBuilder();
        if (soknadDto == null) {
            throw new FunksjonellException("SoknadDto må ikke være null for å opprette en søknadbehandling.");
        }
        PeriodeDto periodeDto = soknadDto.periode;
        if (periodeDto.getFom() == null) {
            feilet = true;
            feilmeldingBuilder.append("søknadsperiodes fra og med dato, ");
        }
        if (!soknadDto.land.erGyldig()) {
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

    private void validerOppgave(Oppgave oppgave) {
        if (!nySakKanOpprettesFraOppgavetype(oppgave.getOppgavetype())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes på bakgrunn av oppgave med type: " + oppgave.getOppgavetype().getBeskrivelse());
        }

        if (StringUtils.isEmpty(oppgave.getJournalpostId())) {
            throw new FunksjonellException("Ny sak kan ikke opprettes fordi oppgave " + oppgave.getOppgaveId() + " mangler journalpost.");
        }
    }

    private static boolean nySakKanOpprettesFraOppgavetype(Oppgavetyper oppgavetype) {
        return oppgavetype == Oppgavetyper.BEH_SAK_MK
            || oppgavetype == Oppgavetyper.BEH_SAK
            || oppgavetype == Oppgavetyper.BEH_SED
            || oppgavetype == Oppgavetyper.VUR
            || oppgavetype == Oppgavetyper.VURD_HENV
            || oppgavetype == Oppgavetyper.VURD_MAN_INNB;
    }

    private void validerJournalpost(Journalpost journalpost) {
        if (journalpost.getJournalposttype() == Journalposttype.UT) {
            throw new FunksjonellException("Ny sak kan ikke opprettes fra utgående journalposter siden brev refererer til mottaksdato.");
        }
        validerSedTilknytning(journalpost);
    }

    private void validerSedTilknytning(Journalpost journalpost) {
        Optional<Fagsak> optionalFagsak = journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost);
        if (optionalFagsak.isPresent()) {
            throw new FunksjonellException(
                "SED-en som er tilknyttet Gosys-oppgaven du har valgt er allerede koblet til %s".formatted(
                    optionalFagsak.get().getSaksnummer()));
        }
    }
}
