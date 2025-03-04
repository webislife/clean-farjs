package farjs.app

import farjs.app.FarjsRoot._
import farjs.app.util._
import org.scalatest.Succeeded
import scommons.nodejs.test.AsyncTestSpec
import scommons.react.blessed._
import scommons.react.test._

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal

class FarjsRootSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  private val withPortalsComp = mockUiComponent[Unit]("WithPortals")
  private val fileListComp = mockUiComponent[Unit]("FileListBrowser").apply()
  private val taskController = mockUiComponent[Unit]("TaskController").apply()
  
  FarjsRoot.logControllerComp = mockUiComponent("LogController")
  FarjsRoot.devToolPanelComp = mockUiComponent("DevToolPanel")

  it should "set devTool and emit resize event when on F12" in {
    //given
    val fileListUiF = Future.successful(fileListComp)
    val root = new FarjsRoot(withPortalsComp, fileListUiF, taskController, DevTool.Hidden)
    val emitMock = mockFunction[String, Unit]
    val program = literal("emit" -> emitMock)
    val onMock = mockFunction[String, js.Function2[js.Object, KeyboardKey, Unit], Unit]
    val offMock = mockFunction[String, js.Function2[js.Object, KeyboardKey, Unit], Unit]
    val screen = literal("program" -> program, "on" -> onMock, "off" -> offMock)
    val boxMock = literal("screen" -> screen)
    var keyListener: js.Function2[js.Object, KeyboardKey, Unit] = null

    onMock.expects("keypress", *).onCall { (_, listener) =>
      keyListener = listener
    }
    var emitCalled = false
    emitMock.expects("resize").onCall { _: String =>
      emitCalled = true
    }
    
    val renderer = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock
      else null
    })
    findComponents(renderer.root.children(0), <.box.name).head.props.width shouldBe "100%"
    
    //when
    TestRenderer.act { () =>
      keyListener(null, literal(full = "f12").asInstanceOf[KeyboardKey])
    }

    //then
    findComponents(renderer.root.children(0), <.box.name).head.props.width shouldBe "70%"
    
    eventually {
      emitCalled shouldBe true
    }.map { _ =>
      //then
      offMock.expects("keypress", keyListener)

      //when
      TestRenderer.act { () =>
        renderer.unmount()
      }
      Succeeded
    }
  }

  it should "set devTool when onActivate" in {
    //given
    val fileListUiF = Future.successful(fileListComp)
    val root = new FarjsRoot(withPortalsComp, fileListUiF, taskController, DevTool.Colors)
    val onMock = mockFunction[String, js.Function2[js.Object, KeyboardKey, Unit], Unit]
    val screen = literal("on" -> onMock)
    val boxMock = literal("screen" -> screen)
    onMock.expects("keypress", *)

    val renderer = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock
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

  it should "render fileListUi when onReady" in {
    //given
    val fileListUiF = Future.successful(fileListComp)
    val root = new FarjsRoot(withPortalsComp, fileListUiF, taskController, DevTool.Hidden)
    val onMock = mockFunction[String, js.Function2[js.Object, KeyboardKey, Unit], Unit]
    val screen = literal("on" -> onMock)
    val boxMock = literal("screen" -> screen)
    onMock.expects("keypress", *)

    val renderer = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock
      else null
    })

    assertComponents(renderer.root.children, List(
      <.box(^.rbWidth := "100%")(
        <(withPortalsComp())()(
          <.text()("Loading..."),
          <(taskController).empty
        )
      ),
      <(logControllerComp())(^.assertWrapped(inside(_) {
        case LogControllerProps(onReady, render) =>
          render("test log content") shouldBe null

          //when
          onReady()
      }))()
    ))

    //then
    fileListUiF.map { _ =>
      assertComponents(renderer.root.children, List(
        <.box(^.rbWidth := "100%")(
          <(withPortalsComp())()(
            <(fileListComp).empty,
            <(taskController).empty
          )
        ),
        <(logControllerComp()).empty
      ))
    }
  }
  
  it should "render component without DevTools" in {
    //given
    val fileListUiF = Future.successful(fileListComp)
    val root = new FarjsRoot(withPortalsComp, fileListUiF, taskController, DevTool.Hidden)
    val onMock = mockFunction[String, js.Function2[js.Object, KeyboardKey, Unit], Unit]
    val screen = literal("on" -> onMock)
    val boxMock = literal("screen" -> screen)
    onMock.expects("keypress", *)

    //when
    val result = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock
      else null
    }).root

    //then
    assertComponents(result.children, List(
      <.box(^.rbWidth := "100%")(
        <(withPortalsComp())()(
          <.text()("Loading..."),
          <(taskController).empty
        )
      ),
      <(logControllerComp())(^.assertWrapped(inside(_) {
        case LogControllerProps(_, render) =>
          render("test log content") shouldBe null
      }))()
    ))
  }
  
  it should "render component with DevTools" in {
    //given
    val fileListUiF = Future.successful(fileListComp)
    val root = new FarjsRoot(withPortalsComp, fileListUiF, taskController, DevTool.Logs)
    val onMock = mockFunction[String, js.Function2[js.Object, KeyboardKey, Unit], Unit]
    val screen = literal("on" -> onMock)
    val boxMock = literal("screen" -> screen)
    onMock.expects("keypress", *)

    //when
    val result = createTestRenderer(<(root())()(), { el =>
      if (el.`type` == <.box.name.asInstanceOf[js.Any]) boxMock
      else null
    }).root

    //then
    assertComponents(result.children, List(
      <.box(^.rbWidth := "70%")(
        <(withPortalsComp())()(
          <.text()("Loading..."),
          <(taskController).empty
        )
      ),
      <(logControllerComp())(^.assertWrapped(inside(_) {
        case LogControllerProps(_, render) =>
          val content = "test log content"

          assertNativeComponent(createTestRenderer(render(content)).root,
            <.box(
              ^.rbWidth := "30%",
              ^.rbHeight := "100%",
              ^.rbLeft := "70%"
            )(), inside(_) { case List(comp) =>
              assertTestComponent(comp, devToolPanelComp) {
                case DevToolPanelProps(devTool, logContent, _) =>
                  devTool shouldBe DevTool.Logs
                  logContent shouldBe content
              }
            }
          )
      }))()
    ))
  }
}
