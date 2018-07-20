// FIXME: Må flyttes ned til relevant pakke
package no.nav.melosys.saksflyt.agent;

import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessDataKey.GSAK_SAK_ID;
import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

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
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        BehandlingType behandlingType = prosessinstans.getBehandling().getType(); // Forutsetter at ingen tidligere steg har endret denne
        String gsakSakID = prosessinstans.getData(GSAK_SAK_ID);
        String brukerID = prosessinstans.getData(BRUKER_ID);

        OpprettOppgaveRequest.Builder builder = OpprettOppgaveRequest.builder();
        builder.medSaksnummer(gsakSakID);
        builder.medAktivFra(LocalDate.now());
        builder.medAnsvarligEnhetId(String.valueOf(MELOSYS_ENHET_ID));
        builder.medOpprettetAvEnhetId(MELOSYS_ENHET_ID);

        if (behandlingType == BehandlingType.SØKNAD) {
            builder.medFagområde(Fagomrade.MED);
            builder.medOppgaveType(Oppgavetype.BEH_SAK_MED);
            builder.medPrioritetType(PrioritetType.NORM_MED);
        } else if (behandlingType == BehandlingType.UNNTAK_MEDL) {
            builder.medFagområde(Fagomrade.UFM);
            builder.medOppgaveType(Oppgavetype.BEH_SAK_MK_UFM);
            builder.medPrioritetType(PrioritetType.NORM_UFM);
        } else {
            String feilmelding = "behandlingType " + behandlingType + " er ikke støttet";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.FUNKSJONELL_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        //builder.medUnderkategori() FIXME Venter. Ekisterer det i den nye GSAK tjenesten?
        builder.medAktørType(AktorType.PERSON);
        builder.medFnr(brukerID); // FIXME er det ikke aktørID?
        //builder.medBeskrivelse(); FIXME settes til? Are T.
        //builder.medMottattDato(); FIXME settes? Are T.
        //builder.medNormertBehandlingsTidInnen(); FIXME settes?
        builder.medLest(false);

        String oppgaveId = gsakFasade.opprettOppgave(builder.build());

        prosessinstans.setSteg(null);
        log.info("Opprettet oppgave {} for prosessinstans {}", oppgaveId, prosessinstans.getId());
    }
}
