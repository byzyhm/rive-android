package app.rive.runtime.example

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import app.rive.runtime.example.databinding.ActivityFontFallbackBinding
import app.rive.runtime.example.databinding.TextRunDemoBinding
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.fonts.FontBytes
import app.rive.runtime.kotlin.fonts.FontFallbackStrategy
import app.rive.runtime.kotlin.fonts.FontHelper
import app.rive.runtime.kotlin.fonts.Fonts


class TextRunDemoActivity : AppCompatActivity(), FontFallbackStrategy {

    private lateinit var binding: TextRunDemoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = TextRunDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateTextRuns()
        FontFallbackStrategy.stylePicker = this
    }

    override fun getFont(weight: Fonts.Weight): List<FontBytes> {
        var fontMatch = Fonts.FontOpts(
            familyName = "serif",
        )
        when {
            // 'Invert' the weights to make the fallback chars more prominent.
            weight.weight < 400 -> fontMatch =
                Fonts.FontOpts(familyName = "sans-serif", weight = Fonts.Weight(900))

            weight.weight > 400 -> fontMatch =
                Fonts.FontOpts(familyName = "sans-serif", weight = Fonts.Weight(100))
        }
        val fonts = listOf(
            fontMatch,
            // Tag a Thai font along so our second view can draw the glyphs
            Fonts.FontOpts("NotoSansThai-Regular.ttf")
        )
        return fonts.mapNotNull { FontHelper.getFallbackFontBytes(it) }
    }


    override fun onDestroy() {
        super.onDestroy()
        // 清理策略引用
        if (FontFallbackStrategy.stylePicker === this) {
            FontFallbackStrategy.stylePicker = null
        }
    }

    private fun updateTextRuns() {
        binding.onboardingPart1Unfont.setTextRunValue("Content_1", "Content_1 ")
        binding.onboardingPart1Unfont.setTextRunValue("Content_2", "Content_2 ")
        binding.onboardingPart1Unfont.setTextRunValue("Content_3", "Content_3 ")
        binding.onboardingPart1Unfont.setTextRunValue("Translated_1", "Translated_1 ")
        binding.onboardingPart1Unfont.setTextRunValue("Translated_2", "Translated_2 ")
        binding.onboardingPart1Unfont.setTextRunValue("Translated_3", "Translated_3 ")
        binding.onboardingPart1WithFont.setTextRunValue("Content_1", "Content_1 ")
        binding.onboardingPart1WithFont.setTextRunValue("Content_2", "Content_2 ")
        binding.onboardingPart1WithFont.setTextRunValue("Content_3", "Content_3 ")
        binding.onboardingPart1WithFont.setTextRunValue("Translated_1", "Translated_1 ")
        binding.onboardingPart1WithFont.setTextRunValue("Translated_2", "Translated_2 ")
        binding.onboardingPart1WithFont.setTextRunValue("Translated_3", "Translated_3 ")
//        binding.untitled.setTextRunValue("Content_3", " Content_3")
    }
}
