package farjs.filelist.popups

import farjs.filelist.FileListServicesSpec.withServicesContext
import farjs.filelist.history.MockFileListHistoryService
import farjs.filelist.popups.MakeFolderPopup._
import farjs.ui._
import farjs.ui.border._
import farjs.ui.popup.ModalProps
import farjs.ui.theme.Theme
import org.scalatest.{Assertion, Succeeded}
import scommons.nodejs.test.AsyncTestSpec
import scommons.react.test._

import scala.concurrent.Future
import scala.scalajs.js

class MakeFolderPopupSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  MakeFolderPopup.modalComp = mockUiComponent("Modal")
  MakeFolderPopup.textLineComp = mockUiComponent("TextLine")
  MakeFolderPopup.comboBoxComp = mockUiComponent("ComboBox")
  MakeFolderPopup.horizontalLineComp = mockUiComponent("HorizontalLine")
  MakeFolderPopup.checkBoxComp = mockUiComponent("CheckBox")
  MakeFolderPopup.buttonsPanelComp = mockUiComponent("ButtonsPanel")

  //noinspection TypeAnnotation
  class HistoryService {
    val getAll = mockFunction[Future[Seq[String]]]

    val service = new MockFileListHistoryService(
      getAllMock = getAll
    )
  }

  it should "set folderName when onChange in ComboBox" in {
    //given
    val folderName = "initial folder name"
    val props = getMakeFolderPopupProps()
    val historyService = new HistoryService
    val itemsF = Future.successful(List("folder", folderName))
    historyService.getAll.expects().returning(itemsF)

    val renderer = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    ))
    itemsF.flatMap { _ =>
      val textBox = findComponentProps(renderer.root, comboBoxComp, plain = true)
      textBox.value shouldBe folderName
      val newFolderName = "new folder name"

      //when
      textBox.onChange(newFolderName)

      //then
      findComponentProps(renderer.root, comboBoxComp, plain = true).value shouldBe newFolderName
    }
  }
  
  it should "set multiple flag when onChange in CheckBox" in {
    //given
    val props = getMakeFolderPopupProps()
    val historyService = new HistoryService
    val itemsF = Future.successful(List("folder", "folder 2"))
    historyService.getAll.expects().returning(itemsF)

    val renderer = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    ))
    itemsF.flatMap { _ =>
      val checkbox = findComponentProps(renderer.root, checkBoxComp, plain = true)
      checkbox.value shouldBe false

      //when
      checkbox.onChange()

      //then
      findComponentProps(renderer.root, checkBoxComp, plain = true).value shouldBe true
    }
  }
  
  it should "call onOk when onEnter in ComboBox" in {
    //given
    val onOk = mockFunction[String, Boolean, Unit]
    val onCancel = mockFunction[Unit]
    val props = getMakeFolderPopupProps(multiple = true, onOk, onCancel)
    val historyService = new HistoryService
    val itemsF = Future.successful(List("folder", "test"))
    historyService.getAll.expects().returning(itemsF)

    val comp = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    )).root
    itemsF.flatMap { _ =>
      val textBox = findComponentProps(comp, comboBoxComp, plain = true)

      //then
      onOk.expects("test", true)
      onCancel.expects().never()

      //when
      textBox.onEnter.get.apply()

      Succeeded
    }
  }
  
  it should "call onOk when press OK button" in {
    //given
    val onOk = mockFunction[String, Boolean, Unit]
    val onCancel = mockFunction[Unit]
    val props = getMakeFolderPopupProps(multiple = true, onOk, onCancel)
    val historyService = new HistoryService
    val itemsF = Future.successful(List("folder", "test"))
    historyService.getAll.expects().returning(itemsF)

    val comp = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    )).root
    itemsF.flatMap { _ =>
      val action = findComponentProps(comp, buttonsPanelComp, plain = true).actions.head

      //then
      onOk.expects("test", true)
      onCancel.expects().never()

      //when
      action.onAction()

      Succeeded
    }
  }
  
  it should "not call onOk if folderName is empty" in {
    //given
    val onOk = mockFunction[String, Boolean, Unit]
    val onCancel = mockFunction[Unit]
    val props = getMakeFolderPopupProps(multiple = true, onOk, onCancel)
    val historyService = new HistoryService
    val itemsF = Future.successful(Nil)
    historyService.getAll.expects().returning(itemsF)

    val comp = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    )).root
    itemsF.flatMap { _ =>
      val action = findComponentProps(comp, buttonsPanelComp, plain = true).actions.head

      //then
      onOk.expects(*, *).never()
      onCancel.expects().never()

      //when
      action.onAction()

      Succeeded
    }
  }
  
  it should "call onCancel when press Cancel button" in {
    //given
    val onOk = mockFunction[String, Boolean, Unit]
    val onCancel = mockFunction[Unit]
    val props = getMakeFolderPopupProps(onOk = onOk, onCancel = onCancel)
    val historyService = new HistoryService
    val itemsF = Future.successful(Nil)
    historyService.getAll.expects().returning(itemsF)

    val comp = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    )).root
    itemsF.flatMap { _ =>
      val action = findComponentProps(comp, buttonsPanelComp, plain = true).actions(1)

      //then
      onOk.expects(*, *).never()
      onCancel.expects()

      //when
      action.onAction()

      Succeeded
    }
  }
  
  it should "render component" in {
    //given
    val props = getMakeFolderPopupProps()
    val historyService = new HistoryService
    val itemsF = Future.successful(List("folder", "folder 2"))
    historyService.getAll.expects().returning(itemsF)

    //when
    val result = createTestRenderer(withServicesContext(
      <(MakeFolderPopup())(^.wrapped := props)(), mkDirsHistory = historyService.service
    )).root

    //then
    result.children.toList should be (empty)
    itemsF.flatMap { items =>
      result.children.toList should not be empty
      assertMakeFolderPopup(result.children(0), props, items, List("[ OK ]", "[ Cancel ]"))
    }
  }

  private def getMakeFolderPopupProps(multiple: Boolean = false,
                                      onOk: (String, Boolean) => Unit = (_, _) => (),
                                      onCancel: () => Unit = () => ()): MakeFolderPopupProps = {
    MakeFolderPopupProps(
      multiple = multiple,
      onOk = onOk,
      onCancel = onCancel
    )
  }

  private def assertMakeFolderPopup(result: TestInstance,
                                    props: MakeFolderPopupProps,
                                    items: List[String],
                                    actions: List[String]): Assertion = {
    val (width, height) = (75, 10)
    val style = Theme.current.popup.regular
    
    assertNativeComponent(result,
      <(modalComp())(^.assertWrapped(inside(_) {
        case ModalProps(title, size, resStyle, onCancel) =>
          title shouldBe "Make Folder"
          size shouldBe width -> height
          resStyle shouldBe style
          onCancel should be theSameInstanceAs props.onCancel
      }))(
        <(textLineComp())(^.assertPlain[TextLineProps](inside(_) {
          case TextLineProps(align, left, top, resWidth, text, resStyle, focused, padding) =>
            align shouldBe TextAlign.left
            left shouldBe 2
            top shouldBe 1
            resWidth shouldBe (width - 10)
            text shouldBe "Create the folder"
            resStyle shouldBe style
            focused shouldBe js.undefined
            padding shouldBe 0
        }))(),
        <(comboBoxComp())(^.assertPlain[ComboBoxProps](inside(_) {
          case ComboBoxProps(left, top, resWidth, resItems, resValue, _, _) =>
            left shouldBe 2
            top shouldBe 2
            resItems.toList shouldBe items.reverse
            resWidth shouldBe (width - 10)
            resValue shouldBe items.lastOption.getOrElse("")
        }))(),
        
        <(horizontalLineComp())(^.assertPlain[HorizontalLineProps](inside(_) {
          case HorizontalLineProps(resLeft, resTop, resLength, lineCh, resStyle, startCh, endCh) =>
            resLeft shouldBe 0
            resTop shouldBe 3
            resLength shouldBe (width - 6)
            lineCh shouldBe SingleChars.horizontal
            resStyle shouldBe style
            startCh shouldBe DoubleChars.leftSingle
            endCh shouldBe DoubleChars.rightSingle
        }))(),
        <(checkBoxComp())(^.assertPlain[CheckBoxProps](inside(_) {
          case CheckBoxProps(left, top, resValue, resLabel, resStyle, _) =>
            left shouldBe 2
            top shouldBe 4
            resValue shouldBe false
            resLabel shouldBe "Process multiple names"
            resStyle shouldBe style
        }))(),

        <(horizontalLineComp())(^.assertPlain[HorizontalLineProps](inside(_) {
          case HorizontalLineProps(resLeft, resTop, resLength, lineCh, resStyle, startCh, endCh) =>
            resLeft shouldBe 0
            resTop shouldBe 5
            resLength shouldBe (width - 6)
            lineCh shouldBe SingleChars.horizontal
            resStyle shouldBe style
            startCh shouldBe DoubleChars.leftSingle
            endCh shouldBe DoubleChars.rightSingle
        }))(),
        <(buttonsPanelComp())(^.assertPlain[ButtonsPanelProps](inside(_) {
          case ButtonsPanelProps(top, resActions, resStyle, padding, margin) =>
            top shouldBe 6
            resActions.map(_.label).toList shouldBe actions
            resStyle shouldBe style
            padding shouldBe js.undefined
            margin shouldBe 2
        }))()
      )
    )
  }
}
