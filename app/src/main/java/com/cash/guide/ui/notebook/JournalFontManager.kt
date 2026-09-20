package com.cash.guide.ui.notebook

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily

/**
 * Options de polices arabes disponibles pour les textes arabes et darija.
 * Chaque option intègre un facteur de normalisation optique (opticalScale)
 * garantissant un volume visuel harmonieux et uniforme.
 */
enum class ArabicFontOption(
    val id: String,
    val title: String,
    val arabicName: String,
    val subtitle: String,
    val badge: String,
    val opticalScale: Float
) {
    BEIRUTI(
        id = "beiruti",
        title = "Beiruti",
        arabicName = "بيروتي",
        subtitle = "حديث وواضح ومريح للعين",
        badge = "Moderne",
        opticalScale = 1.16f
    ),
    ZAIN(
        id = "zain",
        title = "Zain",
        arabicName = "زين",
        subtitle = "يدوي ناعم، دافئ وأصيل (الافتراضي)",
        badge = "Défaut",
        opticalScale = 0.98f
    ),
    CREAM_FROTH(
        id = "cream_froth",
        title = "Cream Froth",
        arabicName = "كريم فروث",
        subtitle = "خط الكناش الأصلي التراثي",
        badge = "Authentique",
        opticalScale = 1.00f
    );

    val family: FontFamily
        get() = when (this) {
            BEIRUTI -> BeirutiFamily
            ZAIN -> ZainFamily
            CREAM_FROTH -> CreamFrothFamily
        }

    companion object {
        fun fromId(id: String?): ArabicFontOption {
            return entries.firstOrNull { it.id == id } ?: ZAIN
        }
    }
}

/**
 * Options de polices latines pour les textes français, anglais et les calculs.
 */
enum class LatinFontOption(
    val id: String,
    val title: String,
    val arabicName: String,
    val subtitle: String,
    val badge: String,
    val opticalScale: Float
) {
    PATRICK_HAND(
        id = "patrick_hand",
        title = "Patrick Hand",
        arabicName = "Patrick Hand",
        subtitle = "ستيلو يدوي حقيقي (الافتراضي)",
        badge = "Défaut",
        opticalScale = 1.00f
    ),
    IBM_PLEX(
        id = "ibm_plex",
        title = "IBM Plex Mono",
        arabicName = "IBM Plex",
        subtitle = "أرقام وحسابات كلاسيكية مستفة",
        badge = "Monospace",
        opticalScale = 0.92f
    ),
    ZAIN(
        id = "zain_latin",
        title = "Zain",
        arabicName = "Zain Latin",
        subtitle = "منحنيات ناعمة بالحروف اللاتينية",
        badge = "Doux",
        opticalScale = 0.98f
    ),
    TAJAWAL(
        id = "tajawal_latin",
        title = "Tajawal",
        arabicName = "Tajawal",
        subtitle = "هندسي متناسق ونظيف",
        badge = "Équilibré",
        opticalScale = 0.96f
    );

    val family: FontFamily
        get() = when (this) {
            PATRICK_HAND -> RawPatrickHandFamily
            IBM_PLEX -> IbmPlexMonoFamily
            ZAIN -> ZainFamily
            TAJAWAL -> RealTajawalFamily
        }

    companion object {
        fun fromId(id: String?): LatinFontOption {
            return entries.firstOrNull { it.id == id } ?: PATRICK_HAND
        }
    }
}

/**
 * Rétrocompatibilité avec les anciens écrans (Laboratoire / Showcase)
 */
enum class AppFontOption(val id: String) {
    ZAIN("zain"),
    BEIRUTI("beiruti"),
    TAJAWAL("tajawal"),
    DUO_PATRICK_ZAIN("duo_patrick_zain"),
    CREAM_FROTH("cream_froth"),
    IBM_PLEX("ibm_plex");

    companion object {
        fun fromId(id: String?): AppFontOption {
            return entries.firstOrNull { it.id == id } ?: ZAIN
        }
    }
}

/**
 * Gestionnaire réactif de la typographie globale du carnet :
 * - Choix indépendant de la police Arabe (Beiruti, Zain, Cream Froth)
 * - Choix indépendant de la police Française/Anglaise (Patrick Hand, IBM Plex, Zain, Tajawal)
 * - Normalisation optique automatique des hauteurs de police
 * - Échelle globale de taille de texte réglable par curseur (0.85x à 1.30x)
 */
object JournalFontManager {
    private const val PREFS_NAME = "journal_font_preferences"
    private const val KEY_ARABIC_FONT = "selected_arabic_font"
    private const val KEY_LATIN_FONT = "selected_latin_font"
    private const val KEY_FONT_SCALE = "journal_font_scale"
    private const val KEY_LEGACY_FONT = "selected_app_font"

    var currentArabicFont: ArabicFontOption by mutableStateOf(ArabicFontOption.ZAIN)
        private set

    var currentLatinFont: LatinFontOption by mutableStateOf(LatinFontOption.PATRICK_HAND)
        private set

    var globalFontScale: Float by mutableFloatStateOf(1.0f)
        private set

    // Bridge de compatibilité
    val currentFontOption: AppFontOption
        get() = when (currentArabicFont) {
            ArabicFontOption.BEIRUTI -> AppFontOption.BEIRUTI
            ArabicFontOption.ZAIN -> if (currentLatinFont == LatinFontOption.ZAIN) AppFontOption.ZAIN else AppFontOption.DUO_PATRICK_ZAIN
            ArabicFontOption.CREAM_FROTH -> AppFontOption.CREAM_FROTH
        }

    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val p = prefs!!

            val savedArabic = p.getString(KEY_ARABIC_FONT, null)
            val savedLatin = p.getString(KEY_LATIN_FONT, null)
            val savedScale = p.getFloat(KEY_FONT_SCALE, 1.0f)

            if (savedArabic != null) {
                currentArabicFont = ArabicFontOption.fromId(savedArabic)
            } else {
                // Migration depuis l'ancien paramètre unique si existant
                val legacy = p.getString(KEY_LEGACY_FONT, null)
                currentArabicFont = when (legacy) {
                    "beiruti" -> ArabicFontOption.BEIRUTI
                    "cream_froth" -> ArabicFontOption.CREAM_FROTH
                    else -> ArabicFontOption.ZAIN
                }
            }

            if (savedLatin != null) {
                currentLatinFont = LatinFontOption.fromId(savedLatin)
            } else {
                val legacy = p.getString(KEY_LEGACY_FONT, null)
                currentLatinFont = when (legacy) {
                    "ibm_plex" -> LatinFontOption.IBM_PLEX
                    "tajawal" -> LatinFontOption.TAJAWAL
                    "zain" -> LatinFontOption.ZAIN
                    else -> LatinFontOption.PATRICK_HAND
                }
            }

            globalFontScale = 1.0f
            p.edit().remove(KEY_FONT_SCALE).apply()
        }
    }

    fun setArabicFont(option: ArabicFontOption, context: Context? = null) {
        currentArabicFont = option
        val targetPrefs = prefs ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        targetPrefs?.edit()?.putString(KEY_ARABIC_FONT, option.id)?.apply()
    }

    fun setLatinFont(option: LatinFontOption, context: Context? = null) {
        currentLatinFont = option
        val targetPrefs = prefs ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        targetPrefs?.edit()?.putString(KEY_LATIN_FONT, option.id)?.apply()
    }

    fun setFontScale(scale: Float, context: Context? = null) {
        globalFontScale = 1.0f
        val targetPrefs = prefs ?: context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        targetPrefs?.edit()?.remove(KEY_FONT_SCALE)?.apply()
    }

    /**
     * Police utilisée pour les textes arabes / darija.
     */
    fun getArabicFont(option: AppFontOption = currentFontOption): FontFamily {
        return currentArabicFont.family
    }

    /**
     * Police utilisée pour les textes latins / français / anglais.
     */
    fun getLatinFont(option: AppFontOption = currentFontOption): FontFamily {
        return currentLatinFont.family
    }

    /**
     * Résout la police dynamiquement selon la langue du texte et la direction RTL.
     */
    fun resolveFont(text: String = "", isRtl: Boolean = false): FontFamily {
        return if (isArabicScript(text) || (text.isBlank() && isRtl)) {
            currentArabicFont.family
        } else {
            currentLatinFont.family
        }
    }

    /**
     * Calcule la taille de police normalisée et mise à l'échelle.
     */
    fun scaleFontSize(baseSp: Float, isArabic: Boolean): Float {
        val optical = if (isArabic) currentArabicFont.opticalScale else currentLatinFont.opticalScale
        return baseSp * optical
    }

    /**
     * Bridge pour préserver la compatibilité avec l'ancien sélecteur 1-clic.
     */
    fun setFont(option: AppFontOption, context: Context? = null) {
        when (option) {
            AppFontOption.BEIRUTI -> {
                setArabicFont(ArabicFontOption.BEIRUTI, context)
                setLatinFont(LatinFontOption.PATRICK_HAND, context)
            }
            AppFontOption.ZAIN -> {
                setArabicFont(ArabicFontOption.ZAIN, context)
                setLatinFont(LatinFontOption.ZAIN, context)
            }
            AppFontOption.TAJAWAL -> {
                setArabicFont(ArabicFontOption.CREAM_FROTH, context)
                setLatinFont(LatinFontOption.TAJAWAL, context)
            }
            AppFontOption.DUO_PATRICK_ZAIN -> {
                setArabicFont(ArabicFontOption.ZAIN, context)
                setLatinFont(LatinFontOption.PATRICK_HAND, context)
            }
            AppFontOption.CREAM_FROTH -> {
                setArabicFont(ArabicFontOption.CREAM_FROTH, context)
                setLatinFont(LatinFontOption.PATRICK_HAND, context)
            }
            AppFontOption.IBM_PLEX -> {
                setArabicFont(ArabicFontOption.CREAM_FROTH, context)
                setLatinFont(LatinFontOption.IBM_PLEX, context)
            }
        }
    }
}
