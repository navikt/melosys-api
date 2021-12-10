package no.nav.melosys.service.dokument;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.behandlingsgrunnlag.SoeknadTrygdeavtale;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.RepresentantIUtlandet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;

import java.util.List;

import static no.nav.melosys.service.dokument.DokgenMalMapperTest.LOVVALGSPERIODE_FOM;
import static no.nav.melosys.service.dokument.DokgenMalMapperTest.LOVVALGSPERIODE_TOM;
import static no.nav.melosys.service.dokument.DokgenTestData.lagBehandling;
import static no.nav.melosys.service.dokument.DokgenTestData.lagFagsak;

public final class DokgenTrygdeavtaleTestData  {

    public static Behandling lagTrygdeavtaleBehandling() {
        RepresentantIUtlandet representantIUtlandet = new RepresentantIUtlandet();
        representantIUtlandet.representantNavn = "Foretaksnavn";
        representantIUtlandet.adresselinjer = List.of("Uk address");
        representantIUtlandet.representantLand = Landkoder.GB.getKode();
        return lagTrygdeavtaleBehandling(representantIUtlandet);
    }

    public static Behandling lagTrygdeavtaleBehandling(RepresentantIUtlandet representantIUtlandet) {
        Behandling behandling = lagBehandling(lagFagsak());
        SoeknadTrygdeavtale behandlingsgrunnlagdata = lagTrygdeavtaleBehandlingsgrunnlagdata(representantIUtlandet);
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);
        return behandling;
    }

    private static SoeknadTrygdeavtale lagTrygdeavtaleBehandlingsgrunnlagdata(RepresentantIUtlandet representantIUtlandet) {
        var behandlingsgrunnlagData = new SoeknadTrygdeavtale();
        behandlingsgrunnlagData.setRepresentantIUtlandet(representantIUtlandet);
        return behandlingsgrunnlagData;
    }

    public static Lovvalgsperiode lagLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(LOVVALGSPERIODE_FOM);
        lovvalgsperiode.setTom(LOVVALGSPERIODE_TOM);
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_FTRL);
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1);
        return lovvalgsperiode;
    }

}
