package farjs.app.filelist

import farjs.app.filelist.fs.{FSDrivePopup, FSDrivePopupProps, FSPlugin}
import farjs.filelist._
import farjs.filelist.popups.FileListPopupsActions.FileListPopupExitAction
import farjs.filelist.stack._
import farjs.ui.menu.BottomMenu
import scommons.nodejs.path
import scommons.react._
import scommons.react.blessed._
import scommons.react.hooks._
import scommons.react.redux.Dispatch

import scala.scalajs.js

case class FileListBrowserProps(dispatch: Dispatch,
                                isRightInitiallyActive: Boolean = false,
                                plugins: Seq[FileListPlugin] = Nil)

object FileListBrowser extends FunctionComponent[FileListBrowserProps] {

  private[filelist] var panelStackComp: UiComponent[PanelStackProps] = PanelStack
  private[filelist] var fsDrivePopup: UiComponent[FSDrivePopupProps] = FSDrivePopup
  private[filelist] var bottomMenuComp: UiComponent[Unit] = BottomMenu
  private[filelist] var fsPlugin: FSPlugin = FSPlugin
  private[filelist] var fileListPopups: ReactClass = FileListPopupsController()

  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped
    val leftButtonRef = useRef[BlessedElement](null)
    val rightButtonRef = useRef[BlessedElement](null)
    val (isRight, setIsRight) = useStateUpdater(false)
    val (isRightActive, setIsRightActive) = useState(props.isRightInitiallyActive)
    val (showLeftDrive, setShowLeftDrive) = useState(false)
    val (showRightDrive, setShowRightDrive) = useState(false)
    
    val (leftStackData, setLeftStackData) = useStateUpdater(() => List[PanelStackItem[_]](
      PanelStackItem[FileListState](fsPlugin.component, None, None, None)
    ))
    val (rightStackData, setRightStackData) = useStateUpdater(() => List[PanelStackItem[_]](
      PanelStackItem[FileListState](fsPlugin.component, None, None, None)
    ))
    val leftStack = new PanelStack(!isRightActive, leftStackData, setLeftStackData)
    val rightStack = new PanelStack(isRightActive, rightStackData, setRightStackData)

    useLayoutEffect({ () =>
      fsPlugin.init(props.dispatch, leftStack)
      fsPlugin.init(props.dispatch, rightStack)

      val element = 
        if (isRightActive) rightButtonRef.current
        else leftButtonRef.current
      
      element.focus()
    }, Nil)
    
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
    
    val onKeypress: js.Function2[js.Dynamic, KeyboardKey, Unit] = { (_, key) =>
      def screen = leftButtonRef.current.screen
      key.full match {
        case "M-l" => setShowLeftDrive(true)
        case "M-r" => setShowRightDrive(true)
        case "f10" => props.dispatch(FileListPopupExitAction(show = true))
        case "tab" | "S-tab" => screen.focusNext()
        case "C-u" =>
          setIsRight(!_)
          screen.focusNext()
        case "enter" =>
          val stack = getStack(isRightActive)
          val maybeState = stack.peek[FileListState].state
          maybeState.filter(_.currentItem.exists(!_.isDir)).foreach { state =>
            openCurrItem(props.plugins, stack, state)
          }
        case keyFull =>
          props.plugins.find(_.triggerKey.contains(keyFull)).foreach { plugin =>
            plugin.onKeyTrigger(leftStack, rightStack)
          }
      }
    }
    
    <(WithPanelStacks())(^.wrapped := WithPanelStacksProps(leftStack, rightStack))(
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
          isRight = isRight,
          panelInput = leftButtonRef.current,
          stack = getStack(isRight)
        ))(
          if (showLeftDrive) Some {
            <(fsDrivePopup())(^.wrapped := FSDrivePopupProps(
              dispatch = props.dispatch,
              onClose = { () =>
                setShowLeftDrive(false)
              },
              showOnLeft = true
            ))()
          }
          else None
        )
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
          isRight = !isRight,
          panelInput = rightButtonRef.current,
          stack = getStack(!isRight)
        ))(
          if (showRightDrive) Some {
            <(fsDrivePopup())(^.wrapped := FSDrivePopupProps(
              dispatch = props.dispatch,
              onClose = { () =>
                setShowRightDrive(false)
              },
              showOnLeft = false
            ))()
          }
          else None
        )
      ),

      <.box(^.rbTop := "100%-1")(
        <(bottomMenuComp())()()
      ),

      <(fileListPopups).empty
    )
  }

  private def openCurrItem(plugins: Seq[FileListPlugin],
                           stack: PanelStack,
                           state: FileListState): Unit = {

    state.currentItem.foreach { item =>
      def onClose: () => Unit = { () =>
        stack.pop()
      }

      val filePath = path.join(state.currDir.path, item.name)
      val maybePluginItem = plugins.foldLeft(Option.empty[PanelStackItem[_]]) {
        case (None, plugin) => plugin.onFileTrigger(filePath, onClose)
        case (res@Some(_), _) => res
      }

      maybePluginItem.foreach { item =>
        stack.push(item)
      }
    }
  }
}
