package farjs.ui.popup

import farjs.ui._
import farjs.ui.popup.ModalContent._
import scommons.react._

import scala.scalajs.js

object MessageBox extends FunctionComponent[MessageBoxProps] {

  private[popup] var popupComp: UiComponent[PopupProps] = Popup
  private[popup] var modalContentComp: UiComponent[ModalContentProps] = ModalContent
  private[popup] var textLineComp: UiComponent[TextLineProps] = TextLine
  private[popup] var buttonsPanelComp: UiComponent[ButtonsPanelProps] = ButtonsPanel

  protected def render(compProps: Props): ReactElement = {
    val props = compProps.plain
    val width = 60
    val textWidth = width - (paddingHorizontal + 2) * 2
    val textLines = UI.splitText(props.message, textWidth)
    val height = (paddingVertical + 1) * 2 + textLines.size + 1
    val onClose = props.actions.find(_.triggeredOnClose).map(_.onAction)

    val actions = props.actions.map { action =>
      ButtonsPanelAction(action.label, action.onAction)
    }

    <(popupComp())(^.wrapped := PopupProps(
      onClose = { () =>
        onClose.getOrElse(dummyOnClose).apply()
      },
      closable = onClose.isDefined
    ))(
      <(modalContentComp())(^.wrapped := ModalContentProps(
        title = props.title,
        size = (width, height),
        style = props.style
      ))(
        textLines.zipWithIndex.map { case (text, index) =>
          <(textLineComp())(^.key := s"$index", ^.plain := TextLineProps(
            align = TextAlign.center,
            left = 2,
            top = 1 + index,
            width = textWidth,
            text = text,
            style = props.style,
            padding = 0
          ))()
        },
        
        <(buttonsPanelComp())(^.plain := ButtonsPanelProps(
          top = 1 + textLines.size,
          actions = actions,
          style = props.style,
          padding = 1
        ))()
      )
    )
  }
  
  private val dummyOnClose: js.Function0[Unit] = () => ()
}
