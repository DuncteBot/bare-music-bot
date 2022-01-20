/*
 *     Bare bones music bot using lavalink
 *     Copyright (C) 2022  Duncan Sterken
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import lavalink.client.io.FunctionalResultHandler
import lavalink.client.io.jda.JdaLavalink
import lavalink.client.player.event.PlayerEvent
import lavalink.client.player.event.TrackExceptionEvent
import lavalink.client.player.event.TrackStartEvent
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.net.URI
import java.util.*

lateinit var lavalink: JdaLavalink

fun main() {
    lateinit var jda: JDA
    val token = System.getenv("TOKEN")
    val userId = token.extractId()

    lavalink = JdaLavalink(
        userId,
        1
    ) { jda }

    lavalink.addNode(URI.create("ws://localhost:2333"), "youshallnotpass")

    jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
        .enableCache(CacheFlag.VOICE_STATE)
        .setVoiceDispatchInterceptor(lavalink.voiceInterceptor)
        .addEventListeners(lavalink, object : EventListener {
            override fun onEvent(event: GenericEvent) = when (event) {
                is MessageReceivedEvent -> onMessage(event)
                else -> Unit
            }
        })
        .build()
}

fun onMessage(event: MessageReceivedEvent) {
    val message = event.message

    if (!message.isFromGuild) return
    if (!message.contentRaw.startsWith("!")) return

    val split = message.contentRaw.substring(1).split("\\s+".toRegex(), 2)

    when (split[0]) {
        "play" -> playSong(event, split[1])
        "leave" -> {
            event.guild.audioManager.closeAudioConnection()
        }
    }
}

fun playSong(event: MessageReceivedEvent, args: String) {
    connectToChannelFirst(event)

    val link = lavalink.getLink(event.guild)

    link.player.addListener {  }

    link.restClient.loadItem(args, FunctionalResultHandler(
        {
            event.channel.sendMessage("Loaded ${it.info.title}").queue()
            event.channel.sendMessage("Length: ${it.info.length}").queue()

            link.player.playTrack(it)
        },
        null, null,
        {
            event.channel.sendMessage("No matches").queue()
        }, {
            event.channel.sendMessage("Error: ${it.message}").queue()
            it.printStackTrace()
        }
    ))
}

fun connectToChannelFirst(event: MessageReceivedEvent) {
    val self = event.guild.selfMember.voiceState!!
    val memberVS = event.member!!.voiceState!!

    if (self.inVoiceChannel()) return
    if (!memberVS.inVoiceChannel()) return

    event.guild.audioManager.openAudioConnection(memberVS.channel!!)

    lavalink.getLink(event.guild).player.addListener(::handlePlayerEvent)
}

fun handlePlayerEvent(event: PlayerEvent) {
    when (event) {
        is TrackStartEvent -> {
            println("${event.track.info.title} started playing")
        }
        is TrackExceptionEvent -> {
            event.exception.printStackTrace()
        }
    }
}

fun String.extractId() = String(Base64.getDecoder().decode(this.split(".")[0]))
