package zhttp.socket

import io.netty.handler.codec.http.websocketx.{WebSocketDecoderConfig => JWebSocketDecoderConfig}

/**
 * Frame decoder configuration
 */
sealed trait SocketDecoder { self =>
  def ++(other: SocketDecoder): SocketDecoder = SocketDecoder.Concat(self, other)
  def javaConfig: JWebSocketDecoderConfig     = SocketDecoder.asJava(self)
}

object SocketDecoder {
  private case class MaxFramePayloadLength(length: Int)         extends SocketDecoder
  private case object RejectMaskedFrames                        extends SocketDecoder
  private case object AllowMaskMismatch                         extends SocketDecoder
  private case object AllowExtensions                           extends SocketDecoder
  private case object AllowProtocolViolation                    extends SocketDecoder
  private case object SkipUTF8Validation                        extends SocketDecoder
  private case class Concat(a: SocketDecoder, b: SocketDecoder) extends SocketDecoder
  private case object Default                                   extends SocketDecoder

  /**
   * Sets Maximum length of a frame's payload. Setting this to an appropriate value for you application helps check for
   * denial of services attacks.
   */
  def maxFramePayloadLength(length: Int): SocketDecoder = MaxFramePayloadLength(length)

  /**
   * Web socket servers must set this to true to reject incoming masked payload.
   */
  def rejectMaskedFrames: SocketDecoder = RejectMaskedFrames

  /**
   * When set to true, frames which are not masked properly according to the standard will still be accepted.
   */
  def allowMaskMismatch: SocketDecoder = AllowMaskMismatch

  /**
   * Allow extensions to be used in the reserved bits of the web socket frame
   */
  def allowExtensions: SocketDecoder = AllowExtensions

  /**
   * Flag to not send close frame immediately on any protocol violation.ion.
   */
  def allowProtocolViolation: SocketDecoder = AllowProtocolViolation

  /**
   * Allows you to avoid adding of Utf8FrameValidator to the pipeline on the WebSocketServerProtocolHandler creation.
   * This is useful (less overhead) when you use only BinaryWebSocketFrame within your web socket connection.
   */
  def skipUTF8Validation: SocketDecoder = SkipUTF8Validation

  /**
   * Creates an default decoder configuration.
   */
  def default: SocketDecoder = Default

  def asJava(decoder: SocketDecoder): JWebSocketDecoderConfig = {
    val b = JWebSocketDecoderConfig.newBuilder()
    def loop(decoder: SocketDecoder): Unit = {
      decoder match {
        case Default                       => ()
        case MaxFramePayloadLength(length) => b.maxFramePayloadLength(length)
        case RejectMaskedFrames            => b.expectMaskedFrames(false)
        case AllowMaskMismatch             => b.allowMaskMismatch(true)
        case AllowExtensions               => b.allowExtensions(true)
        case AllowProtocolViolation        => b.closeOnProtocolViolation(false)
        case SkipUTF8Validation            => b.withUTF8Validator(false)
        case Concat(a, b)                  =>
          loop(a)
          loop(b)
      }
      ()
    }
    loop(decoder)
    b.build()
  }

}
