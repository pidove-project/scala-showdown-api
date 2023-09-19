package io.github.projectpidove.showdown.battle

import io.github.projectpidove.showdown.protocol.{MessageDecoder, ProtocolError}

case class BattleRule(name: String, description: String)

object BattleRule:

  given MessageDecoder[BattleRule] = MessageDecoder.string.mapEither:
    case s"$name: $description" => Right(BattleRule(name, description))
    case value => Left(ProtocolError.InvalidInput(value, "Invalid rule format"))