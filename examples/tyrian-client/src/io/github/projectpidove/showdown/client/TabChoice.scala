package io.github.projectpidove.showdown.client

import io.github.projectpidove.showdown.room.{ChatContent, RoomChat, RoomId}
import io.github.projectpidove.showdown.user.Username
import tyrian.Html
import tyrian.Html.*

enum TabChoice:
  case PrivateMessage(user: Username)
  case Room(room: RoomId)

  def view(app: ClientApp, state: ClientState.Main): Html[ClientMessage] = this match
    case PrivateMessage(user) =>
      val chat =
        app
          .showdownState
          .loggedUser
          .flatMap(_.privateMessages.find(_._1.name == user))
          .fold(RoomChat.empty)(_._2)

      div(
        h2(s"Discussion with $user"),
        input(`type` := "text", name := "message", onInput(ClientMessage.UpdateChatInput.apply)),
        button(onClick(ChatContent.option(state.messageInput).fold(ClientMessage.None)(ClientMessage.SendPrivateMessage(user, _))))("Envoyer"),
        viewPrivateMessages(chat)
      )

    case Room(room) =>
      val joinedRoom = app.showdownState.getJoinedRoomOrEmpty(room)

      div(
        h2(s"${joinedRoom.title.getOrElse(joinedRoom.id)}${joinedRoom.roomType.fold("")(tpe => s" ($tpe)")}"),
        input(`type` := "text", name := "message", onInput(ClientMessage.UpdateChatInput.apply)),
        button(onClick(ChatContent.option(state.messageInput).fold(ClientMessage.None)(ClientMessage.SendMessage(room, _))))("Envoyer"),
        viewRoom(joinedRoom)
      )