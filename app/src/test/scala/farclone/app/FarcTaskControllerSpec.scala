package farclone.app

import farclone.ui.FarcStateDef
import io.github.shogowada.scalajs.reactjs.React.Props
import io.github.shogowada.scalajs.reactjs.redux.Redux.Dispatch
import scommons.react.redux.task.{FutureTask, TaskManager, TaskManagerProps}
import scommons.react.test.TestSpec

import scala.concurrent.Future

class FarcTaskControllerSpec extends TestSpec {

  it should "return component" in {
    //when & then
    FarcTaskController.uiComponent shouldBe TaskManager
  }
  
  it should "map state to props" in {
    //given
    val props = mock[Props[Unit]]
    val dispatch = mock[Dispatch]
    val currentTask = Some(FutureTask("test task", Future.successful(())))
    val state = mock[FarcStateDef]
    (state.currentTask _).expects().returning(currentTask)

    //when
    val result = FarcTaskController.mapStateToProps(dispatch, state, props)
    
    //then
    inside(result) { case TaskManagerProps(task) =>
      task shouldBe currentTask
    }
  }
}
