package farjs.copymove

import farjs.filelist.api.FileListItem
import farjs.ui._
import farjs.ui.border._
import farjs.ui.popup.ModalContent._
import farjs.ui.popup._
import farjs.ui.theme.Theme
import scommons.react._
import scommons.react.blessed._

import scala.scalajs.js

case class FileExistsPopupProps(newItem: FileListItem,
                                existing: FileListItem,
                                onAction: FileExistsAction => Unit,
                                onCancel: () => Unit)

object FileExistsPopup extends FunctionComponent[FileExistsPopupProps] {
  
  private[copymove] var modalComp: UiComponent[ModalProps] = Modal
  private[copymove] var textLineComp: UiComponent[TextLineProps] = TextLine
  private[copymove] var horizontalLineComp: UiComponent[HorizontalLineProps] = HorizontalLine
  private[copymove] var buttonsPanelComp: UiComponent[ButtonsPanelProps] = ButtonsPanel

  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped
    val size@(width, _) = (58, 11)
    val contentWidth = width - (paddingHorizontal + 2) * 2
    val contentLeft = 2
    val theme = Theme.current.popup.error

    def onButton(action: FileExistsAction): js.Function0[Unit] = { () =>
      props.onAction(action)
    }

    val actions = js.Array(
      ButtonsPanelAction("Overwrite", onButton(FileExistsAction.Overwrite)),
      ButtonsPanelAction("All", onButton(FileExistsAction.All)),
      ButtonsPanelAction("Skip", onButton(FileExistsAction.Skip)),
      ButtonsPanelAction("Skip all", onButton(FileExistsAction.SkipAll)),
      ButtonsPanelAction("Append", onButton(FileExistsAction.Append)),
      ButtonsPanelAction("Cancel", props.onCancel)
    )

    <(modalComp())(^.wrapped := ModalProps("Warning", size, theme, props.onCancel))(
      <.text(
        ^.rbLeft := "center",
        ^.rbTop := 1,
        ^.rbStyle := theme,
        ^.content := "File already exists"
      )(),
      <(textLineComp())(^.plain := TextLineProps(
        align = TextAlign.center,
        left = contentLeft,
        top = 2,
        width = contentWidth,
        text = props.newItem.name,
        style = theme,
        padding = 0
      ))(),
      <(horizontalLineComp())(^.plain := HorizontalLineProps(
        left = 0,
        top = 3,
        length = width - 6,
        lineCh = SingleChars.horizontal,
        style = theme,
        startCh = DoubleChars.leftSingle,
        endCh = DoubleChars.rightSingle
      ))(),
      
      <.text(
        ^.rbLeft := contentLeft,
        ^.rbTop := 4,
        ^.rbStyle := theme,
        ^.content :=
          """New
            |Existing""".stripMargin
      )(),
      <(textLineComp())(^.plain := TextLineProps(
        align = TextAlign.right,
        left = contentLeft,
        top = 4,
        width = contentWidth,
        text = {
          val date = new js.Date(props.newItem.mtimeMs)
          f"${props.newItem.size}%.0f ${date.toLocaleDateString()} ${date.toLocaleTimeString()}"
        },
        style = theme,
        padding = 0
      ))(),
      <(textLineComp())(^.plain := TextLineProps(
        align = TextAlign.right,
        left = contentLeft,
        top = 5,
        width = contentWidth,
        text = {
          val date = new js.Date(props.existing.mtimeMs)
          f"${props.existing.size}%.0f ${date.toLocaleDateString()} ${date.toLocaleTimeString()}"
        },
        style = theme,
        padding = 0
      ))(),
      
      <(horizontalLineComp())(^.plain := HorizontalLineProps(
        left = 0,
        top = 6,
        length = width - paddingHorizontal * 2,
        lineCh = SingleChars.horizontal,
        style = theme,
        startCh = DoubleChars.leftSingle,
        endCh = DoubleChars.rightSingle
      ))(),
      <(buttonsPanelComp())(^.plain := ButtonsPanelProps(
        top = 7,
        actions = actions,
        style = theme,
        padding = 1
      ))()
    )
  }
}
