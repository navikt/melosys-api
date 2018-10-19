package no.nav.melosys.saksflyt.agent.iv;

import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.util.LandKoderUtils;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATERMEDL;

/**
 * Avslutter en oppgave i GSAK.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK:
 *     JFR_AVSLUTT_OPPGAVE -> JFR_AKTØR_ID eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 *     JFR_AVSLUTT_OPPGAVE -> JFR_OPPDATER_JOURNALPOST eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterMEDL extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterMEDL.class);

    private final MedlFasade medlFasade;
    private final TpsFasade tpsFasade;
    private final LovvalgsperiodeRepository lovvalgsperiodeRepository;


    @Autowired
    public OppdaterMEDL(MedlFasade medlFasade, TpsFasade tpsFasade, LovvalgsperiodeRepository lovvalgsperiodeRepository) {
        log.info("IverksetteVedtakOppdaterMEDL initialisert");
        this.medlFasade = medlFasade;
        this.tpsFasade = tpsFasade;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;

    }

    @Override
    public ProsessSteg inngangsSteg() {
        return IV_OPPDATERMEDL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        String aktørID = prosessinstans.getData(ProsessDataKey.AKTØR_ID);
        String fnr = tpsFasade.hentIdentForAktørId(aktørID);

        Behandling behandling = prosessinstans.getBehandling();

        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeRepository.findByBehandlingsresultatId(behandling.getId());

        Medlemsperiode medlemsperiode = new Medlemsperiode();

        medlemsperiode.setGrunnlagstype(lovvalgsperiode.hentFellesKodeForGrunnlagMedltype());
        medlemsperiode.setLand(LandKoderUtils.tilIso3(lovvalgsperiode.getLovvalgsland().getKode()));
        medlemsperiode.setTrygdedekning(lovvalgsperiode.hentFellesKodeForTrygdDekningtype());

        medlFasade.opprettPeriode(fnr, medlemsperiode);

    }
}
