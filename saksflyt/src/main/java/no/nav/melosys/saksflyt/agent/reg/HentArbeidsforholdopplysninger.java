package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.StandardAbstraktAgent;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;

/**
 * Steget sørger for å hente arbeidsforholdinfo fra AAREG
 *
 * Transisjoner:
 * HENT_ARBF_OPPL → HENT_INNT_OPPL hvis alt ok
 * HENT_ARBF_OPPL → FEILET_MASKINELT hvis oppslag mot AAREG feilet
 */
@Component
public class HentArbeidsforholdopplysninger extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(HentArbeidsforholdopplysninger.class);

    @Value("${melosys.service.fagsak.arbeidsforholdhistorikk.antallÅr}")
    private Integer arbeidsforholdhistorikkAntallÅr;

    private AaregFasade aaregFasade;

    private FagsakService fagsakService;

    @Autowired
    public HentArbeidsforholdopplysninger(Binge binge, ProsessinstansRepository prosessinstansRepo, AaregFasade aaregFasade, FagsakService fagsakService) {
        super(binge, prosessinstansRepo);
        this.aaregFasade = aaregFasade;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.HENT_ARBF_OPPL;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        LocalDate fom = periode.getFom().minusYears(arbeidsforholdhistorikkAntallÅr);
        LocalDate tom = LocalDate.now();

        try {
            Saksopplysning saksopplysning = aaregFasade.finnArbeidsforholdPrArbeidstaker(brukerId, AaregFasade.REGELVERK_A_ORDNINGEN, fom, tom);
            prosessinstans.getBehandling().getSaksopplysninger().add(saksopplysning);

            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            fagsakService.lagre(fagsak);
        } catch (IntegrasjonException | TekniskException | SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
            return;
        }

        prosessinstans.setSteg(ProsessSteg.HENT_INNT_OPPL);
    }
}
