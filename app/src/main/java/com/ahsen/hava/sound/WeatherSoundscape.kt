package com.ahsen.hava.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.ahsen.hava.domain.model.Forecast
import com.ahsen.hava.domain.model.WeatherCondition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Sesin "tarifi": hepsi 0..1 arası. */
data class SoundscapeParams(
    val rain: Float,
    val wind: Float,
    val thunder: Boolean,
    val ambience: Ambience,
) {
    enum class Ambience { None, Birds, Crickets }

    companion object {
        /** Şu anki havadan ses tarifi çıkarır. */
        fun from(forecast: Forecast): SoundscapeParams {
            val current = forecast.current
            val now = current.observedAt
            val mmPer15 = forecast.minutely15.firstOrNull { !it.time.isBefore(now.minusMinutes(14)) }?.precipitationMm ?: 0.0
            val measured = (mmPer15 * 4 / 6.0).toFloat().coerceIn(0f, 1f) // 6 mm/saat ve üstü = sağanak
            val floor = when (current.condition) {
                WeatherCondition.DRIZZLE -> 0.25f
                WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS -> 0.55f
                WeatherCondition.THUNDERSTORM -> 0.8f
                else -> 0f
            }
            val clear = current.condition in setOf(
                WeatherCondition.CLEAR,
                WeatherCondition.MAINLY_CLEAR,
                WeatherCondition.PARTLY_CLOUDY,
            )
            return SoundscapeParams(
                rain = max(floor, measured),
                wind = ((current.windSpeedKmh ?: 5.0) / 45.0).toFloat().coerceIn(0.08f, 1f),
                thunder = current.condition == WeatherCondition.THUNDERSTORM,
                ambience = when {
                    clear && current.isDay -> Ambience.Birds
                    clear -> Ambience.Crickets
                    else -> Ambience.None
                },
            )
        }
    }
}

/**
 * Havayı dinle: hazır ses kaydı yok, her şey matematikle üretilir.
 *
 * AudioTrack'e saniyede 44.100 sayı (örnek) yazıyoruz. Her sayı hoparlör zarının o anki konumu.
 * - Yağmur = filtrelenmiş gürültü (hışırtı) + rastgele "tık" damlalar
 * - Rüzgâr = filtrelenmiş gürültü + yavaşça değişen esintiler
 * - Gök gürültüsü = alçak frekanslı, yavaş sönen bir gürleme
 * - Kuş ve cırcır böceği = kısa sinüs dalgaları
 *
 * Üretim arka planda (Dispatchers.Default) çalışır; ana thread hiç bloklanmaz.
 */
class WeatherSoundscape {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun play(params: SoundscapeParams) {
        stop()
        job = scope.launch { render(params) }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun render(params: SoundscapeParams) {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(max(minBuffer, SAMPLE_RATE / 4 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val synth = SoundSynth(SAMPLE_RATE, params)
        val buffer = ShortArray(2048) // sol, sağ, sol, sağ... (1024 stereo örnek)
        track.play()
        try {
            while (currentCoroutineContext().isActive) {
                synth.fill(buffer)
                track.write(buffer, 0, buffer.size)
            }
        } finally {
            track.pause()
            track.flush()
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
    }
}

/** Bant geçiren "state variable" filtre. Merkez frekansın çevresini geçirir, gerisini kısar. */
private class BandPass(center: Float, private val damping: Float, rate: Int) {
    private var f = 2f * sin(PI.toFloat() * center / rate)
    private var low = 0f
    private var band = 0f

    fun setCenter(center: Float, rate: Int) {
        f = 2f * sin(PI.toFloat() * center / rate)
    }

    fun process(x: Float): Float {
        val high = x - low - damping * band
        band += f * high
        low += f * band
        return band
    }
}

/**
 * Sesin kendisi: stereo. Her çağrıda bir sonraki sol/sağ örneği hesaplar.
 * Durumlu, bu yüzden tek thread'den kullanılır.
 */
internal class SoundSynth(private val rate: Int, private val p: SoundscapeParams, seed: Int = 7) {

    private val rnd = Random(seed)
    private var sampleIndex = 0L
    private val left = 0
    private val right = 1

    // ---- Yağmur: iki kanal için ayrı ayrı (sağ ve sol birbirinden bağımsız = mekân hissi)
    // Binlerce minik çarpma ("çıtırtı") ve seyrek iri damlalar: her biri hızla sönen gürültü patlaması.
    private val fineEnv = FloatArray(2)
    private val dropEnv = FloatArray(2)
    private val fineFilter = arrayOf(BandPass(2_400f, 1.2f, rate), BandPass(2_400f, 1.2f, rate))
    private val dropFilter = arrayOf(BandPass(950f, 0.9f, rate), BandPass(950f, 0.9f, rate))
    private val fineSmooth = FloatArray(2)
    private val fineDecay = exp(-1f / (rate * 0.0012f))
    private val dropDecay = exp(-1f / (rate * 0.007f))
    // Uzak yağmur uğultusu: pembe gürültü, iki kanal için ayrı.
    private val pinkState = Array(2) { FloatArray(3) }
    private val roarLow = FloatArray(2)
    private val roarBase = FloatArray(2)
    private var roarMod = 0.85f
    private var roarModTarget = 0.85f

    // ---- Rüzgâr
    private val windFilter = arrayOf(BandPass(400f, 0.6f, rate), BandPass(400f, 0.6f, rate))
    private var gust = 0.5f
    private var gustTarget = 0.5f

    // ---- Gök gürültüsü
    private val thunderFilter = BandPass(160f, 0.9f, rate)
    private var thunderCountdown = rate * 6
    private var thunderEnv = 0f
    private var thunderRising = false
    private val thunderDecay = exp(-1f / (rate * 2f))

    // ---- Kuş cıvıltısı
    private var chirpCountdown = rate
    private var notesLeft = 0
    private var noteLength = 0
    private var notePos = 0
    private var noteGap = 0
    private var noteStartFreq = 0f
    private var chirpPhase = 0f
    private var birdPan = 0.5f

    // ---- Cırcır böcekleri: farklı uzaklıkta altı böcek
    private val crickets = List(6) { Cricket(rnd, rate) }
    private val nightLow = FloatArray(2)
    private val nightBase = FloatArray(2)

    private val out = FloatArray(2)

    /** [buffer] sol-sağ sırasıyla doldurulur. */
    fun fill(buffer: ShortArray) {
        var i = 0
        while (i < buffer.size - 1) {
            next()
            buffer[i] = (out[left] * 32_000f).toInt().toShort()
            buffer[i + 1] = (out[right] * 32_000f).toInt().toShort()
            i += 2
        }
    }

    private fun next() {
        // İlk 2 saniyede sesi yavaşça aç: aniden patlamasın.
        val master = min(1f, sampleIndex / (rate * 2f))
        sampleIndex++
        out[left] = 0f
        out[right] = 0f
        rain()
        wind()
        if (p.thunder) {
            val t = thunder()
            out[left] += t
            out[right] += t
        }
        when (p.ambience) {
            SoundscapeParams.Ambience.Birds -> birds()
            SoundscapeParams.Ambience.Crickets -> crickets()
            SoundscapeParams.Ambience.None -> Unit
        }
        // Yumuşak sınırlayıcı: ses ne kadar yükselirse yükselsin cızırdamaz.
        out[left] = tanh(out[left] * master * 1.7f)
        out[right] = tanh(out[right] * master * 1.7f)
    }

    private fun white() = rnd.nextFloat() * 2f - 1f

    private fun pink(ch: Int): Float {
        val st = pinkState[ch]
        val w = white()
        st[0] = 0.99765f * st[0] + w * 0.0990460f
        st[1] = 0.96300f * st[1] + w * 0.2965164f
        st[2] = 0.57000f * st[2] + w * 1.0526913f
        return (st[0] + st[1] + st[2] + w * 0.1848f) * 0.11f
    }

    private fun rain() {
        val r = p.rain
        if (r <= 0f) return
        // Kanal başına olasılık: toplamda saniyede 800–4800 minik çarpma, 30–280 iri damla.
        val fineChance = (800f + 4_000f * r) / 2f / rate
        val dropChance = (30f + 250f * r) / 2f / rate
        if (rnd.nextFloat() < 1f / (rate * 1.5f)) roarModTarget = 0.7f + rnd.nextFloat() * 0.3f
        roarMod += (roarModTarget - roarMod) * 0.00003f
        for (ch in 0..1) {
            if (rnd.nextFloat() < fineChance) fineEnv[ch] += (0.3f + 0.7f * rnd.nextFloat()) * 0.06f * (0.6f + 0.8f * r)
            if (rnd.nextFloat() < dropChance) dropEnv[ch] += (0.3f + 0.7f * rnd.nextFloat()) * 0.18f * (0.5f + r)
            fineEnv[ch] *= fineDecay
            dropEnv[ch] *= dropDecay

            val fineBand = fineFilter[ch].process(white() * fineEnv[ch])
            fineSmooth[ch] += 0.55f * (fineBand - fineSmooth[ch])
            val fine = fineSmooth[ch] * 1.5f
            val drop = dropFilter[ch].process(white() * dropEnv[ch]) * 2.2f

            val n = pink(ch)
            roarLow[ch] += 0.2f * (n - roarLow[ch])
            roarBase[ch] += 0.02f * (roarLow[ch] - roarBase[ch])
            val roar = (roarLow[ch] - roarBase[ch]) * (0.06f + 0.22f * r) * roarMod * 2.8f

            out[ch] += fine + drop + roar
        }
    }

    /**
     * Rüzgâr: beyaz gürültü, merkezi 250–950 Hz arasında gezinen bir bant geçiren filtreden geçer.
     * Esinti arttıkça merkez yükselir, ses "vuuuş" diye açılır. (Kahverengi gürültü daha derin
     * uğuldar ama enerjisi 200 Hz altında kalır ve telefon hoparlöründe duyulmaz.)
     */
    private fun wind() {
        if (rnd.nextFloat() < 1f / (rate * 2.5f)) gustTarget = 0.35f + rnd.nextFloat() * 0.65f
        gust += (gustTarget - gust) * 0.00004f
        val center = 250f + 700f * gust * p.wind
        // Gece havası daha sakin hissettirsin: cırcır böceği varken rüzgâr yarı şiddette.
        val nightCalm = if (p.ambience == SoundscapeParams.Ambience.Crickets) 0.5f else 1f
        val gain = (0.25f + 0.9f * p.wind) * gust * nightCalm
        for (ch in 0..1) {
            windFilter[ch].setCenter(center, rate)
            out[ch] += windFilter[ch].process(white() * 0.5f) * gain
        }
    }

    private fun thunder(): Float {
        if (--thunderCountdown <= 0) {
            thunderCountdown = rate * (14 + rnd.nextInt(22))
            thunderRising = true
        }
        // Hızlı yükseliş (~0,2 sn), yavaş sönüş (~2 sn)
        if (thunderRising) {
            thunderEnv += (1f - thunderEnv) * 0.0003f
            if (thunderEnv > 0.95f) thunderRising = false
        } else {
            thunderEnv *= thunderDecay
        }
        if (thunderEnv < 0.0005f) return 0f
        return thunderFilter.process(white()) * thunderEnv * 2.2f
    }

    private fun birds() {
        if (notesLeft == 0 && --chirpCountdown <= 0) {
            notesLeft = 2 + rnd.nextInt(3)
            noteStartFreq = 2_600f + rnd.nextFloat() * 1_200f
            birdPan = 0.15f + rnd.nextFloat() * 0.7f // her kuş farklı bir yerden ötsün
            startNote()
        }
        if (notesLeft == 0) return
        if (noteGap > 0) {
            noteGap--
            if (noteGap == 0) startNote()
            return
        }
        val x = notePos.toFloat() / noteLength
        val freq = noteStartFreq * (1f + 0.35f * x) + 120f * sin(2f * PI.toFloat() * 30f * notePos / rate)
        chirpPhase += 2f * PI.toFloat() * freq / rate
        val v = sin(chirpPhase) * sin(PI.toFloat() * x) * 0.12f
        notePos++
        if (notePos >= noteLength) {
            notesLeft--
            if (notesLeft > 0) noteGap = (rate * 0.05f).toInt() else chirpCountdown = (rate * (1.5f + rnd.nextFloat() * 3f)).toInt()
        }
        out[left] += v * cos(birdPan * PI.toFloat() / 2f)
        out[right] += v * sin(birdPan * PI.toFloat() / 2f)
    }

    private fun startNote() {
        noteLength = (rate * (0.06f + rnd.nextFloat() * 0.07f)).toInt()
        notePos = 0
        noteStartFreq *= 0.96f + rnd.nextFloat() * 0.08f
    }

    /** Cırcır böceği korosu + gece havasının çok hafif fonu. */
    private fun crickets() {
        for (c in crickets) c.next(rnd, out)
        for (ch in 0..1) {
            val n = pink(ch)
            nightLow[ch] += 0.05f * (n - nightLow[ch])
            nightBase[ch] += 0.004f * (nightLow[ch] - nightBase[ch])
            out[ch] += (nightLow[ch] - nightBase[ch]) * 0.05f
        }
    }
}

/**
 * Tek bir cırcır böceği. Gerçekçilik için:
 * - Her böceğin perdesi, hızı ve uzaklığı farklı (çoğu uzakta, birkaçı yakında).
 * - Ritim düzensiz: her cıvıltı biraz erken ya da geç gelir.
 * - Darbeler aniden açılıp kapanmaz; yumuşakça kabarıp söner.
 * - Kanat sürtünmesinden gelen hafif bir pürüz ve perde kayması var.
 */
private class Cricket(rnd: Random, private val rate: Int) {
    private val freq = 3_800f + rnd.nextFloat() * 1_000f
    private val pulseRate = 24f + rnd.nextFloat() * 10f
    private val period = 0.55f + rnd.nextFloat() * 0.6f
    private val amp = 0.010f + rnd.nextFloat() * rnd.nextFloat() * 0.040f
    private val pan = 0.1f + rnd.nextFloat() * 0.8f
    private val leftGain = cos(pan * PI.toFloat() / 2f)
    private val rightGain = sin(pan * PI.toFloat() / 2f)
    private var phase = 0f
    private var wobble = rnd.nextFloat() * 6.28f
    private var countdown = (rnd.nextFloat() * rate).toInt()
    private var pulsesLeft = 0
    private var pos = 0
    private var pulseLength = 1

    fun next(rnd: Random, out: FloatArray) {
        if (pulsesLeft == 0) {
            if (--countdown <= 0) {
                pulsesLeft = 3 + rnd.nextInt(3)
                pos = 0
                pulseLength = (rate / pulseRate).toInt()
            }
            return
        }
        val x = pos.toFloat() / pulseLength
        val env = if (x < 0.6f) {
            val e = sin(PI.toFloat() * x / 0.6f)
            e * e
        } else {
            0f
        }
        wobble += 2f * PI.toFloat() * 7f / rate
        val f = freq * (1f + 0.004f * sin(wobble))
        phase += 2f * PI.toFloat() * f / rate
        val rough = 0.8f + 0.2f * sin(phase * 0.071f)
        val v = (sin(phase) + 0.18f * sin(2f * phase)) * rough * env * amp
        out[0] += v * leftGain
        out[1] += v * rightGain
        pos++
        if (pos >= pulseLength) {
            pos = 0
            pulsesLeft--
            if (pulsesLeft == 0) countdown = (rate * period * (0.8f + 0.4f * rnd.nextFloat())).toInt()
        }
    }
}
