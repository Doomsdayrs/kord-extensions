@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.modules.unsafe.commands

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.application.user.UserCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.contexts.UnsafeUserCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialUserCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondPublic
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.UserCommandInteractionCreateEvent

/** Like a standard user command, but with less safety features. **/
@UnsafeAPI
public class UnsafeUserCommand(
    extension: Extension
) : UserCommand<UnsafeUserCommandContext>(extension) {
    /** Initial response type. Change this to decide what happens when this user command action is executed. **/
    public var initialResponse: InitialUserCommandResponse = InitialUserCommandResponse.EphemeralAck

    override suspend fun call(event: UserCommandInteractionCreateEvent) {
        try {
            if (!runChecks(event)) {
                return
            }
        } catch (e: CommandException) {
            event.interaction.respondPublic { content = e.reason }

            return
        }

        val response = when (val r = initialResponse) {
            is InitialUserCommandResponse.EphemeralAck -> event.interaction.acknowledgeEphemeral()
            is InitialUserCommandResponse.PublicAck -> event.interaction.acknowledgePublic()

            is InitialUserCommandResponse.EphemeralResponse -> event.interaction.respondEphemeral {
                r.builder!!(event)
            }

            is InitialUserCommandResponse.PublicResponse -> event.interaction.respondPublic {
                r.builder!!(event)
            }
        }

        val context = UnsafeUserCommandContext(event, this, response)

        context.populate()

        firstSentryBreadcrumb(context)

        try {
            checkBotPerms(context)
            body(context)
        } catch (e: CommandException) {
            respondText(context, e.reason)
        } catch (t: Throwable) {
            handleError(context, t)
        }
    }

    override suspend fun respondText(context: UnsafeUserCommandContext, message: String) {
        when (context.interactionResponse) {
            is PublicInteractionResponseBehavior -> context.respondPublic { content = message }
            is EphemeralInteractionResponseBehavior -> context.respondEphemeral { content = message }
        }
    }
}