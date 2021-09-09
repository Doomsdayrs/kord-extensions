package com.kotlindiscord.kord.extensions.components.buttons

import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.component.ActionRowBuilder

/** Class representing a disabled button component, which has no action. **/
public open class DisabledInteractionButton : InteractionButtonWithID() {
    /** Button style - anything but Link is valid. **/
    public open var style: ButtonStyle = ButtonStyle.Primary

    override fun apply(builder: ActionRowBuilder) {
        builder.interactionButton(style, id) {
            emoji = partialEmoji
            label = this@DisabledInteractionButton.label

            disabled = true
        }
    }

    override fun validate() {
        super.validate()

        if (style == ButtonStyle.Link) {
            error("The Link button style is reserved for link buttons.")
        }
    }
}