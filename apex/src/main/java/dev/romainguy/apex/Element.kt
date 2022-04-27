package dev.romainguy.apex

import kotlin.reflect.KClass

open class Element(content: Element.() -> Unit = { }) {
    var parent = this
        private set

    private val children = mutableListOf<Element>()
    private val components = linkedMapOf<KClass<*>, MutableList<Any>>()

    init {
        content()
    }

    fun addChild(child: Element): Element {
        child.parent = this
        children.add(child)
        return this
    }

    fun forEachChild(action: (Element) -> Unit) {
        for (child in children) {
            action(child)
        }
    }

    fun findChild(predicate: (Element) -> Boolean): Element? {
        for (child in children) {
            if (predicate(child)) return child
            val candidate = child.findChild(predicate)
            if (candidate != null) return candidate
        }
        return null
    }

    fun requireChild(predicate: (Element) -> Boolean): Element {
        for (child in children) {
            if (predicate(child)) return child
            val candidate = child.findChild(predicate)
            if (candidate != null) return candidate
        }
        throw IllegalArgumentException("Cannot find child matching predicate")
    }

    inline fun <reified T : Any> findChild(componentValue: T) =
        findChild { it.componentOrNull(T::class) == componentValue }

    inline fun <reified T : Any> requireChild(componentValue: T) =
        requireChild { it.componentOrNull(T::class) == componentValue }

    fun <T: Any> addComponent(type: KClass<T>, component: Any) {
        components[type]?.apply {
            add(component)
        } ?: components.put(type, mutableListOf(component))
    }

    inline fun <reified T: Any> addComponent(component: Any): Element {
        addComponent(T::class, component)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> component(type: KClass<T>) =
        components[type]?.first() as T

    inline fun <reified T : Any> component() = component(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> componentOrNull(type: KClass<T>) =
        components[type]?.firstOrNull() as T?

    inline fun <reified T : Any> componentOrNull() = componentOrNull(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> applyComponent(type: KClass<T>, action: T.()-> Unit) {
        components[type]?.forEach { component ->
            (component as T).action()
        }
    }

    inline fun <reified T : Any> applyComponent(noinline action: T.() -> Unit) {
        applyComponent(T::class, action)
    }

    fun ChildElement(content: Element.() -> Unit): Element {
        val child = Element()
        child.content()
        addChild(child)
        return child
    }
}
