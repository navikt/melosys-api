package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.SakOgBehandlingDTO
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.stereotype.Component
import java.io.File

private val log = KotlinLogging.logger { }

@Component
class OppgaveMigrering(
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveFasade: OppgaveFasade,
) {

    private val nyOppgaveFactory = OppgaveFactory(FakeUnleash().apply {
        enable(ToggleName.NY_GOSYS_MAPPING)
    })

    private val sakerManglerOppgave = mutableListOf<String>()
    private val sakerMedOppgave = mutableListOf<String>()
    private val sakerMedFlereOppgaver = mutableListOf<String>()
    private val sakHvorViSkalHaSedMenSomIkkeFinnes = mutableListOf<String>()
    private val sakHvorMappingFeiler = mutableListOf<String>()
    private val oppgaveMappingKjørelog = StringBuilder()


    fun go() {
        ThreadLocalAccessInfo.executeProcess("Prossess oppgaver") {
            migrering()
        }
    }

    private fun migrering() {
        log.info("Utfører OppgaveMigrering")

        val sakOgBehandlinger = behandlingRepository.findSaksOgBehandlingTyperOgTeam(
            listOf(
                Behandlingsstatus.AVSLUTTET,
                Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING,
                Behandlingsstatus.IVERKSETTER_VEDTAK,
            )
        ).apply {
            log.info("size før erRedigerbar: $size")
        }.filter { it.erRedigerbar() }.sortedBy { it.saksnummer }

        println("sakOgBehandlinger filtrert: ${sakOgBehandlinger.size}")

        sakOgBehandlinger.filter {
            it.saksnummer in sakerSomMangerOppgave
        }.forEach {
            oppgaveFasade.finnÅpneBehandlingsoppgaverMedSaksnummer(it.saksnummer).apply {
                if (size > 1) sakMedFlereOppgaver(it)
                if (size == 0) sakManglerOppgave(it)
            }.firstOrNull()?.apply {
                gammelOppgaveMapping(it)
                nyOppgaveMapping(it)
            }
        }
        printReport()
        saveStatusFiles()
    }

    private fun printReport() {
        println("sakerMedOppgave:                       ${sakerMedOppgave.size}")
        println("sakerManglerOppgave:                   ${sakerManglerOppgave.size}")
        println("sakerMedFlereOppgaver:                 ${sakerMedFlereOppgaver.size}")
        println("sakHvorMappingFeiler:                  ${sakHvorMappingFeiler.size}")
        println("sakHvorViSkalHaSedMenSomIkkeFinnes:    ${sakHvorViSkalHaSedMenSomIkkeFinnes.size}")
    }

    private fun saveStatusFiles() {
        File("saker-med-oppgave.txt").writeText(sakerMedOppgave.joinToString(","))
        File("saker-mangler-oppgave.txt").writeText(sakerManglerOppgave.joinToString(","))
        File("saker-med-flere-oppgave.txt").writeText(sakerMedFlereOppgaver.joinToString("\n"))
        File("sak-hvor-mapping-feiler.txt").writeText(sakHvorMappingFeiler.joinToString("\n"))
        File("sak-hvor-vi-skal-ha-sed-men-som-ikke-finnes.txt").writeText(
            sakHvorViSkalHaSedMenSomIkkeFinnes.joinToString("\n")
        )

        File("oppgaver.txt").writeText(oppgaveMappingKjørelog.toString())
    }

    private fun MutableList<Oppgave>.sakMedFlereOppgaver(sakOgBehandling: SakOgBehandlingDTO) {
        log.error("fant $size for: ${sakOgBehandling.saksnummer}")
        sakerMedFlereOppgaver.add("fant $size oppgaver for: ${sakOgBehandling.saksnummer}")
    }

    private fun Oppgave.gammelOppgaveMapping(sakOgBehandling: SakOgBehandlingDTO) {
        log.info(sakOgBehandling.toString())
        log.info("oppgave:$oppgaveId - beskrivelse:${beskrivelse ?: ""}")
        sakerMedOppgave.add(sakOgBehandling.saksnummer)
        oppgaveMappingKjørelog.appendLine()
        oppgaveMappingKjørelog.appendLine("$sakOgBehandling")
        oppgaveMappingKjørelog.appendLine("oppgaveId=         $oppgaveId")
        oppgaveMappingKjørelog.appendLine("=========== gammel ===========")
        oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
        oppgaveMappingKjørelog.appendLine("tema=              $tema")
        oppgaveMappingKjørelog.appendLine("behandlingstema=   $behandlingstema")
        oppgaveMappingKjørelog.appendLine("behandlingstype=   $behandlingstype")
        oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
        oppgaveMappingKjørelog.appendLine("beskrivelse=       $beskrivelse")
        oppgaveMappingKjørelog.appendLine("=========== nye ===========")
    }

    private fun nyOppgaveMapping(sakOgBehandling: SakOgBehandlingDTO) {
        try {
            val oppgaveBehandlingstema: OppgaveBehandlingstema = sakOgBehandling.utledOppgaveBehandlingstema()
            val oppgavetype: Oppgavetyper = sakOgBehandling.utledOppgaveType()
            val beskrivelse: String = sakOgBehandling.utledBeskrivelse(oppgaveBehandlingstema)
            val tema: Tema = sakOgBehandling.utledTema()
            oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
            oppgaveMappingKjørelog.appendLine("tema=              $tema")
            oppgaveMappingKjørelog.appendLine("behandlingstema=   ${oppgaveBehandlingstema.kode}")
            oppgaveMappingKjørelog.appendLine("oppgavetype=       $oppgavetype")
            oppgaveMappingKjørelog.appendLine("beskrivelse=       $beskrivelse")
            oppgaveMappingKjørelog.appendLine("------------------------------------")
        } catch (e: Exception) {
            sakHvorMappingFeiler.add("${sakOgBehandling.saksnummer}: ${e.message}")
        }
    }

    private fun sakManglerOppgave(sakOgBehandlingDTO: SakOgBehandlingDTO) {
        log.warn("${sakOgBehandlingDTO.saksnummer} har ikke åpen oppgagave")
        sakerManglerOppgave.add(sakOgBehandlingDTO.saksnummer)
    }

    fun SakOgBehandlingDTO.utledOppgaveBehandlingstema(): OppgaveBehandlingstema =
        nyOppgaveFactory.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    private fun SakOgBehandlingDTO.utledOppgaveType(): Oppgavetyper =
        nyOppgaveFactory.utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype)

    fun SakOgBehandlingDTO.utledTema(): Tema =
        nyOppgaveFactory.utledTema(
            sakstype, sakstema, behandlingstema
        )

    fun SakOgBehandlingDTO.utledBeskrivelse(oppgaveBehandlingstema: OppgaveBehandlingstema): String {
        val hentSedDokument = {
            log.info("Henter sed dokuemnt for: $behandlingID")
            sedDokument(behandlingID)
        }
        return try {
            nyOppgaveFactory.utledBeskrivelse(
                oppgaveBehandlingstema,
                sakstype,
                sakstema,
                behandlingstema,
                behandlingstype, hentSedDokument
            )
        } catch (e: Exception) {
            val message = e.message ?: "utledBeskrivelse feilet "
            val msg = "$message feilet for $saksnummer, behandlingID:$behandlingID"
            sakHvorViSkalHaSedMenSomIkkeFinnes.add(msg)
            return msg
        }
    }

    private fun sedDokument(behandlingID: Long): SedDokument? =
        hentBehandlingMedSaksoplysninger(behandlingID)
            .finnSedDokument().orElse(null)

    private fun hentBehandlingMedSaksoplysninger(behandlingID: Long): Behandling = behandlingRepository
        .findWithSaksopplysningerById(behandlingID) ?: throw TekniskException("Fant ikke behandling for $behandlingID")

    private val sakerSomMangerOppgave: Set<String> =
        "MEL-1448,MEL-2363,MEL-2469,MEL-2542,MEL-3211,MEL-3247,MEL-3303,MEL-3393,MEL-3407,MEL-3413,MEL-3415,MEL-3432,MEL-3435,MEL-3436,MEL-3439,MEL-3453,MEL-3470,MEL-3473,MEL-3474,MEL-3475,MEL-3478,MEL-3479,MEL-3480,MEL-3489,MEL-3490,MEL-3493,MEL-3494,MEL-3495,MEL-3496,MEL-3497,MEL-3499,MEL-3501,MEL-3507,MEL-3508,MEL-3509,MEL-3511,MEL-3516,MEL-3517,MEL-3518,MEL-3519,MEL-3523,MEL-3526,MEL-3527,MEL-3528,MEL-3529,MEL-3546,MEL-3547,MEL-3549,MEL-3550,MEL-3551,MEL-3553,MEL-3555,MEL-3569,MEL-3570,MEL-3571,MEL-3588,MEL-3595,MEL-3596,MEL-3599,MEL-3601,MEL-3602,MEL-3607,MEL-3627,MEL-3628,MEL-3647,MEL-3648,MEL-3649,MEL-3691,MEL-3711,MEL-3715,MEL-3724,MEL-3725,MEL-3744,MEL-3757,MEL-3758,MEL-3768,MEL-3769,MEL-3771,MEL-3774,MEL-3791,MEL-3807,MEL-3809,MEL-3814,MEL-3816,MEL-3836,MEL-3837,MEL-3839,MEL-3843,MEL-3845,MEL-3846,MEL-3847,MEL-3848,MEL-3849,MEL-3850,MEL-3851,MEL-3852,MEL-3853,MEL-3856,MEL-3857,MEL-3858,MEL-3860,MEL-3862,MEL-3863,MEL-3865,MEL-3866,MEL-3867,MEL-3868,MEL-3870,MEL-3871,MEL-3872,MEL-3873,MEL-3874,MEL-3875,MEL-3876,MEL-3877,MEL-3878,MEL-3879,MEL-3880,MEL-3881,MEL-3884,MEL-3885,MEL-3886,MEL-3887,MEL-3888,MEL-3889,MEL-3891,MEL-3892,MEL-3896,MEL-3897,MEL-3900,MEL-3918,MEL-3919,MEL-3924,MEL-3925,MEL-3926,MEL-3927,MEL-3928,MEL-3929,MEL-3933,MEL-3956,MEL-3958,MEL-3960,MEL-3961,MEL-3963,MEL-3966,MEL-3967,MEL-3969,MEL-3970,MEL-3971,MEL-3975,MEL-3976,MEL-3978,MEL-3979,MEL-3980,MEL-3981,MEL-3982,MEL-3983,MEL-3985,MEL-3986,MEL-4022,MEL-4030,MEL-4031,MEL-4036,MEL-4042,MEL-4047,MEL-4048,MEL-4056,MEL-4057,MEL-4059,MEL-4063,MEL-4064,MEL-4065,MEL-4066,MEL-4067,MEL-4068,MEL-4069,MEL-4070,MEL-4076,MEL-4077,MEL-4096,MEL-4117,MEL-4118,MEL-4119,MEL-4121,MEL-4122,MEL-4123,MEL-4124,MEL-4125,MEL-4126,MEL-4127,MEL-4128,MEL-4129,MEL-4130,MEL-4131,MEL-4132,MEL-4133,MEL-4135,MEL-4136,MEL-4137,MEL-4139,MEL-4142,MEL-4144,MEL-4146,MEL-4147,MEL-4148,MEL-4150,MEL-4157,MEL-4159,MEL-4160,MEL-4161,MEL-4162,MEL-4163,MEL-4165,MEL-4166,MEL-4169,MEL-4170,MEL-4173,MEL-4177,MEL-4179,MEL-4180,MEL-4182,MEL-4184,MEL-4185,MEL-4211,MEL-4212,MEL-4215,MEL-4218,MEL-4219,MEL-4222,MEL-4234,MEL-4236,MEL-4256,MEL-4257,MEL-4277,MEL-4281,MEL-4282,MEL-4283,MEL-4284,MEL-4285,MEL-4286,MEL-4290,MEL-4291,MEL-4292,MEL-4293,MEL-4294,MEL-4295,MEL-4296,MEL-4299,MEL-4302,MEL-4308,MEL-4316,MEL-4324,MEL-4336,MEL-4337,MEL-4338,MEL-4339,MEL-4340,MEL-4343,MEL-4344,MEL-4345,MEL-4356,MEL-4357,MEL-4358,MEL-4360,MEL-4361,MEL-4362,MEL-4363,MEL-4364,MEL-4365,MEL-4376,MEL-4377,MEL-4378,MEL-4379,MEL-4380,MEL-4381,MEL-4383,MEL-4384,MEL-4385,MEL-4386,MEL-4387,MEL-4388,MEL-4390,MEL-4391,MEL-4392,MEL-4393,MEL-4394,MEL-4396,MEL-4397,MEL-4400,MEL-4402,MEL-4403,MEL-4410,MEL-4411,MEL-4417,MEL-4420,MEL-4422,MEL-4424,MEL-4426,MEL-4436,MEL-4437,MEL-4438,MEL-4439,MEL-4458,MEL-4466,MEL-4469,MEL-4470,MEL-4471,MEL-4478,MEL-4497,MEL-4516,MEL-4537,MEL-4538,MEL-4539,MEL-4541,MEL-4542,MEL-4548,MEL-4554,MEL-4556,MEL-4557,MEL-4558,MEL-4560,MEL-4561,MEL-4562,MEL-4563,MEL-4564,MEL-4567,MEL-4568,MEL-4569,MEL-4590,MEL-4591,MEL-4594,MEL-4596,MEL-4597,MEL-4598,MEL-4600,MEL-4603,MEL-4606,MEL-4607,MEL-4609,MEL-4612,MEL-4622,MEL-4623,MEL-4624,MEL-4625,MEL-4630,MEL-4637,MEL-4639,MEL-4643,MEL-4647,MEL-4652,MEL-4679,MEL-4683,MEL-4689,MEL-4695,MEL-4699,MEL-4700,MEL-4703,MEL-4705,MEL-4706,MEL-4708,MEL-4714,MEL-4715,MEL-4716,MEL-4719,MEL-4726,MEL-4728,MEL-4729,MEL-4730,MEL-4731,MEL-4732,MEL-4733,MEL-4736,MEL-4740,MEL-4742,MEL-4744,MEL-4746,MEL-4757,MEL-4758,MEL-4764,MEL-4766,MEL-4770,MEL-4775,MEL-4777,MEL-4778,MEL-4779,MEL-4780,MEL-4781,MEL-4782,MEL-4783,MEL-4784,MEL-4785,MEL-4786,MEL-4787,MEL-4796,MEL-4797,MEL-4798,MEL-4799,MEL-4800,MEL-4801,MEL-4802,MEL-4806,MEL-4807,MEL-4808,MEL-4810,MEL-4811,MEL-4812,MEL-4813,MEL-4818,MEL-4820,MEL-4821,MEL-4822,MEL-4825,MEL-4828,MEL-4829,MEL-4830,MEL-4834,MEL-4837,MEL-4838,MEL-4840,MEL-4842,MEL-4845,MEL-4846,MEL-4847,MEL-4848,MEL-4850,MEL-4854,MEL-4855,MEL-4856,MEL-4862,MEL-4864,MEL-4867,MEL-4870,MEL-4871,MEL-4874,MEL-4876,MEL-4879,MEL-4880,MEL-4882,MEL-4886,MEL-4887,MEL-4888,MEL-4889,MEL-4892,MEL-4900,MEL-4904,MEL-4911,MEL-4925,MEL-4930,MEL-4931,MEL-4932,MEL-4933,MEL-4934,MEL-4937,MEL-4938,MEL-4940,MEL-4943,MEL-4944,MEL-4945,MEL-4946,MEL-4951,MEL-4952,MEL-4953,MEL-4954,MEL-4961,MEL-4962,MEL-4963,MEL-4969,MEL-4973,MEL-4976,MEL-4979,MEL-4980,MEL-4981,MEL-4982,MEL-5012,MEL-5018,MEL-5022,MEL-5023,MEL-5024,MEL-5026,MEL-5027,MEL-5028,MEL-5029,MEL-5033,MEL-5035,MEL-5058,MEL-5062,MEL-5063,MEL-5066,MEL-5068,MEL-5069,MEL-5071,MEL-5074,MEL-5076,MEL-5078,MEL-5079,MEL-5080,MEL-5082,MEL-5098,MEL-5103,MEL-5105,MEL-5107,MEL-5108,MEL-5110,MEL-5112,MEL-5113,MEL-5118,MEL-5135,MEL-5138,MEL-5142,MEL-5147,MEL-5152,MEL-5154,MEL-5155,MEL-5175,MEL-5176,MEL-5177,MEL-5178,MEL-5179,MEL-5183,MEL-5194,MEL-5201,MEL-5202,MEL-5215,MEL-5240,MEL-5241,MEL-5242,MEL-5244,MEL-5254,MEL-5255,MEL-5256,MEL-5257,MEL-5258,MEL-5262,MEL-5266,MEL-5270,MEL-5271,MEL-5277,MEL-5279,MEL-5280,MEL-5281,MEL-5282,MEL-5288,MEL-5299,MEL-5300,MEL-5301,MEL-5308,MEL-5313,MEL-5318,MEL-5322,MEL-5327,MEL-5328,MEL-5329,MEL-5330,MEL-5331,MEL-5336,MEL-5340,MEL-5343,MEL-5344,MEL-5346,MEL-5347,MEL-5349,MEL-5350,MEL-5359,MEL-5360,MEL-5361,MEL-5368,MEL-5374,MEL-5375,MEL-5378,MEL-5380,MEL-5384,MEL-5385,MEL-5389,MEL-5391,MEL-5392,MEL-5403,MEL-5406,MEL-5409,MEL-5410,MEL-5417,MEL-5422,MEL-5451,MEL-5459,MEL-5460,MEL-5464,MEL-5467,MEL-5469,MEL-5489,MEL-5492,MEL-5496,MEL-5498,MEL-5502,MEL-5512,MEL-5516,MEL-5517,MEL-5518,MEL-5524,MEL-5531,MEL-5536,MEL-5537,MEL-5542,MEL-5560,MEL-5561,MEL-5564,MEL-5567,MEL-5568,MEL-5570,MEL-5571,MEL-5574,MEL-5575,MEL-5576,MEL-5578,MEL-5579,MEL-5581,MEL-5600,MEL-5601,MEL-5604,MEL-5606,MEL-5618,MEL-5623,MEL-5624,MEL-5639,MEL-5645,MEL-5646,MEL-5649,MEL-5651,MEL-5652,MEL-5656,MEL-5660,MEL-5682,MEL-5683,MEL-5684,MEL-5685,MEL-5705,MEL-5709,MEL-5710,MEL-5714,MEL-5716,MEL-5717,MEL-5718,MEL-5720,MEL-5721,MEL-5741,MEL-5743,MEL-5759,MEL-5783,MEL-5802,MEL-5803,MEL-5805,MEL-5808,MEL-5810,MEL-5811,MEL-5812,MEL-5813,MEL-5815,MEL-5822,MEL-5823,MEL-5824,MEL-5825,MEL-5826,MEL-5840,MEL-5842,MEL-5861,MEL-5865,MEL-5869,MEL-5879,MEL-5880,MEL-5881,MEL-5882,MEL-5883,MEL-5884,MEL-5885,MEL-5899,MEL-5900,MEL-5902,MEL-5904,MEL-5905,MEL-5906,MEL-5907,MEL-5908,MEL-5910,MEL-5919,MEL-5939,MEL-5950,MEL-5957,MEL-5959,MEL-5960,MEL-5961,MEL-5972,MEL-5975,MEL-5977,MEL-5982,MEL-5983,MEL-5984,MEL-5988,MEL-5990,MEL-5991,MEL-5992,MEL-5993,MEL-5995,MEL-5996,MEL-5997,MEL-6011,MEL-6012,MEL-6013,MEL-6015,MEL-6016,MEL-6017,MEL-6018,MEL-6019,MEL-6020,MEL-6021,MEL-6022,MEL-6027,MEL-6028,MEL-6030,MEL-6031,MEL-6032,MEL-6040,MEL-6044,MEL-6046,MEL-6047,MEL-6052,MEL-6053,MEL-6054,MEL-6056,MEL-6071,MEL-6072,MEL-6081,MEL-6091,MEL-6094,MEL-6095,MEL-6096,MEL-6097,MEL-6098,MEL-6099,MEL-6100,MEL-6101,MEL-6102,MEL-6103,MEL-6105,MEL-6106,MEL-6107,MEL-6109,MEL-6110,MEL-6111,MEL-6112,MEL-6116,MEL-6118,MEL-6119,MEL-6120,MEL-6121,MEL-6123,MEL-6124,MEL-6140,MEL-6148,MEL-6149,MEL-6154,MEL-6173,MEL-6176,MEL-6182,MEL-6184,MEL-6194,MEL-6195,MEL-6197,MEL-6200,MEL-6202,MEL-6232,MEL-6241,MEL-6254,MEL-6265,MEL-6267,MEL-6268,MEL-6270,MEL-6272,MEL-6273,MEL-6294,MEL-6297,MEL-6298,MEL-6299,MEL-6300,MEL-6303,MEL-6314,MEL-6319,MEL-6323,MEL-6331,MEL-6332,MEL-6335,MEL-6339,MEL-6340,MEL-6349,MEL-6353,MEL-6355,MEL-6356,MEL-6357,MEL-6358,MEL-6359,MEL-6360,MEL-6371,MEL-6372,MEL-6373,MEL-6374,MEL-6375,MEL-6376,MEL-6377,MEL-6378,MEL-6379,MEL-6380,MEL-6389,MEL-6391,MEL-6392,MEL-6393,MEL-6399,MEL-6406,MEL-6408,MEL-6409,MEL-6410,MEL-6412,MEL-6413,MEL-6414,MEL-6415,MEL-6416,MEL-6417,MEL-6418,MEL-6419,MEL-6423,MEL-6429,MEL-6430,MEL-6434,MEL-6436,MEL-6441,MEL-6442,MEL-6444,MEL-6449,MEL-6451,MEL-6452,MEL-6453,MEL-6454,MEL-6455,MEL-6456,MEL-6457,MEL-6464,MEL-6465,MEL-6467,MEL-6468,MEL-6469,MEL-6470,MEL-6471,MEL-6473,MEL-6494,MEL-6495,MEL-6498,MEL-6499,MEL-6500,MEL-6501,MEL-6502,MEL-6504,MEL-6511,MEL-6512,MEL-6514,MEL-6520,MEL-6523,MEL-6524,MEL-6526,MEL-6528,MEL-6529,MEL-6530,MEL-6535,MEL-6537,MEL-6539,MEL-6540,MEL-6542,MEL-6543,MEL-6547,MEL-6549,MEL-6555,MEL-6562,MEL-6565,MEL-6567,MEL-6571,MEL-6572,MEL-6573,MEL-6575,MEL-6576,MEL-6578,MEL-6581,MEL-6582,MEL-6587,MEL-6596,MEL-6599,MEL-6600,MEL-6604,MEL-6605,MEL-6611,MEL-6615,MEL-6617,MEL-6619,MEL-6622,MEL-6626,MEL-6628,MEL-6630,MEL-6634,MEL-6636,MEL-6637,MEL-6638,MEL-6639,MEL-6640,MEL-6641,MEL-6651,MEL-6652,MEL-6674,MEL-6676,MEL-6677,MEL-6678,MEL-6679,MEL-6681,MEL-6684,MEL-6687,MEL-6688,MEL-6690,MEL-6691,MEL-6692,MEL-6693,MEL-6695,MEL-6696,MEL-6703,MEL-6707,MEL-6708,MEL-6709,MEL-6710,MEL-6711,MEL-6715,MEL-6716,MEL-6721,MEL-6723,MEL-6725,MEL-6740,MEL-6741,MEL-6746,MEL-6748,MEL-6752,MEL-6754,MEL-6755,MEL-6757,MEL-6758,MEL-6759,MEL-6760,MEL-6762,MEL-6763,MEL-6768,MEL-6769,MEL-6770,MEL-6771,MEL-6773,MEL-6774,MEL-6775,MEL-6788,MEL-6789,MEL-6795,MEL-6799,MEL-6801,MEL-6810,MEL-6814,MEL-6815,MEL-6817,MEL-6831,MEL-6840,MEL-6841,MEL-6842,MEL-6845,MEL-6848,MEL-6852,MEL-6870,MEL-6878,MEL-6881,MEL-6886,MEL-6889,MEL-6891,MEL-6892,MEL-6918,MEL-6927,MEL-6929,MEL-6931,MEL-6933,MEL-6935,MEL-6942,MEL-6943,MEL-6946,MEL-6948,MEL-6949,MEL-6952,MEL-6964,MEL-6976,MEL-6980,MEL-6981,MEL-6983,MEL-6984,MEL-6987,MEL-6988,MEL-6994,MEL-6996,MEL-7012,MEL-7014,MEL-7027,MEL-7028,MEL-7029,MEL-7030,MEL-7033,MEL-7036,MEL-7039,MEL-7045,MEL-7052,MEL-7054,MEL-7056,MEL-7058,MEL-7060,MEL-7062,MEL-7063,MEL-7065,MEL-7067,MEL-7071,MEL-7074,MEL-7075,MEL-7076,MEL-7077,MEL-7078,MEL-7079,MEL-7080,MEL-7081,MEL-7082,MEL-7085,MEL-7089,MEL-7091,MEL-7092,MEL-7093,MEL-7094,MEL-7096,MEL-7114,MEL-7115,MEL-7117,MEL-7120,MEL-7121,MEL-7122,MEL-7124,MEL-7129,MEL-7130,MEL-7132,MEL-7133,MEL-7135,MEL-7137,MEL-7141,MEL-7142,MEL-7143,MEL-7144,MEL-7145,MEL-7176,MEL-7177,MEL-7179,MEL-7180,MEL-7181,MEL-7186,MEL-7188,MEL-7189,MEL-7190,MEL-7191,MEL-7192,MEL-7203,MEL-7204,MEL-7205,MEL-7206,MEL-7207,MEL-7208,MEL-7210,MEL-7211,MEL-7212,MEL-7213,MEL-7214,MEL-7215,MEL-7217,MEL-7218,MEL-7219,MEL-7220,MEL-7221,MEL-7223,MEL-7224,MEL-7226,MEL-7227,MEL-7230,MEL-7231,MEL-7233,MEL-7235,MEL-7237,MEL-7245,MEL-7259,MEL-7272,MEL-7273,MEL-7274,MEL-7276,MEL-7277,MEL-7278,MEL-7285,MEL-7289,MEL-7291,MEL-7292,MEL-7293,MEL-7296,MEL-7298,MEL-7304,MEL-7306,MEL-7309,MEL-7311,MEL-7312,MEL-7319,MEL-7320,MEL-7321,MEL-7322,MEL-7323,MEL-7324,MEL-7328,MEL-7330,MEL-7332,MEL-7333,MEL-7336,MEL-7342,MEL-7346,MEL-7348,MEL-7358,MEL-7359,MEL-7364,MEL-7365,MEL-7366,MEL-7367,MEL-7368,MEL-7369,MEL-7370,MEL-7372,MEL-7373,MEL-7375,MEL-7376,MEL-7377,MEL-7378,MEL-7379,MEL-7380,MEL-7382,MEL-7383,MEL-7384,MEL-7391,MEL-7392,MEL-7393,MEL-7394,MEL-7395,MEL-7396,MEL-7397,MEL-7398,MEL-7399,MEL-7400,MEL-7402,MEL-7404,MEL-7407,MEL-7411,MEL-7413,MEL-7415,MEL-7417,MEL-7418,MEL-7421,MEL-7424,MEL-7426,MEL-7427,MEL-7428,MEL-7431,MEL-7432,MEL-7433,MEL-7434,MEL-7435,MEL-7436,MEL-7441,MEL-7442,MEL-7445,MEL-7448,MEL-7450,MEL-7451,MEL-7454,MEL-7455,MEL-7456,MEL-7457,MEL-7459,MEL-7471,MEL-7473,MEL-7474,MEL-7475,MEL-7485,MEL-7492,MEL-7495,MEL-7518,MEL-7526,MEL-7529,MEL-7531,MEL-7556,MEL-7590,MEL-7601,MEL-7612,MEL-7626,MEL-7627,MEL-7628,MEL-7629,MEL-7630,MEL-7636,MEL-7637,MEL-7656,MEL-7657,MEL-7658,MEL-7666,MEL-7670,MEL-7671,MEL-7677,MEL-7687,MEL-7688,MEL-7699,MEL-7700,MEL-7721,MEL-7737,MEL-7753,MEL-7759,MEL-7764,MEL-7765,MEL-7766,MEL-7767,MEL-7768,MEL-7769,MEL-7770,MEL-7771,MEL-7773,MEL-7805,MEL-7808,MEL-7813,MEL-7814,MEL-7815,MEL-7816,MEL-7818,MEL-7819,MEL-7820,MEL-7821,MEL-7822,MEL-7824,MEL-7825,MEL-7826,MEL-7830,MEL-7831,MEL-7833,MEL-7836,MEL-7837,MEL-7838,MEL-7841,MEL-7843,MEL-7847,MEL-7848,MEL-7850,MEL-7851,MEL-7858,MEL-7864,MEL-7866,MEL-7867,MEL-7872,MEL-7874,MEL-7875,MEL-7878,MEL-7879,MEL-7882,MEL-7883,MEL-7884,MEL-7885,MEL-7886,MEL-7887,MEL-7888,MEL-7889,MEL-7890,MEL-7891,MEL-7893,MEL-7896,MEL-7897,MEL-7898,MEL-7899,MEL-7900,MEL-7901,MEL-7902,MEL-7903,MEL-7904,MEL-7905,MEL-7906,MEL-7907,MEL-7908,MEL-7909,MEL-7912,MEL-7919,MEL-7921,MEL-7935,MEL-7969,MEL-7998,MEL-8031,MEL-8038,MEL-8044,MEL-8057,MEL-8065,MEL-8078,MEL-8081,MEL-8082,MEL-8085,MEL-8087,MEL-8088,MEL-8093,MEL-8095,MEL-8104,MEL-8106,MEL-8108,MEL-8117,MEL-8140,MEL-8165,MEL-8171,MEL-8179,MEL-8193,MEL-8195,MEL-8196,MEL-8200,MEL-8204,MEL-8220,MEL-8221,MEL-8228,MEL-8231,MEL-8232,MEL-8235,MEL-8238,MEL-8240,MEL-8241,MEL-8243,MEL-8244,MEL-8246,MEL-8247"
            .split(",").toSet()

}

