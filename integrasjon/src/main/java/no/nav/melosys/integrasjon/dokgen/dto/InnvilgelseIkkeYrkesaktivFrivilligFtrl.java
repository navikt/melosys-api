package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.nav.melosys.domain.brev.IkkeYrkesaktivFrivilligFtrlBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.MedlemskapsperiodeDto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class InnvilgelseIkkeYrkesaktivFrivilligFtrl extends DokgenDto {

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;
    private final String sakstype;
    private final String behandlingstype;
    private final boolean flereLandUkjentHvilke;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<String> land;
    private final String trygdedekning;
    private final String bestemmelse;
    private final String nyVurderingBakgrunn;
    private final String innledningFritekst;
    private final String begrunnelseFritekst;
    private final boolean avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
    private final boolean avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<MedlemskapsperiodeDto> medlemskapsperioder;
    private final String ikkeYrkesaktivRelasjonType;

    protected InnvilgelseIkkeYrkesaktivFrivilligFtrl(IkkeYrkesaktivFrivilligFtrlBrevbestilling brevbestilling, List<MedlemskapsperiodeDto> medlemskapsperioder) {
        super(brevbestilling, Mottakerroller.BRUKER);
        var fagsak = brevbestilling.getBehandling().getFagsak();

        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
        this.sakstype = fagsak.getType().getKode();
        this.behandlingstype = fagsak.hentSistOppdatertBehandling().getType().getKode();
        this.flereLandUkjentHvilke = brevbestilling.getFlereLandUkjentHvilke();
        this.land = brevbestilling.getLand();
        this.trygdedekning = brevbestilling.getTrygdedekning();
        this.bestemmelse = brevbestilling.getBestemmelse();
        this.nyVurderingBakgrunn = brevbestilling.getNyVurderingBakgrunn();
        this.innledningFritekst = brevbestilling.getInnledningFritekst();
        this.begrunnelseFritekst = brevbestilling.getBegrunnelseFritekst();
        this.ikkeYrkesaktivRelasjonType = brevbestilling.getIkkeYrkesaktivRelasjonType();
        this.avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = brevbestilling.isAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel();
        this.avslåttMedlemskapsperiodeFørMottaksdatoFullDekning = brevbestilling.isAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning();
        this.medlemskapsperioder = medlemskapsperioder;
    }

    public LocalDate getDatoMottatt() {
        return datoMottatt;
    }

    public String getSakstype() {
        return sakstype;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public boolean getFlereLandUkjentHvilke() {
        return flereLandUkjentHvilke;
    }

    public String getTrygdedekning() {
        return trygdedekning;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public List<String> getLand() {
        return land;
    }

    public String getNyVurderingBakgrunn() {
        return nyVurderingBakgrunn;
    }

    public String getInnledningFritekst() {
        return innledningFritekst;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public String getIkkeYrkesaktivRelasjonType() {
        return ikkeYrkesaktivRelasjonType;
    }

    public boolean isAvslåttMedlemskapsperiodeFørMottaksdatoHelsedel() {
        return avslåttMedlemskapsperiodeFørMottaksdatoHelsedel;
    }

    public boolean isAvslåttMedlemskapsperiodeFørMottaksdatoFullDekning() {
        return avslåttMedlemskapsperiodeFørMottaksdatoFullDekning;
    }

    public List<MedlemskapsperiodeDto> getMedlemskapsperioder() {
        return medlemskapsperioder;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }


    public static InnvilgelseIkkeYrkesaktivFrivilligFtrl av(IkkeYrkesaktivFrivilligFtrlBrevbestilling brevbestilling, List<MedlemskapsperiodeDto> medlemskapsperioder) {
        return new InnvilgelseIkkeYrkesaktivFrivilligFtrl(brevbestilling, medlemskapsperioder);
    }
}
