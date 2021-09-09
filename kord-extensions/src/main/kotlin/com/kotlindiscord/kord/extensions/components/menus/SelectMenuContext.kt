package com.kotlindiscord.kord.extensions.components.menus

import com.kotlindiscord.kord.extensions.components.Component
import com.kotlindiscord.kord.extensions.components.ComponentContext
import dev.kord.core.event.interaction.SelectMenuInteractionCreateEvent

/** Abstract class representing the execution context of a select (dropdown) menu component. **/
public abstract class SelectMenuContext(
    component: Component,
    event: SelectMenuInteractionCreateEvent
) : ComponentContext<SelectMenuInteractionCreateEvent>(component, event) {
    /** Menu options that were selected by the user before de-focusing the menu. **/
    public val selected: List<String> by lazy { event.interaction.values }
}