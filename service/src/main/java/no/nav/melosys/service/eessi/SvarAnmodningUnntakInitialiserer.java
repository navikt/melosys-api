package no.nav.melosys.service.eessi;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kafka.model.MelosysEessiMelding;
import no.nav.melosys.service.sak.FagsakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

//A002,A011
@Service
public class SvarAnmodningUnntakInitialiserer implements BehandleMottattSedInitialiserer {

    private final FagsakService fagsakService;

    @Autowired
    public SvarAnmodningUnntakInitialiserer(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    public void initialiserProsessinstans(Prosessinstans prosessinstans) throws TekniskException {
        MelosysEessiMelding melosysEessiMelding = prosessinstans.getData(ProsessDataKey.EESSI_MELDING, MelosysEessiMelding.class);
        Behandling behandling = hentBehandling(melosysEessiMelding.getGsakSaksnummer());

        if (behandling.getStatus() != Behandlingsstatus.ANMODNING_UNNTAK_SENDT) {
            throw new TekniskException("Finner behandling " + behandling.getId() + " for sed fra rinaSak " + melosysEessiMelding.getRinaSaksnummer()
            + ", men behandlingen har status " + behandling.getStatus());
        }

        if (vedtakFattesAutomatisk(melosysEessiMelding)) {
            prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK);
            prosessinstans.setSteg(ProsessSteg.IV_VALIDERING);
            prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
            prosessinstans.setBehandling(behandling);
        } else {
            throw new TekniskException("Støtte for manuell behandling av svar på A001 ikke støttet enda");
        }
    }

    private boolean vedtakFattesAutomatisk(MelosysEessiMelding melosysEessiMelding) {
        SedType sedType = SedType.valueOf(melosysEessiMelding.getSedType());
        return sedType == SedType.A011 && StringUtils.isEmpty(melosysEessiMelding.getYtterligereInformasjon());
    }

    private Behandling hentBehandling(Long gsakSaksnummer) throws TekniskException {
        Fagsak fagsak = fagsakService.hentFagsakFraGsakSaksnummer(gsakSaksnummer)
            .orElseThrow(() -> new TekniskException("Finner ikke fagsak fra gsakSaksnummer " + gsakSaksnummer));

        return fagsak.getAktivBehandling();
    }

    @Override
    public boolean gjelderSedType(SedType sedType) {
        return sedType == SedType.A011
            || sedType == SedType.A002;
    }
}
