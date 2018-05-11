package no.nav.melosys.saksflyt.impl.agent;

import java.time.LocalDate;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.gsak.AktorType;
import no.nav.melosys.domain.gsak.Fagomrade;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.OpprettOppgaveRequest;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;
import static no.nav.melosys.integrasjon.Konstanter.MELOSYS_ENHET_ID;
import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessDataKey.GSAK_SAK_ID;

@Component
public class OpprettOppgave extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(OpprettOppgave.class);

    GsakFasade gsakFasade;

    @Autowired
    public OpprettOppgave(Binge binge, ProsessinstansRepository prosessinstansRepo, GsakFasade gsakFasade) {
        super(binge, prosessinstansRepo);
        this.gsakFasade = gsakFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OPPGAVE;
    }

    @Override
    public void utfoerSteg(Prosessinstans prosessinstans) {
        BehandlingType behandlingType = BehandlingType.SØKNAD; //TODO kan variere?
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
            throw new TekniskException("BehandlingType " + behandlingType.getBeskrivelse() + " støttes ikke.");
        }

        //builder.medUnderkategori() FIXME Ekisterer det i den nye GSAK tjenesten?
        builder.medAktørType(AktorType.PERSON);
        builder.medFnr(brukerID); // FIXME er det ikke aktørID?
        //builder.medBeskrivelse(); FIXME settes til? Are T.
        //builder.medMottattDato(); FIXME settes? Are T.
        //builder.medNormertBehandlingsTidInnen(); FIXME settes?
        builder.medLest(false);

        try {
            gsakFasade.opprettOppgave(builder.build());
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg " + inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setSteg(OPPRETT_OPPGAVE);
    }
}
