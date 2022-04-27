@file:Suppress("NOTHING_TO_INLINE")

package dev.romainguy.apex

import android.graphics.RectF
import android.util.SizeF
import android.view.MotionEvent

val EmptySize = SizeF(0.0f, 0.0f)
val UnboundedSize = SizeF(Float.MAX_VALUE, Float.MAX_VALUE)

interface RenderComponent {
    fun render(providers: Providers, element: Element, renderer: Renderer)
}

fun Element.Render(render: (providers: Providers, element: Element, renderer: Renderer) -> Unit) {
    addComponent<RenderComponent>(object : RenderComponent {
        override fun render(providers: Providers, element: Element, renderer: Renderer) {
            render(providers, element, renderer)
        }
    })
}

abstract class LayoutComponent {
    var bounds = RectF()

    abstract fun layout(providers: Providers, element: Element, size: SizeF): SizeF

    fun minSize(providers: Providers, element: Element) = EmptySize
    fun maxSize(providers: Providers, element: Element) = UnboundedSize
}

fun Element.Layout(layout: (providers: Providers, element: Element, size: SizeF) -> SizeF) {
    addComponent<LayoutComponent>(object : LayoutComponent() {
        override fun layout(providers: Providers, element: Element, size: SizeF): SizeF {
            return layout(providers, element, size)
        }
    })
}

interface MotionInputComponent {
    fun motionInput(providers: Providers, element: Element, event: MotionEvent): Boolean
}

fun Element.MotionInput(
    action: (providers: Providers, element: Element, event: MotionEvent) -> Boolean
) {
    addComponent<MotionInputComponent>(object : MotionInputComponent {
        override fun motionInput(
            providers: Providers,
            element: Element,
            event: MotionEvent
        ): Boolean {
            return action(providers, element, event)
        }
    })
}

interface ProviderComponent {
    fun provide(providers: Providers, element: Element)
}

inline fun <reified T : Any> Element.Provider(localProvider: T) {
    addComponent<ProviderComponent>(object : ProviderComponent {
        override fun provide(providers: Providers, element: Element) {
            providers.set(localProvider)
        }
    })
}

class PaddingComponent(val padding: RectF) {
    constructor(padding: Float) : this(RectF(padding, padding, padding, padding))
}

inline fun Element.Padding(padding: RectF) {
    addComponent<PaddingComponent>(PaddingComponent(padding))
}

inline fun Element.Padding(padding: Float) {
    addComponent<PaddingComponent>(PaddingComponent(padding))
}

enum class VerticalAlignment {
    Start,
    Center,
    End
}

enum class HorizontalAlignment {
    Start,
    Center,
    End
}

inline fun Element.Alignment(alignment: VerticalAlignment) {
    addComponent<VerticalAlignment>(alignment)
}

inline fun Element.Alignment(alignment: HorizontalAlignment) {
    addComponent<HorizontalAlignment>(alignment)
}

enum class State {
    Enabled,
    Disabled
}

class OnClickModel(
    var onClick: (element: Element) -> Unit = { }
)

class InternalState(
    var state: State = State.Enabled
) {
    val isEnabled get() = state == State.Enabled
    var isPressed: Boolean = false
}

inline fun Element.OnClick(onClickModel: OnClickModel) {
    addComponent<OnClickModel>(onClickModel)

    MotionInput { _, element, event ->
        if (element.component<InternalState>().isEnabled) {
            val internalState = element.component<InternalState>()
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    internalState.isPressed = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (internalState.isPressed) {
                        internalState.isPressed = false
                        element.component<OnClickModel>().onClick(element)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    internalState.isPressed = false
                    true
                }
                else -> false // TODO BUG_FIX MotionEvent.ACTION_MOVE out of the element
            }
        } else {
            false
        }
    }
}