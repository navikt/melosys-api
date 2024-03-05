package no.nav.melosys.integrasjon.dokgen.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.melosys.domain.brev.IkkeYrkesaktivPliktigFtrlBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.integrasjon.dokgen.dto.felles.SaksinfoBruker;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public class InnvilgelseIkkeYrkesaktivPliktigFtrl extends DokgenDto {

    @JsonFormat(shape = STRING)
    private final LocalDate datoMottatt;
    private final String sakstype;
    private final String behandlingstype;
    private final boolean flereLandUkjentHvilke;
    private final List<String> land;
    private final String bestemmelse;
    private final String nyVurderingBakgrunn;
    private final String innledningFritekst;
    private final String begrunnelseFritekst;
    private final String ikkeYrkesaktivOppholdType;
    private final String ikkeYrkesaktivRelasjonType;
    private final Periode medlemskapsperiode;

    protected InnvilgelseIkkeYrkesaktivPliktigFtrl(IkkeYrkesaktivPliktigFtrlBrevbestilling brevbestilling) {
        super(brevbestilling, Mottakerroller.BRUKER);
        var fagsak = brevbestilling.getBehandling().getFagsak();

        this.datoMottatt = instantTilLocalDate(brevbestilling.getForsendelseMottatt());
        this.sakstype = fagsak.getType().getKode();
        this.behandlingstype = fagsak.hentSistOppdatertBehandling().getType().getKode();
        this.flereLandUkjentHvilke = brevbestilling.getFlereLandUkjentHvilke();
        this.land = brevbestilling.getLand();
        this.medlemskapsperiode = brevbestilling.getMedlemskapsperiode();
        this.bestemmelse = brevbestilling.getBestemmelse();
        this.nyVurderingBakgrunn = brevbestilling.getNyVurderingBakgrunn();
        this.innledningFritekst = brevbestilling.getInnledningFritekst();
        this.begrunnelseFritekst = brevbestilling.getBegrunnelseFritekst();
        this.ikkeYrkesaktivOppholdType = brevbestilling.getIkkeYrkesaktivOppholdType();
        this.ikkeYrkesaktivRelasjonType = brevbestilling.getIkkeYrkesaktivRelasjonType();
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

    public List<String> getLand() {
        return land;
    }

    public String getBestemmelse() {
        return bestemmelse;
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

    public String getIkkeYrkesaktivOppholdType() {
        return ikkeYrkesaktivOppholdType;
    }

    public String getIkkeYrkesaktivRelasjonType() {
        return ikkeYrkesaktivRelasjonType;
    }

    public Periode getMedlemskapsperiode() {
        return medlemskapsperiode;
    }

    @Override
    public SaksinfoBruker getSaksinfo() {
        return (SaksinfoBruker) super.getSaksinfo();
    }


    public static InnvilgelseIkkeYrkesaktivPliktigFtrl av(IkkeYrkesaktivPliktigFtrlBrevbestilling brevbestilling) {
        return new InnvilgelseIkkeYrkesaktivPliktigFtrl(brevbestilling);
    }
}
