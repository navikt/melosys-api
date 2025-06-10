package no.nav.melosys.service.kontroll.feature.ferdigbehandling.data

import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.utkast.UtkastBrev
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.person.Persondata

data class FerdigbehandlingKontrollData(
    val medlemskapDokument: MedlemskapDokument? = null,
    val persondata: Persondata,
    val mottatteOpplysningerData: MottatteOpplysningerData?,
    val lovvalgsperiode: Lovvalgsperiode? = null,
    val opprinneligLovvalgsperiode: Lovvalgsperiode? = null,
    val saksopplysningerData: SaksopplysningerData? = null,
    val behandlingstema: Behandlingstema? = null,
    val fullmektig: Aktoer?,
    val organisasjonDokument: OrganisasjonDokument?,
    val persondataTilFullmektig: Persondata?,
    val medlemskapsperiodeData: MedlemskapsperiodeData? = null,
    val brevUtkast: List<UtkastBrev>,
    val antallArbeidsgivere: Int = 0,
    val trygdeavgiftperiodeData: TrygdeavgiftsperiodeData? = null,
    val trygdeavgiftMottaker: Trygdeavgiftmottaker? = null,
    val fullmektigSomBetalerTrygdeavgift: Aktoer? = null,
    val trygdeavgiftsperioderTidligereBehandling: List<Trygdeavgiftsperiode>? = null,
    val behandlingstyper: Behandlingstyper? = null
)
