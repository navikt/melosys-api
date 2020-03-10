package no.nav.melosys.saksflyt.steg.aou.inn;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl;
import no.nav.melosys.integrasjon.medl.MedlFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpprettPeriodeIMedl extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(OpprettPeriodeIMedl.class);

    private final MedlFasade medlFasade;
    private final MedlPeriodeService medlPeriodeService;

    @Autowired
    public OpprettPeriodeIMedl(MedlFasade medlFasade, MedlPeriodeService medlPeriodeService) {
        this.medlFasade = medlFasade;
        this.medlPeriodeService = medlPeriodeService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.AOU_MOTTAK_OPPRETT_PERIODE_MEDL;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        Behandling behandling = prosessinstans.getBehandling();

        Anmodningsperiode anmodningsperiode = medlPeriodeService.hentAnmodningsperiode(behandling);
        String fnr = medlPeriodeService.hentFnr(behandling);

        Long medlPeriodeId = medlFasade.opprettPeriodeUnderAvklaring(fnr, anmodningsperiode, KildedokumenttypeMedl.SED);
        medlPeriodeService.lagreMedlPeriodeId(medlPeriodeId, anmodningsperiode, behandling.getId());

        log.info("Periode under avklaring opprettet i Medl for behandling {}", behandling.getId());
        prosessinstans.setSteg(ProsessSteg.AOU_MOTTAK_OPPRETT_OPPGAVE);
    }
}
