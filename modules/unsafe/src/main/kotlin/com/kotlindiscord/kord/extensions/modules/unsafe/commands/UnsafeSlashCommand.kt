@file:Suppress("TooGenericExceptionCaught")

package com.kotlindiscord.kord.extensions.modules.unsafe.commands

import com.kotlindiscord.kord.extensions.CommandException
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.SlashGroup
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.contexts.UnsafeSlashCommandContext
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondPublic
import dev.kord.core.behavior.interaction.EphemeralInteractionResponseBehavior
import dev.kord.core.behavior.interaction.PublicInteractionResponseBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.entity.interaction.GroupCommand
import dev.kord.core.entity.interaction.SubCommand
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent

/** Like a standard slash command, but with less safety features. **/
@UnsafeAPI
public class UnsafeSlashCommand<A : Arguments>(
    extension: Extension,

    public override val arguments: (() -> A)? = null,
    public override val parentCommand: SlashCommand<*, *>? = null,
    public override val parentGroup: SlashGroup? = null
) : SlashCommand<UnsafeSlashCommandContext<A>, A>(extension) {
    /** Initial response type. Change this to decide what happens when this slash command is executed. **/
    public var initialResponse: InitialSlashCommandResponse = InitialSlashCommandResponse.EphemeralAck

    override suspend fun call(event: ChatInputCommandInteractionCreateEvent) {
        val eventCommand = event.interaction.command

        val commandObj: SlashCommand<*, *> = when (eventCommand) {
            is SubCommand -> {
                val firstSubCommandKey = eventCommand.name

                this.subCommands.firstOrNull { it.name == firstSubCommandKey }
                    ?: error("Unknown subcommand: $firstSubCommandKey")
            }

            is GroupCommand -> {
                val firstEventGroupKey = eventCommand.groupName
                val group = this.groups[firstEventGroupKey] ?: error("Unknown command group: $firstEventGroupKey")
                val firstSubCommandKey = eventCommand.name

                group.subCommands.firstOrNull { it.name == firstSubCommandKey }
                    ?: error("Unknown subcommand: $firstSubCommandKey")
            }

            else -> this
        }

        try {
            if (!commandObj.runChecks(event)) {
                return
            }
        } catch (e: CommandException) {
            event.interaction.respondPublic { content = e.reason }

            return
        }

        commandObj.run(event)
    }

    override suspend fun run(event: ChatInputCommandInteractionCreateEvent) {
        val response = when (val r = initialResponse) {
            is InitialSlashCommandResponse.EphemeralAck -> event.interaction.acknowledgeEphemeral()
            is InitialSlashCommandResponse.PublicAck -> event.interaction.acknowledgePublic()

            is InitialSlashCommandResponse.EphemeralResponse -> event.interaction.respondEphemeral {
                r.builder!!(event)
            }

            is InitialSlashCommandResponse.PublicResponse -> event.interaction.respondPublic {
                r.builder!!(event)
            }
        }

        val context = UnsafeSlashCommandContext(event, this, response)

        context.populate()

        firstSentryBreadcrumb(context, this)

        try {
            checkBotPerms(context)

            if (arguments != null) {
                val args = registry.argumentParser.parse(arguments, context)

                context.populateArgs(args)
            }

            body(context)
        } catch (e: CommandException) {
            respondText(context, e.reason)
        } catch (t: Throwable) {
            handleError(context, t, this)
        }
    }

    override suspend fun respondText(context: UnsafeSlashCommandContext<A>, message: String) {
        when (context.interactionResponse) {
            is PublicInteractionResponseBehavior -> context.respondPublic { content = message }
            is EphemeralInteractionResponseBehavior -> context.respondEphemeral { content = message }
        }
    }
}