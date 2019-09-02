package no.nav.melosys.service.dokument.brev.mapper.felles;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.*;
import no.nav.melosys.exception.TekniskException;

public final class VilkaarbegrunnelseFactory {

    private static final String JA = "true";
    private static final String IKKE_STØTTET = "støttes ikke.";

    private VilkaarbegrunnelseFactory() {
        throw new IllegalStateException("Utility");
    }

    public static Art121BegrunnelseType mapArt121BegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art121BegrunnelseType art121BegrunnelseType = lagArt121BegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_1_begrunnelser artikkel12_1 = Art12_1_begrunnelser.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel12_1) {
                case UTSENDELSE_OVER_24_MN:
                    art121BegrunnelseType.setUtsendelseOver24Mn(JA);
                    break;
                case ERSTATTER_ANNEN:
                    art121BegrunnelseType.setErstatterAnnen(JA);
                    break;
                case IKKE_UTSENDT_PAA_OPPDRAG_FOR_AG:
                    art121BegrunnelseType.setIkkeUtsendtPåOppdragForAg(JA);
                    break;
                case IKKE_OMFATTET_LENGE_NOK_I_NORGE_FOER:
                    art121BegrunnelseType.setIkkeOmfattetLengeNokINorgeFør(JA);
                    break;
                case UNDER_2_MN_SIDEN_FORRIGE_UTSENDING_TIL_SAMME_LAND:
                    art121BegrunnelseType.setUnder2MnSidenForrigeUtsendingTilSammeLand(JA);
                    break;
                case IKKE_VESENTLIG_VIRKSOMHET:
                    art121BegrunnelseType.setIkkeVesentligVirksomhet(JA);
                    break;
                default:
                    throw new TekniskException(artikkel12_1 + IKKE_STØTTET);
            }
        }
        return art121BegrunnelseType;
    }

    private static Art121BegrunnelseType lagArt121BegrunnelseType() {
        Art121BegrunnelseType art121BegrunnelseType = new Art121BegrunnelseType();
        art121BegrunnelseType.setUtsendelseOver24Mn("");
        art121BegrunnelseType.setErstatterAnnen("");
        art121BegrunnelseType.setIkkeUtsendtPåOppdragForAg("");
        art121BegrunnelseType.setIkkeOmfattetLengeNokINorgeFør("");
        art121BegrunnelseType.setUnder2MnSidenForrigeUtsendingTilSammeLand("");
        art121BegrunnelseType.setIkkeVesentligVirksomhet("");
        return art121BegrunnelseType;
    }

    public static Art121ForutgaaendeBegrunnelseType mapArt121ForutgaaendeBegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = lagArt121ForutgaaendeBegrunnelseType();

        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_1_forutgaaende_medl forutgaaendeMedlemskap = Art12_1_forutgaaende_medl.valueOf(vilkaarBegrunnelse.getKode());
            switch (forutgaaendeMedlemskap) {
                case UNNTATT_MEDLEMSKAP:
                    art121ForutgaaendeBegrunnelseType.setUntattMedlemskap(JA);
                    break;
                case FOLKEREGISTRERT_IKKE_ARBEIDET_I_NORGE:
                    art121ForutgaaendeBegrunnelseType.setFolkeregistrertIkkeArbeidetINorge(JA);
                    break;
                case IKKE_FOLKEREGISTRERT_ELLER_ARBEIDET_I_NORGE:
                    art121ForutgaaendeBegrunnelseType.setIkkeFolkeregistrertEllerArbeidetINorge(JA);
                    break;
                default:
                    throw new TekniskException(forutgaaendeMedlemskap + IKKE_STØTTET);
            }
        }
        return art121ForutgaaendeBegrunnelseType;
    }

    private static Art121ForutgaaendeBegrunnelseType lagArt121ForutgaaendeBegrunnelseType() {
        Art121ForutgaaendeBegrunnelseType art121ForutgaaendeBegrunnelseType = new Art121ForutgaaendeBegrunnelseType();
        art121ForutgaaendeBegrunnelseType.setUntattMedlemskap("");
        art121ForutgaaendeBegrunnelseType.setFolkeregistrertIkkeArbeidetINorge("");
        art121ForutgaaendeBegrunnelseType.setIkkeFolkeregistrertEllerArbeidetINorge("");
        return art121ForutgaaendeBegrunnelseType;
    }

    public static Art121VesentligVirksomhetBegrunnelse mapArt121VesentligVirksomhetBegrunnelse(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art121VesentligVirksomhetBegrunnelse brevBegrunnelse = lagArt121VesentligVirksomhetBegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_1_vesentlig_virksomhet vesentligVirksomhetBegrunnelse = Art12_1_vesentlig_virksomhet.valueOf(vilkaarBegrunnelse.getKode());
            switch (vesentligVirksomhetBegrunnelse) {
                case FOR_LITE_KONTRAKTER_NORGE:
                    brevBegrunnelse.setForLiteKontrakterNorge(JA);
                    break;
                case FOR_LITE_OMSETNING_NORGE:
                    brevBegrunnelse.setForLiteOmsetningNorge(JA);
                    break;
                case FOR_LITE_OPPDRAG_NORGE:
                    brevBegrunnelse.setForLiteOppdragNorge(JA);
                    break;
                case FOR_MANGE_ADMIN_ANSATTE:
                    brevBegrunnelse.setForMangeAdminAnsatte(JA);
                    break;
                case REKRUTTERER_ANSATTE_UTL:
                    brevBegrunnelse.setRekruttererAnsatteUtl(JA);
                    break;
                case KUN_ADMIN_ANSATTE:
                    brevBegrunnelse.setKunAdminAnsatte(JA);
                    break;
                case KONTRAKTER_IKKE_NORSK_LOV:
                    brevBegrunnelse.setKontrakterIkkeNorskLov(JA);
                    break;
                default:
                    throw new TekniskException(vesentligVirksomhetBegrunnelse + IKKE_STØTTET);
            }
        }
        return brevBegrunnelse;
    }

    private static Art121VesentligVirksomhetBegrunnelse lagArt121VesentligVirksomhetBegrunnelseType() {
        Art121VesentligVirksomhetBegrunnelse brevBegrunnelse = new Art121VesentligVirksomhetBegrunnelse();
        brevBegrunnelse.setForLiteKontrakterNorge("");
        brevBegrunnelse.setForLiteOmsetningNorge("");
        brevBegrunnelse.setForLiteOppdragNorge("");
        brevBegrunnelse.setForMangeAdminAnsatte("");
        brevBegrunnelse.setKontrakterIkkeNorskLov("");
        brevBegrunnelse.setKunAdminAnsatte("");
        brevBegrunnelse.setRekruttererAnsatteUtl("");
        return brevBegrunnelse;
    }

    public static Art122BegrunnelseType mapArt122BegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art122BegrunnelseType art122BegrunnelseType = lagArt122BegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Art12_2_begrunnelser artikkel12_2 = Art12_2_begrunnelser.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel12_2) {
                case UTSENDELSE_OVER_24_MN:
                    art122BegrunnelseType.setUtsendelseOver24Mn(JA);
                    break;
                case IKKE_LIGNENDE_VIRKSOMHET:
                    art122BegrunnelseType.setIkkeLignendeVirksomhet(JA);
                    break;
                case NORMALT_IKKE_DRIFT_NORGE:
                    art122BegrunnelseType.setNormaltIkkeDriftINorge(JA);
                    break;
                default:
                    throw new TekniskException(artikkel12_2 + IKKE_STØTTET);
            }
        }
        return art122BegrunnelseType;
    }

    private static Art122BegrunnelseType lagArt122BegrunnelseType() {
        Art122BegrunnelseType art122BegrunnelseType = new Art122BegrunnelseType();
        art122BegrunnelseType.setUtsendelseOver24Mn("");
        art122BegrunnelseType.setIkkeLignendeVirksomhet("");
        art122BegrunnelseType.setNormaltIkkeDriftINorge("");
        return art122BegrunnelseType;
    }

    public static Art122NormalVirksomhetBegrunnelseType mapArt122NormalVirksomhetBegrunnelseType(Set<VilkaarBegrunnelse> begrunnelser) throws TekniskException {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = lagArt122NormalVirksomhetBegrunnelseType();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : begrunnelser) {
            Normaltdrivervirksomhet normaltDriverVirksomhet = Normaltdrivervirksomhet.valueOf(vilkaarBegrunnelse.getKode());
            switch (normaltDriverVirksomhet) {
                case IKKE_FORUTGAAENDE_DRIFT:
                    art122NormalVirksomhetBegrunnelseType.setIkkeForutgåendeDrift(JA);
                    break;
                case HAR_IKKE_NØDVENDIG_INFRASTRUKTUR:
                    art122NormalVirksomhetBegrunnelseType.setHarIkkeNødvendigInfrastruktur(JA);
                    break;
                case OPPRETTHOLDER_IKKE_LISENSER_AUTORISASJON:
                    art122NormalVirksomhetBegrunnelseType.setOpprettholderIkkeLisenserAutorisasjon(JA);
                    break;
                default:
                    throw new TekniskException(normaltDriverVirksomhet + IKKE_STØTTET);
            }
        }
        return art122NormalVirksomhetBegrunnelseType;
    }

    private static Art122NormalVirksomhetBegrunnelseType lagArt122NormalVirksomhetBegrunnelseType() {
        Art122NormalVirksomhetBegrunnelseType art122NormalVirksomhetBegrunnelseType = new Art122NormalVirksomhetBegrunnelseType();
        art122NormalVirksomhetBegrunnelseType.setIkkeForutgåendeDrift("");
        art122NormalVirksomhetBegrunnelseType.setHarIkkeNødvendigInfrastruktur("");
        art122NormalVirksomhetBegrunnelseType.setOpprettholderIkkeLisenserAutorisasjon("");
        return art122NormalVirksomhetBegrunnelseType;
    }

    public static Set<VilkaarBegrunnelse> hentVilkaarbegrunnelser(Behandlingsresultat resultat, Vilkaar vilkaarType) {
        return resultat.getVilkaarsresultater().stream()
            .filter(vr -> vr.getVilkaar() == vilkaarType)
            .flatMap(vr -> vr.getBegrunnelser().stream())
            .collect(Collectors.toSet());
    }
}
