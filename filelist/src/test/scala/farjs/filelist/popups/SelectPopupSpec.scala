package farjs.filelist.popups

import farjs.filelist.FileListServicesSpec.withServicesContext
import farjs.filelist.history.MockFileListHistoryService
import farjs.filelist.popups.FileListPopupsActions._
import farjs.filelist.popups.SelectPopup._
import farjs.ui._
import farjs.ui.popup.ModalProps
import farjs.ui.theme.Theme
import org.scalatest.{Assertion, Succeeded}
import scommons.nodejs.test.AsyncTestSpec
import scommons.react.test._

import scala.concurrent.Future

class SelectPopupSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  SelectPopup.modalComp = mockUiComponent("Modal")
  SelectPopup.comboBoxComp = mockUiComponent("ComboBox")

  //noinspection TypeAnnotation
  class HistoryService {
    val getAll = mockFunction[Future[Seq[String]]]

    val service = new MockFileListHistoryService(
      getAllMock = getAll
    )
  }

  it should "set pattern when onChange in TextBox" in {
    //given
    val pattern = "initial pattern"
    val props = getSelectPopupProps(ShowSelect)
    val historyService = new HistoryService
    val itemsF = Future.successful(List("pattern", pattern))
    historyService.getAll.expects().returning(itemsF)

    val renderer = createTestRenderer(withServicesContext(
      <(SelectPopup())(^.wrapped := props)(), selectPatternsHistory = historyService.service
    ))
    itemsF.flatMap { _ =>
      val comboBox = findComponentProps(renderer.root, comboBoxComp, plain = true)
      comboBox.value shouldBe pattern
      val newPattern = "new pattern"

      //when
      comboBox.onChange(newPattern)

      //then
      findComponentProps(renderer.root, comboBoxComp, plain = true).value shouldBe newPattern
    }
  }
  
  it should "call onAction when onEnter in TextBox" in {
    //given
    val onAction = mockFunction[String, Unit]
    val onCancel = mockFunction[Unit]
    val props = getSelectPopupProps(ShowSelect, onAction, onCancel)
    val historyService = new HistoryService
    val itemsF = Future.successful(List("pattern", "test"))
    historyService.getAll.expects().returning(itemsF)

    val comp = createTestRenderer(withServicesContext(
      <(SelectPopup())(^.wrapped := props)(), selectPatternsHistory = historyService.service
    )).root

    itemsF.flatMap { _ =>
      val textBox = findComponentProps(comp, comboBoxComp, plain = true)

      //then
      onAction.expects("test")

      //when
      textBox.onEnter.get.apply()

      Succeeded
    }
  }
  
  it should "not call onAction if pattern is empty" in {
    //given
    val onAction = mockFunction[String, Unit]
    val onCancel = mockFunction[Unit]
    val props = getSelectPopupProps(ShowSelect, onAction, onCancel)
    val historyService = new HistoryService
    val itemsF = Future.successful(Nil)
    historyService.getAll.expects().returning(itemsF)

    val comp = createTestRenderer(withServicesContext(
      <(SelectPopup())(^.wrapped := props)(), selectPatternsHistory = historyService.service
    )).root

    itemsF.flatMap { _ =>
      val textBox = findComponentProps(comp, comboBoxComp, plain = true)

      //then
      onAction.expects(*).never()

      //when
      textBox.onEnter.get.apply()

      Succeeded
    }
  }
  
  it should "render Select component" in {
    //given
    val props = getSelectPopupProps(ShowSelect)
    val historyService = new HistoryService
    val itemsF = Future.successful(List("pattern", "pattern 2"))
    historyService.getAll.expects().returning(itemsF)

    //when
    val result = createTestRenderer(withServicesContext(
      <(SelectPopup())(^.wrapped := props)(), selectPatternsHistory = historyService.service
    )).root

    //then
    itemsF.flatMap { items =>
      assertSelectPopup(result.children(0), props, items, "Select")
    }
  }

  it should "render Deselect component" in {
    //given
    val props = getSelectPopupProps(ShowDeselect)
    val historyService = new HistoryService
    val itemsF = Future.successful(List("pattern", "pattern 2"))
    historyService.getAll.expects().returning(itemsF)

    //when
    val result = createTestRenderer(withServicesContext(
      <(SelectPopup())(^.wrapped := props)(), selectPatternsHistory = historyService.service
    )).root

    //then
    itemsF.flatMap { items =>
      assertSelectPopup(result.children(0), props, items, "Deselect")
    }
  }
  
  private def getSelectPopupProps(action: FileListPopupSelect,
                                  onAction: String => Unit = _ => (),
                                  onCancel: () => Unit = () => ()): SelectPopupProps = {
    SelectPopupProps(
      action = action,
      onAction = onAction,
      onCancel = onCancel
    )
  }

  private def assertSelectPopup(result: TestInstance,
                                props: SelectPopupProps,
                                items: List[String],
                                expectedTitle: String): Assertion = {

    val (width, height) = (55, 5)
    val style = Theme.current.popup.regular
    
    assertNativeComponent(result,
      <(modalComp())(^.assertWrapped(inside(_) {
        case ModalProps(title, size, resStyle, onCancel) =>
          title shouldBe expectedTitle
          size shouldBe width -> height
          resStyle shouldBe style
          onCancel should be theSameInstanceAs props.onCancel
      }))(
        <(comboBoxComp())(^.assertPlain[ComboBoxProps](inside(_) {
          case ComboBoxProps(left, top, resWidth, resItems, resValue, _, _) =>
            left shouldBe 2
            top shouldBe 1
            resWidth shouldBe (width - 10)
            resItems.toList shouldBe items.reverse
            resValue shouldBe items.lastOption.getOrElse("")
        }))()
      )
    )
  }
}
