package farjs.app.filelist

import farjs.filelist.FileListActions.FileListTaskAction
import farjs.filelist._
import farjs.filelist.popups.FileListPopupsActions._
import farjs.filelist.stack._
import farjs.fs.FSPlugin
import farjs.fs.popups.FSPopupsActions._
import farjs.ui.menu.{BottomMenu, BottomMenuProps}
import scommons.nodejs.path
import scommons.react._
import scommons.react.blessed._
import scommons.react.hooks._
import scommons.react.redux.Dispatch
import scommons.react.redux.task.FutureTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.Failure

case class FileListBrowserProps(dispatch: Dispatch,
                                isRightInitiallyActive: Boolean = false,
                                plugins: Seq[FileListPlugin] = Nil)

object FileListBrowser extends FunctionComponent[FileListBrowserProps] {

  private[filelist] var panelStackComp: UiComponent[PanelStackProps] = PanelStack
  private[filelist] var bottomMenuComp: UiComponent[BottomMenuProps] = BottomMenu
  private[filelist] var fsPlugin: FSPlugin = FSPlugin
  private[filelist] var fileListPopups: ReactClass = FileListPopupsController()
  private[filelist] var fsPopups: ReactClass = FSPopupsController()

  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped
    val leftButtonRef = useRef[BlessedElement](null)
    val rightButtonRef = useRef[BlessedElement](null)
    val (isRight, setIsRight) = useStateUpdater(false)
    val (isRightActive, setIsRightActive) = useState(props.isRightInitiallyActive)
    val (currPluginUi, setCurrPluginUi) = useState(Option.empty[ReactClass])
    
    val (leftStackData, setLeftStackData) = useStateUpdater(() => List[PanelStackItem[_]](
      PanelStackItem[FileListState](fsPlugin.component, None, None, None)
    ))
    val (rightStackData, setRightStackData) = useStateUpdater(() => List[PanelStackItem[_]](
      PanelStackItem[FileListState](fsPlugin.component, None, None, None)
    ))
    val leftStack = new PanelStack(!isRightActive, leftStackData, setLeftStackData)
    val rightStack = new PanelStack(isRightActive, rightStackData, setRightStackData)

    def getInput(isRightActive: Boolean): BlessedElement = {
      val (leftEl, rightEl) =
        if (isRight) (rightButtonRef.current, leftButtonRef.current)
        else (leftButtonRef.current, rightButtonRef.current)
        
      if (isRightActive) rightEl
      else leftEl
    }

    def getStack(isRight: Boolean): PanelStack = {
      if (isRight) rightStack
      else leftStack
    }
    
    def onActivate(isRight: Boolean): js.Function0[Unit] = { () =>
      val stack = getStack(isRight)
      if (!stack.isActive) {
        setIsRightActive(isRight)
      }
    }
    
    val stacks = WithPanelStacksProps(
      leftStack = getStack(isRight),
      leftInput = leftButtonRef.current,
      rightStack = getStack(!isRight),
      rightInput = rightButtonRef.current
    )
    val onKeypress: js.Function2[js.Dynamic, KeyboardKey, Unit] = { (_, key) =>
      def screen = leftButtonRef.current.screen
      key.full match {
        case "M-l" => props.dispatch(DrivePopupAction(show = ShowDriveOnLeft))
        case "M-r" => props.dispatch(DrivePopupAction(show = ShowDriveOnRight))
        case "M-h" => props.dispatch(FoldersHistoryPopupAction(show = true))
        case "C-d" => props.dispatch(FolderShortcutsPopupAction(show = true))
        case "f9" => props.dispatch(FileListPopupMenuAction(show = true))
        case "f10" => props.dispatch(FileListPopupExitAction(show = true))
        case "tab" | "S-tab" => screen.focusNext()
        case "C-u" =>
          setIsRight(!_)
          screen.focusNext()
        case "enter" | "C-pagedown" =>
          val stack = getStack(isRightActive)
          val stackItem = stack.peek[js.Any]
          stackItem.actions.zip(stackItem.state).collect {
            case (actions, state: FileListState)
              if actions.isLocalFS && state.currentItem.exists(!_.isDir) =>
              openCurrItem(props.plugins, props.dispatch, stack, actions, state)
          }
        case keyFull =>
          props.plugins.find(_.triggerKeys.contains(keyFull)).foreach { plugin =>
            val maybePluginUi = plugin.onKeyTrigger(keyFull, stacks)
            maybePluginUi.foreach { pluginUi =>
              setCurrPluginUi(Some(pluginUi))
            }
          }
      }
    }
    
    useLayoutEffect({ () =>
      fsPlugin.init(props.dispatch, leftStack)
      fsPlugin.init(props.dispatch, rightStack)

      getInput(isRightActive).focus()
      ()
    }, Nil)

    <(WithPanelStacks())(^.wrapped := stacks)(
      <.button(
        ^("isRight") := false,
        ^.reactRef := leftButtonRef,
        ^.rbMouse := true,
        ^.rbWidth := "50%",
        ^.rbHeight := "100%-1",
        ^.rbOnFocus := onActivate(isRight),
        ^.rbOnKeypress := onKeypress
      )(
        <(panelStackComp())(^.wrapped := PanelStackProps(
          isRight = false,
          panelInput = leftButtonRef.current,
          stack = getStack(isRight)
        ))()
      ),
      <.button(
        ^("isRight") := true,
        ^.reactRef := rightButtonRef,
        ^.rbMouse := true,
        ^.rbWidth := "50%",
        ^.rbHeight := "100%-1",
        ^.rbLeft := "50%",
        ^.rbOnFocus := onActivate(!isRight),
        ^.rbOnKeypress := onKeypress
      )(
        <(panelStackComp())(^.wrapped := PanelStackProps(
          isRight = true,
          panelInput = rightButtonRef.current,
          stack = getStack(!isRight)
        ))()
      ),

      <.box(^.rbTop := "100%-1")(
        <(bottomMenuComp())(^.wrapped := BottomMenuProps(menuItems))()
      ),

      <(fileListPopups).empty,
      <(fsPopups).empty,

      currPluginUi.map { pluginUi =>
        <(pluginUi)(^.plain := FileListPluginUiProps(
          onClose = { () =>
            setCurrPluginUi(None)
          }
        ))()
      }
    )
  }

  private[filelist] val menuItems = List(
    /*  F1 */ "",
    /*  F2 */ "",
    /*  F3 */ "View",
    /*  F4 */ "",
    /*  F5 */ "Copy",
    /*  F6 */ "RenMov",
    /*  F7 */ "MkFolder",
    /*  F8 */ "Delete",
    /*  F9 */ "Menu",
    /* F10 */ "Exit",
    /* F11 */ "",
    /* F12 */ "DevTools"
  )

  private def openCurrItem(plugins: Seq[FileListPlugin],
                           dispatch: Dispatch,
                           stack: PanelStack,
                           actions: FileListActions,
                           state: FileListState): Unit = {

    state.currentItem.foreach { item =>
      def onClose: () => Unit = { () =>
        stack.pop()
      }

      val filePath = path.join(state.currDir.path, item.name)
      val openF = (for {
        source <- actions.readFile(List(state.currDir.path), item, 0.0)
        buff = new Uint8Array(64 * 1024)
        bytesRead <- source.readNextBytes(buff)
        _ <- source.close()
      } yield {
        buff.subarray(0, bytesRead)
      }).map { fileHeader =>
        val maybePluginItem = plugins.foldLeft(Option.empty[PanelStackItem[FileListState]]) {
          case (None, plugin) => plugin.onFileTrigger(filePath, fileHeader, onClose)
          case (res@Some(_), _) => res
        }
  
        maybePluginItem.foreach { item =>
          stack.push(PanelStackItem.initDispatch(dispatch, FileListStateReducer.apply, stack, item))
        }
      }

      openF.andThen {
        case Failure(_) => dispatch(FileListTaskAction(FutureTask("Opening File", openF)))
      }
    }
  }
}
