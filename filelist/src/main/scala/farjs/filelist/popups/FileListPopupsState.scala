package farjs.filelist.popups

import farjs.filelist.popups.FileListPopupsActions._

case class FileListPopupsState(showHelpPopup: Boolean = false,
                               showExitPopup: Boolean = false,
                               showMenuPopup: Boolean = false,
                               showDeletePopup: Boolean = false,
                               showMkFolderPopup: Boolean = false,
                               showSelectPopup: FileListPopupSelect = SelectHidden)

object FileListPopupsStateReducer {

  def apply(state: Option[FileListPopupsState], action: Any): FileListPopupsState = {
    reduce(state.getOrElse(FileListPopupsState()), action)
  }

  private def reduce(state: FileListPopupsState, action: Any): FileListPopupsState = action match {
    case FileListPopupHelpAction(show) => state.copy(showHelpPopup = show)
    case FileListPopupExitAction(show) => state.copy(showExitPopup = show)
    case FileListPopupMenuAction(show) => state.copy(showMenuPopup = show)
    case FileListPopupDeleteAction(show) => state.copy(showDeletePopup = show)
    case FileListPopupMkFolderAction(show) => state.copy(showMkFolderPopup = show)
    case FileListPopupSelectAction(sel) => state.copy(showSelectPopup = sel)
    case _ => state
  }
}
