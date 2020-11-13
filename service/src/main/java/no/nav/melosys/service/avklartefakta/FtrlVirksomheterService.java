package no.nav.melosys.service.avklartefakta;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VIRKSOMHET;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;

@Service
public class FtrlVirksomheterService {

    private final BehandlingsgrunnlagService behandlingsgrunnlagService;
    private final SaksopplysningerService saksopplysningerService;
    private final AvklartefaktaService avklartefaktaService;

    public FtrlVirksomheterService(BehandlingsgrunnlagService behandlingsgrunnlagService, SaksopplysningerService saksopplysningerService, AvklartefaktaService avklartefaktaService) {
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
        this.saksopplysningerService = saksopplysningerService;
        this.avklartefaktaService = avklartefaktaService;
    }

    public void lagreVirksomheterSomAvklartefakta(FtrlVirksomheterDto virksomheter, Long behandlingID) throws IkkeFunnetException {
        for (String orgnr : virksomheter.getOrgnummer()) {
            avklartefaktaService.leggTilAvklarteFakta(behandlingID, VIRKSOMHET, VIRKSOMHET.getKode(), orgnr, "TRUE");
        }
    }

    public boolean erVirksomhetValid(FtrlVirksomheterDto virksomheter, Long behandlingID) throws FunksjonellException {
        for (String orgnr : virksomheter.getOrgnummer()) {
            if (!erOrgnrValid(orgnr, behandlingID)) {
                throw new FunksjonellException("Orgnr " + orgnr + " hører ikke til noen av arbeidsforholdene");
            }
        }
        return true;
    }

    private boolean erOrgnrValid(String orgnr, Long behandlingID) throws IkkeFunnetException {
        BehandlingsgrunnlagData behandlingsgrunnlag = behandlingsgrunnlagService.hentBehandlingsgrunnlag(behandlingID).getBehandlingsgrunnlagdata();
        return erVirksomhetForetakUtland(behandlingsgrunnlag, orgnr)
            || erVirksomhetSelvstendigForetak(behandlingsgrunnlag, orgnr)
            || erVirksomhetArbeidUtland(behandlingsgrunnlag, orgnr)
            || erVirksomhetArbeidNorge(behandlingID, orgnr);
    }

    private boolean erVirksomhetForetakUtland(BehandlingsgrunnlagData behandlingsgrunnlagData, String uuid) {
        return behandlingsgrunnlagData.foretakUtland.stream().anyMatch(foretakUtland -> uuid.equals(foretakUtland.uuid));
    }

    private boolean erVirksomhetSelvstendigForetak(BehandlingsgrunnlagData behandlingsgrunnlagData, String orgnr) {
        return behandlingsgrunnlagData.selvstendigArbeid.selvstendigForetak.stream().anyMatch(selvstendigForetak -> orgnr.equals(selvstendigForetak.orgnr));
    }

    private boolean erVirksomhetArbeidUtland(BehandlingsgrunnlagData behandlingsgrunnlagData, String orgnr) {
        return behandlingsgrunnlagData.arbeidUtland.stream().anyMatch(arbeidUtland -> orgnr.equals(arbeidUtland.foretakOrgnr));
    }

    private boolean erVirksomhetArbeidNorge(Long behandlingID, String orgnr) {
        return saksopplysningerService.finnOrganisasjonsopplysninger(behandlingID).stream().anyMatch(organisasjonDokument -> orgnr.equals(organisasjonDokument.organisasjonDetaljer.orgnummer));
    }

    public FtrlVirksomheterDto tilFtrlVirksomheterDto(Set<Avklartefakta> avklartefaktas) {
        List<String> virksomheter = new ArrayList<>();
        avklartefaktas.stream()
            .filter(avklartefakta -> VIRKSOMHET.getKode().equals(avklartefakta.getReferanse()) && VIRKSOMHET.equals(avklartefakta.getType()))
            .forEach(avklartefakta -> virksomheter.add(avklartefakta.getSubjekt()));

        FtrlVirksomheterDto ftrlVirksomheterDto = new FtrlVirksomheterDto();
        ftrlVirksomheterDto.setOrgnummer(virksomheter);
        return ftrlVirksomheterDto;
    }

}
