package com.kotlindiscord.kord.extensions.commands.events

import com.kotlindiscord.kord.extensions.commands.chat.ChatCommand
import dev.kord.core.event.message.MessageCreateEvent

/** Event emitted when a chat command is invoked. **/
public data class ChatCommandInvocationEvent(
    override val command: ChatCommand<*>,
    override val event: MessageCreateEvent
) : CommandInvocationEvent<ChatCommand<*>, MessageCreateEvent>

/** Event emitted when a chat command invocation succeeds. **/
public data class ChatCommandSucceededEvent(
    override val command: ChatCommand<*>,
    override val event: MessageCreateEvent
) : CommandSucceededEvent<ChatCommand<*>, MessageCreateEvent>

/** Event emitted when a chat command's checks fail. **/
public data class ChatCommandFailedChecksEvent(
    override val command: ChatCommand<*>,
    override val event: MessageCreateEvent,
    override val reason: String,
) : CommandFailedChecksEvent<ChatCommand<*>, MessageCreateEvent>

/** Event emitted when a chat command's argument parsing fails. **/
public data class ChatCommandFailedParsingEvent(
    override val command: ChatCommand<*>,
    override val event: MessageCreateEvent,
    override val reason: String,
) : CommandFailedParsingEvent<ChatCommand<*>, MessageCreateEvent>

/** Event emitted when a chat command's invocation fails with an exception. **/
public data class ChatCommandFailedWithExceptionEvent(
    override val command: ChatCommand<*>,
    override val event: MessageCreateEvent,
    override val throwable: Throwable
) : CommandFailedWithExceptionEvent<ChatCommand<*>, MessageCreateEvent>