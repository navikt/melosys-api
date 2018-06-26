package no.nav.melosys.saksflyt.agent.reg;

import java.time.LocalDate;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessSteg.HENT_MEDL_OPPL;
import static no.nav.melosys.domain.ProsessSteg.OPPRETT_OPPGAVE;

/**
 * Steget sørger for å hente medlemskapsinfo fra MEDL
 *
 * Transisjoner:
 * HENT_MEDL_OPPL → OPPRETT_OPPGAVE hvis alt ok
 * HENT_MEDL_OPPL → FEILET_MASKINELT hvis oppslag mot MEDL feilet
 */
@Component
public class HentMedlemskapsopplysninger extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(HentMedlemskapsopplysninger.class);

    private final MedlFasade medlFasade;

    private final FagsakService fagsakService;

    private final Integer medlemskaphistorikkAntallÅr;

    public HentMedlemskapsopplysninger(MedlFasade medlFasade, FagsakService fagsakService,
                                       @Value("${melosys.service.fagsak.medlemskaphistorikk.antallÅr}") Integer medlemskaphistorikkAntallÅr) {
        this.medlFasade = medlFasade;
        this.fagsakService = fagsakService;
        this.medlemskaphistorikkAntallÅr = medlemskaphistorikkAntallÅr;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return HENT_MEDL_OPPL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String brukerId = prosessinstans.getData(BRUKER_ID);

        Periode periode = prosessinstans.getData(ProsessDataKey.SØKNADSPERIODE, Periode.class);
        LocalDate fom = periode.getFom().minusYears(medlemskaphistorikkAntallÅr);
        LocalDate tom = LocalDate.now();

        try {
            Saksopplysning saksopplysning = medlFasade.hentPeriodeListe(brukerId, fom, tom);
            prosessinstans.getBehandling().getSaksopplysninger().add(saksopplysning);

            Fagsak fagsak = prosessinstans.getBehandling().getFagsak();
            fagsakService.lagre(fagsak);
        } catch (IntegrasjonException | SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            // FIXME: MELOSYS-1316
            return;
        }

        prosessinstans.setSteg(OPPRETT_OPPGAVE);
    }
}
