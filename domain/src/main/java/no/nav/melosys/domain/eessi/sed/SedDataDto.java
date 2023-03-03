package no.nav.melosys.domain.eessi.sed;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;

public class SedDataDto extends SedGrunnlagDto {
    //Persondok.
    private Bruker bruker;
    private Adresse kontaktadresse;
    private Adresse oppholdsadresse;
    private List<FamilieMedlem> familieMedlem = new ArrayList<>();
    private Periode søknadsperiode;

    //Videresending av søknad
    private String avklartBostedsland;

    private Long gsakSaksnummer;

    //Lovvalg
    private List<Lovvalgsperiode> tidligereLovvalgsperioder = new ArrayList<>();

    private List<String> mottakerIder;

    private SvarAnmodningUnntak svarAnmodningUnntak;

    private UtpekingAvvisDto utpekingAvvis;

    private VedtakDto vedtakDto;

    private InvalideringSedDto invalideringSedDto;

    public List<FamilieMedlem> getFamilieMedlem() {
        return familieMedlem;
    }

    public void setFamilieMedlem(List<FamilieMedlem> familieMedlem) {
        this.familieMedlem = familieMedlem;
    }

    public Periode getSøknadsperiode() {
        return søknadsperiode;
    }

    public void setSøknadsperiode(Periode søknadsperiode) {
        this.søknadsperiode = søknadsperiode;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public void setBruker(Bruker bruker) {
        this.bruker = bruker;
    }

    public String getAvklartBostedsland() {
        return avklartBostedsland;
    }

    public void setAvklartBostedsland(String avklartBostedsland) {
        this.avklartBostedsland = avklartBostedsland;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public List<Lovvalgsperiode> getTidligereLovvalgsperioder() {
        return tidligereLovvalgsperioder;
    }

    public void setTidligereLovvalgsperioder(List<Lovvalgsperiode> tidligereLovvalgsperioder) {
        this.tidligereLovvalgsperioder = tidligereLovvalgsperioder;
    }

    public List<String> getMottakerIder() {
        return mottakerIder;
    }

    public void setMottakerIder(List<String> mottakerIder) {
        this.mottakerIder = mottakerIder;
    }

    public SvarAnmodningUnntak getSvarAnmodningUnntak() {
        return svarAnmodningUnntak;
    }

    public void setSvarAnmodningUnntak(SvarAnmodningUnntak svarAnmodningUnntak) {
        this.svarAnmodningUnntak = svarAnmodningUnntak;
    }

    public UtpekingAvvisDto getUtpekingAvvis() {
        return utpekingAvvis;
    }

    public void setUtpekingAvvis(UtpekingAvvisDto utpekingAvvis) {
        this.utpekingAvvis = utpekingAvvis;
    }

    public VedtakDto getVedtakDto() {
        return vedtakDto;
    }

    public void setVedtakDto(VedtakDto vedtakDto) {
        this.vedtakDto = vedtakDto;
    }

    public Adresse getKontaktadresse() {
        return kontaktadresse;
    }

    public void setKontaktadresse(Adresse kontaktadresse) {
        this.kontaktadresse = kontaktadresse;
    }

    public Adresse getOppholdsadresse() {
        return oppholdsadresse;
    }

    public void setOppholdsadresse(Adresse oppholdsadresse) {
        this.oppholdsadresse = oppholdsadresse;
    }

    public InvalideringSedDto getInvalideringSedDto() {
        return invalideringSedDto;
    }

    public void setInvalideringSedDto(InvalideringSedDto invalideringSedDto) {
        this.invalideringSedDto = invalideringSedDto;
    }
}
