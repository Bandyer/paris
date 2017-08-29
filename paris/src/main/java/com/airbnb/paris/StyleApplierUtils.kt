package com.airbnb.paris

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.view.View

@VisibleForTesting
class StyleApplierUtils {

    class DebugListener(
            private val viewToStyles: HashMap<View, MutableSet<Style>>,
            private val styleToAttrNames: HashMap<Style, MutableSet<String>>,
            private val ignoredAttributeIndexes: IntArray?) : Style.DebugListener {

        private fun <K, V> getOrDefault(map: Map<K, V>, key: K, default: V): V {
            return if (map.containsKey(key)) {
                map[key]!!
            } else {
                default
            }
        }

        override fun beforeTypedArrayProcessed(view: View, style: Style, attributes: IntArray, attributesWithDefaultValue: IntArray?, typedArray: TypedArrayWrapper) {
            val styles = getOrDefault(viewToStyles, view, HashSet())
            styles.add(style)
            viewToStyles.put(view, styles)

            val attrIndexes = getAttributeIndexes(typedArray, attributesWithDefaultValue)
            val newAttrNames = getAttrNames(view.context, attributes, attrIndexes)
            val attrNames = getOrDefault(styleToAttrNames, style, HashSet())
            attrNames.addAll(newAttrNames)
            styleToAttrNames.put(style, attrNames)
        }
    }

    companion object {

        private fun getAttrNames(context: Context, attrs: IntArray, attrIndexes: Set<Int>) =
                attrIndexes.map { index -> context.resources.getResourceEntryName(attrs[index]) }.toSet()

        @VisibleForTesting
        fun assertSameAttributes(applier: StyleApplier<*, *>, vararg parentStyles: Style) {
            if (parentStyles.size <= 1) {
                return
            }

            // TODO What about substyles???

            // These can be safely ignored since a value is always applied
            val attributesWithDefaultValue = applier.attributesWithDefaultValue()

            val viewToStyles = HashMap<View, MutableSet<Style>>()
            val styleToAttrNames = HashMap<Style, MutableSet<String>>()

            for (parentStyle in parentStyles) {
                parentStyle.debugListener = DebugListener(viewToStyles, styleToAttrNames, attributesWithDefaultValue)
                applier.apply(parentStyle)
            }

            var hasError = false
            val errorBuilder = StringBuilder()
            for ((view, styles) in viewToStyles) {
                val allAttrNames = styles.flatMap { style ->
                    styleToAttrNames[style]!!
                }.toSet()

                for (style in styles) {
                    val missingAttrNames = allAttrNames.subtract(styleToAttrNames[style]!!)
                    if (missingAttrNames.isNotEmpty()) {
                        hasError = true
                        errorBuilder.append(getMissingStyleAttributesError(view, style, styles.minus(style), missingAttrNames))
                    }
                }
            }

            if (hasError) {
                throw AssertionError(errorBuilder)
            }
        }

        private fun getMissingStyleAttributesError(view: View, style: Style, otherStyles: Set<Style>, missingAttrNames: Set<String>): String {
            val context = view.context
            val viewName = view.javaClass.simpleName
            val styleName = style.name(context)
            return """
                |The $viewName style "$styleName" is missing the following attributes:
                |${missingAttrNames.joinToString("\n") { "✕ $it" }}
                |It must declare the same attributes as the following styles:
                |${otherStyles.joinToString(", ") { it.name(context) }}
                """.trimMargin()
        }

        internal fun getAttributeIndexes(typedArray: TypedArrayWrapper, ignoredAttributeIndexes: IntArray?): Set<Int> {
            return (0 until typedArray.getIndexCount())
                    .map { typedArray.getIndex(it) }
                    .filter { ignoredAttributeIndexes == null || !ignoredAttributeIndexes.contains(it) }
                    .toSet()
        }
    }
}
