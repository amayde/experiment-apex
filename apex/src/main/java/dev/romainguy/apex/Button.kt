package dev.romainguy.apex

import android.graphics.Paint
import android.graphics.RectF
import android.util.SizeF

class ButtonModel(
    var label: String
)

// How to link state from model to internal state ? Is it a viable solution ? => test to implement on Image
fun Element.Button(model: ButtonModel, state: State = State.Enabled, content: Element.() -> Unit = { }) = ChildElement {
    addComponent<ButtonModel>(model)
    addComponent<InternalState>(InternalState(state = state))

    data class ButtonInternalState(
        var textWidth: Float = 0.0f,
        var textHeight: Float = 0.0f
    )
    addComponent<ButtonInternalState>(ButtonInternalState())

    Padding(RectF(8.0f, 4.0f, 8.0f, 4.0f))

    val paint = Paint().apply {
        isAntiAlias = true
    }

    Render { providers, element, renderer ->
        with(providers.get<DensityProvider>()) {
            val theme = providers.get<ThemeProvider>()
            val bounds = element.component<LayoutComponent>().bounds
            val radius = theme.cornerRadius.toPx()
            val buttonInternalState = element.component<ButtonInternalState>()
            val internalState = element.component<InternalState>()

            if (theme.style != Paint.Style.STROKE) {
                paint.style = Paint.Style.FILL
                val color = if (internalState.isEnabled) theme.contentBackground else theme.contentDisabled
                paint.color =
                    (if (!internalState.isPressed) color else color.complementary()).toArgb()
            }

            if (theme.style != Paint.Style.FILL) {
                paint.strokeWidth = theme.strokeWidth.toPx()
                paint.style = Paint.Style.STROKE
                val color = if (internalState.isEnabled) theme.border else theme.disabled
                paint.color =
                    (if (!internalState.isPressed) color else color.complementary()).toArgb()

            }

            renderer.drawRoundRect(
                Rect(0f, 0f, bounds.width(), bounds.height()),
                Point(radius, radius),
                paint
            )


            val color = if (internalState.isEnabled) theme.text else theme.disabled
            paint.color = (if (!internalState.isPressed) color else color.complementary()).toArgb()
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 0.0f

            val x = (bounds.width() - buttonInternalState.textWidth) * 0.5f
            val y = (bounds.height() - buttonInternalState.textHeight) * 0.5f - paint.ascent()
            renderer.move(x, y)
            renderer.drawText(model.label, paint)
            renderer.move(-x, -y)
        }
    }

    Layout { providers, element, _ ->
        with(providers.get<DensityProvider>()) {
            val theme = providers.get<ThemeProvider>()
            val padding = element.component<PaddingComponent>().padding.toPx()
            val buttonInternalState = element.component<ButtonInternalState>()

            paint.typeface = theme.typeface
            paint.textSize = theme.fontSize.toPx()
            buttonInternalState.textWidth = paint.measureText(model.label)
            val fontMetrics = paint.fontMetrics
            buttonInternalState.textHeight = -fontMetrics.top + fontMetrics.bottom

            val strokeWidth = theme.strokeWidth.toPx()
            SizeF(
                buttonInternalState.textWidth + padding.left + padding.right + 2.0f * strokeWidth,
                buttonInternalState.textHeight + padding.top + padding.bottom
            )
        }
    }

    content()
}
