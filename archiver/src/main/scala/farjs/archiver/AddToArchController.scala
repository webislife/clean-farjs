package farjs.archiver

import farjs.archiver.zip.ZipApi
import farjs.filelist.FileListActions._
import farjs.filelist.api.FileListItem
import farjs.filelist.{FileListActions, FileListState}
import farjs.ui.popup.{StatusPopup, StatusPopupProps}
import scommons.react._
import scommons.react.hooks._
import scommons.react.redux.Dispatch
import scommons.react.redux.task.FutureTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class AddToArchControllerProps(dispatch: Dispatch,
                                    actions: FileListActions,
                                    state: FileListState,
                                    zipName: String,
                                    items: Seq[FileListItem],
                                    action: AddToArchAction,
                                    onComplete: String => Unit,
                                    onCancel: () => Unit)

object AddToArchController extends FunctionComponent[AddToArchControllerProps] {

  private[archiver] var addToArchPopup: UiComponent[AddToArchPopupProps] = AddToArchPopup
  private[archiver] var addToArchApi: (String, String, Set[String], () => Unit) => Future[Unit] =
    ZipApi.addToZip
  private[archiver] var statusPopupComp: UiComponent[StatusPopupProps] = StatusPopup

  protected def render(compProps: Props): ReactElement = {
    val (showAddPopup, setShowAddPopup) = useState(true)
    val (showStatusPopup, setShowStatusPopup) = useState(false)
    val (progress, setProgress) = useState(0)
    val props = compProps.wrapped
    
    def onAction(zipFile: String): Unit = {
      setShowAddPopup(false)
      setShowStatusPopup(true)

      val parent = props.state.currDir.path
      val currItems = props.items
      var totalItems = 0
      var addedItems = 0.0
      val resultF = for {
        _ <- props.actions.scanDirs(parent, currItems, onNextDir = { (_, items) =>
          totalItems += items.size
          true
        })
        _ <- addToArchApi(zipFile, parent, currItems.map(_.name).toSet, { () =>
          addedItems += 1
          setProgress(math.min((addedItems / totalItems) * 100, 100).toInt)
        })
      } yield {
        if (props.state.selectedNames.nonEmpty) {
          props.dispatch(FileListParamsChangedAction(
            offset = props.state.offset,
            index = props.state.index,
            selectedNames = Set.empty
          ))
        }

        setShowStatusPopup(false)
        props.onComplete(zipFile)
      }
      resultF.onComplete {
        case Success(_) =>
        case Failure(_) =>
          setShowStatusPopup(false)
          props.dispatch(FileListTaskAction(
            FutureTask(s"${props.action} item(s) to zip archive", resultF)
          ))
      }
    }
    
    <.>()(
      if (showAddPopup) Some(
        <(addToArchPopup())(^.wrapped := AddToArchPopupProps(
          zipName = props.zipName,
          action = props.action,
          onAction = onAction,
          onCancel = props.onCancel
        ))()
      ) else None,

      if (showStatusPopup) Some(
        <(statusPopupComp())(^.wrapped := StatusPopupProps(
          text = s"${props.action} item(s) to zip archive\n$progress%"
        ))()
      ) else None
    )
  }
}
