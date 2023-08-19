package io.github.projectpidove.showdown

import io.github.iltotore.iron.zioJson.given
import io.github.projectpidove.showdown.protocol.MessageDecoder
import zio.json.*

/**
 * Informations about the current search.
 *
 * @param searching the searched formats
 * @param games the found games
 */
case class GameSearch(searching: List[FormatName], games: Map[String, String]) derives JsonDecoder

object GameSearch:

  private given JsonDecoder[Map[String, String]] =
    JsonDecoder
      .option(using JsonDecoder.map[String, String])
      .map(_.getOrElse(Map.empty))

  given MessageDecoder[GameSearch] = MessageDecoder.fromJson
