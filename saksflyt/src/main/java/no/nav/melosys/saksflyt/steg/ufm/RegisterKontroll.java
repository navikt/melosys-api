package no.nav.melosys.saksflyt.steg.ufm;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Avklartefaktatype;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.unntaksperiode.kontroll.RegisterkontrollService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegisterKontroll extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterkontrollService registerkontrollService;
    private final BehandlingService behandlingService;

    @Autowired
    public RegisterKontroll(AvklartefaktaService avklartefaktaService, RegisterkontrollService registerkontrollService, BehandlingService behandlingService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerkontrollService = registerkontrollService;
        this.behandlingService = behandlingService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_REGISTERKONTROLL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = behandlingService.hentBehandling(prosessinstans.getBehandling().getId());

        List<Unntak_periode_begrunnelser> registrerteTreff = registerkontrollService.utførKontroller(behandling);
        registrerFeil(prosessinstans, registrerteTreff);
        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }

    private void registrerFeil(Prosessinstans prosessinstans, List<Unntak_periode_begrunnelser> registrerteTreff) throws IkkeFunnetException {

        boolean funnetTreff = !registrerteTreff.isEmpty();
        avklartefaktaService.leggTilAvklarteFakta(prosessinstans.getBehandling().getId(),
            Avklartefaktatype.VURDERING_UNNTAK_PERIODE, Avklartefaktatype.VURDERING_UNNTAK_PERIODE.name(),
            null, funnetTreff ? "TRUE" : "FALSE");

        long behandlingsId = prosessinstans.getBehandling().getId();
        log.info("Treff ved validering av periode for behandling {}. Treffbegrunnelse: {}", behandlingsId, registrerteTreff);
        for (Unntak_periode_begrunnelser begrunnelse : registrerteTreff) {
            avklartefaktaService.leggTilRegistrering(behandlingsId, Avklartefaktatype.VURDERING_UNNTAK_PERIODE, begrunnelse.getKode());
        }
    }
}
