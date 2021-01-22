package no.nav.melosys.tjenester.gui.dto;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

import java.util.HashSet;
import java.util.Set;

import no.nav.melosys.domain.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.familie.OmfattetFamilie;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;

public class MedfolgendeFamilieDto {
    private AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn;
    private AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer;

    public MedfolgendeFamilieDto(AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn, AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer) {
        this.avklarteMedfolgendeBarn = avklarteMedfolgendeBarn;
        this.avklarteMedfolgendeEktefelleSamboer = avklarteMedfolgendeEktefelleSamboer;
    }

    public AvklarteMedfolgendeFamilie getAvklarteMedfolgendeBarn() {
        return avklarteMedfolgendeBarn;
    }

    public AvklarteMedfolgendeFamilie getAvklarteMedfolgendeEktefelleSamboer() {
        return avklarteMedfolgendeEktefelleSamboer;
    }

    public void setAvklarteMedfolgendeBarn(AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn) {
        this.avklarteMedfolgendeBarn = avklarteMedfolgendeBarn;
    }

    public void setAvklarteMedfolgendeEktefelleSamboer(AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer) {
        this.avklarteMedfolgendeEktefelleSamboer = avklarteMedfolgendeEktefelleSamboer;
    }

    public static MedfolgendeFamilieDto tilMedfolgendeFamilieDto(Set<AvklartefaktaDto> avklartefaktas) {
        AvklarteMedfolgendeFamilie avklarteMedfolgendeBarn = new AvklarteMedfolgendeFamilie(new HashSet<>(), new HashSet<>());
        AvklarteMedfolgendeFamilie avklarteMedfolgendeEktefelleSamboer = new AvklarteMedfolgendeFamilie(new HashSet<>(), new HashSet<>());

        for (AvklartefaktaDto avklartefakta : avklartefaktas) {
            if (VURDERING_LOVVALG_BARN.getKode().equals(avklartefakta.getReferanse()) && VURDERING_LOVVALG_BARN.equals(avklartefakta.getAvklartefaktaType())) {

                if ("TRUE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeBarn.getFamilieOmfattetAvNorskTrygd().add(new OmfattetFamilie(avklartefakta.getSubjektID()));
                }
                else if ("FALSE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeBarn.getFamilieIkkeOmfattetAvNorskTrygd().add(new IkkeOmfattetFamilie(avklartefakta.getSubjektID(), Medfolgende_barn_begrunnelser_ftrl.valueOf(avklartefakta.getBegrunnelseKoder().get(0)), avklartefakta.getBegrunnelseFritekst()));
                }

            } else if (VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.getKode().equals(avklartefakta.getReferanse()) && VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER.equals(avklartefakta.getAvklartefaktaType())) {

                if ("TRUE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeEktefelleSamboer.getFamilieOmfattetAvNorskTrygd().add(new OmfattetFamilie(avklartefakta.getSubjektID()));
                }
                else if ("FALSE".equals(avklartefakta.getFakta().get(0))) {
                    avklarteMedfolgendeEktefelleSamboer.getFamilieIkkeOmfattetAvNorskTrygd().add(new IkkeOmfattetFamilie(avklartefakta.getSubjektID(), Medfolgende_ektefelle_samboer_begrunnelser_ftrl.valueOf(avklartefakta.getBegrunnelseKoder().get(0)), avklartefakta.getBegrunnelseFritekst()));
                }
            }
        }

        return new MedfolgendeFamilieDto(avklarteMedfolgendeBarn, avklarteMedfolgendeEktefelleSamboer);
    }
}
