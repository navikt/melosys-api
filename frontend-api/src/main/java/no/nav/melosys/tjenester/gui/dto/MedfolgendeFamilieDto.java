package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.familie.AvklarteMedfolgendeBarnFtrl;
import no.nav.melosys.domain.familie.AvklarteMedfolgendeEktefelleSamboer;
import no.nav.melosys.domain.familie.IkkeOmfattetBarnFtrl;
import no.nav.melosys.domain.familie.IkkeOmfattetEktefelleSamboer;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class MedfolgendeFamilieDto {
    private AvklarteMedfolgendeBarnFtrl avklarteMedfolgendeBarnFtrl;
    private AvklarteMedfolgendeEktefelleSamboer avklarteMedfolgendeEktefelleSamboer;

    public MedfolgendeFamilieDto(AvklarteMedfolgendeBarnFtrl avklarteMedfolgendeBarnFtrl, AvklarteMedfolgendeEktefelleSamboer avklarteMedfolgendeEktefelleSamboer) {
        this.avklarteMedfolgendeBarnFtrl = avklarteMedfolgendeBarnFtrl;
        this.avklarteMedfolgendeEktefelleSamboer = avklarteMedfolgendeEktefelleSamboer;
    }

    public AvklarteMedfolgendeBarnFtrl getAvklarteMedfolgendeBarnFtrl() {
        return avklarteMedfolgendeBarnFtrl;
    }

    public AvklarteMedfolgendeEktefelleSamboer getAvklarteMedfolgendeEktefelleSamboer() {
        return avklarteMedfolgendeEktefelleSamboer;
    }

    public void setAvklarteMedfolgendeBarnFtrl(AvklarteMedfolgendeBarnFtrl avklarteMedfolgendeBarnFtrl) {
        this.avklarteMedfolgendeBarnFtrl = avklarteMedfolgendeBarnFtrl;
    }

    public void setAvklarteMedfolgendeEktefelleSamboer(AvklarteMedfolgendeEktefelleSamboer avklarteMedfolgendeEktefelleSamboer) {
        this.avklarteMedfolgendeEktefelleSamboer = avklarteMedfolgendeEktefelleSamboer;
    }

    public static MedfolgendeFamilieDto tilMedfolgendeFamilieDto(Set<AvklartefaktaDto> avklartefaktas) {
        AvklarteMedfolgendeBarnFtrl avklarteMedfolgendeBarnFtrl = new AvklarteMedfolgendeBarnFtrl(new HashSet<>(), new HashSet<>());
        AvklarteMedfolgendeEktefelleSamboer avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeEktefelleSamboer(new HashSet<>(), new HashSet<>());

        for (AvklartefaktaDto avklartefakta : avklartefaktas) {
            if (VURDERING_LOVVALG_BARN.getKode().equals(avklartefakta.getReferanse()) && VURDERING_LOVVALG_BARN.equals(avklartefakta.getAvklartefaktaType())) {

                if ("TRUE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeBarnFtrl.barnOmfattetAvNorskTrygd.add(new OmfattetFamilie(avklartefakta.getSubjektID()));
                }
                else if ("FALSE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeBarnFtrl.barnIkkeOmfattetAvNorskTrygd.add(new IkkeOmfattetBarnFtrl(avklartefakta.getSubjektID(), avklartefakta.getBegrunnelseKoder().get(0), avklartefakta.getBegrunnelseFritekst()));
                }

            } else if (VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode().equals(avklartefakta.getReferanse()) && VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.equals(avklartefakta.getAvklartefaktaType())) {

                if ("TRUE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeEktefelleSamboer.ektefelleSamboerOmfattetAvNorskTrygd.add(new OmfattetFamilie(avklartefakta.getSubjektID()));
                }
                else if ("FALSE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeEktefelleSamboer.ektefelleSamboerIkkeOmfattetAvNorskTrygd.add(new IkkeOmfattetEktefelleSamboer(avklartefakta.getSubjektID(), avklartefakta.getBegrunnelseKoder().get(0), avklartefakta.getBegrunnelseFritekst()));
                }
            }
        }

        return new MedfolgendeFamilieDto(avklarteMedfolgendeBarnFtrl, avklarteMedfolgendeEktefelleSamboer);
    }
}
