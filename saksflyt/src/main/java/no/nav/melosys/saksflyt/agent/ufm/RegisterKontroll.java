package no.nav.melosys.saksflyt.agent.ufm;

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
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.unntaksperiode.kontroll.UnntaksperiodeKontroll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RegisterKontroll extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterKontroll.class);

    private final AvklartefaktaService avklartefaktaService;

    @Autowired
    public RegisterKontroll(AvklartefaktaService avklartefaktaService) {
        this.avklartefaktaService = avklartefaktaService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_REGISTER_KONTROLL;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        Behandling behandling = prosessinstans.getBehandling();

        List<Unntak_periode_begrunnelser> registrerteTreff = UnntaksperiodeKontroll.utførKontroller(behandling, "A009");
        if (!registrerteTreff.isEmpty()) {
            registrerFeil(prosessinstans, registrerteTreff);
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPRETT_OPPGAVE);
        } else {
            prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_OPPDATER_MEDL);
        }
    }

    private void registrerFeil(Prosessinstans prosessinstans, List<Unntak_periode_begrunnelser> registrerteTreff) throws IkkeFunnetException {

        avklartefaktaService.leggTilAvklarteFakta(prosessinstans.getBehandling().getId(),
            Avklartefaktatype.VURDERING_UNNTAK_PERIODE, Avklartefaktatype.VURDERING_UNNTAK_PERIODE.name(), null, "TRUE");

        long behandlingsId = prosessinstans.getBehandling().getId();
        log.info("Treff ved validering av periode for behandling {}. Treffbegrunnelse: {}", behandlingsId, registrerteTreff);
        for (Unntak_periode_begrunnelser begrunnelse : registrerteTreff) {
            avklartefaktaService.leggTilRegistrering(behandlingsId, Avklartefaktatype.VURDERING_UNNTAK_PERIODE, begrunnelse.getKode());
        }
    }
}
