package farjs.archiver.zip

import farjs.archiver.ArchiverPlugin
import farjs.filelist.FileListActions
import farjs.filelist.FileListActions._
import scommons.react.redux.Dispatch
import scommons.react.redux.task.FutureTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class ZipActions(protected[zip] var api: ZipApi) extends FileListActions {

  val isLocalFS: Boolean = false

  def getDriveRoot(path: String): Future[Option[String]] = Future.successful(None)

  override def updateDir(dispatch: Dispatch, path: String): FileListDirUpdateAction = {
    val entriesByParentF = ArchiverPlugin.readZip(api.zipPath).andThen {
      case Success(entries) =>
        val totalSize = entries.foldLeft(0.0) { (total, entry) =>
          total + entry._2.foldLeft(0.0)(_ + _.size)
        }
        dispatch(FileListDiskSpaceUpdatedAction(totalSize))
    }
    api = ArchiverPlugin.createApi(api.zipPath, api.rootPath, entriesByParentF)
    
    val future = entriesByParentF.flatMap(_ => api.readDir(path)).andThen {
      case Success(currDir) => dispatch(FileListDirUpdatedAction(currDir))
    }

    FileListDirUpdateAction(FutureTask("Updating Dir", future))
  }
}
