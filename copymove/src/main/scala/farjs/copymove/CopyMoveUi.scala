package farjs.copymove

import farjs.copymove.CopyMoveUi._
import farjs.copymove.CopyMoveUiAction._
import farjs.filelist.FileListActions._
import farjs.filelist._
import farjs.filelist.api.FileListItem
import farjs.ui.popup._
import farjs.ui.theme.Theme
import scommons.nodejs.path
import scommons.react._
import scommons.react.hooks._
import scommons.react.redux.task.FutureTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.util.Success

object CopyMoveUi {

  private[copymove] var copyItemsStats: UiComponent[CopyItemsStatsProps] = CopyItemsStats
  private[copymove] var copyItemsPopup: UiComponent[CopyItemsPopupProps] = CopyItemsPopup
  private[copymove] var copyProcessComp: UiComponent[CopyProcessProps] = CopyProcess
  private[copymove] var messageBoxComp: UiComponent[MessageBoxProps] = MessageBox
  private[copymove] var moveProcessComp: UiComponent[MoveProcessProps] = MoveProcess
}

class CopyMoveUi(show: CopyMoveUiAction,
                 from: FileListData,
                 maybeTo: Option[FileListData]) extends FunctionComponent[FileListPluginUiProps] {

  protected def render(compProps: Props): ReactElement = {
    val services = FileListServices.useServices
    val (maybeTotal, setTotal) = useState[Option[Double]](None)
    val (maybeToPath, setToPath) = useState[Option[(String, String)]](None)
    val (inplace, setInplace) = useState(false)
    val (move, setMove) = useState(false)
    val (showPopup, setShowPopup) = useState(true)
    val (showStats, setShowStats) = useState(false)
    val (showMove, setShowMove) = useState(false)
    val copied = useRef(Set.empty[String])

    val props = compProps.plain
    
    def onTopItem(item: FileListItem): Unit = copied.current += item.name

    def onDone(path: String, toPath: String): () => Unit = { () =>
      val updatedSelection = from.state.selectedNames -- copied.current
      if (updatedSelection != from.state.selectedNames) {
        from.dispatch(FileListParamsChangedAction(
          offset = from.state.offset,
          index = from.state.index,
          selectedNames = updatedSelection
        ))
      }

      val isInplace = inplace
      props.onClose()

      services.copyItemsHistory.save(path)

      val updateAction = from.actions.updateDir(from.dispatch, from.path)
      from.dispatch(updateAction)
      updateAction.task.future.andThen {
        case Success(updatedDir) =>
          if (isInplace) from.dispatch(FileListItemCreatedAction(toPath, updatedDir))
          else maybeTo.foreach(to => to.dispatch(to.actions.updateDir(to.dispatch, to.path)))
      }
    }

    def isMove: Boolean =
      move ||
        show == ShowMoveToTarget ||
        show == ShowMoveInplace

    def isInplace: Boolean = {
      inplace ||
        show == ShowCopyInplace ||
        show == ShowMoveInplace ||
        maybeTo.forall(_.path == from.path) && from.state.selectedItems.isEmpty
    }

    val items =
      if (!isInplace && from.state.selectedItems.nonEmpty) from.state.selectedItems
      else from.state.currentItem.toList

    def onAction(path: String): Unit = {
      val move = isMove
      val inplace = isInplace
      val resolveF =
        if (!from.actions.isLocalFS) Future.successful((path, false))
        else if (!inplace) resolveTargetDir(move, path)
        else Future.successful((path, true))

      resolveF.map { case (toPath, sameDrive) =>
        setInplace(inplace)
        setMove(move)
        setShowPopup(false)

        setToPath(Some(path -> toPath))
        if (move && sameDrive) setShowMove(true)
        else setShowStats(true)
      }
    }

    def resolveTargetDir(move: Boolean, path: String): Future[(String, Boolean)] = {
      val dirF = for {
        dir <- from.actions.readDir(Some(from.path), path)
        sameDrive <-
          if (move) checkSameDrive(from, dir.path)
          else Future.successful(false)
      } yield {
        (dir.path, sameDrive)
      }

      from.dispatch(FileListTaskAction(FutureTask("Resolving target dir", dirF)))
      dirF
    }

    val maybeError = maybeToPath.filter(_ => !inplace).flatMap { case (_, toPath) =>
      val op = if (move) "move" else "copy"
      if (from.path == toPath) Some {
        s"Cannot $op the item\n${items.head.name}\nonto itself"
      }
      else if (toPath.startsWith(from.path + path.sep)) {
        val toSuffix = toPath.stripPrefix(from.path + path.sep)
        val maybeSelf = items.find(i => toSuffix == i.name || toSuffix.startsWith(i.name + path.sep))
        maybeSelf.map { self =>
          s"Cannot $op the item\n${self.name}\ninto itself"
        }
      }
      else None
    }

    <.>()(
      if (showPopup) Some {
        <(copyItemsPopup())(^.wrapped := CopyItemsPopupProps(
          move = isMove,
          path =
            if (!isInplace) maybeTo.map(_.path).getOrElse("")
            else from.state.currentItem.map(_.name).getOrElse(""),
          items = items,
          onAction = onAction,
          onCancel = props.onClose
        ))()
      }
      else if (maybeError.isDefined) maybeError.map { error =>
        <(messageBoxComp())(^.plain := MessageBoxProps(
          title = "Error",
          message = error,
          actions = js.Array(MessageBoxAction.OK(props.onClose)),
          style = Theme.current.popup.error
        ))()
      }
      else if (showStats) Some {
        <(copyItemsStats())(^.wrapped := CopyItemsStatsProps(
          dispatch = from.dispatch,
          actions = from.actions,
          fromPath = from.path,
          items = items,
          title = if (move) "Move" else "Copy",
          onDone = { total =>
            setTotal(Some(total))
            setShowStats(false)
          },
          onCancel = props.onClose
        ))()
      }
      else if (showMove) maybeToPath.map { case (path, toPath) =>
        <(moveProcessComp())(^.wrapped := MoveProcessProps(
          dispatch = from.dispatch,
          actions = from.actions,
          fromPath = from.path,
          items =
            if (!inplace) items.map(i => (i, i.name))
            else items.map(i => (i, toPath)),
          toPath =
            if (!inplace) toPath
            else from.path,
          onTopItem = onTopItem,
          onDone = onDone(path, toPath)
        ))()
      }
      else {
        for {
          (path, toPath) <- maybeToPath
          total <- maybeTotal
        } yield {
          <(copyProcessComp())(^.wrapped := CopyProcessProps(
            from = from,
            to =
              if (!inplace) maybeTo.getOrElse(from)
              else from,
            move = move,
            fromPath = from.path,
            items =
              if (!inplace) items.map(i => (i, i.name))
              else items.map(i => (i, toPath)),
            toPath =
              if (!inplace) toPath
              else from.path,
            total = total,
            onTopItem = onTopItem,
            onDone = onDone(path, toPath)
          ))()
        }
      }
    )
  }
  
  private def checkSameDrive(from: FileListData, toPath: String): Future[Boolean] = {
    for {
      maybeFromRoot <- from.actions.getDriveRoot(from.path)
      maybeToRoot <- from.actions.getDriveRoot(toPath)
    } yield {
      val maybeSameDrive = for {
        fromRoot <- maybeFromRoot
        toRoot <- maybeToRoot
      } yield {
        fromRoot == toRoot
      }

      maybeSameDrive.getOrElse(false)
    }
  }
}
