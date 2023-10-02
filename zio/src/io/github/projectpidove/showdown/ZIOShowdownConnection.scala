package io.github.projectpidove.showdown

import io.github.iltotore.iron.*
import io.github.projectpidove.showdown.protocol.client.{AuthCommand, ClientMessage}
import io.github.projectpidove.showdown.protocol.server.{GlobalMessage, ServerMessage}
import io.github.projectpidove.showdown.protocol.*
import io.github.projectpidove.showdown.room.RoomId
import io.github.projectpidove.showdown.user.Username
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.ws.{WebSocket, WebSocketFrame}
import zio.*
import zio.json.*

class ZIOShowdownConnection(
                             backend: SttpBackend[Task, ZioStreams & WebSockets],
                             socket: WebSocket[Task],
                             stateRef: Ref[ShowdownData]
                           ) extends ShowdownConnection[WebSocketFrame, ProtocolTask]:

  override def sendRawMessage(frame: WebSocketFrame): ProtocolTask[Unit] =
    socket.send(frame).mapError(ProtocolError.Thrown.apply)

  override def sendMessage(room: RoomId, message: ClientMessage): ProtocolTask[Unit] =
    for
      parts <- ZIO.fromEither(MessageEncoder.derivedUnion[ClientMessage].encode(message))
      command = parts.mkString(s"$room|/", ",", "").replaceFirst(",", " ")
      _ <- sendRawMessage(WebSocketFrame.text(command))
    yield
      ()

  override def sendMessage(message: ClientMessage): ProtocolTask[Unit] =
    for
      parts <- ZIO.fromEither(MessageEncoder.derivedUnion[ClientMessage].encode(message))
      command = parts.mkString("|/", ",", "").replaceFirst(",", " ")
      _ <- sendRawMessage(WebSocketFrame.text(command))
    yield
      ()

  override def disconnect(): ProtocolTask[Unit] = sendRawMessage(WebSocketFrame.close)

  private def readMessage(message: String, room: RoomId, text: String, handler: ServerMessage => ProtocolTask[Unit]): ProtocolTask[Unit] =
    (
      for
        msg <-
          MessageDecoder
            .derivedUnion[ServerMessage]
            .decodeZPure(MessageInput.fromInput(message, room))
            .toZIO
        _ <- stateSubscription(msg)
        _ <- handler(msg)
      yield
        ()
    ).catchAll(err => Console.printLineError(s"Message: $message\nErr: $err")).toProtocolZIO


  override def subscribe(handler: ServerMessage => ProtocolTask[Unit]): ProtocolTask[Unit] =
    val receive =
      for
        frame <- socket.receive().toProtocolZIO
        _ <- Console.printLine(frame.toString).toProtocolZIO
        message <- frame match
          case WebSocketFrame.Text(text, _, _) =>
            for
              messagesAndRoom <- text.split(raw"(\r\n|\r|\n)").toList match
                case s">$room" :: tail => RoomId.applyZIO(room).map((tail, _))
                case messages => ZIO.succeed((messages, RoomId("lobby")))
              _ <- ZIO.foreach(messagesAndRoom._1)(readMessage(_, messagesAndRoom._2, text, handler))
            yield
              ()
          case _ => ZIO.unit
      yield
        frame

    receive.repeatWhile {
      case _: WebSocketFrame.Close => false
      case _ => true
    }.unit *> Console.printLine("closed").toProtocolZIO

  override def login(name: Username, password: String): ProtocolTask[LoginResponse] =
    for
      challstr <- stateRef.get.map(_.challStr).someOrFail(ProtocolError.Miscellaneous("A challstr is needed to login"))
      response <-
        basicRequest
          .post(uri"https://play.pokemonshowdown.com/action.php")
          .body(
            "act" -> "login",
            "name" -> name.toString,
            "pass" -> password,
            "challstr" -> challstr.toString
          )
          .send(backend)
          .toProtocolZIO
      prefixedBody <- ZIO.fromEither(response.body).mapError(ProtocolError.AuthentificationFailed.apply)
      body = prefixedBody.tail
      data <- ZIO.fromEither(body.fromJson[LoginResponse]).mapError(msg => ProtocolError.InvalidInput(body, msg))
      _ <- sendMessage(AuthCommand.Trn(name, 0, data.assertion))
    yield
      data

  override def loginGuest(name: Username): ProtocolTask[String] =
    for
      challstr <- stateRef.get.map(_.challStr).someOrFail(ProtocolError.Miscellaneous("A challstr is needed to login"))
      response <-
        basicRequest
          .post(uri"https://play.pokemonshowdown.com/action.php")
          .body(
            "act" -> "getassertion",
            "userid" -> name.toString,
            "challstr" -> challstr.toString
          )
          .send(backend)
          .toProtocolZIO
      assertion <- ZIO.fromEither(response.body).mapError(ProtocolError.AuthentificationFailed.apply)
      _ <- sendMessage(AuthCommand.Trn(name, 0, assertion))
    yield
      assertion

  override def currentState: ProtocolTask[ShowdownData] = stateRef.get

  private def stateSubscription(message: ServerMessage): ProtocolTask[Unit] =
    stateRef.update(_.update(message))

object ZIOShowdownConnection:

  def withHandler(backend: SttpBackend[Task, ZioStreams & WebSockets], handler: ShowdownConnection[WebSocketFrame, ProtocolTask] => ProtocolTask[Unit])(socket: WebSocket[Task]): Task[ZIOShowdownConnection] =
    for
      infoRef <- Ref.make(ShowdownData.empty)
      connection = ZIOShowdownConnection(backend, socket, infoRef)
      result <- handler(connection)
    yield
      connection