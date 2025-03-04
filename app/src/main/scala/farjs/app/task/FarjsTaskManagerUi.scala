package farjs.app.task

import farjs.ui.popup._
import farjs.ui.theme.Theme
import scommons.react._
import scommons.react.hooks._
import scommons.react.redux.task.{TaskManager, TaskManagerUiProps}

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException
import scala.util.{Failure, Try}

/**
  * Displays status and error(s) of running tasks.
  */
object FarjsTaskManagerUi extends FunctionComponent[TaskManagerUiProps] {

  private[task] var logger: String => Unit = println
  private[task] var statusPopupComp: UiComponent[StatusPopupProps] = StatusPopup
  private[task] var messageBoxComp: UiComponent[MessageBoxProps] = MessageBox
  
  val errorHandler: PartialFunction[Try[_], (Option[String], Option[String])] = {
    case Failure(ex@JavaScriptException(error)) =>
      val stackTrace = TaskManager.printStackTrace(ex, sep = " ")
      logger(stackTrace)
      (Some(s"$error"), Some(stackTrace))
    case Failure(ex) =>
      val stackTrace = TaskManager.printStackTrace(ex, sep = " ")
      logger(stackTrace)
      (Some(s"$ex"), Some(stackTrace))
  }

  protected def render(compProps: Props): ReactElement = {
    val (errors, updateErrors) = useStateUpdater(List.empty[String])
    val props = compProps.wrapped
    val statusMessage = props.status.filter(_ => props.showLoading).getOrElse("")
    val errorMessage = props.error.getOrElse("").trim
    val theme = Theme.current.popup

    useLayoutEffect({ () =>
      if (errorMessage.nonEmpty) {
        updateErrors(errorMessage :: _)
      }
      ()
    }, List(errorMessage))
    
    <.>()(
      if (statusMessage.nonEmpty) Some {
        <(statusPopupComp())(^.wrapped := StatusPopupProps(statusMessage))()
      }
      else if (errors.nonEmpty) Some(
        <(messageBoxComp())(^.plain := MessageBoxProps(
          title = "Error",
          message = errors.head.stripPrefix("Error:").trim,
          //message = s"$errorMessage${props.errorDetails.map(d => s"\n\n$d").getOrElse("")}",
          actions = js.Array(MessageBoxAction.OK { () =>
            updateErrors(_.tail)
            props.onCloseErrorPopup()
          }),
          style = theme.error
        ))()
      ) else None
    )
  }
}
