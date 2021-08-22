package com.kotlindiscord.kord.extensions.commands.content

import com.kotlindiscord.kord.extensions.CommandRegistrationException
import com.kotlindiscord.kord.extensions.InvalidCommandException
import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.parser.StringParser
import com.kotlindiscord.kord.extensions.utils.getLocale
import dev.kord.core.event.message.MessageCreateEvent
import mu.KotlinLogging
import org.koin.core.component.inject
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Class representing a grouped command, which is essentially a command with its own subcommands.
 *
 * You shouldn't need to use this class directly - instead, create an `Extension` and use the
 * `group` function to register your command group, by overriding the `Extension` setup function.
 *
 * @param extension The extension that registered this grouped command.
 * @param parent The [MessageContentGroupCommand] this group exists under, if any.
 */
@Suppress("LateinitVarOverridesLateinitVar")
// This is intentional
@ExtensionDSL
public open class MessageContentGroupCommand<T : Arguments>(
    extension: Extension,
    arguments: (() -> T)? = null,
    public open val parent: MessageContentGroupCommand<out Arguments>? = null
) : MessageContentCommand<T>(extension, arguments) {
    /** @suppress **/
    public val botSettings: ExtensibleBotBuilder by inject()

    /** @suppress **/
    public open val commands: MutableList<MessageContentCommand<out Arguments>> = mutableListOf()

    override lateinit var name: String

    /** @suppress **/
    override var body: suspend MessageContentCommandContext<out T>.() -> Unit = {
        sendHelp()
    }

    /**
     * An internal function used to ensure that all of a command group's required arguments are present.
     *
     * @throws InvalidCommandException Thrown when a required argument hasn't been set.
     */
    @Throws(InvalidCommandException::class)
    override fun validate() {
        if (!::name.isInitialized) {
            throw InvalidCommandException(null, "No command name given.")
        }

        if (commands.isEmpty()) {
            throw InvalidCommandException(name, "No subcommands registered.")
        }
    }

    /**
     * DSL function for easily registering a command.
     *
     * Use this in your setup function to register a command that may be executed on Discord.
     *
     * @param body Builder lambda used for setting up the command object.
     */
    @ExtensionDSL
    public open suspend fun <R : Arguments> messageContentCommand(
        arguments: (() -> R)?,
        body: suspend MessageContentCommand<R>.() -> Unit
    ): MessageContentCommand<R> {
        val commandObj = MessageContentSubCommand<R>(extension, arguments, this)
        body.invoke(commandObj)

        return messageContentCommand(commandObj)
    }

    /**
     * DSL function for easily registering a command, without arguments.
     *
     * Use this in your setup function to register a command that may be executed on Discord.
     *
     * @param body Builder lambda used for setting up the command object.
     */
    @ExtensionDSL
    public open suspend fun messageContentCommand(
        body: suspend MessageContentCommand<Arguments>.() -> Unit
    ): MessageContentCommand<Arguments> {
        val commandObj = MessageContentSubCommand<Arguments>(extension, parent = this)
        body.invoke(commandObj)

        return messageContentCommand(commandObj)
    }

    /**
     * Function for registering a custom command object.
     *
     * You can use this if you have a custom command subclass you need to register.
     *
     * @param commandObj MessageCommand object to register.
     */
    @ExtensionDSL
    public open suspend fun <R : Arguments> messageContentCommand(
        commandObj: MessageContentCommand<R>
    ): MessageContentCommand<R> {
        try {
            commandObj.validate()
            commands.add(commandObj)
        } catch (e: CommandRegistrationException) {
            logger.error(e) { "Failed to register subcommand - $e" }
        } catch (e: InvalidCommandException) {
            logger.error(e) { "Failed to register subcommand - $e" }
        }

        return commandObj
    }

    /**
     * DSL function for easily registering a grouped command.
     *
     * Use this in your setup function to register a group of commands.
     *
     * The body of the grouped command will be executed if there is no
     * matching subcommand.
     *
     * @param body Builder lambda used for setting up the command object.
     */
    @ExtensionDSL
    @Suppress("MemberNameEqualsClassName")  // Really?
    public open suspend fun <R : Arguments> messageContentGroupCommand(
        arguments: (() -> R)?,
        body: suspend MessageContentGroupCommand<R>.() -> Unit
    ): MessageContentGroupCommand<R> {
        val commandObj = MessageContentGroupCommand(extension, arguments, this)
        body.invoke(commandObj)

        return messageContentCommand(commandObj) as MessageContentGroupCommand<R>
    }

    /**
     * DSL function for easily registering a grouped command, without its own arguments.
     *
     * Use this in your setup function to register a group of commands.
     *
     * The body of the grouped command will be executed if there is no
     * matching subcommand.
     *
     * @param body Builder lambda used for setting up the command object.
     */
    @ExtensionDSL
    @Suppress("MemberNameEqualsClassName")  // Really?
    public open suspend fun messageContentGroupCommand(
        body: suspend MessageContentGroupCommand<Arguments>.() -> Unit
    ): MessageContentGroupCommand<Arguments> {
        val commandObj = MessageContentGroupCommand<Arguments>(extension, parent = this)
        body.invoke(commandObj)

        return messageContentCommand(commandObj) as MessageContentGroupCommand<Arguments>
    }

    /** @suppress **/
    public open suspend fun getCommand(
        name: String?,
        event: MessageCreateEvent
    ): MessageContentCommand<out Arguments>? {
        name ?: return null

        val defaultLocale = botSettings.i18nBuilder.defaultLocale
        val locale = event.getLocale()

        return commands.firstOrNull { it.getTranslatedName(locale) == name }
            ?: commands.firstOrNull { it.getTranslatedAliases(locale).contains(name) }
            ?: commands.firstOrNull { it.localeFallback && it.getTranslatedName(defaultLocale) == name }
            ?: commands.firstOrNull { it.localeFallback && it.getTranslatedAliases(defaultLocale).contains(name) }
    }

    /**
     * Execute this grouped command, given a [MessageCreateEvent].
     *
     * This function takes a [MessageCreateEvent] (generated when a message is received), and
     * processes it. The command's checks are invoked and, assuming all of the
     * checks passed, the command will search for a subcommand matching the first argument.
     * If a subcommand is found, it will be executed - otherwise, the the
     * [command body][action] is executed.
     *
     * If an exception is thrown by the [command body][action], it is caught and a traceback
     * is printed.
     *
     * @param event The message creation event.
     */
    override suspend fun call(
        event: MessageCreateEvent,
        commandName: String,
        parser: StringParser,
        argString: String,
        skipChecks: Boolean
    ) {
        if (skipChecks || !runChecks(event)) {
            return
        }

        val command = parser.parseNext()?.data?.lowercase()
        val remainingArgs = parser.consumeRemaining()
        val subCommand = getCommand(command, event)

        if (subCommand == null) {
            super.call(event, commandName, parser, argString, true)
        } else {
            subCommand.call(event, commandName, StringParser(remainingArgs), argString)
        }
    }

    /** Get the full command name, translated, with parent commands taken into account. **/
    public open suspend fun getFullTranslatedName(locale: Locale): String {
        parent ?: return this.getTranslatedName(locale)

        return parent!!.getFullTranslatedName(locale) + " " + this.getTranslatedName(locale)
    }
}
