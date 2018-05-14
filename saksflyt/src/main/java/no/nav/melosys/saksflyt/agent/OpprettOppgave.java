// FIXME: Må flyttes ned til relevant pakke
package no.nav.melosys.saksflyt.agent;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.AKTØR_ID;
import static no.nav.melosys.domain.ProsessDataKey.GSAK_SAK_ID;
import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;

/**
 * Oppretter en oppgave i GSAK.
 *
 * Transisjoner:
 * OPPRETT_OPPGAVE -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    private final GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
        log.info("OpprettOppgave initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OPPGAVE;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException, FunksjonellException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        ProsessType prosessType = prosessinstans.getType();
        BehandlingType behandlingType;
        if (prosessType == ProsessType.JFR_NY_SAK || prosessType == ProsessType.JFR_KNYTT) {
            behandlingType = BehandlingType.SØKNAD;
        } else  {
            String feilmelding = "ProsessType " + prosessType + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        String gsakSakID = prosessinstans.getData(GSAK_SAK_ID);
        String aktørID = prosessinstans.getData(AKTØR_ID);
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);


        OppgaveDto oppgaveDto = new OppgaveDto();
        oppgaveDto.setSakreferanse(gsakSakID);
        if (BehandlingType.SØKNAD.equals(behandlingType)) {
            oppgaveDto.setTema(Tema.MED.name());
            oppgaveDto.setOppgavetype(Oppgavetype.BEH_SAK.name());
            oppgaveDto.setPrioritet(PrioritetType.NORM.name());
        } else if (BehandlingType.UNNTAK_MEDL.equals(behandlingType)) {
            oppgaveDto.setTema(Tema.UFM.name());
            oppgaveDto.setOppgavetype(Oppgavetype.BEH_SAK.name());
            oppgaveDto.setPrioritet(PrioritetType.NORM.name());
        } else {
            // Skal ikke kunne skje
            throw new FunksjonellException("OpprettOppgave.utfør(...) har klart å sette behandlingType til noe den selv ikke støtter");
        }
        //builder.medUnderkategori() FIXME Venter. Ekisterer det i den nye GSAK tjenesten?
        oppgaveDto.setAktørId(aktørID);
        oppgaveDto.setJournalpostId(journalpostID);
        //builder.medBeskrivelse(); FIXME settes til? Are T.
        //builder.medMottattDato(); FIXME settes? Are T.
        //builder.medNormertBehandlingsTidInnen(); FIXME settes? Are T
        gsakFasade.opprettOppgave(oppgaveDto);
        prosessinstans.setSteg(null);
    }
}
