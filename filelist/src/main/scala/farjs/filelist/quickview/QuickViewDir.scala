package farjs.filelist.quickview

import farjs.filelist.FileListActions.FileListScanDirsAction
import farjs.filelist.api.FileListItem
import farjs.filelist.stack.PanelStack
import farjs.filelist.{FileListActions, FileListState}
import farjs.ui._
import farjs.ui.popup.{StatusPopup, StatusPopupProps}
import farjs.ui.theme.Theme
import io.github.shogowada.scalajs.reactjs.redux.Redux.Dispatch
import scommons.react._
import scommons.react.blessed._
import scommons.react.hooks._
import scommons.react.redux.task.FutureTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.util.{Failure, Success}

case class QuickViewDirProps(dispatch: Dispatch,
                             actions: FileListActions,
                             state: FileListState,
                             stack: PanelStack,
                             width: Int,
                             currItem: FileListItem)

object QuickViewDir extends FunctionComponent[QuickViewDirProps] {

  private[quickview] var statusPopupComp: UiComponent[StatusPopupProps] = StatusPopup
  private[quickview] var textLineComp: UiComponent[TextLineProps] = TextLine

  protected def render(compProps: Props): ReactElement = {
    val (showPopup, setShowPopup) = useState(false)
    val inProgress = useRef(false)

    val props = compProps.wrapped
    val stack = props.stack
    val params = stack.params[QuickViewParams]
    val theme = Theme.current.fileList

    def scanDir(): Unit = {
      val parent = props.state.currDir.path
      val currItems = List(props.currItem)
      val params = QuickViewParams(props.currItem.name)
      stack.update(params)

      var folders = 0d
      var files = 0d
      var filesSize = 0d
      val resultF = props.actions.scanDirs(parent, currItems, onNextDir = { (_, items) =>
        items.foreach { i =>
          if (i.isDir) folders += 1
          else {
            files += 1
            filesSize += i.size
          }
        }
        inProgress.current
      })
      resultF.onComplete {
        case Success(false) => // already cancelled
        case Success(true) =>
          setShowPopup(false)
          stack.update(params.copy(folders = folders, files = files, filesSize = filesSize))
        case Failure(_) =>
          setShowPopup(false)
          props.dispatch(FileListScanDirsAction(FutureTask("Quick view dir scan", resultF)))
      }
    }

    useLayoutEffect({ () =>
      if (!inProgress.current && showPopup) { // start scan
        inProgress.current = true
        scanDir()
      } else if (inProgress.current && !showPopup) { // stop scan
        inProgress.current = false
      }
      ()
    }, List(showPopup))

    useLayoutEffect({ () =>
      if (stack.params[QuickViewParams].name != props.currItem.name) {
        setShowPopup(true)
      }
    }, List(props.currItem.name, stack.asInstanceOf[js.Any]))

    <.>()(
      if (showPopup) Some(
        <(statusPopupComp())(^.wrapped := StatusPopupProps(
          text = s"Scanning the folder\n${props.currItem.name}",
          title = "View Dir",
          closable = true,
          onClose = { () =>
            setShowPopup(false)
          }
        ))()
      ) else None,

      <.text(
        ^.rbLeft := 2,
        ^.rbTop := 2,
        ^.rbStyle := theme.regularItem,
        ^.content :=
          """Folder
            |
            |Contains:
            |
            |Folders
            |Files
            |Files size""".stripMargin
      )(),

      <(textLineComp())(^.wrapped := TextLineProps(
        align = TextLine.Left,
        pos = (12, 2),
        width = props.width - 14,
        text = s""""${props.currItem.name}"""",
        style = theme.regularItem,
        padding = 0
      ))(),

      <.text(
        ^.rbLeft := 15,
        ^.rbTop := 6,
        ^.rbStyle := theme.selectedItem,
        ^.content :=
          f"""${params.folders}%,.0f
             |${params.files}%,.0f
             |${params.filesSize}%,.0f""".stripMargin
      )()
    )
  }
}
