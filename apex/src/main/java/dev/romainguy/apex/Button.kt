package dev.romainguy.apex

import android.graphics.Paint
import android.graphics.RectF
import android.util.SizeF
import android.view.MotionEvent

enum class State {
    Enabled,
    Disabled
}

class ButtonModel(
    var label: String,
    var state: State = State.Enabled,
    var onClick: () -> Unit = { }
) {
    val isEnabled get() = state == State.Enabled
}

fun Element.Button(model: ButtonModel) = ChildElement {
    addComponent(model)

    data class InternalState(var isPressed: Boolean = false)
    addComponent(InternalState())

    Padding(RectF(8.0f, 4.0f, 8.0f, 4.0f))

    val paint = Paint().apply {
        isAntiAlias = true
    }

    val textLayoutComponent = object : LayoutComponent() {
        var textWidth = 0.0f
        var textHeight = 0.0f

        override fun layout(providers: Providers, element: Element, size: SizeF): SizeF {
            val density = providers.get<DensityProvider>()
            val theme = providers.get<ThemeProvider>()
            val padding = density.toPx(element.component<PaddingComponent>().padding)

            paint.typeface = theme.typeface
            paint.textSize = density.toPx(theme.fontSize)
            textWidth = paint.measureText(model.label)
            val fontMetrics = paint.fontMetrics
            textHeight = -fontMetrics.top + fontMetrics.bottom

            val strokeWidth = density.toPx(theme.strokeWidth)
            return SizeF(
                textWidth + padding.left + padding.right + 2.0f * strokeWidth,
                textHeight + padding.top + padding.bottom
            )
        }
    }

    Render { providers, element, canvas ->
        val density = providers.get<DensityProvider>()
        val theme = providers.get<ThemeProvider>()
        val bounds = element.component<LayoutComponent>().bounds
        val radius = density.toPx(theme.cornerRadius)
        val internalState = element.component<InternalState>()

        if (theme.style != Paint.Style.STROKE) {
            paint.style = Paint.Style.FILL
            val color = if (model.isEnabled) theme.contentBackground else theme.contentDisabled
            paint.color = (if (!internalState.isPressed) color else color.complementary()).toArgb()
            canvas.drawRoundRect(
                0.0f,
                0.0f,
                bounds.width(),
                bounds.height(),
                radius,
                radius,
                paint
            )
        }

        if (theme.style != Paint.Style.FILL) {
            paint.strokeWidth = density.toPx(theme.strokeWidth)
            paint.style = Paint.Style.STROKE
            val color = if (model.isEnabled) theme.border else theme.disabled
            paint.color = (if (!internalState.isPressed) color else color.complementary()).toArgb()

            canvas.drawRoundRect(
                0.0f,
                0.0f,
                bounds.width(),
                bounds.height(),
                radius,
                radius,
                paint
            )
        }

        val color = if (model.isEnabled) theme.text else theme.disabled
        paint.color = (if (!internalState.isPressed) color else color.complementary()).toArgb()
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0.0f

        val x = (bounds.width() - textLayoutComponent.textWidth) * 0.5f
        val y = (bounds.height() - textLayoutComponent.textHeight) * 0.5f - paint.ascent()
        canvas.drawText(model.label, x, y, paint)
    }

    Layout(textLayoutComponent)

    MotionInput { _, element, event ->
        if (element.component<ButtonModel>().isEnabled) {
            val internalState = element.component<InternalState>()
            if (event.action == MotionEvent.ACTION_DOWN) {
                internalState.isPressed = true
                true
            } else if (event.action == MotionEvent.ACTION_UP) {
                if (internalState.isPressed) {
                    internalState.isPressed = false
                    element.component<ButtonModel>().onClick()
                }
                true
            } else if (event.action == MotionEvent.ACTION_CANCEL) {
                internalState.isPressed = false
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}