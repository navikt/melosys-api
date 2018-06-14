package no.nav.melosys.saksflyt.agent.jfr;

import java.time.LocalDate;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessDataKey.GSAK_SAK_ID;
import static no.nav.melosys.domain.ProsessSteg.FERDIG;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPRETT_OPPGAVE;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;

/**
 * Oppretter en oppgave i GSAK.
 *
 * Transisjoner:
 * JFR_OPPRETT_OPPGAVE -> FERDIG eller FEILET_MASKINELT hvis feil
 */
@Component
public class OpprettOppgave extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(GsakFasade gsakFasade) {
        this.gsakFasade = gsakFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPRETT_OPPGAVE;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        ProsessType prosessType = prosessinstans.getType();
        BehandlingType behandlingType = null;
        if (ProsessType.JFR_NY_SAK.equals(prosessType) || ProsessType.JFR_KNYTT.equals(prosessType)) {
            behandlingType = BehandlingType.SØKNAD;
        } else  {
            // FIXME: MELOSYS-1316
            throw new TekniskException("ProsessType " + prosessType + " er ikke støttet");
        }

        String gsakSakID = prosessinstans.getData(GSAK_SAK_ID);
        String brukerID = prosessinstans.getData(BRUKER_ID);

        OpprettOppgaveRequest.Builder builder = OpprettOppgaveRequest.builder();
        builder.medSaksnummer(gsakSakID);
        builder.medAktivFra(LocalDate.now());
        builder.medAnsvarligEnhetId(String.valueOf(MELOSYS_ENHET_ID));
        builder.medOpprettetAvEnhetId(MELOSYS_ENHET_ID);

        if (BehandlingType.SØKNAD.equals(behandlingType)) {
            builder.medFagområde(Fagomrade.MED);
            builder.medOppgaveType(Oppgavetype.BEH_SAK_MED);
            builder.medPrioritetType(PrioritetType.NORM_MED);
        } else if (BehandlingType.UNNTAK_MEDL.equals(behandlingType)) {
            builder.medFagområde(Fagomrade.UFM);
            builder.medOppgaveType(Oppgavetype.BEH_SAK_MK_UFM);
            builder.medPrioritetType(PrioritetType.NORM_UFM);
        } else {
            // FIXME: MELOSYS-1316
            throw new TekniskException("BehandlingType " + behandlingType.getBeskrivelse() + " støttes ikke.");
        }

        //builder.medUnderkategori() FIXME Venter. Ekisterer det i den nye GSAK tjenesten?
        builder.medAktørType(AktorType.PERSON);
        builder.medFnr(brukerID); // FIXME er det ikke aktørID?
        //builder.medBeskrivelse(); FIXME settes til? Are T.
        //builder.medMottattDato(); FIXME settes? Are T.
        //builder.medNormertBehandlingsTidInnen(); FIXME settes?
        builder.medLest(false);

        try {
            gsakFasade.opprettOppgave(builder.build());
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            // FIXME: MELOSYS-1316
        }

        prosessinstans.setSteg(FERDIG);
    }
}
