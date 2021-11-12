package farjs.app

import farjs.app.FarjsRoot._
import farjs.app.FarjsRootSpec._
import farjs.app.util._
import org.scalatest.Succeeded
import scommons.nodejs.test.AsyncTestSpec
import scommons.react.blessed._
import scommons.react.blessed.raw.BlessedProgram
import scommons.react.test._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportAll

class FarjsRootSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  private val withPortalsComp = mockUiComponent[Unit]("WithPortals")
  private val fileListComp = mockUiComponent[Unit]("FileListBrowser").apply()
  private val fileListPopups = mockUiComponent[Unit]("FileListPopups").apply()
  private val taskController = mockUiComponent[Unit]("TaskController").apply()
  
  FarjsRoot.logControllerComp = mockUiComponent("LogController")
  FarjsRoot.devToolPanelComp = mockUiComponent("DevToolPanel")

  it should "set devTool and emit resize event when on F12" in {
    //given
    val root = new FarjsRoot(withPortalsComp, fileListComp, fileListPopups, taskController, DevTool.Hidden)
    val programMock = mock[BlessedProgramMock]
    val screenMock = mock[BlessedScreenMock]
    val boxMock = mock[BlessedElementMock]
    var keyListener: js.Function2[js.Object, KeyboardKey, Unit] = null

    (boxMock.screen _).expects().returning(screenMock.asInstanceOf[BlessedScreen])
    (screenMock.program _).expects().returning(programMock.asInstanceOf[BlessedProgram])
    (screenMock.key _).expects(*, *).onCall { (keys, listener) =>
      keys.toList shouldBe List("f12")
      keyListener = listener
    }
    var emitCalled = false
    (programMock.emit _).expects("resize").onCall { _: String =>
      emitCalled = true
    }
    
    val renderer = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock.asInstanceOf[js.Any]
      else null
    })
    findComponents(renderer.root.children(0), <.box.name).head.props.width shouldBe "100%"
    
    //when
    TestRenderer.act { () =>
      keyListener(null, null)
    }

    //then
    findComponents(renderer.root.children(0), <.box.name).head.props.width shouldBe "70%"
    
    eventually {
      emitCalled shouldBe true
    }.map { _ =>
      //cleanup
      TestRenderer.act { () =>
        renderer.unmount()
      }
      Succeeded
    }
  }

  it should "set devTool when onActivate" in {
    //given
    val root = new FarjsRoot(withPortalsComp, fileListComp, fileListPopups, taskController, DevTool.Colors)
    val screenMock = mock[BlessedScreenMock]
    val boxMock = mock[BlessedElementMock]
    (boxMock.screen _).expects().returning(screenMock.asInstanceOf[BlessedScreen])
    (screenMock.key _).expects(*, *)

    val renderer = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock.asInstanceOf[js.Any]
      else null
    })
    val devToolProps = {
      val logProps = findComponentProps(renderer.root, logControllerComp)
      val renderedContent = createTestRenderer(logProps.render("test log content")).root
      findComponentProps(renderedContent, devToolPanelComp)
    }
    devToolProps.devTool shouldBe DevTool.Colors

    //when
    devToolProps.onActivate(DevTool.Logs)
    
    //then
    val updatedProps = {
      val logProps = findComponentProps(renderer.root, logControllerComp)
      val renderedContent = createTestRenderer(logProps.render("test log content")).root
      findComponentProps(renderedContent, devToolPanelComp)
    }
    updatedProps.devTool shouldBe DevTool.Logs
  }

  it should "render component without DevTools" in {
    //given
    val root = new FarjsRoot(withPortalsComp, fileListComp, fileListPopups, taskController, DevTool.Hidden)
    val screenMock = mock[BlessedScreenMock]
    val boxMock = mock[BlessedElementMock]
    (boxMock.screen _).expects().returning(screenMock.asInstanceOf[BlessedScreen])
    (screenMock.key _).expects(*, *)

    //when
    val result = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock.asInstanceOf[js.Any]
      else null
    }).root

    //then
    inside(result.children.toList) { case List(main, log) =>
      assertNativeComponent(main, <.box(^.rbWidth := "100%")(
        <(withPortalsComp())()(
          <(fileListComp).empty,
          <(fileListPopups).empty,
          <(taskController).empty
        )
      ))

      assertTestComponent(log, logControllerComp) { case LogControllerProps(render) =>
        render("test log content") shouldBe null
      }
    }
  }
  
  it should "render component with LogPanel" in {
    //given
    val root = new FarjsRoot(withPortalsComp, fileListComp, fileListPopups, taskController, DevTool.Logs)
    val screenMock = mock[BlessedScreenMock]
    val boxMock = mock[BlessedElementMock]
    (boxMock.screen _).expects().returning(screenMock.asInstanceOf[BlessedScreen])
    (screenMock.key _).expects(*, *)

    //when
    val result = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock.asInstanceOf[js.Any]
      else null
    }).root

    //then
    inside(result.children.toList) { case List(main, log) =>
      assertNativeComponent(main, <.box(^.rbWidth := "70%")(
        <(withPortalsComp())()(
          <(fileListComp).empty,
          <(fileListPopups).empty,
          <(taskController).empty
        )
      ))

      assertTestComponent(log, logControllerComp) { case LogControllerProps(render) =>
        val content = "test log content"
        
        assertNativeComponent(createTestRenderer(render(content)).root,
          <.box(
            ^.rbWidth := "30%",
            ^.rbHeight := "100%",
            ^.rbLeft := "70%"
          )(), { case List(comp) =>
            assertTestComponent(comp, devToolPanelComp) { case DevToolPanelProps(devTool, logContent, _) =>
              devTool shouldBe DevTool.Logs
              logContent shouldBe content
            }
          }
        )
      }
    }
  }
  
  it should "render component with ColorPanel" in {
    //given
    val root = new FarjsRoot(withPortalsComp, fileListComp, fileListPopups, taskController, DevTool.Colors)
    val screenMock = mock[BlessedScreenMock]
    val boxMock = mock[BlessedElementMock]
    (boxMock.screen _).expects().returning(screenMock.asInstanceOf[BlessedScreen])
    (screenMock.key _).expects(*, *)

    //when
    val result = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock.asInstanceOf[js.Any]
      else null
    }).root

    //then
    inside(result.children.toList) { case List(main, log) =>
      assertNativeComponent(main, <.box(^.rbWidth := "70%")(
        <(withPortalsComp())()(
          <(fileListComp).empty,
          <(fileListPopups).empty,
          <(taskController).empty
        )
      ))

      assertTestComponent(log, logControllerComp) { case LogControllerProps(render) =>
        val content = "test log content"
        
        assertNativeComponent(createTestRenderer(render(content)).root,
          <.box(
            ^.rbWidth := "30%",
            ^.rbHeight := "100%",
            ^.rbLeft := "70%"
          )(), { case List(comp) =>
            assertTestComponent(comp, devToolPanelComp) { case DevToolPanelProps(devTool, logContent, _) =>
              devTool shouldBe DevTool.Colors
              logContent shouldBe content
            }
          }
        )
      }
    }
  }
}

object FarjsRootSpec {

  @JSExportAll
  trait BlessedProgramMock {

    def emit(eventName: String): Unit
  }

  @JSExportAll
  trait BlessedScreenMock {

    def program: BlessedProgram

    def key(keys: js.Array[String], onKey: js.Function2[js.Object, KeyboardKey, Unit]): Unit
  }

  @JSExportAll
  trait BlessedElementMock {

    def screen: BlessedScreen
  }
}
