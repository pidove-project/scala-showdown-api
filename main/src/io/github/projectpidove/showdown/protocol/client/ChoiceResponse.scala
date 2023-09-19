package io.github.projectpidove.showdown.protocol.client

import io.github.projectpidove.showdown.protocol.{MessageEncoder, ProtocolError}

case class ChoiceResponse(choice: BattleChoice, requestId: Option[Int] = None)

object ChoiceResponse:

  given (using choiceEncoder: MessageEncoder[BattleChoice], requestEncoder: MessageEncoder[Option[Int]]): MessageEncoder[ChoiceResponse] with
    override def encode(value: ChoiceResponse): Either[ProtocolError, List[String]] =
      for
        encodedChoice <- choiceEncoder.encode(value.choice)
        encodedRequest <- requestEncoder.encode(value.requestId)
      yield
        if encodedRequest.isEmpty then encodedChoice
        else encodedChoice.dropRight(1) :+ s"${encodedChoice.last}|${encodedRequest.mkString}"