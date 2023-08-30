package io.github.projectpidove.showdown

import io.github.projectpidove.showdown.protocol.ProtocolError
import zio.*

private type ProtocolTask[A] = IO[ProtocolError, A]
extension [R, A](program: ZIO[R, Throwable, A])

  def toProtocolZIO: ZIO[R, ProtocolError, A] = program.mapError(ProtocolError.Thrown.apply)