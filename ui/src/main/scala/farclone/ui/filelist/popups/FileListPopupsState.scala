package farclone.ui.filelist.popups

import farclone.ui.filelist.popups.FileListPopupsActions._

case class FileListPopupsState(showHelpPopup: Boolean = false,
                               showExitPopup: Boolean = false,
                               showDeletePopup: Boolean = false,
                               showMkFolderPopup: Boolean = false)

object FileListPopupsStateReducer {

  def apply(state: Option[FileListPopupsState], action: Any): FileListPopupsState = {
    reduce(state.getOrElse(FileListPopupsState()), action)
  }

  private def reduce(state: FileListPopupsState, action: Any): FileListPopupsState = action match {
    case FileListPopupHelpAction(show) => state.copy(showHelpPopup = show)
    case FileListPopupExitAction(show) => state.copy(showExitPopup = show)
    case FileListPopupDeleteAction(show) => state.copy(showDeletePopup = show)
    case FileListPopupMkFolderAction(show) => state.copy(showMkFolderPopup = show)
    case _ => state
  }
}
