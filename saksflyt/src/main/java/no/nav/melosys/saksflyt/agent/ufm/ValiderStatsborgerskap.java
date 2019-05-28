package no.nav.melosys.saksflyt.agent.ufm;

import java.util.Arrays;
import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.repository.SaksopplysningRepository;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValiderStatsborgerskap extends RegistreringUnntakValiderer {

    private static final Logger log = LoggerFactory.getLogger(ValiderStatsborgerskap.class);
    @Autowired
    ValiderStatsborgerskap(SaksopplysningRepository saksopplysningRepository, AvklartefaktaService avklartefaktaService) {
        super(saksopplysningRepository, avklartefaktaService);
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.REG_UNNTAK_VALIDER_STATSBORGERSKAP;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws TekniskException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());
        //TODO: avklar om dette er ok måte å sjekke på. Dekker behovet slik kodeverket er nå
        SedDokument sedDokument = (SedDokument) hentSedSaksopplysning(prosessinstans).getDokument();
        boolean harStatsborgerskapIGyldigLand = Arrays.stream(Landkoder.values())
            .anyMatch(landkode -> sedDokument.getStatsborgerskapKoder().contains(landkode.getKode()));

        if (!harStatsborgerskapIGyldigLand) {
            registrerFeil(prosessinstans, Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND);
        }

        prosessinstans.setSteg(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }
}
