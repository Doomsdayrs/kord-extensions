/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.kotlindiscord.kord.extensions.commands

import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.userFor
import com.kotlindiscord.kord.extensions.i18n.TranslationsProvider
import com.kotlindiscord.kord.extensions.sentry.SentryContext
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MemberBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.ChannelBehavior
import dev.kord.core.event.Event
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

/**
 * Light wrapper class representing the context for a command's action.
 *
 * This is what `this` refers to in a command action body. You shouldn't have to
 * instantiate this yourself.
 *
 * @param command Respective command for this context object.
 * @param eventObj Event that triggered this command.
 * @param commandName Command name given by the user to invoke the command - lower-cased.
 */
@ExtensionDSL
public abstract class CommandContext(
    public open val command: Command,
    public open val eventObj: Event,
    public open val commandName: String,
) : KoinComponent {
    /** Translations provider, for retrieving translations. **/
    public val translationsProvider: TranslationsProvider by inject()

    /** Current Sentry context, containing breadcrumbs and other goodies. **/
    public val sentry: SentryContext = SentryContext()

    /** Cached locale variable, stored and retrieved by [getLocale]. **/
    public open var resolvedLocale: Locale? = null

    /** Called before processing, used to populate any extra variables from event data. **/
    public abstract suspend fun populate()

    /** Extract channel information from event data, if that context is available. **/
    public abstract suspend fun getChannel(): ChannelBehavior?

    /** Extract guild information from event data, if that context is available. **/
    public abstract suspend fun getGuild(): GuildBehavior?

    /** Extract member information from event data, if that context is available. **/
    public abstract suspend fun getMember(): MemberBehavior?

    /** Extract user information from event data, if that context is available. **/
    public abstract suspend fun getUser(): UserBehavior?

    /** Resolve the locale for this command context. **/
    public suspend fun getLocale(): Locale {
        var locale: Locale? = resolvedLocale

        if (locale != null) {
            return locale
        }

        val guild = guildFor(eventObj)
        val channel = channelFor(eventObj)
        val user = userFor(eventObj)

        for (resolver in command.extension.bot.settings.i18nBuilder.localeResolvers) {
            val result = resolver(guild, channel, user)

            if (result != null) {
                locale = result
                break
            }
        }

        resolvedLocale = locale ?: command.extension.bot.settings.i18nBuilder.defaultLocale

        return resolvedLocale!!
    }

    /**
     * Given a translation key and bundle name, return the translation for the locale provided by the bot's configured
     * locale resolvers.
     */
    public suspend fun translate(
        key: String,
        bundleName: String?,
        replacements: Array<Any?> = arrayOf()
    ): String {
        val locale = getLocale()

        return translationsProvider.translate(key, locale, bundleName, replacements)
    }

    /**
     * Given a translation key and possible replacements,return the translation for the given locale in the
     * extension's configured bundle, for the locale provided by the bot's configured locale resolvers.
     */
    public suspend fun translate(key: String, replacements: Array<Any?> = arrayOf()): String = translate(
        key,
        command.extension.bundle,
        replacements
    )
}
