package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeBarn;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.*;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static no.nav.melosys.domain.avklartefakta.Avklartefakta.VALGT_FAKTA;

public class InnvilgelseFtrl extends DokgenDto {

    @JsonSerialize(using = InstantSerializer.class)
    @JsonFormat(shape = STRING)
    private final Instant datoMottatt;

    private final List<Periode> perioder;
    private final boolean erFullstendigInnvilget;
    private final String ftrl_2_8_begrunnelse;
    private final boolean vurderingMedlemskapEktefelle;
    private final boolean vurderingLovvalgBarn;
    private final List<FamilieInfo> omfattetFamilie;
    private final List<IkkeOmfattetBarn> ikkeOmfattetBarn;
    private final IkkeOmfattetEktefelle ikkeOmfattetEktefelle;
    private final String fritekstInnledning;
    private final String fritekstBegrunnelse;
    private final String fritekstEktefelle;
    private final String fritekstBarn;
    private final String saksbehandlerNavn;
    private final String arbeidsgiverNavn;
    private final String arbeidsland;
    private final boolean trygdeavtaleMedArbeidsland;
    private final VurderingTrygdeavgift vurderingTrygdeavgift;
    private final String loennsforhold;
    private final String arbeidsgiverFullmektigNavn;
    private final boolean brukerHarFullmektig;
    private final String avgiftssatsAar;
    private final boolean loennNorgeSkattepliktig;
    private final boolean loennUtlandSkattepliktig;

    public InnvilgelseFtrl(InnvilgelseBrevbestilling brevbestilling,
                           List<Periode> perioder,
                           boolean erFullstendigInnvilget,
                           String ftrl_2_8_begrunnelse,
                           boolean vurderingMedlemskapEktefelle,
                           boolean vurderingLovvalgBarn,
                           List<FamilieInfo> omfattetFamilie,
                           List<IkkeOmfattetBarn> ikkeOmfattetBarn,
                           IkkeOmfattetEktefelle ikkeOmfattetEktefelle,
                           String arbeidsgiverNavn,
                           String arbeidsland,
                           boolean trygdeavtaleMedArbeidsland,
                           VurderingTrygdeavgift vurderingTrygdeavgift,
                           String loennsforhold,
                           String arbeidsgiverFullmektigNavn,
                           boolean brukerHarFullmektig,
                           String avgiftssatsAar,
                           boolean loennNorgeSkattepliktig,
                           boolean loennUtlandSkattepliktig) {
        super(brevbestilling);
        this.datoMottatt = brevbestilling.getForsendelseMottatt();
        this.fritekstInnledning = brevbestilling.getInnledningFritekst();
        this.fritekstBegrunnelse = brevbestilling.getBegrunnelseFritekst();
        this.fritekstEktefelle = brevbestilling.getEktefelleFritekst();
        this.fritekstBarn = brevbestilling.getBarnFritekst();
        this.saksbehandlerNavn = brevbestilling.getSaksbehandlerNavn();
        this.perioder = perioder;
        this.erFullstendigInnvilget = erFullstendigInnvilget;
        this.ftrl_2_8_begrunnelse = ftrl_2_8_begrunnelse;
        this.vurderingMedlemskapEktefelle = vurderingMedlemskapEktefelle;
        this.vurderingLovvalgBarn = vurderingLovvalgBarn;
        this.omfattetFamilie = omfattetFamilie;
        this.ikkeOmfattetBarn = ikkeOmfattetBarn;
        this.ikkeOmfattetEktefelle = ikkeOmfattetEktefelle;
        this.arbeidsgiverNavn = arbeidsgiverNavn;
        this.arbeidsland = arbeidsland;
        this.trygdeavtaleMedArbeidsland = trygdeavtaleMedArbeidsland;
        this.vurderingTrygdeavgift = vurderingTrygdeavgift;
        this.loennsforhold = loennsforhold;
        this.arbeidsgiverFullmektigNavn = arbeidsgiverFullmektigNavn;
        this.brukerHarFullmektig = brukerHarFullmektig;
        this.avgiftssatsAar = avgiftssatsAar;
        this.loennNorgeSkattepliktig = loennNorgeSkattepliktig;
        this.loennUtlandSkattepliktig = loennUtlandSkattepliktig;
    }

    private AvklarteMedfolgendeBarn hentAvklartMedfølgendeBarn(Set<Avklartefakta> avklartefakta) {
        Set<OmfattetFamilie> barnOmfattetAvNorskTrygd = new HashSet<>();
        Set<no.nav.melosys.domain.person.familie.IkkeOmfattetBarn> barnIkkeOmfattetAvNorskTrygd = new HashSet<>();

        for (Avklartefakta a : avklartefakta) {
            if (a.getFakta().equals(VALGT_FAKTA)) {
                barnOmfattetAvNorskTrygd.add(new OmfattetFamilie(a.getSubjekt()));
            } else {
                String begrunnelse = a.getRegistreringer().iterator().next().getBegrunnelseKode();
                new no.nav.melosys.domain.person.familie.IkkeOmfattetBarn(a.getSubjekt(), begrunnelse, a.getBegrunnelseFritekst());
            }
        }
        return new AvklarteMedfolgendeBarn(barnOmfattetAvNorskTrygd, barnIkkeOmfattetAvNorskTrygd);
    }

    public Instant getDatoMottatt() {
        return datoMottatt;
    }

    public List<Periode> getPerioder() {
        return perioder;
    }

    public boolean isErFullstendigInnvilget() {
        return erFullstendigInnvilget;
    }

    public String getFtrl_2_8_begrunnelse() {
        return ftrl_2_8_begrunnelse;
    }

    public boolean isVurderingMedlemskapEktefelle() {
        return vurderingMedlemskapEktefelle;
    }

    public boolean isVurderingLovvalgBarn() {
        return vurderingLovvalgBarn;
    }

    public List<FamilieInfo> getOmfattetFamilie() {
        return omfattetFamilie;
    }

    public List<IkkeOmfattetBarn> getIkkeOmfattetBarn() {
        return ikkeOmfattetBarn;
    }

    public IkkeOmfattetEktefelle getIkkeOmfattetEktefelle() {
        return ikkeOmfattetEktefelle;
    }

    public String getFritekstInnledning() {
        return fritekstInnledning;
    }

    public String getFritekstBegrunnelse() {
        return fritekstBegrunnelse;
    }

    public String getFritekstEktefelle() {
        return fritekstEktefelle;
    }

    public String getFritekstBarn() {
        return fritekstBarn;
    }

    public String getSaksbehandlerNavn() {
        return saksbehandlerNavn;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public String getArbeidsland() {
        return arbeidsland;
    }

    public boolean isTrygdeavtaleMedArbeidsland() {
        return trygdeavtaleMedArbeidsland;
    }

    public VurderingTrygdeavgift getVurderingTrygdeavgift() {
        return vurderingTrygdeavgift;
    }

    public String getLoennsforhold() {
        return loennsforhold;
    }

    public String getArbeidsgiverFullmektigNavn() {
        return arbeidsgiverFullmektigNavn;
    }

    public boolean isBrukerHarFullmektig() {
        return brukerHarFullmektig;
    }

    public String getAvgiftssatsAar() {
        return avgiftssatsAar;
    }

    public boolean isLoennNorgeSkattepliktig() {
        return loennNorgeSkattepliktig;
    }

    public boolean isLoennUtlandSkattepliktig() {
        return loennUtlandSkattepliktig;
    }
}
