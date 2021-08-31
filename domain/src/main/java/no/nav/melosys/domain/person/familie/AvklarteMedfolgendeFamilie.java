package no.nav.melosys.domain.person.familie;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering;
import no.nav.melosys.domain.behandlingsgrunnlag.data.MedfolgendeFamilie;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;

import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_LOVVALG_BARN;
import static no.nav.melosys.domain.kodeverk.Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;

public class AvklarteMedfolgendeFamilie {
    private final Set<OmfattetFamilie> familieOmfattetAvNorskTrygd;
    private final Set<IkkeOmfattetFamilie> familieIkkeOmfattetAvNorskTrygd;

    public AvklarteMedfolgendeFamilie(Set<OmfattetFamilie> familieOmfattetAvNorskTrygd, Set<IkkeOmfattetFamilie> familieIkkeOmfattetAvNorskTrygd) {
        this.familieOmfattetAvNorskTrygd = familieOmfattetAvNorskTrygd;
        this.familieIkkeOmfattetAvNorskTrygd = familieIkkeOmfattetAvNorskTrygd;
    }

    public Set<OmfattetFamilie> getFamilieOmfattetAvNorskTrygd() {
        return familieOmfattetAvNorskTrygd;
    }

    public Set<IkkeOmfattetFamilie> getFamilieIkkeOmfattetAvNorskTrygd() {
        return familieIkkeOmfattetAvNorskTrygd;
    }

    public Collection<Avklartefakta> tilAvklartefakta(Map<String, MedfolgendeFamilie.Relasjonsrolle> uuidOgRolle) {
        Set<Avklartefakta> avklartefakta = new HashSet<>();
        getFamilieOmfattetAvNorskTrygd().forEach(omfattet ->
            avklartefakta.add(lagAvklarteFakta(omfattet.getUuid(), tilAvklartefaktaTyper(uuidOgRolle.get(omfattet.getUuid())))));
        getFamilieIkkeOmfattetAvNorskTrygd().forEach(ikkeOmfattet ->
            avklartefakta.add(lagAvklarteFakta(ikkeOmfattet.getUuid(), tilAvklartefaktaTyper(uuidOgRolle.get(ikkeOmfattet.getUuid())), ikkeOmfattet.getBegrunnelse(), ikkeOmfattet.getBegrunnelseFritekst())));

        return avklartefakta;
    }

    private Avklartefakta lagAvklarteFakta(String subjekt, Avklartefaktatyper type) {
        var avklartefakta = new Avklartefakta();
        avklartefakta.setReferanse(type.getKode());
        avklartefakta.setType(type);
        avklartefakta.setFakta(Avklartefakta.VALGT_FAKTA);
        avklartefakta.setSubjekt(subjekt);

        return avklartefakta;
    }

    private Avklartefakta lagAvklarteFakta(String subjekt, Avklartefaktatyper type, String begrunnelseKode, String begrunnelseFritekst) {
        var avklartefakta = new Avklartefakta();
        avklartefakta.setReferanse(type.getKode());
        avklartefakta.setType(type);
        avklartefakta.setFakta(Avklartefakta.IKKE_VALGT_FAKTA);
        avklartefakta.setSubjekt(subjekt);
        avklartefakta.setBegrunnelseFritekst(begrunnelseFritekst);

        var registrering = new AvklartefaktaRegistrering();
        registrering.setBegrunnelseKode(begrunnelseKode);
        registrering.setAvklartefakta(avklartefakta);

        avklartefakta.getRegistreringer().add(registrering);

        return avklartefakta;
    }

    private Avklartefaktatyper tilAvklartefaktaTyper(MedfolgendeFamilie.Relasjonsrolle relasjonsrolle) {
        return MedfolgendeFamilie.Relasjonsrolle.BARN.equals(relasjonsrolle) ? VURDERING_LOVVALG_BARN : VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER;
    }

    public boolean finnes() {
        return !(familieOmfattetAvNorskTrygd.isEmpty() && familieIkkeOmfattetAvNorskTrygd.isEmpty());
    }
}
