package farjs.archiver

import farjs.archiver.zip.ZipApi
import farjs.filelist.api.{FileListDir, FileListItem}
import farjs.filelist.stack._
import farjs.filelist.{FileListState, MockFileListActions}
import scommons.nodejs.test.TestSpec
import scommons.react.ReactClass

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

class ArchiverPluginSpec extends TestSpec {

  ArchiverPlugin.readZip = _ => Future.successful(Map.empty)
  ArchiverPlugin.createApi = ZipApi.apply

  it should "define triggerKeys" in {
    //when & then
    ArchiverPlugin.triggerKeys.toList shouldBe List("S-f7")
  }

  it should "return None if .. when onKeyTrigger" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = new MockFileListActions
    val leftState = FileListState(currDir = FileListDir("/sub-dir", isRoot = false, items = List(
      FileListItem.up,
      FileListItem("item 1")
    )))
    val leftStack = new PanelStack(isActive = true, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], Some(dispatch), Some(actions), Some(leftState))
    ), updater = null)

    val rightStack = new PanelStack(isActive = false, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], None, None, None)
    ), updater = null)
    val stacks = WithPanelStacksProps(leftStack, null, rightStack, null)

    //when & then
    ArchiverPlugin.onKeyTrigger("", stacks) shouldBe None
  }

  it should "return None if non-local fs when onKeyTrigger" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = new MockFileListActions(isLocalFSMock = false)
    val leftState = FileListState(currDir = FileListDir("/sub-dir", isRoot = false, items = List(
      FileListItem("item 1")
    )))
    val leftStack = new PanelStack(isActive = true, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], Some(dispatch), Some(actions), Some(leftState))
    ), updater = null)

    val rightStack = new PanelStack(isActive = false, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], None, None, None)
    ), updater = null)
    val stacks = WithPanelStacksProps(leftStack, null, rightStack, null)

    //when & then
    ArchiverPlugin.onKeyTrigger("", stacks) shouldBe None
  }

  it should "return Some(ui) if not .. when onKeyTrigger" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = new MockFileListActions
    val leftState = FileListState(currDir = FileListDir("/sub-dir", isRoot = false, items = List(
      FileListItem("item 1")
    )))
    val leftStack = new PanelStack(isActive = true, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], Some(dispatch), Some(actions), Some(leftState))
    ), updater = null)

    val rightStack = new PanelStack(isActive = false, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], None, None, None)
    ), updater = null)
    val stacks = WithPanelStacksProps(leftStack, null, rightStack, null)

    //when & then
    ArchiverPlugin.onKeyTrigger("", stacks) should not be None
  }

  it should "return Some(ui) if selected items when onKeyTrigger" in {
    //given
    val dispatch = mockFunction[Any, Any]
    val actions = new MockFileListActions
    val leftState = FileListState(currDir = FileListDir("/sub-dir", isRoot = false, items = List(
      FileListItem.up,
      FileListItem("item 1")
    )), selectedNames = Set("item 1"))
    val leftStack = new PanelStack(isActive = true, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], Some(dispatch), Some(actions), Some(leftState))
    ), updater = null)

    val rightStack = new PanelStack(isActive = false, List(
      PanelStackItem("fsComp".asInstanceOf[ReactClass], None, None, None)
    ), updater = null)
    val stacks = WithPanelStacksProps(leftStack, null, rightStack, null)

    //when & then
    ArchiverPlugin.onKeyTrigger("", stacks) should not be None
  }

  it should "trigger plugin on .zip and .jar file extensions" in {
    //given
    val header = new Uint8Array(5)
    
    //when & then
    ArchiverPlugin.onFileTrigger("filePath.txt", header, () => ()) shouldBe None
    ArchiverPlugin.onFileTrigger("filePath.zip", header, () => ()) should not be None
    ArchiverPlugin.onFileTrigger("filePath.ZIP", header, () => ()) should not be None
    ArchiverPlugin.onFileTrigger("filePath.jar", header, () => ()) should not be None
    ArchiverPlugin.onFileTrigger("filePath.Jar", header, () => ()) should not be None
  }

  it should "trigger plugin on PK34 file header" in {
    //given
    val header = new Uint8Array(js.Array[Short]('P', 'K', 0x03, 0x04, 0x01))
    
    //when & then
    ArchiverPlugin.onFileTrigger("filePath.txt", new Uint8Array(2), () => ()) shouldBe None
    ArchiverPlugin.onFileTrigger("filePath.txt", header, () => ()) should not be None
  }
}
