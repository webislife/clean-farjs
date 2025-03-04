package farjs.ui.border

import scommons.react.ReactClass

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
sealed trait BorderExports {

  val DoubleBorder: ReactClass = farjs.ui.border.DoubleBorder()

  val DoubleChars: DoubleChars = farjs.ui.border.DoubleChars

  val HorizontalLine: ReactClass = farjs.ui.border.HorizontalLine()

  val SingleBorder: ReactClass = farjs.ui.border.SingleBorder()

  val SingleChars: SingleChars = farjs.ui.border.SingleChars

  val VerticalLine: ReactClass = farjs.ui.border.VerticalLine()
}

object BorderExports extends BorderExports
