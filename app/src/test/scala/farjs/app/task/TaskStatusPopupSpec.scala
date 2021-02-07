package farjs.app.task

import farjs.app.task.TaskStatusPopup._
import farjs.ui._
import farjs.ui.border._
import farjs.ui.popup.PopupProps
import farjs.ui.theme.Theme
import org.scalatest._
import scommons.nodejs.test.TestSpec
import scommons.react._
import scommons.react.blessed._
import scommons.react.test._

class TaskStatusPopupSpec extends TestSpec with BaseTestSpec with TestRendererUtils {

  TaskStatusPopup.popupComp = () => "Popup".asInstanceOf[ReactClass]
  TaskStatusPopup.doubleBorderComp = () => "DoubleBorder".asInstanceOf[ReactClass]
  TaskStatusPopup.textLineComp = () => "TextLine".asInstanceOf[ReactClass]

  private val theme = Theme.current.popup.regular
  private val style = new BlessedStyle {
    override val bold = theme.bold
    override val bg = theme.bg
    override val fg = theme.fg
  }
  
  it should "render component" in {
    //given
    val props = TaskStatusPopupProps(
      "Toooooooooooooooooooooooooooooo looooooooooooooooooooooooong test message"
    )

    //when
    val result = testRender(<(TaskStatusPopup())(^.wrapped := props)())

    //then
    assertTaskStatusPopup(result, props)
  }

  private def assertTaskStatusPopup(result: TestInstance,
                                    props: TaskStatusPopupProps): Unit = {
    
    val width = 35
    val textWidth = width - 8
    val textLines = UI.splitText(props.text, textWidth - 2) //exclude padding
    val height = 4 + textLines.size

    assertTestComponent(result, popupComp)({ case PopupProps(onClose, resClosable, focusable, _) =>
      onClose()
      resClosable shouldBe false
      focusable shouldBe true
    }, { case List(box) =>
      assertNativeComponent(box,
        <.box(
          ^.rbClickable := true,
          ^.rbAutoFocus := false,
          ^.rbWidth := width,
          ^.rbHeight := height,
          ^.rbTop := "center",
          ^.rbLeft := "center",
          ^.rbShadow := true,
          ^.rbStyle := style
        )(), { case List(border, btn) =>
          assertTestComponent(border, doubleBorderComp) {
            case DoubleBorderProps(resSize, resStyle, pos, title) =>
              resSize shouldBe (width - 6) -> (height - 2)
              assertObject(resStyle, style)
              pos shouldBe 3 -> 1
              title shouldBe Some("Status")
          }
          
          assertNativeComponent(btn,
            <.button(
              ^.rbWidth := textWidth,
              ^.rbHeight := textLines.size,
              ^.rbLeft := 4,
              ^.rbTop := 2,
              ^.rbStyle := style
            )(), { msgs =>
              msgs.size shouldBe textLines.size
              msgs.zip(textLines).zipWithIndex.foreach { case ((msg, textLine), index) =>
                assertTestComponent(msg, textLineComp) {
                  case TextLineProps(align, pos, resWidth, text, resStyle, focused, padding) =>
                    align shouldBe TextLine.Center
                    pos shouldBe 0 -> index
                    resWidth shouldBe textWidth
                    text shouldBe textLine
                    assertObject(resStyle, style)
                    focused shouldBe false
                    padding shouldBe 1
                }
              }
              Succeeded
            }
          )
        }
      )
    })
  }
}
