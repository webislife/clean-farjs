package farjs.app.util

import farjs.app.util.InputController._
import scommons.react.test._

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

class InputControllerSpec extends TestSpec with TestRendererUtils {

  InputController.logPanelComp = mockUiComponent("LogPanel")
  InputController.maxBufferLength = 10

  it should "render component and collect keys input" in {
    //given
    js.typeOf(g.farjsLogKeys) shouldBe "undefined"
    
    //when & then
    val renderer = createTestRenderer(<(InputController())()())
    js.typeOf(g.farjsLogKeys) should not be "undefined"

    //when & then
    g.farjsLogKeys("key 1")
    assertTestComponent(renderer.root.children(0), logPanelComp) {
      case LogPanelProps(content) => content shouldBe "key 1\n"
    }
    
    //when & then
    g.farjsLogKeys("k 2")
    assertTestComponent(renderer.root.children(0), logPanelComp) {
      case LogPanelProps(content) => content shouldBe "key 1\nk 2\n"
    }
    
    //when & then
    g.farjsLogKeys("k 3")
    assertTestComponent(renderer.root.children(0), logPanelComp) {
      case LogPanelProps(content) => content shouldBe "k 2\nk 3\n"
    }
    
    //when & then
    TestRenderer.act { () =>
      renderer.unmount()
    }
    js.typeOf(g.farjsLogKeys) shouldBe "undefined"
  }
}
