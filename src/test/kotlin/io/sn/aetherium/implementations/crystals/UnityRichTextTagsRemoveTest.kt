package io.sn.aetherium.implementations.crystals

import kotlin.test.Test
import kotlin.test.assertEquals

class UnityRichTextTagsRemoveTest {

    private val unityRichTextTags = listOf(
        "align", "allcaps", "alpha", "b", "br", "color", "cspace", "font", "font-weight", "gradient", "i",
        "indent", "line-height", "line-indent", "link", "lowercase", "margin", "mark", "mspace", "nobr",
        "noparse", "page", "pos", "rotate", "s", "size", "smallcaps", "space", "sprite", "strikethrough",
        "style", "sub", "sup", "u", "uppercase", "voffset", "width"
    )

    @Test
    fun `test unity rich text tags remover`() {
        val before =
            "<u><color=#ee7cc5>S<color=#a783fb>U<color=#e687f1>P<color=#ffffff>E<color=#ee7cc5>R<color=#a783fb>N<color=#e687f1>O<color=#ffffff>V<color=#ee7cc5>A</color>"
        val expected = "SUPERNOVA"

        assertEquals(removeUnityRichTextTags(before), expected)
    }

    private fun removeUnityRichTextTags(input: String): String {
        var rst = input
        unityRichTextTags.forEach { tag ->
            rst = rst.replace("</?$tag=[^>]+>|</$tag>|<$tag>".toRegex(), "")
        }
        return rst
    }

}